import type { DisputeCameraPreset, DisputeStation } from './disputeTypes';

export const DISPUTE_SECTOR_CENTER: [number, number, number] = [-40, 0, 80];

export const DISPUTE_STATIONS: Record<
  DisputeStation,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
  }
> = {
  DISPUTE_CRYSTAL: {
    name: 'Floating Crimson Dispute Crystal',
    description: 'Levitating energy crystal reflecting active dispute severity, lifecycle state, and tension',
    relativePosition: [0, 2.2, 0],
    worldPosition: [-40, 2.2, 80],
  },
  DEFECT_HOLOTANK: {
    name: '3D Defect Coordinate Holotank',
    description: 'Holographic vehicle twin projection displaying spatial 3D damage pins and defect coordinates',
    relativePosition: [0, 0, -1.8],
    worldPosition: [-40, 0, 78.2],
  },
  EVIDENCE_CAROUSEL: {
    name: 'Immutable Evidence Staging Carousel',
    description: 'Evidence inspection panels displaying verified photos, uploader signatures, and timestamped audit receipts',
    relativePosition: [-3.6, 0, 1.8],
    worldPosition: [-43.6, 0, 81.8],
  },
  STAFF_CONSOLE: {
    name: 'Staff Mediation & Review Console',
    description: 'Mediation desk for platform staff to review disputes, record factual notes, and propose settlements',
    relativePosition: [3.6, 0, 1.8],
    worldPosition: [-36.4, 0, 81.8],
  },
  ADMIN_DAIS: {
    name: 'Admin Arbitration Dais of Finality',
    description: 'Sovereign dais for administrators to review complete dossiers and execute binding balance adjustments',
    relativePosition: [0, 0, 4.2],
    worldPosition: [-40, 0, 84.2],
  },
  STATUS_STELA: {
    name: 'Chronological Audit & Status Stela',
    description: 'Monolithic obsidian stela displaying immutable audit events and official resolution history',
    relativePosition: [0, 0, -5.2],
    worldPosition: [-40, 0, 74.8],
  },
};

export const DISPUTE_CAMERA_PRESETS: Record<string, DisputeCameraPreset> = {
  CHAMBER_OVERVIEW: {
    name: 'Chamber Overview',
    position: [-40, 8.5, 92],
    target: [-40, 1.4, 80],
  },
  CRYSTAL_FOCUS: {
    name: 'Dispute Crystal',
    position: [-40, 2.8, 83.8],
    target: [-40, 2.2, 80],
  },
  HOLOTANK_FOCUS: {
    name: 'Defect Holotank',
    position: [-40, 2.4, 76.5],
    target: [-40, 1.1, 78.2],
  },
  EVIDENCE_FOCUS: {
    name: 'Evidence Carousel',
    position: [-43.6, 2.4, 84.8],
    target: [-43.6, 1.4, 81.8],
  },
  STAFF_CONSOLE_FOCUS: {
    name: 'Staff Mediation Console',
    position: [-36.4, 2.4, 84.8],
    target: [-36.4, 1.4, 81.8],
  },
  ADMIN_DAIS_FOCUS: {
    name: 'Admin Arbitration Dais',
    position: [-40, 2.8, 87.8],
    target: [-40, 1.8, 84.2],
  },
};

export const DISPUTE_THEME = {
  primary: '#ff1744', // Vibrant arbitration crimson
  secondary: '#fbbf24', // Gold justice accent
  darkBase: '#1a0303', // Deep obsidian slate
  panelBg: '#2b0707',
  cyberCyan: '#38bdf8',
  alertRed: '#ef4444',
  verdictGreen: '#10b981',
  textLight: '#f8fafc',
  textMuted: '#cbd5e1',
};
