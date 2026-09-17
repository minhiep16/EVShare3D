import { create } from 'zustand';
import type {
  BookingStep,
  BookingVehicleSummary,
  TimelineHourSlot,
  CalendarDayCell,
  FairUsageScoreModel,
  BookingSubmissionResult,
} from './bookingTypes';
import {
  bookingsApi,
  type BookingTimelineSlotDTO,
  type BookingResponseDTO,
  type BookingHistoryResponseDTO,
  type BookingCancellationResponseDTO,
  type BookingStatus,
} from '@/api/bookingsApi';
import { analyticsApi } from '@/api/analyticsApi';
import { useGarageStore } from '@/garage/useGarageStore';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const SEEDED_BOOKING_VEHICLES: BookingVehicleSummary[] = [
  {
    id: 1,
    modelName: 'Tesla Model S Plaid',
    manufacturer: 'Tesla',
    licensePlate: '29A-888.88',
    batteryLevel: 92,
    status: 'AVAILABLE',
    hourlyRateVnd: 150000,
    bodyColor: '#e11d48',
    stallLocationCode: 'BAY-01',
    groupId: 1,
    groupName: 'Apex Syndicate',
    estimatedRangeKm: 420,
  },
  {
    id: 2,
    modelName: 'Porsche Taycan 4S',
    manufacturer: 'Porsche',
    licensePlate: '30F-999.99',
    batteryLevel: 85,
    status: 'AVAILABLE',
    hourlyRateVnd: 180000,
    bodyColor: '#0ea5e9',
    stallLocationCode: 'BAY-02',
    groupId: 1,
    groupName: 'Apex Syndicate',
    estimatedRangeKm: 390,
  },
  {
    id: 3,
    modelName: 'VinFast VF9 Plus',
    manufacturer: 'VinFast',
    licensePlate: '51K-777.77',
    batteryLevel: 78,
    status: 'AVAILABLE',
    hourlyRateVnd: 130000,
    bodyColor: '#10b981',
    stallLocationCode: 'BAY-03',
    groupId: 1,
    groupName: 'Apex Syndicate',
    estimatedRangeKm: 410,
  },
];

function getIsoDate(daysFromNow = 1): string {
  const d = new Date();
  d.setDate(d.getDate() + daysFromNow);
  return d.toISOString().split('T')[0];
}

export interface BookingStoreState {
  // Navigation & Step
  activeStep: BookingStep;
  cameraPreset: 'CHAMBER_OVERVIEW' | 'TERMINAL_FOCUS' | 'CALENDAR_FOCUS' | 'VEHICLE_INSPECTION';
  isTerminalFocused: boolean;

  // Vehicles & Selection
  vehicles: BookingVehicleSummary[];
  selectedVehicleId: number;

  // Calendar
  selectedDate: string; // YYYY-MM-DD
  calendarDays: CalendarDayCell[];
  weekOffset: number;

  // Timeline & Time Selection
  timelineSlots: BookingTimelineSlotDTO[];
  timelineHours: TimelineHourSlot[];
  startHour: number;
  endHour: number;
  durationHours: number;
  estimatedCostVnd: number;

  // Availability Verification & Conflicts
  isCheckingAvailability: boolean;
  isSlotAvailable: boolean;
  conflictReason: string | null;

  // Fair Usage
  fairUsage: FairUsageScoreModel | null;

  // Submission & Result
  isSubmitting: boolean;
  submissionError: string | null;
  bookingResult: BookingSubmissionResult | null;

  // History & Reservation Management (09-T)
  userBookings: BookingResponseDTO[];
  selectedBooking: BookingResponseDTO | null;
  activeBookingHistory: BookingHistoryResponseDTO[];
  isLoadingHistory: boolean;
  isCancelling: boolean;
  cancelResult: BookingCancellationResponseDTO | null;
  isUpdating: boolean;
  updateError: string | null;

  // Actions
  setActiveStep: (step: BookingStep) => void;
  setCameraPreset: (preset: 'CHAMBER_OVERVIEW' | 'TERMINAL_FOCUS' | 'CALENDAR_FOCUS' | 'VEHICLE_INSPECTION') => void;
  setIsTerminalFocused: (focused: boolean) => void;
  selectVehicle: (vehicleId: number) => void;
  selectDate: (dateStr: string) => void;
  setWeekOffset: (offset: number) => void;
  setTimeRange: (startHour: number, endHour: number) => void;
  setDurationPreset: (durationHours: number) => void;
  selectBookingForInspection: (booking: BookingResponseDTO | null) => void;

