import { apiClient } from './apiClient';
import type { ApiResponse } from './vehiclesApi';

export type ProposalType =
  | 'ROUTINE_EXPENSE'
  | 'MAJOR_MAINTENANCE'
  | 'AMENDMENT'
  | 'ASSET_UPGRADE'
  | 'OTHER';

export type ProposalStatus = 'ACTIVE' | 'PASSED' | 'REJECTED' | 'EXPIRED';

export type VoteOptionKey = 'APPROVE' | 'REJECT' | 'ABSTAIN';

export interface VoteOptionDTO {
  id: number;
  optionKey: VoteOptionKey;
  label: string;
  description?: string;
}

export interface ProposalDTO {
  id: number;
  groupId: number;
  groupName?: string;
  proposerUserId?: number;
  proposerFullName?: string;
  proposerEmail?: string;
  title: string;
  description: string;
  proposalType: ProposalType;
  status: ProposalStatus;
  votingDeadline: string;
  createdAt: string;
  options: VoteOptionDTO[];
}

export interface VoteDTO {
  id: number;
  proposalId: number;
  userId: number;
  voterName: string;
  voteOptionId: number;
  optionKey: VoteOptionKey;
  optionLabel: string;
  equityWeight: number;
  votedAt: string;
}

export interface ProposalTallyDTO {
  proposalId: number;
  proposalTitle: string;
  proposalType: ProposalType;
  status: ProposalStatus;
  totalGroupActiveEquity: number;
  totalParticipatingEquity: number;
  participationRatePercentage: number;
  totalVotersCount: number;
  quorumPercentage: number;
  quorumReached: boolean;
  approveEquity: number;
  rejectEquity: number;
  abstainEquity: number;
  approvePercentageOfParticipating: number;
  rejectPercentageOfParticipating: number;
  abstainPercentageOfParticipating: number;
  approvePercentageOfTotal: number;
  requiredThresholdPercentage: number;
  thresholdType: string;
  passed: boolean;
  outcomeReason: string;
  ballots: VoteDTO[];
}

export interface ProposalResultsDTO {
  proposalId: number;
  proposalTitle: string;
  proposalType: ProposalType;
  status: ProposalStatus;
  totalEligibleEquity: number;
  participatingEquity: number;
  approveWeight: number;
  rejectWeight: number;
  abstainWeight: number;
  quorumStatus: string;
  quorumReached: boolean;
  quorumPercentage: number;
  participationRatePercentage: number;
  threshold: number;
  thresholdType: string;
  thresholdDescription: string;
  finalDecision: string;
  passed: boolean;
  decisionReason: string;
}

export interface CastVotePayload {
  optionKey: VoteOptionKey;
  notes?: string;
}

export interface CreateProposalPayload {
  groupId: number;
  title: string;
  description: string;
  proposalType: ProposalType;
  votingDeadline?: string;
}

export interface ProposerEligibilityDTO {
  groupId: number;
  userId?: number;
  isGroupMember: boolean;
  isActiveCoOwner: boolean;
  activeEquityPercentage: number;
  minimumRequiredPercentage: number;
  eligible: boolean;
  statusReason: string;
}

export interface ProposalAuditLogDTO {
  id: number;
  actorUserId?: number;
  actorName?: string;
  action: string;
  oldStateJson?: string;
  newStateJson?: string;
  createdAt: string;
}

export const proposalsApi = {
  getProposalsByGroupId: async (groupId: number, status?: ProposalStatus): Promise<ProposalDTO[]> => {
    const params = status ? { status } : {};
    const res = await apiClient.get<ApiResponse<ProposalDTO[]>>(`/proposals/group/${groupId}`, { params });
    return res.data.data;
  },

  getProposalById: async (id: number): Promise<ProposalDTO> => {
    const res = await apiClient.get<ApiResponse<ProposalDTO>>(`/proposals/${id}`);
    return res.data.data;
  },

  createProposal: async (payload: CreateProposalPayload): Promise<ProposalDTO> => {
    const res = await apiClient.post<ApiResponse<ProposalDTO>>('/proposals', payload);
    return res.data.data;
  },

  castVote: async (proposalId: number, payload: CastVotePayload): Promise<VoteDTO> => {
    const res = await apiClient.post<ApiResponse<VoteDTO>>(`/proposals/${proposalId}/votes`, payload);
    return res.data.data;
  },

  getVotesByProposalId: async (proposalId: number): Promise<VoteDTO[]> => {
    const res = await apiClient.get<ApiResponse<VoteDTO[]>>(`/proposals/${proposalId}/votes`);
    return res.data.data;
  },

  getMyVote: async (proposalId: number): Promise<VoteDTO | null> => {
    const res = await apiClient.get<ApiResponse<VoteDTO | null>>(`/proposals/${proposalId}/votes/my-vote`);
    return res.data.data;
  },

  getProposalTally: async (proposalId: number): Promise<ProposalTallyDTO> => {
    const res = await apiClient.get<ApiResponse<ProposalTallyDTO>>(`/proposals/${proposalId}/tally`);
    return res.data.data;
  },

  getProposalResults: async (proposalId: number): Promise<ProposalResultsDTO> => {
    const res = await apiClient.get<ApiResponse<ProposalResultsDTO>>(`/proposals/${proposalId}/results`);
    return res.data.data;
  },

  checkEligibility: async (groupId: number): Promise<ProposerEligibilityDTO> => {
    const res = await apiClient.get<ApiResponse<ProposerEligibilityDTO>>(`/proposals/group/${groupId}/eligibility`);
    return res.data.data;
  },

  getProposalHistory: async (proposalId: number): Promise<ProposalAuditLogDTO[]> => {
    const res = await apiClient.get<ApiResponse<ProposalAuditLogDTO[]>>(`/proposals/${proposalId}/history`);
    return res.data.data;
  },
};
