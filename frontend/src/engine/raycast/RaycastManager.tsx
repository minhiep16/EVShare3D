import React, { useEffect, useRef } from 'react';
import { useFrame, useThree } from '@react-three/fiber';
import { useInputStore } from '../input/useInputStore';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { InputDispatcher } from '../input/inputDispatcher';
import {
  computeRaycastHit,
  createRaycastTracker,
  shouldPerformRaycast,
} from './raycastEngine';
import { InteractableRegistry } from './interactableRegistry';
import { RaycastHit } from './raycastTypes';

export const RaycastManager: React.FC = () => {
  const { camera, raycaster } = useThree();
  const tracker = useRef(createRaycastTracker());
  const currentHitRef = useRef<RaycastHit | null>(null);
  const lastHoveredIdRef = useRef<string | null>(null);

  // Store hooks
  const setHoveredObjectId = useInteractionStore((state) => state.setHoveredObjectId);
  const setSelectedObjectId = useInteractionStore((state) => state.setSelectedObjectId);
  const setCursorMode = useInteractionStore((state) => state.setCursorMode);

  // Subscribe to central pointer clicks and touch taps
  useEffect(() => {
    const unregisterClick = InputDispatcher.onAction('POINTER_CLICK', () => {
      const activeHit = currentHitRef.current;
      if (activeHit) {
        const config = InteractableRegistry.get(activeHit.id);
        config?.callbacks?.onClick?.(activeHit);
        setSelectedObjectId(activeHit.id);
      } else {
        setSelectedObjectId(null);
      }
    });

    return () => {
      unregisterClick();
    };
  }, [setSelectedObjectId]);

  useFrame(() => {
    const { pointer } = useInputStore.getState();
    const ndcX = pointer.ndcX;
    const ndcY = pointer.ndcY;

    // Check if pointer or camera has moved (Skip expensive raycasting if stationary)
    const needsRaycast = shouldPerformRaycast(ndcX, ndcY, camera, tracker.current);

    if (needsRaycast) {
      tracker.current.raycastCount += 1;
      const hit = computeRaycastHit(ndcX, ndcY, camera, raycaster);

      tracker.current.cachedHit = hit;
      tracker.current.lastNdcX = ndcX;
      tracker.current.lastNdcY = ndcY;
      tracker.current.lastCamMatrix.copy(camera.matrixWorld);

      currentHitRef.current = hit;

      // Hover State Transitions
      const hitId = hit ? hit.id : null;
      if (hitId !== lastHoveredIdRef.current) {
        // 1. Leave previous object
        if (lastHoveredIdRef.current) {
          const prevConfig = InteractableRegistry.get(lastHoveredIdRef.current);
          prevConfig?.callbacks?.onHoverLeave?.();
        }

        // 2. Enter new object
        if (hit && hitId) {
          const nextConfig = InteractableRegistry.get(hitId);
          nextConfig?.callbacks?.onHoverEnter?.(hit);
          setCursorMode(nextConfig?.cursor ?? 'POINTER');
        } else {
          setCursorMode('DEFAULT');
        }

        lastHoveredIdRef.current = hitId;
        setHoveredObjectId(hitId);
      }
    } else {
      tracker.current.skippedCount += 1;
    }
  });

  return null;
};
