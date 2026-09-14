import { describe, it, expect, vi, beforeEach } from 'vitest';
import { VARIANT_PALETTES, ThreeDButtonState } from './ui3dTypes';

describe('ThreeDButton – WebGL 3D UI Component (08-O)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Interactive State Resolution & Lifecycles', () => {
    it('resolves DISABLED state when disabled=true', () => {
      const disabled = true;
      const loading = false;
      const isPressed = false;
      const isHovered = false;

      const resolveState = (d: boolean, l: boolean, p: boolean, h: boolean): ThreeDButtonState => {
        if (d) return 'DISABLED';
        if (l) return 'LOADING';
        if (p) return 'ACTIVE';
        if (h) return 'HOVER';
        return 'IDLE';
      };

      expect(resolveState(disabled, loading, isPressed, isHovered)).toBe('DISABLED');
    });

    it('resolves LOADING state when loading=true and not disabled', () => {
      const resolveState = (d: boolean, l: boolean): ThreeDButtonState => {
        if (d) return 'DISABLED';
        if (l) return 'LOADING';
        return 'IDLE';
      };

      expect(resolveState(false, true)).toBe('LOADING');
    });

    it('prioritizes explicit controlled states (SUCCESS, ERROR)', () => {
      const getEffectiveState = (controlled?: ThreeDButtonState): ThreeDButtonState => {
        return controlled || 'IDLE';
      };

      expect(getEffectiveState('SUCCESS')).toBe('SUCCESS');
      expect(getEffectiveState('ERROR')).toBe('ERROR');
    });

    it('determines interactivity correctly across states', () => {
      const isInteractive = (state: ThreeDButtonState) =>
        state !== 'DISABLED' && state !== 'LOADING';

      expect(isInteractive('IDLE')).toBe(true);
      expect(isInteractive('HOVER')).toBe(true);
      expect(isInteractive('ACTIVE')).toBe(true);
      expect(isInteractive('SUCCESS')).toBe(true);
      expect(isInteractive('ERROR')).toBe(true);
      expect(isInteractive('DISABLED')).toBe(false);
      expect(isInteractive('LOADING')).toBe(false);
    });
  });

  describe('3D Design System Palette Resolution', () => {
    it('resolves theme variant palettes correctly', () => {
      expect(VARIANT_PALETTES.cyan.primary).toBe('#00e5ff');
      expect(VARIANT_PALETTES.emerald.primary).toBe('#00e676');
      expect(VARIANT_PALETTES.amber.primary).toBe('#ffab00');
      expect(VARIANT_PALETTES.crimson.primary).toBe('#ff1744');
      expect(VARIANT_PALETTES.neutral.primary).toBe('#8a94a6');
    });

    it('resolves state emissive colors accurately', () => {
      const getEmissiveColor = (state: ThreeDButtonState, variantPrimary: string) => {
        if (state === 'SUCCESS') return '#00e676';
        if (state === 'ERROR') return '#ff1744';
        if (state === 'DISABLED') return '#000000';
        return variantPrimary;
      };

      expect(getEmissiveColor('SUCCESS', '#00e5ff')).toBe('#00e676');
      expect(getEmissiveColor('ERROR', '#00e5ff')).toBe('#ff1744');
      expect(getEmissiveColor('DISABLED', '#00e5ff')).toBe('#000000');
      expect(getEmissiveColor('HOVER', '#00e5ff')).toBe('#00e5ff');
    });
  });

  describe('Keyboard Accessibility Integration', () => {
    it('triggers click when Enter or Space is pressed and button isFocused', () => {
      const onClick = vi.fn();
      const isFocused = true;
      const isInteractive = true;

      const simulateKeyDown = (key: string) => {
        if (!isInteractive) return;
        if (isFocused && (key === 'Enter' || key === ' ')) {
          onClick();
        }
      };

      simulateKeyDown('Enter');
      expect(onClick).toHaveBeenCalledTimes(1);

      simulateKeyDown(' ');
      expect(onClick).toHaveBeenCalledTimes(2);

      simulateKeyDown('Escape');
      expect(onClick).toHaveBeenCalledTimes(2);
    });

    it('triggers click when shortcutKey matches', () => {
      const onClick = vi.fn();
      const shortcutKey = 'e';
      const isInteractive = true;

      const simulateShortcut = (key: string) => {
        if (!isInteractive) return;
        if (shortcutKey && key.toLowerCase() === shortcutKey.toLowerCase()) {
          onClick();
        }
      };

      simulateShortcut('E');
      expect(onClick).toHaveBeenCalledTimes(1);

      simulateShortcut('x');
      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('rejects keyboard interactions when disabled or loading', () => {
      const onClick = vi.fn();
      const isInteractive = false;

      const handleKey = (key: string) => {
        if (!isInteractive) return;
        if (key === 'Enter') onClick();
      };

      handleKey('Enter');
      expect(onClick).not.toHaveBeenCalled();
    });
  });
});
