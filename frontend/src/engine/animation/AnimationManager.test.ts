import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  AnimationRegistry,
  IS_LOOPING_STATE,
  TRANSIENT_DURATIONS,
} from './AnimationRegistry';
import {
  computeActivationScale,
  computeShakeOffset,
  sinePulse,
  springDamp,
} from './easing';

describe('AnimationManager & Spatial Animation Subsystem', () => {
  beforeEach(() => {
    AnimationRegistry.clear();
  });

  describe('Track Lifecycle & Uncontrolled Loop Prevention', () => {
    it('correctly classifies looping vs transient states', () => {
      expect(IS_LOOPING_STATE.HOVER).toBe(true);
      expect(IS_LOOPING_STATE.SELECTED).toBe(true);
      expect(IS_LOOPING_STATE.LOADING).toBe(true);

      expect(IS_LOOPING_STATE.ACTIVE).toBe(false);
      expect(IS_LOOPING_STATE.SUCCESS).toBe(false);
      expect(IS_LOOPING_STATE.ERROR).toBe(false);
    });

    it('defines finite durations for all transient states', () => {
      expect(TRANSIENT_DURATIONS.ACTIVE).toBe(350);
      expect(TRANSIENT_DURATIONS.SUCCESS).toBe(750);
      expect(TRANSIENT_DURATIONS.ERROR).toBe(500);
    });

    it('auto-completes transient states and returns to IDLE to prevent uncontrolled loops', () => {
      const completeSpy = vi.fn();

      AnimationRegistry.register({
        id: 'terminal_button_01',
        state: 'ERROR',
        onAnimationComplete: completeSpy,
      });

      const initialTrack = AnimationRegistry.getTrack('terminal_button_01');
      expect(initialTrack?.state).toBe('ERROR');
      expect(initialTrack?.durationMs).toBe(500);
      expect(initialTrack?.isFinished).toBe(false);

      // Advance by 300ms (still within duration)
      AnimationRegistry.advanceTrack('terminal_button_01', 300);
      expect(AnimationRegistry.getTrack('terminal_button_01')?.isFinished).toBe(false);
      expect(completeSpy).not.toHaveBeenCalled();

      // Advance past duration (+250ms -> 550ms total >= 500ms)
      AnimationRegistry.advanceTrack('terminal_button_01', 250);

      // Verify completion callback fired with 'ERROR' and track auto-settled to 'IDLE'
      expect(completeSpy).toHaveBeenCalledWith('ERROR');
      expect(AnimationRegistry.getTrack('terminal_button_01')?.state).toBe('IDLE');
    });

    it('does not terminate continuous looping states like LOADING or HOVER', () => {
      AnimationRegistry.register({
        id: 'radar_spinner',
        state: 'LOADING',
      });

      // Advance by 10 seconds
      AnimationRegistry.advanceTrack('radar_spinner', 10000);
      const track = AnimationRegistry.getTrack('radar_spinner');
      expect(track?.state).toBe('LOADING');
      expect(track?.isFinished).toBe(false);
      expect(track?.isLooping).toBe(true);
    });
  });

  describe('Mathematical Dampening and Easing Curves', () => {
    it('computeShakeOffset decays smoothly and returns strictly 0 at or after duration', () => {
      // In middle of shake
      const midShake = computeShakeOffset(200, 500, 0.08, 3);
      expect(Math.abs(midShake)).toBeGreaterThan(0);

      // Exactly at end of duration
      const endShake = computeShakeOffset(500, 500, 0.08, 3);
      expect(endShake).toBe(0);

      // Past duration
      const pastShake = computeShakeOffset(750, 500, 0.08, 3);
      expect(pastShake).toBe(0);
    });

    it('computeActivationScale performs punch and settles to 1.0', () => {
      // Initial state
      expect(computeActivationScale(0, 350)).toBe(1.0);

      // Compression phase (t = 0.15)
      const compressed = computeActivationScale(50, 350);
      expect(compressed).toBeLessThan(1.0);

      // Overshoot pop phase (t = 0.5)
      const pop = computeActivationScale(175, 350);
      expect(pop).toBeGreaterThan(1.0);

      // Finished
      expect(computeActivationScale(350, 350)).toBeCloseTo(1.0);
      expect(computeActivationScale(400, 350)).toBe(1.0);
    });

    it('springDamp smoothly approaches target value', () => {
      let current = 0;
      const target = 10;
      const lambda = 15;
      const dt = 0.016;

      // After one frame
      current = springDamp(current, target, lambda, dt);
      expect(current).toBeGreaterThan(0);
      expect(current).toBeLessThan(target);

      // After 30 frames
      for (let i = 0; i < 30; i++) {
        current = springDamp(current, target, lambda, dt);
      }
      expect(current).toBeCloseTo(target, 0);
    });

    it('sinePulse oscillates strictly between min and max bounds', () => {
      for (let t = 0; t < 2; t += 0.1) {
        const val = sinePulse(t, 2, 0.9, 1.1);
        expect(val).toBeGreaterThanOrEqual(0.899);
        expect(val).toBeLessThanOrEqual(1.101);
      }
    });
  });
});
