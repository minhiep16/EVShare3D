import { apiClient } from './apiClient';
import type { ApiResponse } from './vehiclesApi';

export type ContractStatus =
  | 'DRAFT'
  | 'PENDING_SIGNATURE'
  | 'SIGNED'
  | 'ACTIVE'
  | 'EXPIRED'
  | 'TERMINATED'
  | 'REJECTED';

export interface ContractDTO {
  id: number;
  groupId: number;
  groupName: string;
  contractTitle: string;
  contractTermsText: string;
  version: number;
  status: ContractStatus;
  effectiveDate: string;
  expiryDate?: string;
  createdAt: string;
}

export interface ContractSignatureDTO {
  id: number;
  contractId: number;
  contractVersion: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  signatureHash: string;
  signedAt: string;
  ipAddress: string;
}

export interface PendingSignerDTO {
  userId: number;
  userFullName: string;
  userEmail: string;
  sharePercentage: number;
}

export interface ContractSignaturesOverviewDTO {
  contractId: number;
  contractVersion: number;
  contractStatus: string;
  totalRequiredSignatures: number;
  totalSubmittedSignatures: number;
  allSigned: boolean;
  signatures: ContractSignatureDTO[];
  pendingSigners: PendingSignerDTO[];
}

export interface SignContractPayload {
  acceptTerms: boolean;
  signatureNote?: string;
}

export interface TransitionContractStatusPayload {
  targetStatus: ContractStatus;
  reason?: string;
}

export interface CreateContractPayload {
  groupId: number;
  contractTitle: string;
  contractTermsText: string;
  effectiveDate?: string;
  expiryDate?: string;
}

export interface UpdateContractPayload {
  contractTitle?: string;
  contractTermsText?: string;
  effectiveDate?: string;
  expiryDate?: string;
}

export const contractsApi = {
  getContractById: async (id: number): Promise<ContractDTO> => {
    const res = await apiClient.get<ApiResponse<ContractDTO>>(`/contracts/${id}`);
    return res.data.data;
  },

  getContractsByGroupId: async (groupId: number): Promise<ContractDTO[]> => {
    const res = await apiClient.get<ApiResponse<ContractDTO[]>>(`/contracts/group/${groupId}`);
    return res.data.data;
  },

  getActiveContractByGroupId: async (groupId: number): Promise<ContractDTO> => {
    const res = await apiClient.get<ApiResponse<ContractDTO>>(`/contracts/group/${groupId}/active`);
    return res.data.data;
  },

  getSignaturesOverview: async (id: number): Promise<ContractSignaturesOverviewDTO> => {
    const res = await apiClient.get<ApiResponse<ContractSignaturesOverviewDTO>>(`/contracts/${id}/signatures`);
    return res.data.data;
  },

  signContract: async (id: number, payload: SignContractPayload): Promise<ContractSignatureDTO> => {
    const res = await apiClient.post<ApiResponse<ContractSignatureDTO>>(`/contracts/${id}/sign`, payload);
    return res.data.data;
  },

  transitionStatus: async (
    id: number,
    payload: TransitionContractStatusPayload
  ): Promise<ContractDTO> => {
    const res = await apiClient.patch<ApiResponse<ContractDTO>>(`/contracts/${id}/status`, payload);
    return res.data.data;
  },

  createContract: async (payload: CreateContractPayload): Promise<ContractDTO> => {
    const res = await apiClient.post<ApiResponse<ContractDTO>>('/contracts', payload);
    return res.data.data;
  },

  updateDraftContract: async (
    id: number,
    payload: UpdateContractPayload
  ): Promise<ContractDTO> => {
    const res = await apiClient.put<ApiResponse<ContractDTO>>(`/contracts/${id}`, payload);
    return res.data.data;
  },
};