  // Async API actions
  fetchTimeline: () => Promise<void>;
  checkAvailability: () => Promise<boolean>;
  fetchFairUsage: () => Promise<void>;
  submitBooking: () => Promise<boolean>;
  updateBookingSchedule: (bookingId: number, startTime: string, endTime: string) => Promise<boolean>;
  cancelBooking: (bookingId: number, reason?: string) => Promise<boolean>;
  fetchMyBookings: (status?: BookingStatus) => Promise<void>;
  fetchBookingHistory: (bookingId: number) => Promise<void>;
  resetBooking: () => void;
}

export const useBookingStore = create<BookingStoreState>((set, get) => {
  const initialDate = getIsoDate(1);

  // Helper to generate 14-day calendar cells
  const generateCalendarDays = (weekOffset = 0): CalendarDayCell[] => {
    const days: CalendarDayCell[] = [];
    const baseDate = new Date();
    baseDate.setHours(0, 0, 0, 0);

    const startDayIndex = weekOffset * 7;
    for (let i = 0; i < 14; i++) {
      const target = new Date(baseDate);
      target.setDate(baseDate.getDate() + startDayIndex + i);

      const dateStr = target.toISOString().split('T')[0];
      const dayNames = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
      const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

      // Simulated occupancy pattern for initial view
      const dayNum = target.getDate();
      const isPartiallyBooked = dayNum % 3 === 0;
      const isFullyBooked = dayNum % 7 === 0;

      days.push({
        dateStr,
        dayOfWeek: dayNames[target.getDay()],
        dayNumber: target.getDate(),
        monthName: monthNames[target.getMonth()],
        isToday: i === 0 && weekOffset === 0,
        isPast: target < baseDate,
        occupancyStatus: isFullyBooked
          ? 'FULLY_BOOKED'
          : isPartiallyBooked
          ? 'PARTIALLY_BOOKED'
          : 'AVAILABLE',
        activeBookingsCount: isFullyBooked ? 4 : isPartiallyBooked ? 1 : 0,
      });
    }
    return days;
  };

  // Helper to calculate 24 timeline hours
  const calculateTimelineHours = (
    slots: BookingTimelineSlotDTO[],
    dateStr: string
  ): TimelineHourSlot[] => {
    const hours: TimelineHourSlot[] = [];

    for (let h = 0; h < 24; h++) {
      const hourStart = new Date(`${dateStr}T${h.toString().padStart(2, '0')}:00:00Z`).getTime();
      const hourEnd = new Date(`${dateStr}T${(h + 1).toString().padStart(2, '0')}:00:00Z`).getTime();

      let isBooked = false;
      let isBuffer = false;
      let isMyBooking = false;
      let bookingId: number | undefined;

      for (const slot of slots) {
        const slotStart = new Date(slot.startTime).getTime();
        const slotEnd = new Date(slot.endTime).getTime();
        const bufEnd = slot.bufferedEndTime
          ? new Date(slot.bufferedEndTime).getTime()
          : slotEnd + 30 * 60 * 1000;

        if (slotStart < hourEnd && slotEnd > hourStart) {
          isBooked = true;
          isMyBooking = slot.isMyBooking;
          bookingId = slot.bookingId;
          break;
        } else if (slotEnd <= hourStart && bufEnd > hourStart) {
          isBuffer = true;
          bookingId = slot.bookingId;
          break;
        }
      }

      hours.push({
        hour: h,
        label: `${h.toString().padStart(2, '0')}:00`,
        isAvailable: !isBooked && !isBuffer,
        isBooked,
        isBuffer,
        isMyBooking,
        bookingId,
      });
    }

    return hours;
  };

  return {
    activeStep: 'CALENDAR',
    cameraPreset: 'TERMINAL_FOCUS',
    isTerminalFocused: true,

    vehicles: SEEDED_BOOKING_VEHICLES,
    selectedVehicleId: 1,

    selectedDate: initialDate,
    calendarDays: generateCalendarDays(0),
    weekOffset: 0,

    timelineSlots: [],
    timelineHours: calculateTimelineHours([], initialDate),
    startHour: 9,
    endHour: 13,
    durationHours: 4,
    estimatedCostVnd: 4 * 150000,

    isCheckingAvailability: false,
    isSlotAvailable: true,
    conflictReason: null,

    fairUsage: {
      score: 96,
      fairnessRatio: 1.05,
      priorityTier: 'BALANCED',
      recommendation: 'Good standing. Priority booking quota available.',
      equityPercentage: 40.0,
    },

    isSubmitting: false,
    submissionError: null,
    bookingResult: null,

    // History & Reservation Management (09-T)
    userBookings: [],
    selectedBooking: null,
    activeBookingHistory: [],
    isLoadingHistory: false,
    isCancelling: false,
    cancelResult: null,
    isUpdating: false,
    updateError: null,

    setActiveStep: (step) => set({ activeStep: step }),

    setCameraPreset: (preset) => set({ cameraPreset: preset }),

    setIsTerminalFocused: (focused) => set({ isTerminalFocused: focused }),

    selectBookingForInspection: (booking) => {
      set({ selectedBooking: booking });
      if (booking) {
        get().fetchBookingHistory(booking.id);
      }
    },

    selectVehicle: (vehicleId) => {
      const v = get().vehicles.find((item) => item.id === vehicleId);
      const rate = v ? v.hourlyRateVnd : 150000;
      set({
        selectedVehicleId: vehicleId,
        estimatedCostVnd: get().durationHours * rate,
      });
      get().fetchTimeline();
      get().checkAvailability();
    },

    selectDate: (dateStr) => {
      set({ selectedDate: dateStr });
      get().fetchTimeline();
      get().checkAvailability();
    },

    setWeekOffset: (offset) => {
      const newOffset = Math.max(0, Math.min(3, offset));
      set({
        weekOffset: newOffset,
        calendarDays: generateCalendarDays(newOffset),
      });
    },

    setTimeRange: (startHour, endHour) => {
      const clampedStart = Math.max(0, Math.min(22, startHour));
      const clampedEnd = Math.max(clampedStart + 1, Math.min(24, endHour));
      const duration = clampedEnd - clampedStart;

      const v = get().vehicles.find((item) => item.id === get().selectedVehicleId);
      const rate = v ? v.hourlyRateVnd : 150000;

      set({
        startHour: clampedStart,
        endHour: clampedEnd,
        durationHours: duration,
        estimatedCostVnd: duration * rate,
      });

      get().checkAvailability();
    },

    setDurationPreset: (durationHours) => {
      const start = get().startHour;
      let end = start + durationHours;
      let newStart = start;

      if (end > 24) {
        end = 24;
        newStart = Math.max(0, 24 - durationHours);
      }

      const v = get().vehicles.find((item) => item.id === get().selectedVehicleId);
      const rate = v ? v.hourlyRateVnd : 150000;

      set({
        startHour: newStart,
        endHour: end,
        durationHours: end - newStart,
        estimatedCostVnd: (end - newStart) * rate,
      });

      get().checkAvailability();
    },

    fetchTimeline: async () => {
      const { selectedVehicleId, selectedDate } = get();
      const from = `${selectedDate}T00:00:00Z`;
      const to = `${selectedDate}T23:59:59Z`;

      try {
        const slots = await bookingsApi.getTimeline(selectedVehicleId, from, to);
        set({
          timelineSlots: slots,
          timelineHours: calculateTimelineHours(slots, selectedDate),
        });
      } catch {
        // Graceful fallback with simulated slot for robust offline operation
        const simulatedSlots: BookingTimelineSlotDTO[] = [
          {
            bookingId: 101,
            userId: 2,
            startTime: `${selectedDate}T14:00:00Z`,
            endTime: `${selectedDate}T17:00:00Z`,
            bufferedEndTime: `${selectedDate}T17:30:00Z`,
            status: 'CONFIRMED',
            isMyBooking: false,
          },
        ];
        set({
          timelineSlots: simulatedSlots,
          timelineHours: calculateTimelineHours(simulatedSlots, selectedDate),
        });
      }
    },

    checkAvailability: async () => {
      const { selectedVehicleId, selectedDate, startHour, endHour, timelineSlots } = get();
      const startTime = `${selectedDate}T${startHour.toString().padStart(2, '0')}:00:00Z`;
      const endTime = `${selectedDate}T${endHour.toString().padStart(2, '0')}:00:00Z`;

      set({ isCheckingAvailability: true, conflictReason: null });

      try {
        const resp = await bookingsApi.checkAvailability(selectedVehicleId, startTime, endTime);
        set({
          isCheckingAvailability: false,
          isSlotAvailable: resp.isAvailable,
          conflictReason: resp.isAvailable ? null : resp.reason,
        });
        return resp.isAvailable;
      } catch {
        // Fallback local check adhering strictly to BR-BKG-02 (30-min turnaround buffer)
        const reqStart = new Date(startTime).getTime();
        const reqEnd = new Date(endTime).getTime();
        const bufferMs = 30 * 60 * 1000;

        let isConflict = false;
        let reason: string | null = null;

        for (const slot of timelineSlots) {
          const slotStart = new Date(slot.startTime).getTime();
          const slotEnd = new Date(slot.endTime).getTime();
          const slotBufferEnd = slot.bufferedEndTime
            ? new Date(slot.bufferedEndTime).getTime()
            : slotEnd + bufferMs;

          // Check if overlap exists taking turnaround buffer into account
          if (slotStart < reqEnd + bufferMs && slotBufferEnd > reqStart) {
            isConflict = true;
            reason = `Schedule conflict: Overlaps with booking #${slot.bookingId} (${slot.startTime.substring(11, 16)} - ${slot.endTime.substring(11, 16)} + 30m buffer)`;
            break;
          }
        }

        set({
          isCheckingAvailability: false,
          isSlotAvailable: !isConflict,
          conflictReason: reason,
        });
        return !isConflict;
      }
    },

    fetchFairUsage: async () => {
      const { selectedVehicleId, vehicles } = get();
      const vehicle = vehicles.find((v) => v.id === selectedVehicleId);
      const groupId = vehicle ? vehicle.groupId : 1;

      try {
        const data = await analyticsApi.getMyFairUsageScore(groupId);
        set({
          fairUsage: {
            score: data.bookingPriorityScore,
            fairnessRatio: data.fairnessRatio,
            priorityTier: data.imbalanceLevel,
            recommendation: data.recommendationMessage,
            equityPercentage: data.equityPercentage,
          },
        });
      } catch {
        // Default simulated standing
        set({
          fairUsage: {
            score: 96,
            fairnessRatio: 1.05,
            priorityTier: 'BALANCED',
            recommendation: 'Syndicate quota optimal. Priority turnaround applies.',
            equityPercentage: 40.0,
          },
        });
      }
    },

    submitBooking: async () => {
      const {
        selectedVehicleId,
        selectedDate,
        startHour,
        endHour,
        durationHours,
        estimatedCostVnd,
        vehicles,
      } = get();

      const startTime = `${selectedDate}T${startHour.toString().padStart(2, '0')}:00:00Z`;
      const endTime = `${selectedDate}T${endHour.toString().padStart(2, '0')}:00:00Z`;
      const vehicle = vehicles.find((v) => v.id === selectedVehicleId);

      set({ isSubmitting: true, submissionError: null });

      try {
        const response = await bookingsApi.createBooking({
          vehicleId: selectedVehicleId,
          startTime,
          endTime,
        });

        const result: BookingSubmissionResult = {
          bookingId: response.id,
          vehicleId: response.vehicleId,
          vehicleModel: response.vehicleModel || vehicle?.modelName || 'EV Model',
          vehicleLicensePlate: response.vehicleLicensePlate || vehicle?.licensePlate || '',
          startTime: response.startTime,
          endTime: response.endTime,
          bufferedEndTime: response.bufferedEndTime,
          status: response.status,
          estimatedCostVnd: Number(response.estimatedCost) || estimatedCostVnd,
          createdAt: response.createdAt,
        };

        // Play procedural audio chime
        AudioEngine.play('NOTIF_SUCCESS');

        // Synchronize Digital Twin state
        useDigitalTwinStore.getState().updateBooking(selectedVehicleId, {
          activeBookingId: response.id,
          reservedByUserId: response.userId,
          reservedByUserName: response.userName,
          startTime: response.startTime,
          endTime: response.endTime,
          conflictStatus: 'NONE',
          purpose: 'Syndicate Reservation',
        });
        await useDigitalTwinStore.getState().updateStatus(
          selectedVehicleId,
          'BOOKED',
          `Reservation confirmed (bookingId=${response.id})`
        );

        // Synchronize vehicle state in Garage store
        useGarageStore.getState().updateVehicleStatus(selectedVehicleId, 'BOOKED');

        set({
          isSubmitting: false,
          bookingResult: result,
          activeStep: 'RESULT',
        });

        // Refresh timeline and bookings
        get().fetchTimeline();
        get().fetchMyBookings();
        return true;
      } catch (err: unknown) {
        const errorStatus = (err as any)?.status || (err as any)?.response?.status;
        const errorMessage =
          (err as any)?.response?.data?.message || (err as Error)?.message || '';
        const isConflict =
          errorStatus === 409 ||
          errorMessage.toLowerCase().includes('conflict') ||
          errorMessage.toLowerCase().includes('overlap') ||
          errorMessage.toLowerCase().includes('buffer');

        // If it's a conflict or bad request, preserve and surface the conflict error!
        if (isConflict || errorStatus === 400 || errorStatus === 422) {
          const conflictMsg =
            errorMessage || 'Schedule conflict: Requested interval overlaps with an existing reservation or 30-min buffer (BR-BKG-02)';
          useDigitalTwinStore.getState().updateBooking(selectedVehicleId, {
            conflictStatus: 'OVERLAP',
          });
          set({
            isSubmitting: false,
            submissionError: conflictMsg,
            conflictReason: conflictMsg,
          });
          AudioEngine.play('UI_CLICK');
          return false;
        }

        // Fallback optimistic submission when backend is offline / unseeded
        const fallbackId = Math.floor(1000 + Math.random() * 9000);
        const bufferDate = new Date(new Date(endTime).getTime() + 30 * 60 * 1000);

        const result: BookingSubmissionResult = {
          bookingId: fallbackId,
          vehicleId: selectedVehicleId,
          vehicleModel: vehicle?.modelName || 'Tesla Model S Plaid',
          vehicleLicensePlate: vehicle?.licensePlate || '29A-888.88',
          startTime,
          endTime,
          bufferedEndTime: bufferDate.toISOString(),
          status: 'CONFIRMED',
          estimatedCostVnd,
          createdAt: new Date().toISOString(),
        };

        AudioEngine.play('NOTIF_SUCCESS');

        // Update Digital Twin state in fallback
        useDigitalTwinStore.getState().updateBooking(selectedVehicleId, {
          activeBookingId: fallbackId,
          reservedByUserId: 1,
          reservedByUserName: 'Nguyen Van A',
          startTime,
          endTime,
          conflictStatus: 'NONE',
          purpose: 'Simulated Reservation',
        });
        await useDigitalTwinStore.getState().updateStatus(
          selectedVehicleId,
          'BOOKED',
          `Reservation confirmed (bookingId=${fallbackId})`
        );
        useGarageStore.getState().updateVehicleStatus(selectedVehicleId, 'BOOKED');

        set({
          isSubmitting: false,
          bookingResult: result,
          activeStep: 'RESULT',
        });

        get().fetchTimeline();
        return true;
      }
    },

    updateBookingSchedule: async (bookingId, startTime, endTime) => {
      set({ isUpdating: true, updateError: null });
      try {
        const updated = await bookingsApi.updateBooking(bookingId, { startTime, endTime });
        const vehicleId = updated.vehicleId;

        // Synchronize Digital Twin state
        useDigitalTwinStore.getState().updateBooking(vehicleId, {
          startTime: updated.startTime,
          endTime: updated.endTime,
          conflictStatus: 'NONE',
        });

        set((state) => ({
          isUpdating: false,
          userBookings: state.userBookings.map((b) => (b.id === bookingId ? updated : b)),
          selectedBooking: state.selectedBooking?.id === bookingId ? updated : state.selectedBooking,
        }));

        AudioEngine.play('NOTIF_SUCCESS');
        get().fetchTimeline();
        return true;
      } catch (err: unknown) {
        const errMsg =
          (err as any)?.response?.data?.message ||
          (err as Error)?.message ||
          'Schedule conflict during update';

        const target = get().userBookings.find((b) => b.id === bookingId);
        if (target) {
          useDigitalTwinStore.getState().updateBooking(target.vehicleId, {
            conflictStatus: 'OVERLAP',
          });
        }

        set({ isUpdating: false, updateError: errMsg });
        return false;
      }
    },

    cancelBooking: async (bookingId, reason) => {
      set({ isCancelling: true, submissionError: null });
      try {
        const res = await bookingsApi.cancelBooking(bookingId, reason);
        const targetBooking = get().userBookings.find((b) => b.id === bookingId);
        const vehicleId = targetBooking?.vehicleId || get().selectedVehicleId;

        // Update Digital Twin state on cancellation
        useDigitalTwinStore.getState().updateBooking(vehicleId, {
          activeBookingId: null,
          reservedByUserId: null,
          reservedByUserName: null,
          startTime: null,
          endTime: null,
          conflictStatus: 'NONE',
        });

        // Release vehicle to AVAILABLE
        await useDigitalTwinStore.getState().updateStatus(
          vehicleId,
          'AVAILABLE',
          'Reservation cancelled (BR-BKG-03)'
        );
        useGarageStore.getState().updateVehicleStatus(vehicleId, 'AVAILABLE');

        // Update userBookings list
        set((state) => ({
          isCancelling: false,
          cancelResult: res,
          userBookings: state.userBookings.map((b) =>
            b.id === bookingId ? { ...b, status: 'CANCELLED' as BookingStatus } : b
          ),
          selectedBooking:
            state.selectedBooking?.id === bookingId
              ? { ...state.selectedBooking, status: 'CANCELLED' as BookingStatus }
              : state.selectedBooking,
        }));

        AudioEngine.play('NOTIF_SUCCESS');
        get().fetchTimeline();
        return true;
      } catch (err: unknown) {
        const errMsg =
          (err as any)?.response?.data?.message ||
          (err as Error)?.message ||
          'Failed to cancel reservation';
        set({ isCancelling: false, submissionError: errMsg });
        return false;
      }
    },

    fetchMyBookings: async (status) => {
      set({ isLoadingHistory: true });
      try {
        const list = await bookingsApi.getMyBookings(status);
        set({ userBookings: list || [], isLoadingHistory: false });
      } catch {
        // Simulated fallback when offline
        const fallbackBookings: BookingResponseDTO[] = [
          {
            id: 101,
            vehicleId: 1,
            vehicleModel: 'Tesla Model S Plaid',
            vehicleLicensePlate: '29A-888.88',
            userId: 1,
            userName: 'Nguyen Van A',
            userEmail: 'nguyen.a@evshare.vn',
            startTime: `${getIsoDate(0)}T14:00:00Z`,
            endTime: `${getIsoDate(0)}T18:00:00Z`,
            bufferedEndTime: `${getIsoDate(0)}T18:30:00Z`,
            status: 'CONFIRMED',
            estimatedCost: 600000,
            createdAt: new Date().toISOString(),
          },
        ];
        set({ userBookings: fallbackBookings, isLoadingHistory: false });
      }
    },

    fetchBookingHistory: async (bookingId) => {
      set({ isLoadingHistory: true });
      try {
        const history = await bookingsApi.getBookingHistory(bookingId);
        set({ activeBookingHistory: history || [], isLoadingHistory: false });
      } catch {
        const fallbackTrail: BookingHistoryResponseDTO[] = [
          {
            id: 1,
            bookingId,
            action: 'CREATE_BOOKING',
            actingUserId: 1,
            actingUserName: 'Nguyen Van A',
            oldStateJson: '{}',
            newStateJson: JSON.stringify({ status: 'CONFIRMED' }),
            createdAt: new Date().toISOString(),
          },
        ];
        set({ activeBookingHistory: fallbackTrail, isLoadingHistory: false });
      }
    },

    resetBooking: () => {
      set({
        activeStep: 'CALENDAR',
        selectedVehicleId: 1,
        bookingResult: null,
        submissionError: null,
        cancelResult: null,
        updateError: null,
        startHour: 9,
        endHour: 13,
        durationHours: 4,
        estimatedCostVnd: 4 * 150000,
        isSlotAvailable: true,
        conflictReason: null,
      });
      get().fetchTimeline();
    },
  };
});
