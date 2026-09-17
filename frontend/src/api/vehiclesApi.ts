import { apiClient } from './apiClient';

export type BackendVehicleStatus =
  | 'AVAILABLE'
  | 'BOOKED'
  | 'IN_USE'
  | 'CHARGING'
  | 'MAINTENANCE'
  | 'DAMAGED'
  | 'UNAVAILABLE'
  | 'RESERVED';

export interface VehicleResponseDTO {
  id: number;
  vin: string;
  licensePlate: string;
  modelName: string;
  manufacturer: string;
  model3dAssetPath?: string;
  status: BackendVehicleStatus;
  batteryLevel: number;
  odometerKm: number;
  stallLocationCode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface VehicleTelemetryResponseDTO {
  vehicleId: number;
  vin: string;
  status: BackendVehicleStatus;
  batteryLevel: number;
  odometerKm: number;
  stallLocationCode: string;
  estimatedRangeKm: number;
  lastTelemetrySync: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PagedData<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export const vehiclesApi = {
  getVehicles: async (): Promise<VehicleResponseDTO[]> => {
    const res = await apiClient.get<ApiResponse<PagedData<VehicleResponseDTO>>>('/vehicles');
    return res.data.data.content;
  },

  getVehicleById: async (id: number): Promise<VehicleResponseDTO> => {
    const res = await apiClient.get<ApiResponse<VehicleResponseDTO>>(`/vehicles/${id}`);
    return res.data.data;
  },

  getVehicleTelemetry: async (id: number): Promise<VehicleTelemetryResponseDTO> => {
    const res = await apiClient.get<ApiResponse<VehicleTelemetryResponseDTO>>(`/vehicles/${id}/telemetry`);
    return res.data.data;
  },

  updateStatus: async (
    id: number,
    targetStatus: BackendVehicleStatus,
    reason?: string
  ): Promise<VehicleResponseDTO> => {
    // Map RESERVED to BOOKED if target is RESERVED to comply with Spring Boot canonical enum
    const canonicalStatus = targetStatus === 'RESERVED' ? 'BOOKED' : targetStatus;
    const res = await apiClient.patch<ApiResponse<VehicleResponseDTO>>(`/vehicles/${id}/status`, {
      status: canonicalStatus,
      reason: reason || 'Updated from 3D Central Garage interactive terminal',
    });
    return res.data.data;
  },
};
