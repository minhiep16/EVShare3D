import * as THREE from 'three';

export type EngineQuality = 'LOW' | 'MEDIUM' | 'HIGH' | 'ULTRA';

export interface EngineConfig {
  quality: EngineQuality;
  shadows: boolean;
  shadowMapSize: number;
  antialias: boolean;
  dpr: [number, number];
  toneMapping: THREE.ToneMapping;
  toneMappingExposure: number;
  powerPreference: WebGLPowerPreference;
}

export interface CameraConfig {
  fov: number;
  near: number;
  far: number;
  position: [number, number, number];
  target: [number, number, number];
  enableControls?: boolean;
  minDistance?: number;
  maxDistance?: number;
  maxPolarAngle?: number;
}

export interface LightingConfig {
  ambientColor: string;
  ambientIntensity: number;
  keyLightColor: string;
  keyLightIntensity: number;
  keyLightPosition: [number, number, number];
  fillLightColor: string;
  fillLightIntensity: number;
  fillLightPosition: [number, number, number];
  rimLightColor?: string;
  rimLightIntensity?: number;
  rimLightPosition?: [number, number, number];
  castShadows?: boolean;
}
