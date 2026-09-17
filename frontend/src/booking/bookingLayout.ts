import type { Vector3Tuple } from 'three';
import type { BookingChamberThemeColors } from './bookingTypes';

export const BOOKING_CHAMBER_THEME: BookingChamberThemeColors = {
  CHRONO_CYAN: '#00e5ff',
  CHRONO_CYAN_DIM: 'rgba(0, 229, 255, 0.2)',
  CHRONO_CYAN_GLOW: '#38bdf8',
  BUFFER_PURPLE: '#c084fc',
  OCCUPIED_AMBER: '#f59e0b',
  AVAILABLE_GREEN: '#10b981',
  CONFLICT_RED: '#ef4444',
  DARK_OBSIDIAN: '#060913',
  PANEL_BG: '#0a1222',
  WIRE_FRAME: 'rgba(0, 229, 255, 0.35)',
};

export const BOOKING_CHAMBER_LAYOUT = {
  CHAMBER_RADIUS: 14.0,
  TURNTABLE_RADIUS: 4.2,
  TURNTABLE_HEIGHT: 0.25,

  // Key architectural positions relative to BOOKING_CHAMBER center [0, 0, -40]
  POSITIONS: {
    TURNTABLE: [0, 0, 0] as Vector3Tuple,
    VEHICLE: [0, 0.26, 0] as Vector3Tuple,
    TERMINAL: [0, 0, 3.0] as Vector3Tuple,
    CALENDAR_ARC_CENTER: [0, 1.9, 0] as Vector3Tuple,
    TIMELINE_RIBBON: [0, 1.45, 2.9] as Vector3Tuple,
    FAIR_USAGE_PILLAR: [-3.8, 0, 1.8] as Vector3Tuple,
    SECTOR_PORTAL_EXIT: [0, 0, 8.5] as Vector3Tuple,
  },

  CAMERA_PRESETS: {
    CHAMBER_OVERVIEW: {
      position: [0, 5.8, 9.8] as Vector3Tuple,
      target: [0, 1.2, 0] as Vector3Tuple,
      fov: 42,
    },
    TERMINAL_FOCUS: {
      position: [0, 1.75, 4.8] as Vector3Tuple,
      target: [0, 1.45, 3.0] as Vector3Tuple,
      fov: 36,
    },
    CALENDAR_FOCUS: {
      position: [0, 2.2, 5.4] as Vector3Tuple,
      target: [0, 2.0, 1.5] as Vector3Tuple,
      fov: 40,
    },
    VEHICLE_INSPECTION: {
      position: [3.8, 2.2, 3.5] as Vector3Tuple,
      target: [0, 0.8, 0] as Vector3Tuple,
      fov: 42,
    },
  },
};
