import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group } from 'three';
import { create } from 'zustand';
import type { SectorId } from './worldTypes';
import { SECTOR_METADATA_REGISTRY } from './worldCoordinates';

interface WorldLoaderState {
  loadedSectors: Set<SectorId>;
  activeLoadingSector: SectorId | null;
  loadingProgress: number; // 0 - 100
  isStreaming: boolean;

  startLoadingSector: (sectorId: SectorId) => void;
  updateProgress: (progress: number) => void;
  finishLoadingSector: (sectorId: SectorId) => void;
  isSectorLoaded: (sectorId: SectorId) => boolean;
}

export const useWorldLoaderStore = create<WorldLoaderState>((set, get) => ({
  loadedSectors: new Set<SectorId>(['SECURITY_CHECKPOINT', 'CENTRAL_GARAGE']),
  activeLoadingSector: null,
  loadingProgress: 100,
  isStreaming: false,

  startLoadingSector: (sectorId: SectorId) => {
    set({
      activeLoadingSector: sectorId,
      loadingProgress: 0,
      isStreaming: true,
    });
  },

  updateProgress: (progress: number) => {
    set({ loadingProgress: Math.min(100, Math.max(0, progress)) });
  },

  finishLoadingSector: (sectorId: SectorId) => {
    const updated = new Set(get().loadedSectors);
    updated.add(sectorId);
    set({
      loadedSectors: updated,
      activeLoadingSector: null,
      loadingProgress: 100,
      isStreaming: false,
    });
  },

  isSectorLoaded: (sectorId: SectorId) => {
    return get().loadedSectors.has(sectorId);
  },
}));

/**
 * 3D Holographic Loading Beacon displayed in pure 3D space when a sector is streaming.
 */
export const WorldLoader: React.FC = () => {
  const isStreaming = useWorldLoaderStore((state) => state.isStreaming);
  const activeSector = useWorldLoaderStore((state) => state.activeLoadingSector);
  const progress = useWorldLoaderStore((state) => state.loadingProgress);

  const ring1Ref = useRef<Group>(null);
  const ring2Ref = useRef<Group>(null);

  useFrame((_, delta) => {
    if (!isStreaming) return;
    if (ring1Ref.current) {
      ring1Ref.current.rotation.y += delta * 2.0;
      ring1Ref.current.rotation.x += delta * 0.8;
    }
    if (ring2Ref.current) {
      ring2Ref.current.rotation.y -= delta * 1.5;
      ring2Ref.current.rotation.z += delta * 1.2;
    }
  });

  if (!isStreaming || !activeSector) return null;

  const sectorMeta = SECTOR_METADATA_REGISTRY[activeSector];
  const center = sectorMeta ? sectorMeta.centerCoordinates : [0, 0, 0];

  return (
    <group position={[center[0], center[1] + 3, center[2]]} name="World3DLoader">
      {/* Outer Holographic Spinner Ring */}
      <group ref={ring1Ref}>
        <mesh>
          <torusGeometry args={[1.5, 0.04, 16, 64]} />
          <meshBasicMaterial color="#00e5ff" wireframe />
        </mesh>
      </group>

      {/* Inner Counter-Rotating Ring */}
      <group ref={ring2Ref}>
        <mesh>
          <torusGeometry args={[1.1, 0.03, 16, 48]} />
          <meshBasicMaterial color="#00e676" wireframe />
        </mesh>
      </group>

      {/* Core Glowing Orb */}
      <mesh>
        <sphereGeometry args={[0.3, 24, 24]} />
        <meshBasicMaterial color="#00e5ff" />
      </mesh>

      {/* 3D SDF Status Text */}
      <Text
        position={[0, -2.0, 0]}
        fontSize={0.32}
        color="#00e5ff"
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
      >
        {`STREAMING ${sectorMeta?.name.toUpperCase() || 'SECTOR'} [${Math.round(progress)}%]`}
      </Text>
    </group>
  );
};
