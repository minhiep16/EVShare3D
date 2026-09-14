import { describe, it, expect, beforeEach } from 'vitest';
import { useTouchStore } from './useTouchStore';
import { useInputStore } from '../input/useInputStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { usePerformanceStore } from '../performance/usePerformanceStore';

describe('Mobile & Tablet 3D Touch Subsystem (08-AF)', () => {
  beforeEach(() => {
    useTouchStore.getState().resetJoystick();
    usePlayerStore.getState().setMovementMode('IDLE');
    usePerformanceStore.getState().setTier('HIGH');
  });

  describe('1. Virtual Joystick & Touch Movement', () => {
    it('translates joystick forward vector into walking player movement', () => {
      const { updateJoystick } = useTouchStore.getState();

      // Half forward [0, 0.5]
      updateJoystick([100, 100], [100, 75], [0, 0.5]);

      const movement = useInputStore.getState().movement;
      expect(movement.forward).toBe(0.5);
      expect(movement.strafe).toBe(0);
      expect(movement.sprint).toBe(false);
      expect(usePlayerStore.getState().movementMode).toBe('WALKING');
    });

    it('translates full joystick vector into sprinting player movement', () => {
      const { updateJoystick } = useTouchStore.getState();

      // Full forward-right [0.707, 0.707] (magnitude = 1.0 > 0.85)
      updateJoystick([100, 100], [135, 65], [0.707, 0.707]);

      const movement = useInputStore.getState().movement;
      expect(movement.forward).toBeCloseTo(0.707, 3);
      expect(movement.strafe).toBeCloseTo(0.707, 3);
      expect(movement.sprint).toBe(true);
      expect(usePlayerStore.getState().movementMode).toBe('SPRINTING');
    });

    it('clears player movement and resets mode to IDLE upon joystick release', () => {
      const { updateJoystick, resetJoystick } = useTouchStore.getState();

      updateJoystick([100, 100], [100, 50], [0, 1.0]);
      expect(usePlayerStore.getState().movementMode).toBe('SPRINTING');

      resetJoystick();

      const movement = useInputStore.getState().movement;
      expect(movement.forward).toBe(0);
      expect(movement.strafe).toBe(0);
      expect(movement.sprint).toBe(false);
      expect(usePlayerStore.getState().movementMode).toBe('IDLE');
      expect(useTouchStore.getState().joystickState.active).toBe(false);
    });
  });

  describe('2. Touch Selection & Tap Registration', () => {
    it('records touch tap with normalized device coordinates', () => {
      const { registerTap } = useTouchStore.getState();

      const tap = {
        screenX: 400,
        screenY: 300,
        ndcX: 0.0,
        ndcY: 0.0,
        timestamp: Date.now(),
      };

      registerTap(tap);
      expect(useTouchStore.getState().lastTap).toEqual(tap);
    });

    it('validates tap criteria for selection threshold (<250ms, <10px)', () => {
      const isTapValid = (durationMs: number, moveDistPx: number) => {
        return durationMs <= 250 && moveDistPx <= 10;
      };

      expect(isTapValid(120, 4)).toBe(true);
      expect(isTapValid(350, 4)).toBe(false); // Too slow -> drag
      expect(isTapValid(100, 25)).toBe(false); // Moved too far -> drag
    });
  });

  describe('3. Responsive Viewport & Performance Adaptation', () => {
    it('calculates expanded FOV for portrait mobile viewports (aspect < 1.0)', () => {
      const calculateAdaptedFov = (width: number, height: number, baseFov: number) => {
        const aspect = width / height;
        if (aspect < 1.0) {
          const fovMultiplier = Math.min(1.4, 1.0 / aspect);
          return Math.round(baseFov * fovMultiplier);
        }
        return baseFov;
      };

      const landscapeFov = calculateAdaptedFov(1920, 1080, 50);
      expect(landscapeFov).toBe(50);

      // Portrait phone (width 390, height 844, aspect ~0.46)
      const portraitFov = calculateAdaptedFov(390, 844, 50);
      expect(portraitFov).toBe(70); // Clamped at 1.4 * 50 = 70
    });

    it('applies mobile / tablet performance profile rules', () => {
      const adaptPerformanceForDevice = (width: number, isTouch: boolean) => {
        if (width <= 768) {
          usePerformanceStore.getState().setTier('LOW');
          usePerformanceStore.getState().setAdaptiveDpr(1.25);
        } else if (width <= 1024 || isTouch) {
          usePerformanceStore.getState().setTier('MEDIUM');
          usePerformanceStore.getState().setAdaptiveDpr(1.5);
        } else {
          usePerformanceStore.getState().setTier('HIGH');
        }
      };

      // Mobile phone
      adaptPerformanceForDevice(400, true);
      expect(usePerformanceStore.getState().currentTier).toBe('LOW');
      expect(usePerformanceStore.getState().adaptiveDpr).toBe(1.0); // Clamped by LOW tier max DPR 1.0

      // Tablet
      adaptPerformanceForDevice(820, true);
      expect(usePerformanceStore.getState().currentTier).toBe('MEDIUM');
      expect(usePerformanceStore.getState().adaptiveDpr).toBe(1.5);

      // Desktop
      adaptPerformanceForDevice(1920, false);
      expect(usePerformanceStore.getState().currentTier).toBe('HIGH');
    });
  });
});
