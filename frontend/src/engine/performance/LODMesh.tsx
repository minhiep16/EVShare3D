import React, { useRef, useState } from 'react';
import { useFrame, useThree } from '@react-three/fiber';
import type { Group } from 'three';
import { Vector3 } from 'three';
import { usePerformanceStore } from './usePerformanceStore';

export type LODLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export interface LODMeshProps {
  id?: string;
  position?: [number, number, number];
  rotation?: [number, number, number];
  highDetail: React.ReactNode;
  mediumDetail?: React.ReactNode;
  lowDetail: React.ReactNode;
  nearDistance?: number; // Default 12m
  farDistance?: number;  // Default 25m
}

const _meshWorldPos = new Vector3();

export const LODMesh: React.FC<LODMeshProps> = ({
  position = [0, 0, 0],
  rotation = [0, 0, 0],
  highDetail,
  mediumDetail,
  lowDetail,
  nearDistance = 12,
  farDistance = 25,
}) => {
  const groupRef = useRef<Group>(null);
  const camera = useThree((state) => state.camera);
  const lodBias = usePerformanceStore((state) => state.activeProfile.lodBias);

  const [currentLevel, setCurrentLevel] = useState<LODLevel>('HIGH');

  useFrame(() => {
    if (!groupRef.current) return;

    groupRef.current.getWorldPosition(_meshWorldPos);
    const dist = camera.position.distanceTo(_meshWorldPos);

    // Dynamic distance thresholds influenced by performance tier lodBias
    const effectiveNear = nearDistance / lodBias;
    const effectiveFar = farDistance / lodBias;

    let nextLevel: LODLevel = 'HIGH';
    if (dist >= effectiveFar) {
      nextLevel = 'LOW';
    } else if (dist >= effectiveNear) {
      nextLevel = mediumDetail ? 'MEDIUM' : 'LOW';
    }

    if (nextLevel !== currentLevel) {
      setCurrentLevel(nextLevel);
    }
  });

  return (
    <group ref={groupRef} position={position} rotation={rotation}>
      {currentLevel === 'HIGH' && highDetail}
      {currentLevel === 'MEDIUM' && (mediumDetail || highDetail)}
      {currentLevel === 'LOW' && lowDetail}
    </group>
  );
};
