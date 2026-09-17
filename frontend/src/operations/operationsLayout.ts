import type { OperationsCameraPreset, OperationsStation } from './operationsTypes';

export const OPERATIONS_SECTOR_CENTER: [number, number, number] = [-40, 0, 0];

export const OPERATIONS_STATIONS: Record<
  OperationsStation,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
  }
> = {
  QR_DESK: {
    name: 'QR Scanner Station',
    description: 'Optical QR scanner kiosk for check-in validation & token issuance',
    relativePosition: [-3.4, 0, 1.2],
    worldPosition: [-43.4, 0, 1.2],
  },
  DISPATCH_CONSOLE: {
    name: 'Dispatch & Operations Console',
    description: 'Vehicle telematics, odometer/battery log, and trip check-in/out engine',
    relativePosition: [0, 0, 2.5],
    worldPosition: [-40, 0, 2.5],
  },
  FLEET_STELA: {
    name: 'Fleet Status Stela',
    description: 'Curved digital twin telematics stela for multi-bay fleet monitoring',
    relativePosition: [0, 0, -4.8],
    worldPosition: [-40, 0, -4.8],
  },
  NOTIFICATION_BOARD: {
    name: 'Operational Alerts Hologram',
    description: 'Real-time incident dispatch, telemetry alerts, and bay notifications',
    relativePosition: [3.6, 0, 1.2],
    worldPosition: [-36.4, 0, 1.2],
  },
  INSPECTION_BAY: {
    name: 'Chassis Inspection Bay',
    description: 'Vehicle physical staging pad with laser LiDAR scan & condition verification',
    relativePosition: [0, 0, -1.2],
    worldPosition: [-40, 0, -1.2],
  },
};

export const OPERATIONS_CAMERA_PRESETS: Record<string, OperationsCameraPreset> = {
  HANGAR_OVERVIEW: {
    name: 'Hangar Overview',
    position: [-40, 8.5, 12],
    target: [-40, 1.2, 0],
  },
  QR_STATION_FOCUS: {
    name: 'QR Scanner Station',
    position: [-43.4, 2.4, 4.4],
    target: [-43.4, 1.3, 1.2],
  },
  DISPATCH_CONSOLE_FOCUS: {
    name: 'Dispatch Console',
    position: [-40, 2.8, 5.8],
    target: [-40, 1.2, 2.5],
  },
  FLEET_STATUS_FOCUS: {
    name: 'Fleet Status Matrix',
    position: [-40, 3.2, -1.2],
    target: [-40, 1.8, -4.8],
  },
  NOTIFICATION_BOARD_FOCUS: {
    name: 'Notification Board',
    position: [-36.4, 2.4, 4.4],
    target: [-36.4, 1.4, 1.2],
  },
  INSPECTION_BAY_FOCUS: {
    name: 'Inspection Bay',
    position: [-40, 4.2, 2.2],
    target: [-40, 0.5, -1.2],
  },
};

export const OPERATIONS_THEME = {
  primary: '#f97316', // Operations Orange
  secondary: '#fbbf24', // Amber Caution
  darkBase: '#080b12', // Deep Industrial Slate
  panelBg: '#0f172a',
  hazardYellow: '#eab308',
  hazardBlack: '#111827',
  cyberCyan: '#06b6d4',
  alertRed: '#ef4444',
  successGreen: '#10b981',
  textLight: '#f8fafc',
  textMuted: '#94a3b8',
};

export const SERVICE_BAYS = [
  { id: 'BAY_01', name: 'BAY 01 — FAST CHARGE', offset: [-4.2, 0, -1.2] as [number, number, number], defaultVehicleId: 1 },
  { id: 'BAY_02', name: 'BAY 02 — INSPECTION STAGING', offset: [0, 0, -1.2] as [number, number, number], defaultVehicleId: 2 },
  { id: 'BAY_03', name: 'BAY 03 — DISPATCH READY', offset: [4.2, 0, -1.2] as [number, number, number], defaultVehicleId: 3 },
];
