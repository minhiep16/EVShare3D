import { apiClient } from './apiClient';
import type { ApiResponse } from './vehiclesApi';

export interface OwnershipShareDTO {
  id: number;
  userId: number;
  userName: string;
  userEmail?: string;
  sharePercentage: number;
  percentage?: number;
  votingPowerPercentage: number;
  shareCertificateNumber?: string;
  acquiredAt?: string;
  isRepresentative: boolean;
  isActive?: boolean;
}

export interface OwnershipShareResponseDTO {
  id: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  percentage: number;
  shareCertificateNumber: string;
  acquiredAt: string;
  isActive: boolean;
}

export interface OwnershipHistoryDTO {
  id: number;
  shareId: number;
  action: string;
  actingUserId: number;
  previousPercentage: number;
  newPercentage: number;
  previousCertificateNumber?: string;
  newCertificateNumber?: string;
  previousIsActive?: boolean;
  newIsActive?: boolean;
  effectiveDate: string;
  oldStateJson?: string;
  newStateJson?: string;
}

export interface OwnershipGroupResponseDTO {
  id: number;
  groupName: string;
  vehicleId: number;
  vehicleModelName?: string;
  vehicleLicensePlate?: string;
  vehicleManufacturer?: string;
  formationDate: string;
  isActive: boolean;
  memberCount: number;
  memberShares: OwnershipShareDTO[];
}

export interface TransferSharePayload {
  fromUserId: number;
  toUserId: number;
  percentage: number;
}

export const ownershipGroupsApi = {
  getGroups: async (): Promise<OwnershipGroupResponseDTO[]> => {
    try {
      const res = await apiClient.get<ApiResponse<OwnershipGroupResponseDTO[]>>('/ownership-groups');
      return res.data.data;
    } catch {
      // Fallback for co-owner scoped access
      const myRes = await apiClient.get<ApiResponse<OwnershipGroupResponseDTO[]>>('/ownership-groups/my-groups');
      return myRes.data.data;
    }
  },

  getMyGroups: async (): Promise<OwnershipGroupResponseDTO[]> => {
    const res = await apiClient.get<ApiResponse<OwnershipGroupResponseDTO[]>>('/ownership-groups/my-groups');
    return res.data.data;
  },

  getGroupById: async (id: number): Promise<OwnershipGroupResponseDTO> => {
    const res = await apiClient.get<ApiResponse<OwnershipGroupResponseDTO>>(`/ownership-groups/${id}`);
    return res.data.data;
  },

  getGroupShares: async (groupId: number, activeOnly = true): Promise<OwnershipShareResponseDTO[]> => {
    const res = await apiClient.get<ApiResponse<OwnershipShareResponseDTO[]>>(
      `/ownership-groups/${groupId}/shares`,
      { params: { activeOnly } }
    );
    return res.data.data;
  },

  getGroupOwnershipHistory: async (groupId: number): Promise<OwnershipHistoryDTO[]> => {
    const res = await apiClient.get<ApiResponse<OwnershipHistoryDTO[]>>(
      `/ownership-groups/${groupId}/shares/history`
    );
    return res.data.data;
  },

  validateGroupShares: async (groupId: number): Promise<{ valid: boolean; totalPercentage: number }> => {
    const res = await apiClient.post<ApiResponse<number>>(`/ownership-groups/${groupId}/shares/validate`);
    return {
      valid: true,
      totalPercentage: Number(res.data.data),
    };
  },

  transferShare: async (groupId: number, payload: TransferSharePayload): Promise<OwnershipGroupResponseDTO> => {
    const res = await apiClient.post<ApiResponse<OwnershipGroupResponseDTO>>(
      `/ownership-groups/${groupId}/transfer-share`,
      payload
    );
    return res.data.data;
  },
};
