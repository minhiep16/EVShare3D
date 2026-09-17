import type { Vector3Tuple } from 'three';

export interface CameraPreset {
  position: Vector3Tuple;
  target: Vector3Tuple;
  fov: number;
}

export const VAULT_LAYOUT = {
  sectorCenter: [-40, 0, -80] as Vector3Tuple,
  floorRadius: 16.0,

  // Subsystem Station Positions (Relative to sector origin [0, 0, 0])
  terminalPosition: [0, 0, 4.5] as Vector3Tuple,
  liquidColumnPosition: [0, 0, -4.5] as Vector3Tuple,
  ribbonCenterPosition: [0, 2.2, 0.5] as Vector3Tuple,
  auditStelaPosition: [-4.8, 0, 2.0] as Vector3Tuple,
  quickDepositKioskPosition: [4.8, 0, 2.0] as Vector3Tuple,

  // Camera Presets (Relative to sector origin [0, 0, 0])
  cameras: {
    VAULT_OVERVIEW: {
      position: [0, 8.5, 13.5],
      target: [0, 1.5, 0.5],
      fov: 42,
    } as CameraPreset,

    TERMINAL_FOCUS: {
      position: [0, 1.9, 6.8],
      target: [0, 1.35, 4.5],
      fov: 38,
    } as CameraPreset,

    LIQUID_COLUMN_FOCUS: {
      position: [0, 4.0, 2.2],
      target: [0, 2.6, -4.5],
      fov: 40,
    } as CameraPreset,

    RIBBON_FOCUS: {
      position: [0, 3.2, 5.0],
      target: [0, 2.2, 0.5],
      fov: 42,
    } as CameraPreset,

    AUDIT_FOCUS: {
      position: [-4.8, 2.4, 5.8],
      target: [-4.8, 1.8, 2.0],
      fov: 38,
    } as CameraPreset,
  },

  // Cybernetic Gold & Titanium Palette
  theme: {
    bgDark: '#040912',
    titaniumDark: '#070f1a',
    titaniumLight: '#162338',
    goldPrimary: '#ffb300',
    goldGlow: 'rgba(255, 179, 0, 0.35)',
    amberAccent: '#ff8f00',
    solvencyGreen: '#00e676',
    dangerRed: '#ff1744',
    textWhite: '#f0f4fc',
    textMuted: '#8a94a6',
  },
};
