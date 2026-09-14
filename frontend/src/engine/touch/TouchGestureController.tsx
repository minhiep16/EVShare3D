import React, { useEffect, useRef } from 'react';
import { useTouchStore } from './useTouchStore';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { InputDispatcher } from '../input/inputDispatcher';

const TAP_MAX_DURATION_MS = 250;
const TAP_MAX_MOVE_PX = 10;

/**
 * TouchGestureController
 * Listens for single-finger camera look dragging, single-finger selection taps,
 * and two-finger pinch-to-zoom gestures on touch/tablet devices.
 */
export const TouchGestureController: React.FC = () => {
  const registerTap = useTouchStore((state) => state.registerTap);
  const touchStartPos = useRef<{ x: number; y: number; time: number } | null>(null);
  const touchLookId = useRef<number | null>(null);
  const lastLookPos = useRef<{ x: number; y: number } | null>(null);
  const lastPinchDist = useRef<number | null>(null);

  useEffect(() => {
    const handleTouchStart = (e: TouchEvent) => {
      if (e.touches.length === 1) {
        const touch = e.touches[0];
        // If touch starts on the right 60% of the screen, reserve for camera look/selection
        if (touch.clientX > window.innerWidth * 0.35) {
          touchStartPos.current = { x: touch.clientX, y: touch.clientY, time: Date.now() };
          touchLookId.current = touch.identifier;
          lastLookPos.current = { x: touch.clientX, y: touch.clientY };
        }
      } else if (e.touches.length === 2) {
        // Two-finger pinch initialization
        const dx = e.touches[0].clientX - e.touches[1].clientX;
        const dy = e.touches[0].clientY - e.touches[1].clientY;
        lastPinchDist.current = Math.hypot(dx, dy);
        touchLookId.current = null;
      }
    };

    const handleTouchMove = (e: TouchEvent) => {
      // 1. Camera Look (Single touch drag on look area)
      if (e.touches.length === 1 && touchLookId.current !== null && lastLookPos.current) {
        for (let i = 0; i < e.changedTouches.length; i++) {
          const t = e.changedTouches[i];
          if (t.identifier === touchLookId.current) {
            const dx = t.clientX - lastLookPos.current.x;
            const dy = t.clientY - lastLookPos.current.y;
            lastLookPos.current = { x: t.clientX, y: t.clientY };

            const cameraMode = useCameraStore.getState().mode;
            if (cameraMode === 'FIRST_PERSON' || cameraMode === 'THIRD_PERSON') {
              const [pitch, yaw, roll] = usePlayerStore.getState().rotation;
              const newYaw = yaw - dx * 0.005;
              const newPitch = Math.max(-Math.PI / 4, Math.min(Math.PI / 4, pitch - dy * 0.003));
              usePlayerStore.getState().setRotation([newPitch, newYaw, roll]);
            }
            break;
          }
        }
      }

      // 2. Pinch to Zoom (Two fingers)
      if (e.touches.length === 2 && lastPinchDist.current !== null) {
        const dx = e.touches[0].clientX - e.touches[1].clientX;
        const dy = e.touches[0].clientY - e.touches[1].clientY;
        const dist = Math.hypot(dx, dy);
        const delta = dist - lastPinchDist.current;
        lastPinchDist.current = dist;

        // Dynamic FOV / zoom adjustment
        const currentFov = useCameraStore.getState().desiredFov;
        const targetFov = Math.max(25, Math.min(85, currentFov - delta * 0.05));
        useCameraStore.getState().setFov(targetFov);
      }
    };

    const handleTouchEnd = (e: TouchEvent) => {
      if (touchStartPos.current) {
        for (let i = 0; i < e.changedTouches.length; i++) {
          const t = e.changedTouches[i];
          if (t.identifier === touchLookId.current) {
            const elapsed = Date.now() - touchStartPos.current.time;
            const moveDist = Math.hypot(
              t.clientX - touchStartPos.current.x,
              t.clientY - touchStartPos.current.y
            );

            // Verified Touch Tap: Fast duration and minimal movement
            if (elapsed <= TAP_MAX_DURATION_MS && moveDist <= TAP_MAX_MOVE_PX) {
              const ndcX = (t.clientX / window.innerWidth) * 2 - 1;
              const ndcY = -(t.clientY / window.innerHeight) * 2 + 1;

              registerTap({
                screenX: t.clientX,
                screenY: t.clientY,
                ndcX,
                ndcY,
                timestamp: Date.now(),
              });

              // Dispatch spatial selection raycast
              useInteractionStore.getState().setPointerCoords([ndcX, ndcY]);
              InputDispatcher.dispatchAction('POINTER_CLICK');
            }
            break;
          }
        }
      }

      if (e.touches.length === 0) {
        touchStartPos.current = null;
        touchLookId.current = null;
        lastLookPos.current = null;
        lastPinchDist.current = null;
      }
    };

    window.addEventListener('touchstart', handleTouchStart, { passive: true });
    window.addEventListener('touchmove', handleTouchMove, { passive: true });
    window.addEventListener('touchend', handleTouchEnd, { passive: true });
    window.addEventListener('touchcancel', handleTouchEnd, { passive: true });

    return () => {
      window.removeEventListener('touchstart', handleTouchStart);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('touchend', handleTouchEnd);
      window.removeEventListener('touchcancel', handleTouchEnd);
    };
  }, [registerTap]);

  return null;
};
