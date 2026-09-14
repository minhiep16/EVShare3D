import React from 'react';
import { useFrame } from '@react-three/fiber';
import { AnimationRegistry } from './AnimationRegistry';

/**
 * AnimationManager
 * Mounts in the 3D Canvas as the single master animation director.
 * Drives all spatial animation tracks via centralized delta ticks and
 * strictly guarantees that finished one-shot animations terminate without
 * uncontrolled loop iterations or orphaned timers.
 */
export const AnimationManager: React.FC = () => {
  useFrame((_, delta) => {
    const dt = Math.min(delta, 0.1);
    const deltaMs = dt * 1000;
    AnimationRegistry.tickAll(deltaMs);
  });

  return null;
};
