import type { ProposalDTO, ProposalTallyDTO, ProposalResultsDTO, VoteDTO, VoteOptionKey } from '../api/proposalsApi';

export type DecisionTab = 'OVERVIEW' | 'BALLOT' | 'EQUITY' | 'QUORUM' | 'RESULTS';

export type DecisionChamberStation =
  | 'POD_CLUSTER'
  | 'VOTING_TERMINAL'
  | 'EQUITY_PILLARS'
  | 'QUORUM_COLUMN'
  | 'RESULT_STELA';

export interface SyndicateMemberEquity {
  userId: number;
  memberName: string;
  equityPercentage: number;
  role: string;
  avatarColor: string;
  hasVoted: boolean;
  voteChoice?: VoteOptionKey;
  votedAt?: string;
}

export interface DecisionCameraPreset {
  name: string;
  position: [number, number, number];
  target: [number, number, number];
}

export interface DecisionState {
  // Proposals
  proposals: ProposalDTO[];
  selectedProposalId: number | null;
  activeProposal: ProposalDTO | null;

  // Backend authoritative governance state
  tally: ProposalTallyDTO | null;
  results: ProposalResultsDTO | null;
  myVote: VoteDTO | null;

  // Syndicate co-owners equity distribution
  members: SyndicateMemberEquity[];

  // Voting interaction state
  selectedChoice: VoteOptionKey | null;
  isSubmittingVote: boolean;
  voteError: string | null;
  lastCastVote: VoteDTO | null;

  // UI / Navigation
  activeTab: DecisionTab;
  activeStation: DecisionChamberStation;
  isLoading: boolean;
  errorMessage: string | null;
}
