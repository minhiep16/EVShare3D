import React, { useEffect, useRef } from 'react';
import { useInputStore } from './useInputStore';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { InputDispatcher } from './inputDispatcher';
import {
  computeMovementVector,
  mapKeyCodeToAction,
  normalizePointerCoords,
  calculatePinchDistance,
} from './inputNormalizer';

interface InputManagerProps {
  targetElementRef?: React.RefObject<HTMLElement>;
}

const DRAG_THRESHOLD_PX = 5;

export const InputManager: React.FC<InputManagerProps> = ({ targetElementRef }) => {
  const activeKeys = useRef(new Set<string>());
  const pointerDownPos = useRef<[number, number] | null>(null);
  const isDraggingRef = useRef(false);
  const lastTouchDist = useRef<number | null>(null);
  const rAfId = useRef<number | null>(null);

  // Store hooks
  const setPointer = useInputStore((state) => state.setPointer);
  const setMovement = useInputStore((state) => state.setMovement);
  const setTouch = useInputStore((state) => state.setTouch);
  const isTypingMode = useInputStore((state) => state.isTypingMode);
  const setPointerCoords = useInteractionStore((state) => state.setPointerCoords);
  const focusedInputId = useInteractionStore((state) => state.focusedInputId);
  const setIsSprinting = usePlayerStore((state) => state.setIsSprinting);
  const setMovementMode = usePlayerStore((state) => state.setMovementMode);

  // Synchronize typing mode with focused 3D inputs
  useEffect(() => {
    useInputStore.getState().setTypingMode(Boolean(focusedInputId));
  }, [focusedInputId]);

  useEffect(() => {
    const targetElement = targetElementRef?.current || window;

    // --- KEYBOARD LISTENERS ---
    const handleKeyDown = (e: KeyboardEvent): void => {
      // 1. Text Input Mode Routing
      if (isTypingMode) {
        if (e.key.length === 1 || e.key === 'Backspace' || e.key === 'Enter') {
          InputDispatcher.dispatchKeyTyped(e.key);
        }
        if (e.key === 'Escape') {
          useInteractionStore.getState().setFocusedInputId(null);
        }
        return;
      }

      // 2. Action & Movement Mapping
      activeKeys.current.add(e.code);
      const action = mapKeyCodeToAction(e.code);
      if (action) {
        InputDispatcher.dispatchAction(action);
      }

      const movement = computeMovementVector(activeKeys.current);
      setMovement(movement);
      setIsSprinting(movement.sprint);

      if (movement.forward !== 0 || movement.strafe !== 0) {
        setMovementMode(movement.sprint ? 'SPRINTING' : 'WALKING');
      }
    };

    const handleKeyUp = (e: KeyboardEvent): void => {
      if (isTypingMode) return;

      activeKeys.current.delete(e.code);
      const movement = computeMovementVector(activeKeys.current);
      setMovement(movement);
      setIsSprinting(movement.sprint);

      if (movement.forward === 0 && movement.strafe === 0) {
        setMovementMode('IDLE');
      }
    };

    // --- POINTER LISTENERS ---
    const handlePointerDown = (e: PointerEvent): void => {
      const el = (targetElementRef?.current || document.body) as HTMLElement;
      const rect = el.getBoundingClientRect();
      const { ndcX, ndcY } = normalizePointerCoords(e.clientX, e.clientY, rect);

      pointerDownPos.current = [e.clientX, e.clientY];
      isDraggingRef.current = false;

      setPointer({
        screenX: e.clientX,
        screenY: e.clientY,
        ndcX,
        ndcY,
        deltaX: 0,
        deltaY: 0,
        isDown: true,
        isDragging: false,
        button: e.button,
        pointerType: e.pointerType,
      });

      setPointerCoords([ndcX, ndcY]);
    };

    const handlePointerMove = (e: PointerEvent): void => {
      // Throttle pointer move to requestAnimationFrame
      if (rAfId.current !== null) {
        cancelAnimationFrame(rAfId.current);
      }

      rAfId.current = requestAnimationFrame(() => {
        const el = (targetElementRef?.current || document.body) as HTMLElement;
        const rect = el.getBoundingClientRect();
        const { ndcX, ndcY } = normalizePointerCoords(e.clientX, e.clientY, rect);

        let isDragging = isDraggingRef.current;
        if (pointerDownPos.current && !isDragging) {
          const dx = e.clientX - pointerDownPos.current[0];
          const dy = e.clientY - pointerDownPos.current[1];
          if (Math.sqrt(dx * dx + dy * dy) > DRAG_THRESHOLD_PX) {
            isDragging = true;
            isDraggingRef.current = true;
          }
        }

        setPointer({
          screenX: e.clientX,
          screenY: e.clientY,
          ndcX,
          ndcY,
          deltaX: e.movementX,
          deltaY: e.movementY,
          isDragging,
        });

        setPointerCoords([ndcX, ndcY]);
      });
    };

    const handlePointerUp = (e: PointerEvent): void => {
      const el = (targetElementRef?.current || document.body) as HTMLElement;
      const rect = el.getBoundingClientRect();
      const { ndcX, ndcY } = normalizePointerCoords(e.clientX, e.clientY, rect);

      const wasDragging = isDraggingRef.current;
      pointerDownPos.current = null;
      isDraggingRef.current = false;

      setPointer({
        screenX: e.clientX,
        screenY: e.clientY,
        ndcX,
        ndcY,
        isDown: false,
        isDragging: false,
      });

      // If released without exceeding drag threshold, dispatch normalized click
      if (!wasDragging) {
        InputDispatcher.dispatchAction('POINTER_CLICK');
      }
    };

    // --- TOUCH LISTENERS ---
    const handleTouchStart = (e: TouchEvent): void => {
      if (e.touches.length === 1) {
        const touch = e.touches[0];
        setTouch({
          touchCount: 1,
          primaryTouch: [touch.clientX, touch.clientY],
          gesture: 'TAP',
        });
      } else if (e.touches.length === 2) {
        const dist = calculatePinchDistance(e.touches[0], e.touches[1]);
        lastTouchDist.current = dist;
        setTouch({
          touchCount: 2,
          pinchDistance: dist,
          gesture: 'PINCH',
        });
      }
    };

    const handleTouchMove = (e: TouchEvent): void => {
      if (e.touches.length === 2 && lastTouchDist.current !== null) {
        const dist = calculatePinchDistance(e.touches[0], e.touches[1]);
        const delta = dist - lastTouchDist.current;
        lastTouchDist.current = dist;

        setTouch({
          touchCount: 2,
          pinchDistance: dist,
          pinchDelta: delta,
          gesture: 'PINCH',
        });
      } else if (e.touches.length === 1) {
        const touch = e.touches[0];
        setTouch({
          touchCount: 1,
          primaryTouch: [touch.clientX, touch.clientY],
          gesture: 'PAN',
        });
      }
    };

    const handleTouchEnd = (): void => {
      lastTouchDist.current = null;
      setTouch({
        touchCount: 0,
        primaryTouch: null,
        pinchDistance: null,
        pinchDelta: 0,
        gesture: 'NONE',
      });
    };

    // Attach listeners
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    const eventTarget = targetElementRef?.current || window;
    eventTarget.addEventListener('pointerdown', handlePointerDown as EventListener);
    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);

    eventTarget.addEventListener('touchstart', handleTouchStart as EventListener, { passive: true });
    window.addEventListener('touchmove', handleTouchMove, { passive: true });
    window.addEventListener('touchend', handleTouchEnd);
    window.addEventListener('touchcancel', handleTouchEnd);

    // Clean up all listeners
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
      eventTarget.removeEventListener('pointerdown', handlePointerDown as EventListener);
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);
      eventTarget.removeEventListener('touchstart', handleTouchStart as EventListener);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('touchend', handleTouchEnd);
      window.removeEventListener('touchcancel', handleTouchEnd);

      if (rAfId.current !== null) {
        cancelAnimationFrame(rAfId.current);
      }
    };
  }, [
    targetElementRef,
    isTypingMode,
    setMovement,
    setPointer,
    setTouch,
    setIsSprinting,
    setMovementMode,
    setPointerCoords,
  ]);

  return null;
};
