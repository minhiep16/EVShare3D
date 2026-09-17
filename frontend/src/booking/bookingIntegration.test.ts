import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useBookingStore } from './useBookingStore';
import { useGarageStore } from '@/garage/useGarageStore';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';
import { bookingsApi } from '@/api/bookingsApi';
import { analyticsApi } from '@/api/analyticsApi';

// Procedural audio mock
vi.mock('@/engine/audio/AudioEngine', () => ({
  AudioEngine: {
    play: vi.fn(),
    playSpatial: vi.fn(),
  },
}));

// API layer mocks
vi.mock('@/api/bookingsApi', () => ({
  bookingsApi: {
    checkAvailability: vi.fn(),
    getTimeline: vi.fn(),
    createBooking: vi.fn(),
    updateBooking: vi.fn(),
    cancelBooking: vi.fn(),
    getMyBookings: vi.fn(),
    getBookingHistory: vi.fn(),
    getVehicleBookings: vi.fn(),
  },
}));

vi.mock('@/api/analyticsApi', () => ({
  analyticsApi: {
    getMyFairUsageScore: vi.fn(),
    getGroupFairUsage: vi.fn(),
  },
}));

describe('09-T: Booking Integration & Digital Twin Synchronization', () => {
  const VEHICLE_ID = 1;

  beforeEach(() => {
    vi.clearAllMocks();
    useBookingStore.getState().resetBooking();
    useDigitalTwinStore.getState().resetToDefaults();
  });

  describe('1. Availability Checking & BR-BKG-02 30-min Turnaround Buffer', () => {
    it('queries backend availability with exact ISO datetimes and reports true when slot is open', async () => {
      vi.mocked(bookingsApi.checkAvailability).mockResolvedValueOnce({
        vehicleId: VEHICLE_ID,
        startTime: '2026-09-20T09:00:00Z',
        endTime: '2026-09-20T13:00:00Z',
        isAvailable: true,
      });

      useBookingStore.setState({
        selectedVehicleId: VEHICLE_ID,
        selectedDate: '2026-09-20',
        startHour: 9,
        endHour: 13,
      });

      const isAvailable = await useBookingStore.getState().checkAvailability();

      expect(bookingsApi.checkAvailability).toHaveBeenCalledWith(
        VEHICLE_ID,
        '2026-09-20T09:00:00Z',
        '2026-09-20T13:00:00Z'
      );
      expect(isAvailable).toBe(true);
      expect(useBookingStore.getState().isSlotAvailable).toBe(true);
      expect(useBookingStore.getState().conflictReason).toBeNull();
    });

    it('captures availability conflict reason when backend flags turnaround buffer overlap', async () => {
      vi.mocked(bookingsApi.checkAvailability).mockResolvedValueOnce({
        vehicleId: VEHICLE_ID,
        startTime: '2026-09-20T14:00:00Z',
        endTime: '2026-09-20T18:00:00Z',
        isAvailable: false,
        reason: 'Requested interval overlaps with 30-minute turnaround buffer of booking #101',
      });

      useBookingStore.setState({
        selectedVehicleId: VEHICLE_ID,
        selectedDate: '2026-09-20',
        startHour: 14,
        endHour: 18,
      });

      const isAvailable = await useBookingStore.getState().checkAvailability();

      expect(isAvailable).toBe(false);
      expect(useBookingStore.getState().isSlotAvailable).toBe(false);
      expect(useBookingStore.getState().conflictReason).toContain('30-minute turnaround buffer');
    });
  });

  describe('2. Booking Creation & Authoritative Digital Twin State Update', () => {
    it('creates booking via REST and synchronizes Digital Twin booking and status facets', async () => {
      const mockCreated = {
        id: 902,
        vehicleId: VEHICLE_ID,
        vehicleModel: 'Tesla Model S Plaid',
        vehicleLicensePlate: '29A-888.88',
        userId: 1,
        userName: 'Nguyen Van A',
        userEmail: 'nguyen.a@evshare.vn',
        startTime: '2026-09-20T10:00:00Z',
        endTime: '2026-09-20T14:00:00Z',
        bufferedEndTime: '2026-09-20T14:30:00Z',
        status: 'CONFIRMED' as const,
        estimatedCost: 600000,
        createdAt: '2026-09-16T13:00:00Z',
      };

      vi.mocked(bookingsApi.createBooking).mockResolvedValueOnce(mockCreated);

      useBookingStore.setState({
        selectedVehicleId: VEHICLE_ID,
        selectedDate: '2026-09-20',
        startHour: 10,
        endHour: 14,
        estimatedCostVnd: 600000,
      });

      const success = await useBookingStore.getState().submitBooking();

      expect(success).toBe(true);
      expect(bookingsApi.createBooking).toHaveBeenCalledWith({
        vehicleId: VEHICLE_ID,
        startTime: '2026-09-20T10:00:00Z',
        endTime: '2026-09-20T14:00:00Z',
      });

      // 1. Verify Booking Result in Booking Store
      const bookingStore = useBookingStore.getState();
      expect(bookingStore.activeStep).toBe('RESULT');
      expect(bookingStore.bookingResult?.bookingId).toBe(902);
      expect(bookingStore.bookingResult?.status).toBe('CONFIRMED');

      // 2. Authoritative Digital Twin Check: Booking Facet
      const twinVehicle = useDigitalTwinStore.getState().digitalTwins[VEHICLE_ID];
      expect(twinVehicle).toBeDefined();
      expect(twinVehicle.booking.activeBookingId).toBe(902);
      expect(twinVehicle.booking.reservedByUserId).toBe(1);
      expect(twinVehicle.booking.reservedByUserName).toBe('Nguyen Van A');
      expect(twinVehicle.booking.startTime).toBe('2026-09-20T10:00:00Z');
      expect(twinVehicle.booking.endTime).toBe('2026-09-20T14:00:00Z');
      expect(twinVehicle.booking.conflictStatus).toBe('NONE');

      // 3. Authoritative Digital Twin Check: Status Facet
      expect(twinVehicle.status.status).toBe('BOOKED');

      // 4. Central Garage synchronization
      const garageVehicle = useGarageStore.getState().vehicles.find((v) => v.id === VEHICLE_ID);
      expect(garageVehicle?.status).toBe('BOOKED');
    });
  });

  describe('3. Conflict Errors (HTTP 409 & Buffer Violations)', () => {
    it('surfaces 409 Conflict accurately, sets digital twin OVERLAP status, and never fakes offline success', async () => {
      const conflictError: any = new Error('Slot overlap with booking #101');
      conflictError.status = 409;
      conflictError.response = {
        status: 409,
        data: {
          message: 'Schedule conflict: Requested interval overlaps with an existing reservation or 30-min buffer (BR-BKG-02)',
        },
      };

      vi.mocked(bookingsApi.createBooking).mockRejectedValueOnce(conflictError);

      useBookingStore.setState({
        selectedVehicleId: VEHICLE_ID,
        selectedDate: '2026-09-20',
        startHour: 10,
        endHour: 14,
      });

      const success = await useBookingStore.getState().submitBooking();

      // Must fail cleanly
      expect(success).toBe(false);

      // Store must record the conflict error
      const bookingStore = useBookingStore.getState();
      expect(bookingStore.submissionError).toContain('Schedule conflict');
      expect(bookingStore.conflictReason).toContain('Schedule conflict');
      expect(bookingStore.activeStep).not.toBe('RESULT'); // Must not advance to success result

      // Digital Twin booking facet must be marked with OVERLAP
      const twinVehicle = useDigitalTwinStore.getState().digitalTwins[VEHICLE_ID];
      expect(twinVehicle.booking.conflictStatus).toBe('OVERLAP');
    });
  });

  describe('4. Reschedule / Update Booking (PUT /api/v1/bookings/{id})', () => {
    it('updates booking time interval and synchronizes Digital Twin booking schedule', async () => {
      const updatedDto = {
        id: 902,
        vehicleId: VEHICLE_ID,
        vehicleModel: 'Tesla Model S Plaid',
        vehicleLicensePlate: '29A-888.88',
        userId: 1,
        userName: 'Nguyen Van A',
        userEmail: 'nguyen.a@evshare.vn',
        startTime: '2026-09-20T11:00:00Z',
        endTime: '2026-09-20T15:00:00Z',
        bufferedEndTime: '2026-09-20T15:30:00Z',
        status: 'CONFIRMED' as const,
        estimatedCost: 600000,
        createdAt: '2026-09-16T13:00:00Z',
      };

      vi.mocked(bookingsApi.updateBooking).mockResolvedValueOnce(updatedDto);

      useBookingStore.setState({
        userBookings: [updatedDto],
      });

      const success = await useBookingStore
        .getState()
        .updateBookingSchedule(902, '2026-09-20T11:00:00Z', '2026-09-20T15:00:00Z');

      expect(success).toBe(true);
      expect(bookingsApi.updateBooking).toHaveBeenCalledWith(902, {
        startTime: '2026-09-20T11:00:00Z',
        endTime: '2026-09-20T15:00:00Z',
      });

      // Verify Digital Twin booking facet updated with new times
      const twinVehicle = useDigitalTwinStore.getState().digitalTwins[VEHICLE_ID];
      expect(twinVehicle.booking.startTime).toBe('2026-09-20T11:00:00Z');
      expect(twinVehicle.booking.endTime).toBe('2026-09-20T15:00:00Z');
    });
  });

  describe('5. Cancel Booking & BR-BKG-03 Cancellation Terms', () => {
    it('cancels booking with payload reason, records penalty feedback, and releases Digital Twin to AVAILABLE', async () => {
      const cancelResponse = {
        bookingId: 902,
        status: 'CANCELLED' as const,
        cancelledAt: '2026-09-16T14:00:00Z',
        cancellationReason: 'Need to reschedule trip',
        penaltyFeeApplied: true,
        feePercentage: 20,
        feeAmountVnd: 120000,
        refundAmountVnd: 480000,
        message: 'Cancelled within 12 hours. 20% penalty fee applied per BR-BKG-03.',
      };

      vi.mocked(bookingsApi.cancelBooking).mockResolvedValueOnce(cancelResponse);

      // Pre-set vehicle as BOOKED
      useDigitalTwinStore.getState().updateBooking(VEHICLE_ID, {
        activeBookingId: 902,
        startTime: '2026-09-20T10:00:00Z',
        endTime: '2026-09-20T14:00:00Z',
      });
      useDigitalTwinStore.getState().updateStatus(VEHICLE_ID, 'BOOKED');
      useGarageStore.getState().updateVehicleStatus(VEHICLE_ID, 'BOOKED');

      useBookingStore.setState({
        selectedVehicleId: VEHICLE_ID,
        userBookings: [
          {
            id: 902,
            vehicleId: VEHICLE_ID,
            vehicleModel: 'Tesla Model S Plaid',
            vehicleLicensePlate: '29A-888.88',
            userId: 1,
            userName: 'Nguyen Van A',
            userEmail: 'nguyen.a@evshare.vn',
            startTime: '2026-09-20T10:00:00Z',
            endTime: '2026-09-20T14:00:00Z',
            status: 'CONFIRMED',
            estimatedCost: 600000,
            createdAt: '2026-09-16T13:00:00Z',
          },
        ],
      });

      const success = await useBookingStore
        .getState()
        .cancelBooking(902, 'Need to reschedule trip');

      expect(success).toBe(true);
      expect(bookingsApi.cancelBooking).toHaveBeenCalledWith(902, 'Need to reschedule trip');

      // Verify penalty result captured
      const cancelResult = useBookingStore.getState().cancelResult;
      expect(cancelResult?.penaltyFeeApplied).toBe(true);
      expect(cancelResult?.feePercentage).toBe(20);
      expect(cancelResult?.refundAmountVnd).toBe(480000);

      // Verify Digital Twin released back to AVAILABLE and activeBookingId cleared
      const twinVehicle = useDigitalTwinStore.getState().digitalTwins[VEHICLE_ID];
      expect(twinVehicle.booking.activeBookingId).toBeNull();
      expect(twinVehicle.status.status).toBe('AVAILABLE');

      // Verify Central Garage store released to AVAILABLE
      const garageVehicle = useGarageStore.getState().vehicles.find((v) => v.id === VEHICLE_ID);
      expect(garageVehicle?.status).toBe('AVAILABLE');
    });
  });

  describe('6. Booking History & Audit Trail', () => {
    it('fetches user reservation history and sets list in store', async () => {
      const mockList = [
        {
          id: 901,
          vehicleId: VEHICLE_ID,
          vehicleModel: 'Tesla Model S Plaid',
          vehicleLicensePlate: '29A-888.88',
          userId: 1,
          userName: 'Nguyen Van A',
          userEmail: 'nguyen.a@evshare.vn',
          startTime: '2026-09-18T10:00:00Z',
          endTime: '2026-09-18T14:00:00Z',
          status: 'COMPLETED' as const,
          estimatedCost: 600000,
          createdAt: '2026-09-16T10:00:00Z',
        },
      ];

      vi.mocked(bookingsApi.getMyBookings).mockResolvedValueOnce(mockList);

      await useBookingStore.getState().fetchMyBookings();

      expect(bookingsApi.getMyBookings).toHaveBeenCalled();
      expect(useBookingStore.getState().userBookings).toHaveLength(1);
      expect(useBookingStore.getState().userBookings[0].id).toBe(901);
    });

    it('fetches booking history audit trail for cryptographic log review', async () => {
      const mockAudit = [
        {
          id: 1,
          bookingId: 902,
          actorUserId: 1,
          actorName: 'Nguyen Van A',
          action: 'CREATED',
          details: 'Reservation booked for 4.0 hours',
          timestamp: '2026-09-16T13:00:00Z',
        },
        {
          id: 2,
          bookingId: 902,
          actorUserId: 1,
          actorName: 'Nguyen Van A',
          action: 'CANCELLED',
          details: 'Cancelled with 20% penalty fee (BR-BKG-03)',
          timestamp: '2026-09-16T14:00:00Z',
        },
      ];

      vi.mocked(bookingsApi.getBookingHistory).mockResolvedValueOnce(mockAudit);

      await useBookingStore.getState().fetchBookingHistory(902);

      expect(bookingsApi.getBookingHistory).toHaveBeenCalledWith(902);
      expect(useBookingStore.getState().activeBookingHistory).toHaveLength(2);
      expect(useBookingStore.getState().activeBookingHistory[1].action).toBe('CANCELLED');
    });
  });

  describe('7. Fair Usage Quota & Priority Standing', () => {
    it('loads live syndicate fair usage score and priority standing from analytics API', async () => {
      vi.mocked(analyticsApi.getMyFairUsageScore).mockResolvedValueOnce({
        groupId: 1,
        userId: 1,
        bookingPriorityScore: 98,
        fairnessRatio: 1.02,
        equityPercentage: 40.0,
        hoursUsedThisMonth: 12.5,
        targetHoursAllowed: 15.0,
        imbalanceLevel: 'BALANCED',
        recommendationMessage: 'Syndicate quota optimal. Priority turnaround applies.',
      });

      await useBookingStore.getState().fetchFairUsage();

      const fairUsage = useBookingStore.getState().fairUsage;
      expect(fairUsage).not.toBeNull();
      expect(fairUsage?.score).toBe(98);
      expect(fairUsage?.fairnessRatio).toBe(1.02);
      expect(fairUsage?.priorityTier).toBe('BALANCED');
      expect(fairUsage?.equityPercentage).toBe(40.0);
    });
  });
});
