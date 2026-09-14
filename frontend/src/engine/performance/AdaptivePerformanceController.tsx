import React, { useRef } from 'react';
import { useFrame, useThree } from '@react-three/fiber';
import { usePerformanceStore } from './usePerformanceStore';

/**
 * AdaptivePerformanceController
 * Runs inside R3F Canvas. Continuously samples rendering framerate,
 * dynamically adjusts DPR, and applies quality tier changes to WebGLRenderer.
 */
export const AdaptivePerformanceController: React.FC = () => {
  const gl = useThree((state) => state.gl);
  const activeProfile = usePerformanceStore((state) => state.activeProfile);
  const adaptiveDpr = usePerformanceStore((state) => state.adaptiveDpr);
  const recordFpsSample = usePerformanceStore((state) => state.recordFpsSample);

  const frameCountRef = useRef(0);
  const timeAccumulatorRef = useRef(0);
  const currentDprRef = useRef(adaptiveDpr);

  useFrame((_, delta) => {
    const dt = Math.min(delta, 0.1);
    frameCountRef.current += 1;
    timeAccumulatorRef.current += dt;

    // Sample framerate every 0.5 seconds
    if (timeAccumulatorRef.current >= 0.5) {
      const currentFps = frameCountRef.current / timeAccumulatorRef.current;
      recordFpsSample(currentFps, timeAccumulatorRef.current);

      frameCountRef.current = 0;
      timeAccumulatorRef.current = 0;
    }

    // Smoothly apply adaptive DPR to WebGLRenderer
    if (Math.abs(currentDprRef.current - adaptiveDpr) > 0.05) {
      currentDprRef.current += (adaptiveDpr - currentDprRef.current) * 0.1;
      gl.setPixelRatio(currentDprRef.current);
    }
  });

  return null;
};
