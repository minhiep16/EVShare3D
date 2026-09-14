import { CameraMode } from '@/stores/types';
import { CameraTransform } from '../camera/cameraTypes';

export type FocusCategory = 'VEHICLE' | 'TERMINAL' | 'PORTAL' | 'OBJECT';

export interface FocusPreset {
  distance: number;
  elevation: number;
  azimuth?: number;
  fov?: number;
  speed?: number;
  minOrbitDistance?: number;
  maxOrbitDistance?: number;
}

export interface FocusTargetConfig {
  id: string;
  name: string;
  category: FocusCategory;
  targetPosition: [number, number, number];
  boundingRadius?: number;
  preset?: Partial<FocusPreset>;
  customOffset?: [number, number, number];
  onFocusEnter?: () => void;
  onFocusExit?: () => void;
}

export interface SavedCameraState {
  transform: CameraTransform;
  mode: CameraMode;
}
