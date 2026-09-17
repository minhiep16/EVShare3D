import { apiClient } from './apiClient';
import type { ApiResponse } from './vehiclesApi';

export type UsageSessionStatus =
  | 'ACTIVE'
  | 'COMPLETED'
  | 'OVERDUE'
  | 'CANCELLED'
  | 'DISPUTED';

export type VehicleStatus =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'IN_USE'
  | 'MAINTENANCE'
  | 'OUT_OF_SERVICE';

export interface GenerateQrPayload {
  bookingId: number;
}

export interface QrCodeDTO {
  qrToken: string;
  bookingId: number;
  vehicleId: number;
  expiresAt: string;
}

export interface QrValidationPayload {
  qrToken: string;
  vehicleId?: number;
  bookingId?: number;
}

export interface QrValidationDTO {
  valid: boolean;
  bookingId?: number;
  vehicleId?: number;
  vehicleLicensePlate?: string;
  vehicleModel?: string;
  userId?: number;
  userName?: string;
  checkInWindowStart?: string;
  checkInWindowEnd?: string;
  message: string;
}

export interface CheckInPayload {
  bookingId: number;
  vehicleId?: number;
  startOdometer: number;
  startBattery: number;
  conditionMeshFlags?: string;
  inspectionNotes?: string;
  evidencePhotoUrls?: string[];
}

export interface CheckOutPayload {
  endOdometer: number;
  endBattery: number;
  isPluggedIn?: boolean;
  hasDamage?: boolean;
  conditionMeshFlags?: string;
  inspectionNotes?: string;
  evidencePhotoUrls?: string[];
  otherAdditionalCost?: number;
  vehicleId?: number;
  userId?: number;
}

export interface UsageSessionDTO {
  id: number;
  bookingId: number;
  userId: number;
  userName: string;
  userEmail: string;
  vehicleId: number;
  vehicleLicensePlate: string;
  vehicleModel: string;
  startOdometer: number;
  endOdometer?: number;
  mileage?: number;
  startBattery: number;
  endBattery?: number;
  batteryDelta?: number;
  checkInTime: string;
  checkOutTime?: string;
  durationMinutes?: number;
  status: UsageSessionStatus;
  additionalCost?: number;
  costBreakdown?: Record<string, number>;
}

export interface UpdateVehicleStatusPayload {
  targetStatus: VehicleStatus;
  reason?: string;
}

export interface VehicleTelemetryDTO {
  vehicleId: number;
  batterySoc: number;
  odometer: number;
  status: VehicleStatus;
  currentBay: string;
  speed: number;
  chargingStatus: string;
  lastUpdated: string;
}

export const usageSessionsApi = {
  generateQr: async (payload: GenerateQrPayload): Promise<QrCodeDTO> => {
    const res = await apiClient.post<ApiResponse<QrCodeDTO>>('/usage-sessions/generate-qr', payload);
    return res.data.data;
  },

  validateQr: async (payload: QrValidationPayload): Promise<QrValidationDTO> => {
    const res = await apiClient.post<ApiResponse<QrValidationDTO>>('/usage-sessions/validate-qr', payload);
    return res.data.data;
  },

  checkIn: async (payload: CheckInPayload): Promise<UsageSessionDTO> => {
    const res = await apiClient.post<ApiResponse<UsageSessionDTO>>('/usage-sessions/check-in', payload);
    return res.data.data;
  },

  checkOut: async (id: number, payload: CheckOutPayload): Promise<UsageSessionDTO> => {
    const res = await apiClient.post<ApiResponse<UsageSessionDTO>>(`/usage-sessions/${id}/check-out`, payload);
    return res.data.data;
  },

  getSessionById: async (id: number): Promise<UsageSessionDTO> => {
    const res = await apiClient.get<ApiResponse<UsageSessionDTO>>(`/usage-sessions/${id}`);
    return res.data.data;
  },

  getSessionByBookingId: async (bookingId: number): Promise<UsageSessionDTO> => {
    const res = await apiClient.get<ApiResponse<UsageSessionDTO>>(`/usage-sessions/booking/${bookingId}`);
    return res.data.data;
  },

  getSessionsByVehicle: async (vehicleId: number): Promise<UsageSessionDTO[]> => {
    const res = await apiClient.get<ApiResponse<UsageSessionDTO[]>>(`/usage-sessions/vehicle/${vehicleId}`);
    return res.data.data;
  },

  getMySessions: async (): Promise<UsageSessionDTO[]> => {
    const res = await apiClient.get<ApiResponse<UsageSessionDTO[]>>('/usage-sessions/my-sessions');
    return res.data.data;
  },

  updateVehicleStatus: async (
    id: number,
    payload: UpdateVehicleStatusPayload
  ): Promise<any> => {
    const res = await apiClient.patch<ApiResponse<any>>(`/vehicles/${id}/status`, payload);
    return res.data.data;
  },

  getVehicleTelemetry: async (id: number): Promise<VehicleTelemetryDTO> => {
    const res = await apiClient.get<ApiResponse<VehicleTelemetryDTO>>(`/vehicles/${id}/telemetry`);
    return res.data.data;
  },
};
