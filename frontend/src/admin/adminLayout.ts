import type { AdminCoreId, AdminCameraPreset } from './adminTypes';

export const ADMIN_SECTOR_CENTER: [number, number, number] = [0, 25, 0];

export const ADMIN_CORES_CONFIG: Record<
  AdminCoreId,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
    primaryColor: string;
    accentColor: string;
  }
> = {
  USER_CORE: {
    name: 'User Identity & Biometric Core',
    description: 'Autonomous KYC verification, role privilege governance, and account lock interlocks',
    relativePosition: [0, 0, -5.4],
    worldPosition: [0, 25, -5.4],
    primaryColor: '#38bdf8',
    accentColor: '#0284c7',
  },
  VEHICLE_CORE: {
    name: 'Fleet Telematics & Lockdown Core',
    description: 'Real-time vehicle telemetry matrix, diagnostic health status, and remote lockdown',
    relativePosition: [4.8, 0, -2.6],
    worldPosition: [4.8, 25, -2.6],
    primaryColor: '#06b6d4',
    accentColor: '#0891b2',
  },
  OWNERSHIP_CORE: {
    name: 'Syndicate Equity & Governance Core',
    description: 'Cap table cryptographic verification, equity pool balances, and transfer freezes',
    relativePosition: [4.8, 0, 2.6],
    worldPosition: [4.8, 25, 2.6],
    primaryColor: '#a855f7',
    accentColor: '#9333ea',
  },
  BOOKING_CORE: {
    name: 'Chrono-Spatial Booking Core',
    description: 'Autonomous scheduling conflict resolution, priority preemption, and expired hold purges',
    relativePosition: [0, 0, 5.4],
    worldPosition: [0, 25, 5.4],
    primaryColor: '#f59e0b',
    accentColor: '#d97706',
  },
  FINANCE_CORE: {
    name: 'SharedFund Treasury & Liquidity Core',
    description: 'Vault reserve balances, liquidity injections, audit ledger hashes, and disbursement freeze',
    relativePosition: [-4.8, 0, 2.6],
    worldPosition: [-4.8, 25, 2.6],
    primaryColor: '#10b981',
    accentColor: '#059669',
  },
  DISPUTE_CORE: {
    name: 'Arbitration Docket & Summary Core',
    description: 'Binding arbitration docket, summary dispute enforcement, and compensatory credits',
    relativePosition: [-4.8, 0, -2.6],
    worldPosition: [-4.8, 25, -2.6],
    primaryColor: '#ff1744',
    accentColor: '#dc2626',
  },
  SYSTEM_CORE: {
    name: 'Omni-Command Zenith System Core',
    description: 'Master Metaverse node health, shader throughput, and global platform lockdown',
    relativePosition: [0, 1.8, 0],
    worldPosition: [0, 26.8, 0],
    primaryColor: '#e0f2fe',
    accentColor: '#38bdf8',
  },
};

export const ADMIN_CAMERA_PRESETS: Record<string, AdminCameraPreset> = {
  ORBITAL_OVERVIEW: {
    name: 'Orbital Overview',
    position: [0, 36, 18],
    target: [0, 26.5, 0],
  },
  USER_CORE_FOCUS: {
    name: 'User Core',
    position: [0, 27, -2.2],
    target: [0, 26.2, -5.4],
  },
  VEHICLE_CORE_FOCUS: {
    name: 'Vehicle Core',
    position: [2.2, 27, -1.2],
    target: [4.8, 26.2, -2.6],
  },
  OWNERSHIP_CORE_FOCUS: {
    name: 'Ownership Core',
    position: [2.2, 27, 1.2],
    target: [4.8, 26.2, 2.6],
  },
  BOOKING_CORE_FOCUS: {
    name: 'Booking Core',
    position: [0, 27, 2.2],
    target: [0, 26.2, 5.4],
  },
  FINANCE_CORE_FOCUS: {
    name: 'Finance Core',
    position: [-2.2, 27, 1.2],
    target: [-4.8, 26.2, 2.6],
  },
  DISPUTE_CORE_FOCUS: {
    name: 'Dispute Core',
    position: [-2.2, 27, -1.2],
    target: [-4.8, 26.2, -2.6],
  },
  SYSTEM_CORE_FOCUS: {
    name: 'Zenith System Core',
    position: [0, 27.2, 4.2],
    target: [0, 26.8, 0],
  },
};

export const COMMAND_THEME = {
  primary: '#38bdf8',
  accentGold: '#fbbf24',
  alertRed: '#ff1744',
  successGreen: '#10b981',
  royalPurple: '#c084fc',
  darkObsidian: '#020617',
  deckGlass: '#030712',
  laserGrid: '#1e3a8a',
  textLight: '#f8fafc',
  textMuted: '#94a3b8',
};
