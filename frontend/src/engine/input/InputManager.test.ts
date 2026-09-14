import { describe, it, expect, beforeEach } from 'vitest';
import {
  normalizePointerCoords,
  computeMovementVector,
  mapKeyCodeToAction,
  calculatePinchDistance,
} from './inputNormalizer';
import { InputDispatcher } from './inputDispatcher';
import { useInputStore } from './useInputStore';

describe('InputManager & Normalized Input System', () => {
  beforeEach(() => {
    InputDispatcher.clear();
    useInputStore.getState().resetInputs();
  });

  describe('normalizePointerCoords', () => {
    const viewportRect = { left: 0, top: 0, width: 1000, height: 500 };

    it('normalizes top-left screen pixels to NDC [-1, 1]', () => {
      const { ndcX, ndcY } = normalizePointerCoords(0, 0, viewportRect);
      expect(ndcX).toBe(-1);
      expect(ndcY).toBe(1);
    });

    it('normalizes viewport center pixels to NDC [0, 0]', () => {
      const { ndcX, ndcY } = normalizePointerCoords(500, 250, viewportRect);
      expect(ndcX).toBeCloseTo(0);
      expect(ndcY).toBeCloseTo(0);
    });

    it('normalizes bottom-right screen pixels to NDC [1, -1]', () => {
      const { ndcX, ndcY } = normalizePointerCoords(1000, 500, viewportRect);
      expect(ndcX).toBe(1);
      expect(ndcY).toBe(-1);
    });

    it('clamps coordinates outside viewport bounds to [-1, 1]', () => {
      const { ndcX, ndcY } = normalizePointerCoords(2000, -500, viewportRect);
      expect(ndcX).toBe(1);
      expect(ndcY).toBe(1);
    });

    it('safely handles zero-dimension containers without dividing by zero', () => {
      const { ndcX, ndcY } = normalizePointerCoords(100, 100, {
        left: 0,
        top: 0,
        width: 0,
        height: 0,
      });
      expect(ndcX).toBe(0);
      expect(ndcY).toBe(0);
    });
  });

  describe('computeMovementVector & mapKeyCodeToAction', () => {
    it('maps standard WASD keys to directional vectors', () => {
      expect(computeMovementVector(new Set(['KeyW']))).toEqual({
        forward: 1,
        strafe: 0,
        sprint: false,
        jump: false,
        interact: false,
      });

      expect(computeMovementVector(new Set(['KeyS']))).toEqual({
        forward: -1,
        strafe: 0,
        sprint: false,
        jump: false,
        interact: false,
      });

      expect(computeMovementVector(new Set(['KeyA']))).toEqual({
        forward: 0,
        strafe: -1,
        sprint: false,
        jump: false,
        interact: false,
      });

      expect(computeMovementVector(new Set(['KeyD']))).toEqual({
        forward: 0,
        strafe: 1,
        sprint: false,
        jump: false,
        interact: false,
      });
    });

    it('handles diagonal movement, sprinting, and jump modifiers', () => {
      const keys = new Set(['KeyW', 'KeyD', 'ShiftLeft', 'Space']);
      const move = computeMovementVector(keys);
      expect(move.forward).toBe(1);
      expect(move.strafe).toBe(1);
      expect(move.sprint).toBe(true);
      expect(move.jump).toBe(true);
    });

    it('maps key codes to canonical InputActions', () => {
      expect(mapKeyCodeToAction('KeyW')).toBe('MOVE_FORWARD');
      expect(mapKeyCodeToAction('Escape')).toBe('CANCEL');
      expect(mapKeyCodeToAction('KeyH')).toBe('TOGGLE_HUD');
      expect(mapKeyCodeToAction('KeyE')).toBe('INTERACT');
      expect(mapKeyCodeToAction('F12')).toBeNull();
    });
  });

  describe('calculatePinchDistance', () => {
    it('computes Euclidean distance for two touch points', () => {
      const touchA = { clientX: 0, clientY: 0 } as Touch;
      const touchB = { clientX: 30, clientY: 40 } as Touch;
      const dist = calculatePinchDistance(touchA, touchB);
      expect(dist).toBeCloseTo(50);
    });
  });

  describe('InputDispatcher (No Per-Object DOM Handlers Invariant)', () => {
    it('registers and dispatches spatial events to specific target meshes without DOM listeners', () => {
      let clicked = false;
      let clickedDistance = 0;

      const unregister = InputDispatcher.registerInteractable('vehicle_pedestal_01', {
        onClick: (hit) => {
          clicked = true;
          clickedDistance = hit.distance;
        },
      });

      // Dispatch hit
      InputDispatcher.dispatchClick({
        objectId: 'vehicle_pedestal_01',
        point: [0, 1.5, 0],
        distance: 4.2,
      });

      expect(clicked).toBe(true);
      expect(clickedDistance).toBe(4.2);

      // Unregister
      unregister();
      clicked = false;
      InputDispatcher.dispatchClick({
        objectId: 'vehicle_pedestal_01',
        point: [0, 1.5, 0],
        distance: 4.2,
      });
      expect(clicked).toBe(false);
    });

    it('routes keyboard typing to active text inputs', () => {
      const typedChars: string[] = [];
      const unregister = InputDispatcher.onKeyTyped((char) => {
        typedChars.push(char);
      });

      InputDispatcher.dispatchKeyTyped('E');
      InputDispatcher.dispatchKeyTyped('V');
      InputDispatcher.dispatchKeyTyped('3');
      InputDispatcher.dispatchKeyTyped('D');

      expect(typedChars).toEqual(['E', 'V', '3', 'D']);
      unregister();
    });
  });

  describe('useInputStore', () => {
    it('updates normalized pointer and typing mode correctly', () => {
      const { setPointer, setTypingMode } = useInputStore.getState();

      setPointer({ ndcX: 0.5, ndcY: -0.2, isDown: true, isDragging: true });
      expect(useInputStore.getState().pointer.ndcX).toBe(0.5);
      expect(useInputStore.getState().pointer.ndcY).toBe(-0.2);
      expect(useInputStore.getState().pointer.isDragging).toBe(true);

      setTypingMode(true);
      expect(useInputStore.getState().isTypingMode).toBe(true);
    });
  });
});
