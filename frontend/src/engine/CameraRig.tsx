import React, { useEffect, useRef } from 'react';
import { useThree } from '@react-three/fiber';
import { PerspectiveCamera, OrbitControls } from '@react-three/drei';
import type { OrbitControls as OrbitControlsImpl } from 'three-stdlib';
import type { PerspectiveCamera as PerspectiveCameraImpl } from 'three';
import { CameraConfig } from './types';
import { useEngineStore } from './engineStore';
import { useCameraStore } from '@/stores/useCameraStore';

interface CameraRigProps {
  config?: Partial<CameraConfig>;
}

const DEFAULT_CAMERA_CONFIG: CameraConfig = {
  fov: 45,
  near: 0.1,
  far: 1000,
  position: [0, 4, 10],
  target: [0, 0, 0],
  enableControls: true,
  minDistance: 1.5,
  maxDistance: 60,
  maxPolarAngle: Math.PI / 2 + 0.05, // Just below horizon, prevents underground flipping
};

export const CameraRig: React.FC<CameraRigProps> = ({ config }) => {
  const mergedConfig: CameraConfig = { ...DEFAULT_CAMERA_CONFIG, ...config };
  const cameraRef = useRef<PerspectiveCameraImpl>(null);
  const controlsRef = useRef<OrbitControlsImpl>(null);
  const { size } = useThree();
  const setDimensions = useEngineStore((state) => state.setDimensions);

  // Sync with useCameraStore
  const storePosition = useCameraStore((state) => state.position);
  const storeTarget = useCameraStore((state) => state.target);
  const storeFov = useCameraStore((state) => state.fov);

  // Sync canvas size to engine store
  useEffect(() => {
    setDimensions(size.width, size.height);
  }, [size.width, size.height, setDimensions]);

  // Synchronize store position & target changes
  useEffect(() => {
    if (cameraRef.current && controlsRef.current) {
      cameraRef.current.position.set(storePosition[0], storePosition[1], storePosition[2]);
      controlsRef.current.target.set(storeTarget[0], storeTarget[1], storeTarget[2]);
      controlsRef.current.update();
    }
  }, [storePosition, storeTarget]);

  // Adjust camera aspect ratio on resize
  useEffect(() => {
    if (cameraRef.current) {
      cameraRef.current.aspect = size.width / size.height;
      const baseFov = storeFov || mergedConfig.fov;

      // Responsive FOV adjustment for narrow/portrait mobile screens
      if (size.width < size.height) {
        cameraRef.current.fov = baseFov * (size.height / size.width) * 0.7;
      } else {
        cameraRef.current.fov = baseFov;
      }
      cameraRef.current.updateProjectionMatrix();
    }
  }, [size.width, size.height, storeFov, mergedConfig.fov]);

  return (
    <>
      <PerspectiveCamera
        ref={cameraRef}
        makeDefault
        fov={storeFov || mergedConfig.fov}
        near={mergedConfig.near}
        far={mergedConfig.far}
        position={storePosition || mergedConfig.position}
      />
      {mergedConfig.enableControls && (
        <OrbitControls
          ref={controlsRef}
          target={storeTarget || mergedConfig.target}
          enableDamping
          dampingFactor={0.05}
          minDistance={mergedConfig.minDistance}
          maxDistance={mergedConfig.maxDistance}
          maxPolarAngle={mergedConfig.maxPolarAngle}
          makeDefault
        />
      )}
    </>
  );
};
