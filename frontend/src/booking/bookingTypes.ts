import type { BookingStatus } from '@/api/bookingsApi';

export type BookingStep =
  | 'VEHICLE_SELECT'
  | 'CALENDAR'
  | 'TIMELINE'
  | 'TIME_SELECT'
  | 'CONFIRMATION'
  | 'RESULT'
  | 'HISTORY';

export interface BookingVehicleSummary {
  id: number;
  modelName: string;
  manufacturer: string;
  licensePlate: string;
  batteryLevel: number;
  status: string;
  hourlyRateVnd: number;
  bodyColor: string;
  stallLocationCode?: string;
  groupId: number;
  groupName: string;
  estimatedRangeKm: number;
}

export interface TimelineHourSlot {
  hour: number;
  label: string;
  isAvailable: boolean;
  isBooked: boolean;
  isBuffer: boolean;
  isMyBooking: boolean;
  bookingId?: number;
  occupantName?: string;
}

export interface CalendarDayCell {
  dateStr: string; // YYYY-MM-DD
  dayOfWeek: string; // MON, TUE, etc.
  dayNumber: number; // 1-31
  monthName: string; // SEP, OCT, etc.
  isToday: boolean;
  isPast: boolean;
  occupancyStatus: 'AVAILABLE' | 'PARTIALLY_BOOKED' | 'FULLY_BOOKED';
  activeBookingsCount: number;
}

export interface FairUsageScoreModel {
  score: number;
  fairnessRatio: number;
  priorityTier: 'BALANCED' | 'MODERATE_OVERUSE' | 'HIGH_OVERUSE' | 'UNDERUSE';
  recommendation: string;
  equityPercentage: number;
}

export interface BookingSubmissionResult {
  bookingId: number;
  vehicleId: number;
  vehicleModel: string;
  vehicleLicensePlate: string;
  startTime: string;
  endTime: string;
  bufferedEndTime: string;
  status: BookingStatus;
  estimatedCostVnd: number;
  createdAt: string;
}

export interface BookingChamberThemeColors {
  CHRONO_CYAN: string;
  CHRONO_CYAN_DIM: string;
  CHRONO_CYAN_GLOW: string;
  BUFFER_PURPLE: string;
  OCCUPIED_AMBER: string;
  AVAILABLE_GREEN: string;
  CONFLICT_RED: string;
  DARK_OBSIDIAN: string;
  PANEL_BG: string;
  WIRE_FRAME: string;
}
