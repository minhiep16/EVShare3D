import type { Vector3Tuple } from 'three';

export interface CameraPreset {
  position: Vector3Tuple;
  target: Vector3Tuple;
  fov: number;
}

export const FINANCE_LAYOUT = {
  sectorCenter: [0, 0, -80] as Vector3Tuple,
  floorRadius: 16.0,

  // Subsystem Stations
  terminalPosition: [0, 0, -75.0] as Vector3Tuple,
  expenseClusterPosition: [-5.5, 0, -78.0] as Vector3Tuple,
  allocationBarPosition: [5.5, 0, -78.0] as Vector3Tuple,
  liquidReservePosition: [0, 0, -84.0] as Vector3Tuple,
  paymentKioskPosition: [3.4, 0, -73.2] as Vector3Tuple,

  // Camera Presets
  cameras: {
    FINANCE_OVERVIEW: {
      position: [0, 9.5, -66.0],
      target: [0, 1.5, -79.0],
      fov: 42,
    } as CameraPreset,

    TERMINAL_FOCUS: {
      position: [0, 2.0, -72.6],
      target: [0, 1.35, -75.0],
      fov: 38,
    } as CameraPreset,

    EXPENSE_CLUSTER: {
      position: [-5.5, 3.4, -73.2],
      target: [-5.5, 1.4, -78.0],
      fov: 40,
    } as CameraPreset,

    ALLOCATION_FOCUS: {
      position: [5.5, 3.8, -73.0],
      target: [5.5, 1.5, -78.0],
      fov: 40,
    } as CameraPreset,

    PAYMENT_KIOSK_FOCUS: {
      position: [3.4, 2.0, -70.8],
      target: [3.4, 1.32, -73.2],
      fov: 36,
    } as CameraPreset,

    VAULT_COLUMN_FOCUS: {
      position: [0, 4.2, -78.2],
      target: [0, 2.8, -84.0],
      fov: 40,
    } as CameraPreset,
  },

  // Surgical Financial Palette
  theme: {
    bgDark: '#030813',
    glassBg: 'rgba(3, 14, 28, 0.88)',
    emeraldPrimary: '#00e676',
    emeraldGlow: 'rgba(0, 230, 118, 0.35)',
    cyanAccent: '#00e5ff',
    amberReserve: '#ffab00',
    amberGlow: 'rgba(255, 171, 0, 0.3)',
    dangerRed: '#ff1744',
    textWhite: '#f0f4fc',
    textMuted: '#8a94a6',
  },
};
