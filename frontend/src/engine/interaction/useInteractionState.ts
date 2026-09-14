import { useEffect, useState } from 'react';
import { VisualInteractionState } from './interactionTypes';
import { InteractionPipeline } from './InteractionPipeline';

/**
 * Hook for 3D mesh components to purely consume visual interaction states
 * (IDLE, HOVER, ACTIVE, SELECTED, DISABLED, ERROR).
 * Strictly decouples rendering from backend authorization and business logic.
 */
export const useInteractionVisualState = (targetId: string): VisualInteractionState => {
  const [visualState, setVisualState] = useState<VisualInteractionState>(() => {
    return InteractionPipeline.getVisualState(targetId);
  });

  useEffect(() => {
    setVisualState(InteractionPipeline.getVisualState(targetId));
    const unsubscribe = InteractionPipeline.onVisualStateChange(targetId, (next) => {
      setVisualState(next);
    });

    return () => {
      unsubscribe();
    };
  }, [targetId]);

  return visualState;
};
