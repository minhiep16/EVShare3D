import { apiClient } from './apiClient';
import type { ApiResponse } from './vehiclesApi';

export type DisputeStatus =
  | 'OPEN'
  | 'UNDER_REVIEW'
  | 'RESOLVED'
  | 'ESCALATED'
  | 'DISMISSED';

export interface DisputeEvidenceDTO {
  id: number;
  disputeId: number;
  uploadedByUserId: number;
  uploadedByUserName: string;
  fileUrl: string;
  mesh3dDefectCoordinates: string; // JSON: {"x": number, "y": number, "z": number}
  description: string;
  createdAt: string;
}

export interface DisputeDTO {
  id: number;
  groupId: number;
  groupName: string;
  usageSessionId?: number;
  complainantUserId: number;
  complainantUserName: string;
  respondentUserId?: number;
  respondentUserName?: string;
  title: string;
  description: string;
  status: DisputeStatus;
  resolutionSummary?: string;
  mediationNotes?: string;
  proposedResolution?: string;
  mediatorUserId?: number;
  mediatorUserName?: string;
  arbitratorUserId?: number;
  arbitratorUserName?: string;
  resolvedAt?: string;
  fundAdjustmentAmount?: number;
  fundTransactionId?: number;
  fundTransactionReference?: string;
  evidences: DisputeEvidenceDTO[];
  createdAt: string;
}

export interface CreateDisputeEvidencePayload {
  fileUrl: string;
  description?: string;
  mesh3dDefectCoordinates?: string;
}

export interface CreateDisputePayload {
  groupId: number;
  usageSessionId?: number;
  respondentUserId?: number;
  title: string;
  description: string;
  evidences?: CreateDisputeEvidencePayload[];
  initialEvidenceFileUrl?: string;
  mesh3dDefectCoordinates?: string;
}

export interface TransitionDisputeStatusPayload {
  targetStatus: DisputeStatus;
  reason?: string;
  resolutionSummary?: string;
}

export interface AddEvidencePayload {
  fileUrl: string;
  mesh3dDefectCoordinates?: string;
  description?: string;
}

export interface AddMediationNotesPayload {
  mediationNotes: string;
}

export interface ProposeResolutionPayload {
  proposedResolution: string;
}

export interface AdminArbitratePayload {
  reason?: string;
  resolutionSummary?: string;
  fundAdjustmentAmount?: number;
  fundAdjustmentType?: 'DEBIT' | 'CREDIT';
  targetStatus?: 'RESOLVED' | 'DISMISSED';
  arbitrationVerdict?: string;
  faultPartyUserId?: number;
  deductibleAmount?: number;
}

export interface DisputeFundAdjustmentPayload {
  amount?: number;
  entryType?: 'DEBIT' | 'CREDIT';
  reason?: string;
  resolutionSummary?: string;
  adjustmentAmount?: number;
  adjustmentDirection?: 'CREDIT_FUND' | 'DEBIT_FUND';
  responsibleUserId?: number;
  arbitrationVerdict?: string;
}

export interface DisputeAuditLogDTO {
  id: number;
  disputeId: number;
  action: string;
  actorUserId: number;
  actorUserName: string;
  actorRole: string;
  details: string;
  timestamp: string;
}

export interface DisputeArbitrationDossierDTO {
  dispute: DisputeDTO;
  evidences: DisputeEvidenceDTO[];
  history: DisputeAuditLogDTO[];
  dossierSummary: string;
  certifiedAt: string;
}

