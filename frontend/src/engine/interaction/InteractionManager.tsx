import React, { useEffect, useRef } from 'react';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useAppStore } from '@/stores/useAppStore';
import { InputDispatcher } from '../input/inputDispatcher';
import { InteractionPipeline } from './InteractionPipeline';
import { InteractionContext } from './interactionTypes';

export const InteractionManager: React.FC = () => {
  const hoveredObjectId = useInteractionStore((state) => state.hoveredObjectId);
  const selectedObjectId = useInteractionStore((state) => state.selectedObjectId);

  const lastHoveredId = useRef<string | null>(null);

  // Synchronize hover state transitions with pipeline
  useEffect(() => {
    // 1. Handle previous object hover exit
    if (lastHoveredId.current && lastHoveredId.current !== hoveredObjectId) {
      const prevDef = InteractionPipeline.getDefinition(lastHoveredId.current);
      if (prevDef) {
        const isSelected = selectedObjectId === lastHoveredId.current;
        const isDisabled = prevDef.requirements?.disabled;
        InteractionPipeline.setVisualState(
          lastHoveredId.current,
          isDisabled ? 'DISABLED' : isSelected ? 'SELECTED' : 'IDLE'
        );
      }
    }

    // 2. Handle new object hover enter
    if (hoveredObjectId) {
      const def = InteractionPipeline.getDefinition(hoveredObjectId);
      if (def) {
        const isDisabled = def.requirements?.disabled;
        InteractionPipeline.setVisualState(
          hoveredObjectId,
          isDisabled ? 'DISABLED' : 'HOVER'
        );
      }
    }

    lastHoveredId.current = hoveredObjectId;
  }, [hoveredObjectId, selectedObjectId]);

  // Execute pipeline upon Click or Touch selection
  useEffect(() => {
    const unregisterClick = InputDispatcher.onAction('POINTER_CLICK', () => {
      const activeHoverId = useInteractionStore.getState().hoveredObjectId;
      if (!activeHoverId) return;

      const def = InteractionPipeline.getDefinition(activeHoverId);
      if (!def) return;

      const playerPos = usePlayerStore.getState().position;
      const targetPos = def.targetPosition;

      const dx = playerPos[0] - targetPos[0];
      const dy = playerPos[1] - targetPos[1];
      const dz = playerPos[2] - targetPos[2];
      const distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      const auth = useAppStore.getState().auth;

      const context: InteractionContext = {
        targetId: activeHoverId,
        playerPosition: playerPos,
        targetPosition: targetPos,
        distanceToPlayer: distance,
        userRoles: auth.roles,
        isAuthenticated: useAppStore.getState().isAuthenticated,
        userId: auth.userId,
      };

      // Trigger Stage 4 -> 5 -> 6 of the interaction pipeline
      InteractionPipeline.execute(activeHoverId, context);
    });

    return () => {
      unregisterClick();
    };
  }, []);

  return null;
};
