import { CameraMode } from '@/stores/types';

export interface CameraTransform {
  position: [number, number, number];
  target: [number, number, number];
  fov: number;
}

export interface CinematicKeyframe {
  position: [number, number, number];
  target: [number, number, number];
  fov?: number;
  durationSeconds: number;
}

export interface FocusOptions {
  distance?: number;
  elevation?: number;
  azimuthAngle?: number;
  fov?: number;
  immediate?: boolean;
  speed?: number;
}

export interface MoveOptions {
  fov?: number;
  immediate?: boolean;
  speed?: number;
}

export interface FirstPersonConfig {
  eyeHeight: number;
  mouseSensitivity: number;
}

export interface ThirdPersonConfig {
  distance: number;
  height: number;
  damping: number;
  targetOffset: [number, number, number];
}
