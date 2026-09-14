import * as THREE from 'three';

export type PerformanceTier = 'HIGH' | 'MEDIUM' | 'LOW';

export interface PerformanceProfile {
  tier: PerformanceTier;
  dprRange: [number, number];
  targetFps: number;
  shadowsEnabled: boolean;
  shadowMapSize: number;
  shadowType: THREE.ShadowMapType;
  antialias: boolean;
  maxTextureResolution: number;
  maxAnisotropy: number;
  lodBias: number;
  enablePostProcessing: boolean;
  powerPreference: WebGLPowerPreference;
}

export interface FrameRateSample {
  timestamp: number;
  fps: number;
}