export const disputesApi = {
  getDisputesByGroupId: async (groupId: number, status?: DisputeStatus): Promise<DisputeDTO[]> => {
    const params = status ? { status } : {};
    const res = await apiClient.get<ApiResponse<DisputeDTO[]>>(`/disputes/group/${groupId}`, { params });
    return res.data.data;
  },

  getDisputeById: async (id: number): Promise<DisputeDTO> => {
    const res = await apiClient.get<ApiResponse<DisputeDTO>>(`/disputes/${id}`);
    return res.data.data;
  },

  createDispute: async (payload: CreateDisputePayload): Promise<DisputeDTO> => {
    let evidences = payload.evidences;
    if (!evidences || evidences.length === 0) {
      if (payload.initialEvidenceFileUrl) {
        evidences = [
          {
            fileUrl: payload.initialEvidenceFileUrl,
            description: payload.description || 'Initial photographic damage evidence',
            mesh3dDefectCoordinates: payload.mesh3dDefectCoordinates || '{"x": 0, "y": 0, "z": 0}',
          },
        ];
      }
    }
    const body = {
      groupId: payload.groupId,
      usageSessionId: payload.usageSessionId,
      respondentUserId: payload.respondentUserId,
      title: payload.title,
      description: payload.description,
      evidences: evidences || [],
    };
    const res = await apiClient.post<ApiResponse<DisputeDTO>>('/disputes', body);
    return res.data.data;
  },

  transitionDisputeStatus: async (
    id: number,
    payload: TransitionDisputeStatusPayload
  ): Promise<DisputeDTO> => {
    const res = await apiClient.post<ApiResponse<DisputeDTO>>(`/disputes/${id}/transition`, payload);
    return res.data.data;
  },

  addEvidence: async (id: number, payload: AddEvidencePayload): Promise<DisputeEvidenceDTO> => {
    const res = await apiClient.post<ApiResponse<DisputeEvidenceDTO>>(`/disputes/${id}/evidence`, payload);
    return res.data.data;
  },

  getDisputeEvidences: async (id: number): Promise<DisputeEvidenceDTO[]> => {
    const res = await apiClient.get<ApiResponse<DisputeEvidenceDTO[]>>(`/disputes/${id}/evidence`);
    return res.data.data;
  },

  getDisputeEvidenceById: async (id: number, evidenceId: number): Promise<DisputeEvidenceDTO> => {
    const res = await apiClient.get<ApiResponse<DisputeEvidenceDTO>>(`/disputes/${id}/evidence/${evidenceId}`);
    return res.data.data;
  },

  getDisputeHistory: async (id: number): Promise<DisputeAuditLogDTO[]> => {
    const res = await apiClient.get<ApiResponse<DisputeAuditLogDTO[]>>(`/disputes/${id}/history`);
    return res.data.data;
  },

  getDisputesForStaffReview: async (status?: DisputeStatus): Promise<DisputeDTO[]> => {
    const params = status ? { status } : {};
    const res = await apiClient.get<ApiResponse<DisputeDTO[]>>('/disputes/staff/review', { params });
    return res.data.data;
  },

  addMediationNotes: async (id: number, payload: AddMediationNotesPayload): Promise<DisputeDTO> => {
    const res = await apiClient.post<ApiResponse<DisputeDTO>>(`/disputes/${id}/mediation-notes`, payload);
    return res.data.data;
  },

  proposeResolution: async (id: number, payload: ProposeResolutionPayload): Promise<DisputeDTO> => {
    const res = await apiClient.post<ApiResponse<DisputeDTO>>(`/disputes/${id}/propose-resolution`, payload);
    return res.data.data;
  },

  arbitrateDispute: async (id: number, payload: AdminArbitratePayload): Promise<DisputeDTO> => {
    const body = {
      reason: payload.reason || payload.arbitrationVerdict || 'Administrator final binding arbitration',
      resolutionSummary: payload.resolutionSummary || payload.arbitrationVerdict || 'Dispute resolved by administrator',
      fundAdjustmentAmount: payload.fundAdjustmentAmount ?? payload.deductibleAmount,
      fundAdjustmentType: payload.fundAdjustmentType,
    };
    const res = await apiClient.post<ApiResponse<DisputeDTO>>(`/disputes/${id}/arbitrate`, body);
    return res.data.data;
  },

  getArbitrationDossier: async (id: number): Promise<DisputeArbitrationDossierDTO> => {
    const res = await apiClient.get<ApiResponse<DisputeArbitrationDossierDTO>>(`/disputes/${id}/arbitration-dossier`);
    return res.data.data;
  },

  arbitrateDisputeWithFundAdjustment: async (
    id: number,
    payload: DisputeFundAdjustmentPayload
  ): Promise<DisputeDTO> => {
    const body = {
      amount: payload.amount ?? payload.adjustmentAmount ?? 0,
      entryType: payload.entryType ?? (payload.adjustmentDirection === 'DEBIT_FUND' ? 'DEBIT' : 'CREDIT'),
      reason: payload.reason ?? payload.arbitrationVerdict ?? 'Arbitration fund adjustment',
      resolutionSummary: payload.resolutionSummary ?? payload.arbitrationVerdict ?? 'Dispute resolved with SharedFund adjustment',
    };
    const res = await apiClient.post<ApiResponse<DisputeDTO>>(`/disputes/${id}/fund-adjustment`, body);
    return res.data.data;
  },
};
