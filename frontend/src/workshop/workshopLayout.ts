import type { WorkshopCameraPreset, WorkshopStation } from './workshopTypes';

export const WORKSHOP_SECTOR_CENTER: [number, number, number] = [-40, 0, 40];

export const WORKSHOP_STATIONS: Record<
  WorkshopStation,
  {
    name: string;
    description: string;
    relativePosition: [number, number, number];
    worldPosition: [number, number, number];
  }
> = {
  HYDRAULIC_LIFT: {
    name: 'Dual-Column Hydraulic Lift',
    description: 'Telescoping 4-ton vehicle lift for undercarriage and powertrain maintenance',
    relativePosition: [0, 0, 0],
    worldPosition: [-40, 0, 40],
  },
  DIAGNOSTIC_CART: {
    name: 'OBD-II Diagnostic Bench Cart',
    description: 'Mobile telematics scanner trolley for fault code extraction & calibration',
    relativePosition: [-3.6, 0, 2.4],
    worldPosition: [-43.6, 0, 42.4],
  },
  PARTS_RACK: {
    name: 'Modular Parts Replacement Rack',
    description: 'Heavy inventory shelving with ceramic rotors, LiDAR modules, and battery cells',
    relativePosition: [3.8, 0, 2.4],
    worldPosition: [-36.2, 0, 42.4],
  },
  WORK_ORDER_STELA: {
    name: 'Service Status & Work Order Stela',
    description: 'Curved digital dispatch stela connected to backend ledger and vehicle release pipeline',
    relativePosition: [0, 0, -4.8],
    worldPosition: [-40, 0, 35.2],
  },
};

export const WORKSHOP_CAMERA_PRESETS: Record<string, WorkshopCameraPreset> = {
  WORKSHOP_OVERVIEW: {
    name: 'Workshop Overview',
    position: [-40, 8.5, 52],
    target: [-40, 1.2, 40],
  },
  HYDRAULIC_LIFT_FOCUS: {
    name: 'Hydraulic Lift',
    position: [-40, 2.8, 45.2],
    target: [-40, 1.4, 40],
  },
  DIAGNOSTIC_CART_FOCUS: {
    name: 'Diagnostic Cart',
    position: [-43.6, 2.2, 43.8],
    target: [-43.6, 1.3, 42.4],
  },
  PARTS_RACK_FOCUS: {
    name: 'Parts Rack',
    position: [-36.2, 2.2, 43.8],
    target: [-36.2, 1.4, 42.4],
  },
  WORK_ORDER_STELA_FOCUS: {
    name: 'Work Order Stela',
    position: [-40, 3.2, 38.8],
    target: [-40, 1.8, 35.2],
  },
  UNDERCARRIAGE_INSPECTION: {
    name: 'Undercarriage Inspection',
    position: [-40, 0.9, 41.8],
    target: [-40, 1.2, 40],
  },
};

export const WORKSHOP_THEME = {
  primary: '#38bdf8', // Electric cyan / steel blue
  secondary: '#f59e0b', // Industrial amber
  darkBase: '#09090b', // Deep metallic slate
  panelBg: '#18181b',
  steelMetal: '#27272a',
  hazardYellow: '#eab308',
  hazardBlack: '#18181b',
  alertRed: '#ef4444',
  successGreen: '#10b981',
  textLight: '#f8fafc',
  textMuted: '#94a3b8',
};
