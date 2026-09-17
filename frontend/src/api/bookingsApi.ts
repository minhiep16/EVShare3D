import { apiClient } from './apiClient';
import type { ApiResponse, PagedData } from './vehiclesApi';

export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'
  | 'REJECTED';

export interface BookingResponseDTO {
  id: number;
  vehicleId: number;
  vehicleModel: string;
  vehicleLicensePlate: string;
  userId: number;
  userName: string;
  userEmail: string;
  startTime: string;
  endTime: string;
  bufferedEndTime: string;
  status: BookingStatus;
  estimatedCost: number;
  createdAt: string;
}

export interface BookingTimelineSlotDTO {
  bookingId: number;
  userId: number;
  startTime: string;
  endTime: string;
  bufferedEndTime: string;
  status: BookingStatus;
  isMyBooking: boolean;
}

export interface VehicleAvailabilityDTO {
  vehicleId: number;
  vehicleStatus: string;
  isAvailable: boolean;
  reason: string;
  requestedStartTime: string;
  requestedEndTime: string;
  bufferMinutes: number;
  conflictingBookings: BookingTimelineSlotDTO[];
}

export interface CreateBookingPayload {
  vehicleId: number;
  startTime: string;
  endTime: string;
  userId?: number;
}

export interface BookingCancellationResponseDTO {
  bookingId: number;
  status: BookingStatus;
  penaltyFeeApplied: boolean;
  penaltyFeeAmount?: number;
  message: string;
}

export interface BookingHistoryResponseDTO {
  id: number;
  bookingId: number;
  action: string;
  actingUserId?: number;
  actingUserName?: string;
  oldStateJson?: string;
  newStateJson?: string;
  createdAt: string;
}

export interface UpdateBookingPayload {
  startTime: string;
  endTime: string;
}

export const bookingsApi = {
  /**
   * Evaluates vehicle availability for a specific time range enforcing the 30-min turnaround buffer.
   */
  checkAvailability: async (
    vehicleId: number,
    startTime: string,
    endTime: string
  ): Promise<VehicleAvailabilityDTO> => {
    const res = await apiClient.get<ApiResponse<VehicleAvailabilityDTO>>('/bookings/availability', {
      params: { vehicleId, startTime, endTime },
    });
    return res.data.data;
  },

  /**
   * Retrieves chronological scheduled timeline slots for a vehicle between 'from' and 'to'.
   */
  getTimeline: async (
    vehicleId: number,
    from: string,
    to: string
  ): Promise<BookingTimelineSlotDTO[]> => {
    const res = await apiClient.get<ApiResponse<BookingTimelineSlotDTO[]>>('/bookings/timeline', {
      params: { vehicleId, from, to },
    });
    return res.data.data;
  },

  /**
   * Atomically creates a vehicle reservation with turnaround buffer and equity verification.
   */
  createBooking: async (payload: CreateBookingPayload): Promise<BookingResponseDTO> => {
    const res = await apiClient.post<ApiResponse<BookingResponseDTO>>('/bookings', payload);
    return res.data.data;
  },

  /**
   * Retrieves booking details by ID.
   */
  getBookingById: async (id: number): Promise<BookingResponseDTO> => {
    const res = await apiClient.get<ApiResponse<BookingResponseDTO>>(`/bookings/${id}`);
    return res.data.data;
  },

  /**
   * Updates/reschedules an existing vehicle reservation.
   */
  updateBooking: async (
    id: number,
    payload: UpdateBookingPayload
  ): Promise<BookingResponseDTO> => {
    const res = await apiClient.put<ApiResponse<BookingResponseDTO>>(`/bookings/${id}`, payload);
    return res.data.data;
  },

  /**
   * Retrieves paginated bookings for the authenticated co-owner.
   */
  getMyBookings: async (status?: BookingStatus): Promise<BookingResponseDTO[]> => {
    const res = await apiClient.get<ApiResponse<PagedData<BookingResponseDTO>>>('/bookings/my-bookings', {
      params: status ? { status } : undefined,
    });
    return res.data.data.content;
  },

  /**
   * Retrieves paginated bookings for a syndicate vehicle.
   */
  getVehicleBookings: async (
    vehicleId: number,
    status?: BookingStatus
  ): Promise<BookingResponseDTO[]> => {
    const res = await apiClient.get<ApiResponse<PagedData<BookingResponseDTO>>>(`/bookings/vehicle/${vehicleId}`, {
      params: status ? { status } : undefined,
    });
    return res.data.data.content;
  },

  /**
   * Cancels a reservation applying BR-BKG-03 penalty rules.
   */
  cancelBooking: async (
    id: number,
    reason?: string
  ): Promise<BookingCancellationResponseDTO> => {
    const res = await apiClient.post<ApiResponse<BookingCancellationResponseDTO>>(
      `/bookings/${id}/cancel`,
      { reason }
    );
    return res.data.data;
  },

  /**
   * Retrieves immutable audit trail entries recorded for this reservation.
   */
  getBookingHistory: async (id: number): Promise<BookingHistoryResponseDTO[]> => {
    const res = await apiClient.get<ApiResponse<BookingHistoryResponseDTO[]>>(`/bookings/${id}/history`);
    return res.data.data;
  },
};
