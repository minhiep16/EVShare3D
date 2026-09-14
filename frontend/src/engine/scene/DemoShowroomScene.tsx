import React, { useEffect, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import type { Group, Mesh } from 'three';
import { CollisionEngine } from '../player/collisionEngine';
import { TeleportPad } from '../player/TeleportPad';
import { useWorldStore } from '@/stores/useWorldStore';

export const DemoShowroomScene: React.FC = () => {
  const ringRef = useRef<Group>(null);
  const coreRef = useRef<Mesh>(null);
  const setActiveRoomBounds = useWorldStore((state) => state.setActiveRoomBounds);

  useEffect(() => {
    // 1. Showroom Room Boundaries
    setActiveRoomBounds({ minX: -18, maxX: 18, minZ: -18, maxZ: 18 });

    // 2. Register Center Pedestal Obstacle Collider
    const unregisterCore = CollisionEngine.registerCylinderCollider({
      id: 'showroom_pedestal',
      center: [0, 0, 0],
      radius: 1.2,
      height: 2.5,
    });

    // 3. Register Corner Pillars Colliders
    const pillarCleanups: (() => void)[] = [];
    [-3.5, 3.5].forEach((x) => {
      [-3.5, 3.5].forEach((z) => {
        pillarCleanups.push(
          CollisionEngine.registerCylinderCollider({
            id: `pillar_${x}_${z}`,
            center: [x, 0, z],
            radius: 0.35,
            height: 3.0,
          })
        );
      });
    });

    return () => {
      unregisterCore();
      pillarCleanups.forEach((cleanup) => cleanup());
      setActiveRoomBounds(null);
    };
  }, [setActiveRoomBounds]);

  useFrame((_, delta) => {
    if (ringRef.current) {
      ringRef.current.rotation.y -= delta * 0.4;
    }
    if (coreRef.current) {
      coreRef.current.rotation.x += delta * 0.3;
      coreRef.current.rotation.z += delta * 0.2;
    }
  });

  return (
    <group name="DemoShowroomScene" position={[0, 0, 0]}>
      {/* Polished Circular Showroom Turntable Platform */}
      <mesh position={[0, 0.15, 0]} receiveShadow castShadow>
        <cylinderGeometry args={[4.5, 4.8, 0.3, 64]} />
        <meshStandardMaterial
          color="#121620"
          metalness={0.7}
          roughness={0.2}
        />
      </mesh>

      {/* Outer Golden/Amber Telemetry Ring */}
      <group ref={ringRef} position={[0, 0.32, 0]}>
        <mesh rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[4.1, 4.3, 64]} />
          <meshBasicMaterial color="#ffab00" transparent opacity={0.6} />
        </mesh>
      </group>

      {/* Central Holographic Vehicle Pedestal Marker */}
      <mesh ref={coreRef} position={[0, 1.8, 0]} castShadow>
        <dodecahedronGeometry args={[0.8]} />
        <meshStandardMaterial
          color="#ffffff"
          emissive="#ffab00"
          emissiveIntensity={0.8}
          metalness={0.85}
          roughness={0.15}
        />
      </mesh>

      {/* 4 Architectural Corner Light Pillars */}
      {[-3.5, 3.5].map((x) =>
        [-3.5, 3.5].map((z) => (
          <mesh key={`${x}_${z}`} position={[x, 1.5, z]} castShadow receiveShadow>
            <cylinderGeometry args={[0.1, 0.1, 3, 16]} />
            <meshStandardMaterial
              color="#ffab00"
              emissive="#ffab00"
              emissiveIntensity={0.5}
            />
          </mesh>
        ))
      )}

      {/* Showroom Exit Teleport Pad */}
      <TeleportPad
        id="showroom_teleport_pad"
        name="Showroom Platform Exit"
        position={[0, 0, 5.5]}
        targetPosition={[0, 0, -5.5]}
        color="#ffab00"
      />
    </group>
  );
};
