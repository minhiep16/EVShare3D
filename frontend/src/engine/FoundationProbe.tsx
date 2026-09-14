import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import type { Mesh, Group } from 'three';

/**
 * Minimal foundational diagnostic probe to verify 3D scene rendering,
 *
 * Strict Checkpoint Rule: NO EVShare world yet.
 */
export const FoundationProbe: React.FC = () => {
  const cubeRef = useRef<Mesh>(null);
  const ringRef = useRef<Group>(null);

  useFrame((_, delta) => {
    if (cubeRef.current) {
      cubeRef.current.rotation.x += delta * 0.4;
      cubeRef.current.rotation.y += delta * 0.6;
    }
    if (ringRef.current) {
      ringRef.current.rotation.z -= delta * 0.3;
    }
  });

  return (
    <group position={[0, 1.2, 0]}>
      {/* Central Diagnostic Geometry */}
      <mesh ref={cubeRef} castShadow receiveShadow>
        <boxGeometry args={[1.5, 1.5, 1.5]} />
        <meshStandardMaterial
          color="#00e5ff"
          metalness={0.8}
          roughness={0.2}
          wireframe={false}
        />
      </mesh>

      {/* Orbiting Wireframe Foundation Ring */}
      <group ref={ringRef}>
        <mesh rotation={[Math.PI / 3, 0, 0]}>
          <torusGeometry args={[2.2, 0.03, 16, 64]} />
          <meshBasicMaterial color="#00e5ff" wireframe />
        </mesh>
      </group>

      {/* Ground Glow Indicator */}
      <mesh position={[0, -1.19, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.8, 2.5, 32]} />
        <meshBasicMaterial
          color="#00e5ff"
          transparent
          opacity={0.15}
        />
      </mesh>
    </group>
  );
};
