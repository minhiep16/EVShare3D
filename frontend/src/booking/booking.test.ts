import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useBookingStore } from './useBookingStore';
import { useGarageStore } from '@/garage/useGarageStore';
import { bookingsApi } from '@/api/bookingsApi';
import { analyticsApi } from '@/api/analyticsApi';

vi.mock('@/engine/audio/AudioEngine', () => ({
  AudioEngine: {
    play: vi.fn(),
    playSpatial: vi.fn(),
  },
}));

vi.mock('@/api/bookingsApi', () => ({
  bookingsApi: {
    checkAvailability: vi.fn(),
    getTimeline: vi.fn(),
    createBooking: vi.fn(),
    getMyBookings: vi.fn(),
    cancelBooking: vi.fn(),
  },
}));

vi.mock('@/api/analyticsApi', () => ({
  analyticsApi: {
    getMyFairUsageScore: vi.fn(),
    getGroupFairUsage: vi.fn(),
  },
}));

describe('3D Booking Chamber & Workflow Subsystem (09-E)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useBookingStore.getState().resetBooking();
  });

  describe('1. Store State & Vehicle Selection', () => {
    it('initializes with default vehicles, selected vehicle, and CALENDAR step', () => {
      const state = useBookingStore.getState();
      expect(state.activeStep).toBe('CALENDAR');
      expect(state.vehicles.length).toBeGreaterThanOrEqual(3);
      expect(state.selectedVehicleId).toBe(1);
      expect(state.startHour).toBe(9);
      expect(state.endHour).toBe(13);
      expect(state.durationHours).toBe(4);
      expect(state.estimatedCostVnd).toBe(4 * 150000);
    });

    it('switches vehicle and dynamically recalculates cost based on hourly rate', () => {
      const { selectVehicle } = useBookingStore.getState();
      // Select vehicle 2 (Porsche Taycan 4S: 180,000 VND/h)
      selectVehicle(2);

      const state = useBookingStore.getState();
      expect(state.selectedVehicleId).toBe(2);
      expect(state.estimatedCostVnd).toBe(4 * 180000);
    });
  });

  describe('2. 3D Calendar & Date Navigation', () => {
    it('generates 14-day calendar cells with valid structure and dates', () => {
      const { calendarDays } = useBookingStore.getState();
      expect(calendarDays.length).toBe(14);

      const firstDay = calendarDays[0];
      expect(firstDay.dateStr).toMatch(/^\d{4}-\d{2}-\d{2}$/);
      expect(firstDay.dayOfWeek).toBeDefined();
      expect(firstDay.dayNumber).toBeGreaterThanOrEqual(1);
      expect(['AVAILABLE', 'PARTIALLY_BOOKED', 'FULLY_BOOKED']).toContain(firstDay.occupancyStatus);
    });

    it('updates selected date and maintains valid ISO string format', () => {
      const { selectDate } = useBookingStore.getState();
      selectDate('2026-09-20');

      expect(useBookingStore.getState().selectedDate).toBe('2026-09-20');
    });

    it('navigates week offset within [0, 3] boundary clamps', () => {
      const { setWeekOffset } = useBookingStore.getState();
      setWeekOffset(2);
      expect(useBookingStore.getState().weekOffset).toBe(2);

      // Clamps max to 3
      setWeekOffset(10);
      expect(useBookingStore.getState().weekOffset).toBe(3);

      // Clamps min to 0
      setWeekOffset(-5);
      expect(useBookingStore.getState().weekOffset).toBe(0);
    });
  });

  describe('3. Timeline & Time Range Selection', () => {
    it('updates time range and duration with boundary clamping', () => {
      const { setTimeRange } = useBookingStore.getState();
      setTimeRange(10, 16);

      const state = useBookingStore.getState();
      expect(state.startHour).toBe(10);
      expect(state.endHour).toBe(16);
      expect(state.durationHours).toBe(6);
      expect(state.estimatedCostVnd).toBe(6 * 150000);
    });

    it('applies quick duration presets correctly', () => {
      const { setDurationPreset } = useBookingStore.getState();
      setDurationPreset(2);

      let state = useBookingStore.getState();
      expect(state.durationHours).toBe(2);
      expect(state.endHour - state.startHour).toBe(2);

      setDurationPreset(8);
      state = useBookingStore.getState();
      expect(state.durationHours).toBe(8);
      expect(state.endHour - state.startHour).toBe(8);
    });
  });

  describe('4. Turnaround Buffer (BR-BKG-02) & Conflict Detection', () => {
    it('delegates to backend availability API and updates slot state', async () => {
      vi.mocked(bookingsApi.checkAvailability).mockResolvedValueOnce({
        vehicleId: 1,
        vehicleStatus: 'AVAILABLE',
        isAvailable: false,
        reason: 'Overlaps with booking #101',
        requestedStartTime: '2026-09-25T14:00:00Z',
        requestedEndTime: '2026-09-25T18:00:00Z',
        bufferMinutes: 30,
        conflictingBookings: [],
      });

      const isAvailable = await useBookingStore.getState().checkAvailability();
      expect(isAvailable).toBe(false);
      expect(useBookingStore.getState().isSlotAvailable).toBe(false);
      expect(useBookingStore.getState().conflictReason).toContain('101');
    });

    it('falls back to local conflict check with 30-min buffer when API is offline', async () => {
      // Simulate offline API rejection
      vi.mocked(bookingsApi.checkAvailability).mockRejectedValue(new Error('Network offline'));

      const testDate = '2026-09-25';
      useBookingStore.setState({
        selectedDate: testDate,
        timelineSlots: [
          {
            bookingId: 888,
            userId: 3,
            startTime: `${testDate}T14:00:00Z`,
            endTime: `${testDate}T17:00:00Z`,
            bufferedEndTime: `${testDate}T17:30:00Z`,
            status: 'CONFIRMED',
            isMyBooking: false,
          },
        ],
      });

      // Attempt to book 15:00 - 18:00 (overlaps directly with 14:00 - 17:00)
      useBookingStore.getState().setTimeRange(15, 18);
      const isAvailableDirect = await useBookingStore.getState().checkAvailability();
      expect(isAvailableDirect).toBe(false);
      expect(useBookingStore.getState().isSlotAvailable).toBe(false);
      expect(useBookingStore.getState().conflictReason).toContain('888');

      // Attempt to book 17:00 - 19:00 (overlaps with 30m turnaround buffer 17:00–17:30)
      useBookingStore.getState().setTimeRange(17, 19);
      const isAvailableBuffer = await useBookingStore.getState().checkAvailability();
      expect(isAvailableBuffer).toBe(false);
      expect(useBookingStore.getState().isSlotAvailable).toBe(false);

      // Attempt to book safe slot: 08:00 - 12:00 (well before 14:00)
      useBookingStore.getState().setTimeRange(8, 12);
      const isAvailableSafe = await useBookingStore.getState().checkAvailability();
      expect(isAvailableSafe).toBe(true);
      expect(useBookingStore.getState().isSlotAvailable).toBe(true);
    });
  });

  describe('5. Step Transitions & Workflow Navigation', () => {
    it('transitions through the complete workflow steps', () => {
      const { setActiveStep } = useBookingStore.getState();

      setActiveStep('CALENDAR');
      expect(useBookingStore.getState().activeStep).toBe('CALENDAR');

      setActiveStep('TIMELINE');
      expect(useBookingStore.getState().activeStep).toBe('TIMELINE');

      setActiveStep('TIME_SELECT');
      expect(useBookingStore.getState().activeStep).toBe('TIME_SELECT');

      setActiveStep('CONFIRMATION');
      expect(useBookingStore.getState().activeStep).toBe('CONFIRMATION');

      setActiveStep('RESULT');
      expect(useBookingStore.getState().activeStep).toBe('RESULT');
    });

    it('switches camera presets smoothly', () => {
      const { setCameraPreset } = useBookingStore.getState();
      setCameraPreset('CHAMBER_OVERVIEW');
      expect(useBookingStore.getState().cameraPreset).toBe('CHAMBER_OVERVIEW');

      setCameraPreset('TERMINAL_FOCUS');
      expect(useBookingStore.getState().cameraPreset).toBe('TERMINAL_FOCUS');

      setCameraPreset('CALENDAR_FOCUS');
      expect(useBookingStore.getState().cameraPreset).toBe('CALENDAR_FOCUS');
    });
  });

  describe('6. Booking Submission & 3D State Update', () => {
    it('submits reservation, generates booking certificate, and updates 3D vehicle status', async () => {
      vi.mocked(bookingsApi.createBooking).mockResolvedValueOnce({
        id: 777,
        vehicleId: 1,
        vehicleModel: 'Tesla Model S Plaid',
        vehicleLicensePlate: '29A-888.88',
        userId: 1,
        userName: 'Nguyen Van A',
        userEmail: 'nguyen.a@evshare.vn',
        startTime: '2026-09-16T09:00:00Z',
        endTime: '2026-09-16T13:00:00Z',
        bufferedEndTime: '2026-09-16T13:30:00Z',
        status: 'CONFIRMED',
        estimatedCost: 600000,
        createdAt: '2026-09-15T08:00:00Z',
      });

      const store = useBookingStore.getState();
      store.selectVehicle(1);
      store.setTimeRange(9, 13);

      const success = await store.submitBooking();
      expect(success).toBe(true);

      const updatedState = useBookingStore.getState();
      expect(updatedState.activeStep).toBe('RESULT');
      expect(updatedState.bookingResult).not.toBeNull();
      expect(updatedState.bookingResult?.bookingId).toBe(777);
      expect(updatedState.bookingResult?.vehicleModel).toContain('Tesla');
      expect(updatedState.bookingResult?.status).toBe('CONFIRMED');

      // Verify synchronization with 3D Central Garage store
      const garageVehicles = useGarageStore.getState().vehicles;
      const garageVehicle = garageVehicles.find((v) => v.id === 1);
      if (garageVehicle) {
        expect(garageVehicle.status).toBe('BOOKED');
      }
    });

    it('resets booking workflow cleanly', () => {
      const store = useBookingStore.getState();
      store.setActiveStep('RESULT');
      store.resetBooking();

      const state = useBookingStore.getState();
      expect(state.activeStep).toBe('CALENDAR');
      expect(state.bookingResult).toBeNull();
      expect(state.submissionError).toBeNull();
      expect(state.startHour).toBe(9);
      expect(state.endHour).toBe(13);
      expect(state.selectedVehicleId).toBe(1);
    });
  });

  describe('7. Fair Usage Analytics Integration', () => {
    it('holds valid syndicate member fair usage scores and recommendations', async () => {
      vi.mocked(analyticsApi.getMyFairUsageScore).mockResolvedValueOnce({
        userId: 1,
        groupId: 1,
        userName: 'Nguyen Van A',
        userEmail: 'nguyen.a@evshare.vn',
        equityPercentage: 40.0,
        actualUsageHours: 12.5,
        fairShareHours: 16.0,
        fairnessRatio: 0.78,
        imbalanceLevel: 'BALANCED',
        bookingPriorityScore: 98,
        evaluatedWindowDays: 30,
        windowStartDate: '2026-08-16T00:00:00Z',
        windowEndDate: '2026-09-15T00:00:00Z',
        recommendationMessage: 'Quota in balance. Immediate reservation approved.',
      });

      await useBookingStore.getState().fetchFairUsage();

      const { fairUsage } = useBookingStore.getState();
      expect(fairUsage).not.toBeNull();
      expect(fairUsage?.score).toBe(98);
      expect(fairUsage?.fairnessRatio).toBe(0.78);
      expect(fairUsage?.priorityTier).toBe('BALANCED');
      expect(fairUsage?.recommendation).toContain('Quota in balance');
    });
  });
});
