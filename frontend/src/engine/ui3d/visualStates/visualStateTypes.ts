import { UI3DColorVariant } from '../ui3dTypes';

/**
 * 8 Standard 3D Visual States
 * Shared across all WebGL 3D UI components and interactive scene meshes.
 */
export type VisualState =
  | 'IDLE'
  | 'HOVER'
  | 'ACTIVE'
  | 'SELECTED'
  | 'DISABLED'
  | 'LOADING'
  | 'SUCCESS'
  | 'ERROR';

export interface VisualStateParams {
  state: VisualState;
  primaryColor: string;
  backgroundColor: string;
  borderColor: string;
  textColor: string;
  emissiveColor: string;
  emissiveIntensity: number;
  scale: number;
  elevationZ: number;
  opacity: number;
  cursor: 'POINTER' | 'DEFAULT' | 'NOT_ALLOWED' | 'WAIT';
  isInteractive: boolean;
}

export interface VisualStateInput {
  disabled?: boolean;
  loading?: boolean;
  isSelected?: boolean;
  isPressed?: boolean;
  isHovered?: boolean;
  controlledState?: VisualState;
  variant?: UI3DColorVariant;
}
