import React, { useEffect } from 'react';
import { useThree } from '@react-three/fiber';
import { usePerformanceStore } from '../performance/usePerformanceStore';
import { useTouchStore } from './useTouchStore';
import { useCameraStore } from '@/stores/useCameraStore';

/**
 * ResponsiveViewportController
 * Dynamically adapts the 3D viewport FOV, aspect ratio, and graphics performance
 * profile for mobile and tablet devices.
 */
export const ResponsiveViewportController: React.FC = () => {
  const { size, camera } = useThree();
  const setTier = usePerformanceStore((state) => state.setTier);
  const setAdaptiveDpr = usePerformanceStore((state) => state.setAdaptiveDpr);
  const setTouchDevice = useTouchStore((state) => state.setTouchDevice);

  useEffect(() => {
    const isTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    setTouchDevice(isTouch);

    const isMobile = window.innerWidth <= 768;
    const isTablet = window.innerWidth > 768 && window.innerWidth <= 1024;

    // Mobile / Tablet Performance Adaptation
    if (isMobile) {
      setTier('LOW');
      setAdaptiveDpr(Math.min(window.devicePixelRatio || 1, 1.25));
    } else if (isTablet || isTouch) {
      setTier('MEDIUM');
      setAdaptiveDpr(Math.min(window.devicePixelRatio || 1, 1.5));
    }
  }, [setTier, setAdaptiveDpr, setTouchDevice]);

  // Handle responsive camera FOV scaling for narrow mobile viewports
  useEffect(() => {
    const aspect = size.width / size.height;
    const baseFov = useCameraStore.getState().desiredFov;

    if (aspect < 1.0) {
      // Portrait mode: Expand vertical FOV to preserve horizontal framing
      const fovMultiplier = Math.min(1.4, 1.0 / aspect);
      const adaptedFov = Math.round(baseFov * fovMultiplier);
      if ('fov' in camera) {
        (camera as any).fov = adaptedFov;
        camera.updateProjectionMatrix();
      }
    }
  }, [size.width, size.height, camera]);

  return null;
};
