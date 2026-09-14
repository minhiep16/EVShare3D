import { ProceduralSoundId } from '../../audio/audioTypes';
import { UI3DColorVariant, VARIANT_PALETTES } from '../ui3dTypes';
import { VisualState, VisualStateInput, VisualStateParams } from './visualStateTypes';

/**
 * Resolves the effective 3D visual state based on component props and pointer/focus state.
 * Priority hierarchy:
 * 1. Explicit controlledState (if set)
 * 2. DISABLED
 * 3. LOADING
 * 4. ACTIVE (pressed)
 * 5. SELECTED
 * 6. HOVER
 * 7. IDLE (default)
 */
export function resolveVisualState(input: VisualStateInput): VisualState {
  if (input.controlledState) {
    return input.controlledState;
  }
  if (input.disabled) {
    return 'DISABLED';
  }
  if (input.loading) {
    return 'LOADING';
  }
  if (input.isPressed) {
    return 'ACTIVE';
  }
  if (input.isSelected) {
    return 'SELECTED';
  }
  if (input.isHovered) {
    return 'HOVER';
  }
  return 'IDLE';
}

/**
 * Resolves full PBR visual parameters for a given visual state and color variant.
 */
export function resolveVisualStateParams(
  state: VisualState,
  variant: UI3DColorVariant = 'cyan'
): VisualStateParams {
  const palette = VARIANT_PALETTES[variant] || VARIANT_PALETTES.cyan;

  switch (state) {
    case 'HOVER':
      return {
        state,
        primaryColor: palette.primary,
        backgroundColor: palette.background,
        borderColor: palette.primary,
        textColor: '#ffffff',
        emissiveColor: palette.glow,
        emissiveIntensity: 1.6,
        scale: 1.04,
        elevationZ: 0.025,
        opacity: 0.98,
        cursor: 'POINTER',
        isInteractive: true,
      };

    case 'ACTIVE':
      return {
        state,
        primaryColor: palette.primary,
        backgroundColor: palette.background,
        borderColor: '#ffffff',
        textColor: '#ffffff',
        emissiveColor: palette.glow,
        emissiveIntensity: 2.5,
        scale: 1.0,
        elevationZ: -0.018, // Physical depth depression
        opacity: 1.0,
        cursor: 'POINTER',
        isInteractive: true,
      };

    case 'SELECTED':
      return {
        state,
        primaryColor: '#ffab00', // Distinct amber gold highlight for selection
        backgroundColor: '#1c1608',
        borderColor: '#ffab00',
        textColor: '#ffffff',
        emissiveColor: '#ffab00',
        emissiveIntensity: 2.0,
        scale: 1.06,
        elevationZ: 0.035, // High elevation scan
        opacity: 1.0,
        cursor: 'POINTER',
        isInteractive: true,
      };

    case 'DISABLED':
      return {
        state,
        primaryColor: '#404550',
        backgroundColor: '#12161f',
        borderColor: '#252a34',
        textColor: '#555e6d',
        emissiveColor: '#000000',
        emissiveIntensity: 0.0,
        scale: 1.0,
        elevationZ: 0.0,
        opacity: 0.45,
        cursor: 'NOT_ALLOWED',
        isInteractive: false,
      };

    case 'LOADING':
      return {
        state,
        primaryColor: palette.primary,
        backgroundColor: '#0c121a',
        borderColor: palette.border,
        textColor: '#8a94a6',
        emissiveColor: palette.glow,
        emissiveIntensity: 0.8,
        scale: 1.0,
        elevationZ: 0.0,
        opacity: 0.75,
        cursor: 'WAIT',
        isInteractive: false,
      };

    case 'SUCCESS':
      return {
        state,
        primaryColor: '#00e676',
        backgroundColor: '#081c14',
        borderColor: '#00e676',
        textColor: '#ffffff',
        emissiveColor: '#00e676',
        emissiveIntensity: 2.4,
        scale: 1.08,
        elevationZ: 0.015,
        opacity: 1.0,
        cursor: 'POINTER',
        isInteractive: true,
      };

    case 'ERROR':
      return {
        state,
        primaryColor: '#ff1744',
        backgroundColor: '#22080e',
        borderColor: '#ff1744',
        textColor: '#ffffff',
        emissiveColor: '#ff1744',
        emissiveIntensity: 2.4,
        scale: 1.0,
        elevationZ: 0.0,
        opacity: 1.0,
        cursor: 'POINTER',
        isInteractive: true,
      };

    case 'IDLE':
    default:
      return {
        state: 'IDLE',
        primaryColor: palette.primary,
        backgroundColor: '#0a0e16',
        borderColor: palette.border,
        textColor: palette.text,
        emissiveColor: palette.glow,
        emissiveIntensity: 0.35,
        scale: 1.0,
        elevationZ: 0.0,
        opacity: 0.95,
        cursor: 'POINTER',
        isInteractive: true,
      };
  }
}

/**
 * Maps visual states to procedural sound triggers.
 */
export function getVisualStateSoundId(state: VisualState): ProceduralSoundId | null {
  switch (state) {
    case 'HOVER':
      return 'UI_HOVER';
    case 'ACTIVE':
      return 'UI_CLICK';
    case 'SUCCESS':
      return 'NOTIF_SUCCESS';
    case 'ERROR':
      return 'NOTIF_ERROR';
    default:
      return null;
  }
}
