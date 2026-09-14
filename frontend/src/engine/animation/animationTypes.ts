/**
 * Spatial Animation System Types
 * Standardized 3D state animations (hover, selection, activation, loading, success, error)
 * with strict lifecycle management to prevent uncontrolled loops.
 */

export type AnimationState =
  | 'IDLE'
  | 'HOVER'
  | 'SELECTED'
  | 'ACTIVE'
  | 'LOADING'
  | 'SUCCESS'
  | 'ERROR';

export interface SpatialTransform3D {
  positionOffset: [number, number, number];
  rotationOffset: [number, number, number];
  scale: [number, number, number];
  emissiveIntensity: number;
  colorHex?: string;
  opacity: number;
}

export interface AnimationConfig {
  id: string;
  state: AnimationState;
  basePosition?: [number, number, number];
  baseRotation?: [number, number, number];
  baseScale?: [number, number, number];
  baseColor?: string;
  hoverElevation?: number;
  hoverScale?: number;
  errorShakeIntensity?: number;
  onAnimationComplete?: (state: AnimationState) => void;
}

export interface ActiveAnimationTrack {
  id: string;
  state: AnimationState;
  elapsedMs: number;
  durationMs: number;
  isLooping: boolean;
  isFinished: boolean;
}
