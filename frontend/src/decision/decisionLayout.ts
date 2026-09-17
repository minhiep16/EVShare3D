import type { DecisionCameraPreset } from './decisionTypes';

export const DECISION_LAYOUT = {
  // Sector center offset
  sectorCenter: [40, 0, -40] as [number, number, number],

  // Station positions relative to sector origin [0, 0, 0]
  votingTerminalPosition: [0, 0, 1.2] as [number, number, number],
  proposalPodClusterPosition: [0, 0, -3.2] as [number, number, number],
  equityPillarsPosition: [-4.5, 0, -0.5] as [number, number, number],
  quorumColumnPosition: [4.5, 0, -0.5] as [number, number, number],
  resultStelaPosition: [0, 0, -6.5] as [number, number, number],

  // Arena floor dimensions
  arenaRadius: 11,
  podArcRadius: 3.8,

  // Theme palette
  colors: {
    arenaDark: '#080a14',
    arenaGrid: '#1e1b4b',
    indigoPrimary: '#4f46e5',
    indigoLight: '#818cf8',
    violetNeon: '#a855f7',
    violetGlow: '#c084fc',
    cyanQuorum: '#06b6d4',
    cyanGlow: '#67e8f9',
    goldThreshold: '#fbbf24',
    approveEmerald: '#10b981',
    rejectRuby: '#ef4444',
    abstainAmber: '#f59e0b',
    stelaSlate: '#0f172a',
    textWhite: '#f8fafc',
    textMuted: '#94a3b8',
  },

  cameraPresets: {
    ARENA_OVERVIEW: {
      name: 'Arena Overview',
      position: [0, 6.8, 9.8],
      target: [0, 1.2, -1.8],
    } as DecisionCameraPreset,
    TERMINAL_FOCUS: {
      name: 'Voting Terminal',
      position: [0, 2.3, 3.6],
      target: [0, 1.1, 1.2],
    } as DecisionCameraPreset,
    PROPOSAL_PODS_FOCUS: {
      name: 'Proposal Deliberation',
      position: [0, 3.2, 0.8],
      target: [0, 1.5, -3.2],
    } as DecisionCameraPreset,
    EQUITY_FOCUS: {
      name: 'Member Equity Weight',
      position: [-2.8, 3.0, 3.0],
      target: [-4.5, 1.4, -0.5],
    } as DecisionCameraPreset,
    QUORUM_RESULTS_FOCUS: {
      name: 'Quorum Threshold',
      position: [2.8, 3.0, 3.0],
      target: [4.5, 1.4, -0.5],
    } as DecisionCameraPreset,
    STELA_FOCUS: {
      name: 'Authoritative Verdict',
      position: [0, 3.2, -2.2],
      target: [0, 2.2, -6.5],
    } as DecisionCameraPreset,
  },
};
