import React, { useEffect, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { useWorldEnvironmentStore } from './useWorldEnvironmentStore';
import { AudioEngine } from '../engine/audio/AudioEngine';
import type { SectorId } from './worldTypes';

interface EnvironmentTransitionControllerProps {
  transitionDuration?: number; // in seconds, default 0.8s
  onTransitionComplete?: (sectorId: SectorId) => void;
}

export const EnvironmentTransitionController: React.FC<EnvironmentTransitionControllerProps> = ({
  transitionDuration = 0.8,
  onTransitionComplete,
}) => {
  const transitioning = useWorldEnvironmentStore((state) => state.transitioning);
  const transitionProgress = useWorldEnvironmentStore((state) => state.transitionProgress);
  const setTransitionProgress = useWorldEnvironmentStore((state) => state.setTransitionProgress);
  const completeTransition = useWorldEnvironmentStore((state) => state.completeTransition);
  const activeSectorId = useWorldEnvironmentStore((state) => state.activeSectorId);

  const progressRef = useRef(transitionProgress);
  progressRef.current = transitionProgress;

  useEffect(() => {
    if (transitioning) {
      AudioEngine.playSpatial('portal_teleport', [0, 2, 0]);
    }
  }, [transitioning]);

  useFrame((_, delta) => {
    if (!transitioning) return;

    const nextProgress = progressRef.current + delta / transitionDuration;
    if (nextProgress >= 1.0) {
      setTransitionProgress(1.0);
      completeTransition();
      onTransitionComplete?.(activeSectorId);
    } else {
      setTransitionProgress(nextProgress);
    }
  });

  return null;
};
