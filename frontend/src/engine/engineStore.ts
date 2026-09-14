import * as THREE from 'three';
import { create } from 'zustand';
import { EngineConfig, EngineQuality } from './types';

const QUALITY_PRESETS: Record<EngineQuality, EngineConfig> = {
  LOW: {
    quality: 'LOW',
    shadows: false,
    shadowMapSize: 512,
    antialias: false,
    dpr: [1, 1],
    toneMapping: THREE.ACESFilmicToneMapping,
    toneMappingExposure: 1.0,
    powerPreference: 'default',
  },
  MEDIUM: {
    quality: 'MEDIUM',
    shadows: true,
    shadowMapSize: 1024,
    antialias: true,
    dpr: [1, 1.5],
    toneMapping: THREE.ACESFilmicToneMapping,
    toneMappingExposure: 1.0,
    powerPreference: 'high-performance',
  },
  HIGH: {
    quality: 'HIGH',
    shadows: true,
    shadowMapSize: 2048,
    antialias: true,
    dpr: [1, 2],
    toneMapping: THREE.ACESFilmicToneMapping,
    toneMappingExposure: 1.05,
    powerPreference: 'high-performance',
  },
  ULTRA: {
    quality: 'ULTRA',
    shadows: true,
    shadowMapSize: 4096,
    antialias: true,
    dpr: [1, 2],
    toneMapping: THREE.ACESFilmicToneMapping,
    toneMappingExposure: 1.1,
    powerPreference: 'high-performance',
  },
};

interface EngineState {
  quality: EngineQuality;
  isReady: boolean;
  webglSupported: boolean | null;
  fps: number;
  canvasDimensions: { width: number; height: number; aspect: number };

  // Actions
  setQuality: (quality: EngineQuality) => void;
  setReady: (ready: boolean) => void;
  setWebglSupported: (supported: boolean) => void;
  setFps: (fps: number) => void;
  setDimensions: (width: number, height: number) => void;
  getConfig: () => EngineConfig;
}

const initialQuality: EngineQuality =
  (import.meta.env.VITE_DEFAULT_QUALITY as EngineQuality) || 'HIGH';

export const useEngineStore = create<EngineState>((set, get) => ({
  quality: initialQuality in QUALITY_PRESETS ? initialQuality : 'HIGH',
  isReady: false,
  webglSupported: null,
  fps: 60,
  canvasDimensions: {
    width: typeof window !== 'undefined' ? window.innerWidth : 1920,
    height: typeof window !== 'undefined' ? window.innerHeight : 1080,
    aspect: typeof window !== 'undefined' ? window.innerWidth / window.innerHeight : 16 / 9,
  },

  setQuality: (quality) => set({ quality }),
  setReady: (isReady) => set({ isReady }),
  setWebglSupported: (webglSupported) => set({ webglSupported }),
  setFps: (fps) => set({ fps }),
  setDimensions: (width, height) =>
    set({
      canvasDimensions: {
        width,
        height,
        aspect: height > 0 ? width / height : 1,
      },
    }),
  getConfig: () => QUALITY_PRESETS[get().quality],
}));
