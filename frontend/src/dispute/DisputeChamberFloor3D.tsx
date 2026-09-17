import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { DISPUTE_THEME } from './disputeLayout';

export const DisputeChamberFloor3D: React.FC = () => {
  const innerRingRef = useRef<THREE.Mesh>(null);
  const outerRingRef = useRef<THREE.Mesh>(null);

  useFrame((_, delta) => {
    if (innerRingRef.current) {
      innerRingRef.current.rotation.z += delta * 0.08;
    }
    if (outerRingRef.current) {
      outerRingRef.current.rotation.z -= delta * 0.04;
    }
  });

  // 8 Perimeter Arbitration Monolith Pillars
  const monoliths = Array.from({ length: 8 }, (_, i) => {
    const angle = (i * Math.PI) / 4;
    const radius = 10.5;
    return {
      id: `monolith-${i}`,
      x: Math.cos(angle) * radius,
      z: Math.sin(angle) * radius,
    };
  });

  return (
    <group name="DisputeChamberFloor">
      {/* 1. Heavy Base Chamber Slab */}
      <mesh position={[0, -0.2, 0]} receiveShadow>
        <cylinderGeometry args={[11.5, 11.8, 0.4, 48]} />
        <meshStandardMaterial
          color={DISPUTE_THEME.darkBase}
          roughness={0.7}
          metalness={0.5}
        />
      </mesh>

      {/* 2. Top Obsidian Deck Plate */}
      <mesh position={[0, 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <ringGeometry args={[0, 11.2, 48]} />
        <meshStandardMaterial
          color="#140202"
          roughness={0.3}
          metalness={0.8}
        />
      </mesh>

      {/* 3. Perimeter Caution Stripes Ring */}
      <mesh position={[0, 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[10.6, 11.1, 48]} />
        <meshStandardMaterial
          color="#dc2626"
          roughness={0.3}
          metalness={0.4}
          emissive="#dc2626"
          emissiveIntensity={0.3}
        />
      </mesh>

      {/* 4. Concentric Glowing Crimson Arbitration Circuit Rings */}
      <mesh ref={innerRingRef} position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[3.8, 3.95, 64]} />
        <meshBasicMaterial
          color={DISPUTE_THEME.primary}
          transparent
          opacity={0.8}
          side={THREE.DoubleSide}
        />
      </mesh>
      <mesh ref={outerRingRef} position={[0, 0.018, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[6.4, 6.55, 64]} />
        <meshBasicMaterial
          color={DISPUTE_THEME.secondary}
          transparent
          opacity={0.65}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 5. Overhead Chamber Sign */}
      <group position={[0, 5.2, -4.8]}>
        <Text
          fontSize={0.38}
          color={DISPUTE_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.12}
        >
          DISPUTE RESOLUTION & ARBITRATION CHAMBER // SECTOR 10
        </Text>
        <Text
          position={[0, -0.42, 0]}
          fontSize={0.16}
          color={DISPUTE_THEME.secondary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          3D DEFECT HOLOTANK • IMMUTABLE EVIDENCE • STAFF MEDIATION • ADMIN ARBITRATION
        </Text>
      </group>

      {/* 6. Station Conduit Lines */}
      {/* Conduit to Defect Holotank */}
      <mesh position={[0, 0.022, -0.9]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[0.08, 1.8]} />
        <meshBasicMaterial color={DISPUTE_THEME.primary} opacity={0.7} transparent />
      </mesh>
      {/* Conduit to Evidence Carousel */}
      <mesh position={[-1.8, 0.022, 0.9]} rotation={[-Math.PI / 2, 0, -0.46]}>
        <planeGeometry args={[0.08, 4.0]} />
        <meshBasicMaterial color={DISPUTE_THEME.primary} opacity={0.7} transparent />
      </mesh>
      {/* Conduit to Staff Console */}
      <mesh position={[1.8, 0.022, 0.9]} rotation={[-Math.PI / 2, 0, 0.46]}>
        <planeGeometry args={[0.08, 4.0]} />
        <meshBasicMaterial color={DISPUTE_THEME.primary} opacity={0.7} transparent />
      </mesh>
      {/* Conduit to Admin Dais */}
      <mesh position={[0, 0.022, 2.1]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[0.08, 4.2]} />
        <meshBasicMaterial color={DISPUTE_THEME.secondary} opacity={0.8} transparent />
      </mesh>

      {/* 7. Perimeter Arbitration Monolith Pillars & Floating Crimson Shards */}
      {monoliths.map((mono) => (
        <group key={mono.id} position={[mono.x, 0, mono.z]}>
          {/* Main Obsidian Obelisk */}
          <mesh position={[0, 2.5, 0]}>
            <boxGeometry args={[0.5, 5.0, 0.5]} />
            <meshStandardMaterial
              color="#1c0707"
              metalness={0.8}
              roughness={0.2}
            />
          </mesh>
          {/* Crimson Energy Bands */}
          <mesh position={[0, 1.4, 0]}>
            <boxGeometry args={[0.55, 0.15, 0.55]} />
            <meshStandardMaterial
              color={DISPUTE_THEME.primary}
              emissive={DISPUTE_THEME.primary}
              emissiveIntensity={0.8}
            />
          </mesh>
          <mesh position={[0, 3.8, 0]}>
            <boxGeometry args={[0.55, 0.15, 0.55]} />
            <meshStandardMaterial
              color={DISPUTE_THEME.primary}
              emissive={DISPUTE_THEME.primary}
              emissiveIntensity={0.8}
            />
          </mesh>
          {/* Floating Levitating Shard on Top */}
          <mesh position={[0, 5.3, 0]} rotation={[0.4, 0.4, 0]}>
            <octahedronGeometry args={[0.22]} />
            <meshStandardMaterial
              color={DISPUTE_THEME.primary}
              emissive={DISPUTE_THEME.primary}
              emissiveIntensity={1.4}
              roughness={0.1}
            />
          </mesh>
          <pointLight
            position={[0, 5.2, 0]}
            color={DISPUTE_THEME.primary}
            intensity={0.6}
            distance={8}
          />
        </group>
      ))}
    </group>
  );
};
