import { useEffect, useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { AnimationConfig, AnimationState, SpatialTransform3D } from './animationTypes';
import { AnimationRegistry } from './AnimationRegistry';
import {
  computeActivationScale,
  computeShakeOffset,
  sinePulse,
  springDamp,
} from './easing';

export interface UseSpatialAnimationResult {
  transform: SpatialTransform3D;
  currentState: AnimationState;
  triggerOneShot: (transientState: 'ACTIVE' | 'SUCCESS' | 'ERROR') => void;
}

const DEFAULT_TRANSFORM: SpatialTransform3D = {
  positionOffset: [0, 0, 0],
  rotationOffset: [0, 0, 0],
  scale: [1, 1, 1],
  emissiveIntensity: 1.0,
  opacity: 1.0,
};

export const useSpatialAnimation = (config: AnimationConfig): UseSpatialAnimationResult => {
  const {
    id,
    state,
    baseColor = '#00e5ff',
    hoverElevation = 0.08,
    hoverScale = 1.05,
    onAnimationComplete,
  } = config;

  const [currentAnimState, setCurrentAnimState] = useState<AnimationState>(state);
  const transformRef = useRef<SpatialTransform3D>({
    ...DEFAULT_TRANSFORM,
    colorHex: baseColor,
  });

  // Keep callback fresh in ref
  const onCompleteRef = useRef(onAnimationComplete);
  onCompleteRef.current = onAnimationComplete;

  // Register with AnimationRegistry
  useEffect(() => {
    const unregister = AnimationRegistry.register({
      id,
      state: currentAnimState,
      onAnimationComplete: (completed) => {
        onCompleteRef.current?.(completed);
        setCurrentAnimState('IDLE');
      },
    });

    return () => {
      unregister();
    };
  }, [id, currentAnimState]);

  // Synchronize incoming state props
  useEffect(() => {
    setCurrentAnimState(state);
    AnimationRegistry.setTrackState(id, state);
  }, [id, state]);

  // Unified Frame Ticker (No uncontrolled loop allocations)
  useFrame((r3fState, delta) => {
    const dt = Math.min(delta, 0.1);
    const deltaMs = dt * 1000;
    const timeSec = r3fState.clock.elapsedTime;

    // Track lifecycle is driven centrally by AnimationManager
    const track = AnimationRegistry.getTrack(id);
    const animState = track ? track.state : currentAnimState;
    const elapsedMs = track ? track.elapsedMs : 0;
    const durationMs = track ? track.durationMs : 300;

    const t = transformRef.current;

    // 1. Position Offset Calculations
    let targetPosY = 0;
    let targetPosX = 0;

    switch (animState) {
      case 'HOVER':
        // Gentle levitation with subtle breathing sine wave
        targetPosY = hoverElevation + Math.sin(timeSec * 3.5) * 0.02;
        break;
      case 'SELECTED':
        targetPosY = hoverElevation * 1.2;
        break;
      case 'ERROR':
        // Rapid horizontal wobble shake decaying to 0
        targetPosX = computeShakeOffset(elapsedMs, durationMs, 0.08, 3.5);
        break;
      case 'LOADING':
        targetPosY = Math.sin(timeSec * 4) * 0.03;
        break;
      default:
        targetPosY = 0;
        targetPosX = 0;
        break;
    }

    t.positionOffset[0] = animState === 'ERROR' ? targetPosX : springDamp(t.positionOffset[0], 0, 15, dt);
    t.positionOffset[1] = springDamp(t.positionOffset[1], targetPosY, 12, dt);
    t.positionOffset[2] = springDamp(t.positionOffset[2], 0, 15, dt);

    // 2. Scale Calculations
    let targetScale = 1.0;
    switch (animState) {
      case 'HOVER':
        targetScale = hoverScale;
        break;
      case 'SELECTED':
        targetScale = hoverScale * 1.02;
        break;
      case 'ACTIVE':
        targetScale = computeActivationScale(elapsedMs, durationMs);
        break;
      case 'LOADING':
        targetScale = sinePulse(timeSec, 1.5, 0.96, 1.04);
        break;
      case 'SUCCESS':
        targetScale = sinePulse(elapsedMs / 1000, 2.0, 1.0, 1.1);
        break;
      default:
        targetScale = 1.0;
        break;
    }

    if (animState === 'ACTIVE') {
      t.scale = [targetScale, targetScale, targetScale];
    } else {
      const s = springDamp(t.scale[0], targetScale, 14, dt);
      t.scale = [s, s, s];
    }

    // 3. Emissive & Color Modulation
    let targetEmissive = 1.0;
    let targetColor = baseColor;

    switch (animState) {
      case 'HOVER':
        targetEmissive = 2.4;
        targetColor = '#00e5ff';
        break;
      case 'SELECTED':
        targetEmissive = 2.8;
        targetColor = '#ffab00';
        break;
      case 'ACTIVE':
        targetEmissive = 3.8;
        targetColor = '#ffffff';
        break;
      case 'SUCCESS':
        targetEmissive = 3.2;
        targetColor = '#00e676'; // Emerald success pulse
        break;
      case 'ERROR':
        targetEmissive = 2.9;
        targetColor = '#ff1744'; // Crimson error flash
        break;
      case 'LOADING':
        targetEmissive = sinePulse(timeSec, 2.0, 1.2, 2.6);
        targetColor = '#00e5ff';
        break;
      default:
        targetEmissive = 1.0;
        targetColor = baseColor;
        break;
    }

    t.emissiveIntensity = springDamp(t.emissiveIntensity, targetEmissive, 12, dt);
    t.colorHex = targetColor;
  });

  const triggerOneShot = (transientState: 'ACTIVE' | 'SUCCESS' | 'ERROR') => {
    setCurrentAnimState(transientState);
    AnimationRegistry.setTrackState(id, transientState);
  };

  return {
    transform: transformRef.current,
    currentState: currentAnimState,
    triggerOneShot,
  };
};
