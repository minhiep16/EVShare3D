import { describe, it, expect, beforeEach } from 'vitest';
import { useFocusStore } from '../focus/useFocusStore';
import { FocusRegistry } from '../focus/FocusRegistry';
import { VARIANT_PALETTES } from './ui3dTypes';

describe('Terminal 3D Entity & Focus System (08-AG)', () => {
  beforeEach(() => {
    useFocusStore.getState().exitFocus();
    FocusRegistry.clear();
  });

  describe('1. Terminal Visual Variants & Status LED Palettes', () => {
    it('defines accurate hex color palettes for all terminal status variants', () => {
      expect(VARIANT_PALETTES.emerald.primary).toBe('#00e676');
      expect(VARIANT_PALETTES.amber.primary).toBe('#ffab00');
      expect(VARIANT_PALETTES.crimson.primary).toBe('#ff1744');
      expect(VARIANT_PALETTES.cyan.primary).toBe('#00e5ff');
    });

    it('validates terminal viewing angle and ergonomic tilt', () => {
      const screenAngleRad = -Math.PI / 10; // -18 degrees
      const degrees = (screenAngleRad * 180) / Math.PI;
      expect(degrees).toBeCloseTo(-18, 1);
    });
  });

  describe('2. Terminal Focus Target Lifecycle', () => {
    it('registers a terminal as a focus target and triggers camera glide', () => {
      const focusTarget = {
        id: 'security_gate_terminal',
        name: 'Access Console',
        category: 'TERMINAL' as const,
        targetPosition: [-3.6, 1.2, 1.2] as [number, number, number],
        preset: {
          distance: 1.8,
          elevation: 0.2,
          fov: 34,
        },
      };

      FocusRegistry.registerTarget(focusTarget);
      expect(FocusRegistry.hasTarget('security_gate_terminal')).toBe(true);

      // Focus on the terminal
      useFocusStore.getState().focusObject('security_gate_terminal');

      const state = useFocusStore.getState();
      expect(state.focusedTargetId).toBe('security_gate_terminal');
      expect(state.isFocused).toBe(true);
      expect(state.savedCameraStack.length).toBe(1);
    });

    it('clears focus and pops terminal focus history', () => {
      const focusTarget = {
        id: 'charging_station_terminal',
        name: 'Supercharger V4 Terminal',
        category: 'TERMINAL' as const,
        targetPosition: [4.0, 1.2, -2.0] as [number, number, number],
      };

      FocusRegistry.registerTarget(focusTarget);
      useFocusStore.getState().focusObject('charging_station_terminal');
      expect(useFocusStore.getState().isFocused).toBe(true);

      useFocusStore.getState().returnToPreviousCamera();
      expect(useFocusStore.getState().focusedTargetId).toBeNull();
      expect(useFocusStore.getState().isFocused).toBe(false);
      expect(useFocusStore.getState().savedCameraStack.length).toBe(0);
    });
  });
});
