import type { Vector3Tuple } from 'three';

export interface CameraPreset {
  position: Vector3Tuple;
  target: Vector3Tuple;
  fov: number;
}

export const CONTRACT_LAYOUT = {
  sectorCenter: [40, 0, -80] as Vector3Tuple,
  floorRadius: 16.0,

  // Subsystem Station Positions (Relative to sector center [0, 0, 0])
  documentLecternPosition: [0, 0, -3.2] as Vector3Tuple,
  signaturePedestalPosition: [4.2, 0, 1.2] as Vector3Tuple,
  versionStelaPosition: [-4.2, 0, 0.8] as Vector3Tuple,
  statusSealPosition: [0, 3.2, -3.8] as Vector3Tuple,
  quickActionsConsolePosition: [0, 0, 4.2] as Vector3Tuple,

  // Camera Presets
  cameras: {
    ROOM_OVERVIEW: {
      position: [0, 8.5, 13.5],
      target: [0, 1.5, 0.5],
      fov: 42,
    } as CameraPreset,

    DOCUMENT_FOCUS: {
      position: [0, 2.2, -0.6],
      target: [0, 1.45, -3.2],
      fov: 38,
    } as CameraPreset,

    SIGNATURE_FOCUS: {
      position: [4.2, 2.2, 3.8],
      target: [4.2, 1.35, 1.2],
      fov: 38,
    } as CameraPreset,

    VERSION_FOCUS: {
      position: [-4.2, 2.4, 3.6],
      target: [-4.2, 1.8, 0.8],
      fov: 38,
    } as CameraPreset,

    STATUS_FOCUS: {
      position: [0, 3.6, -0.8],
      target: [0, 2.8, -3.8],
      fov: 40,
    } as CameraPreset,
  },

  // Executive Slate Theme Palette
  theme: {
    slateDark: '#040813',
    slateSurface: '#091224',
    slateCard: '#0f1d38',
    sapphirePrimary: '#3b82f6',
    sapphireGlow: 'rgba(59, 130, 246, 0.4)',
    ceruleanNeon: '#60a5fa',
    platinumLegal: '#f1f5f9',
    textMuted: '#94a3b8',
    sealGreen: '#00e676',
    warningAmber: '#ffb300',
    dangerRed: '#ff1744',
    watermarkCyan: 'rgba(56, 189, 248, 0.25)',
  },
};
