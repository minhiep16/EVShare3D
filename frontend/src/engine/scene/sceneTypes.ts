import React from 'react';
import { LightingProfile } from '@/stores/useWorldStore';

export type SceneLifecycleState =
  | 'UNLOADED'
  | 'INITIALIZING'
  | 'ACTIVE'
  | 'EXITING'
  | 'DISPOSED';

export interface SceneEnvironmentConfig {
  lightingProfile: LightingProfile;
  backgroundColor: string;
  fogColor: string;
  fogNear: number;
  fogFar: number;
  ambientIntensity: number;
  ambientColor?: string;
  keyLightIntensity: number;
  keyLightColor?: string;
  keyLightPosition: [number, number, number];
  fillColor: string;
  fillIntensity: number;
  fillPosition?: [number, number, number];
  rimColor?: string;
  rimIntensity?: number;
  rimPosition?: [number, number, number];
  enableGroundGrid?: boolean;
}

export interface SceneCameraConfig {
  position: [number, number, number];
  target: [number, number, number];
  fov: number;
  minDistance?: number;
  maxDistance?: number;
}

export interface SceneDefinition {
  id: string;
  name: string;
  description: string;
  environment: SceneEnvironmentConfig;
  camera: SceneCameraConfig;
  Component: React.ComponentType;
  onEnter?: () => Promise<void> | void;
  onExit?: () => Promise<void> | void;
}
