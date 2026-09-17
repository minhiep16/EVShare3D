import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group, Mesh } from 'three';
import { CONTRACT_LAYOUT } from './contractLayout';

export const ContractRoomFloor3D: React.FC = () => {
  const outerCircuitRef = useRef<Group>(null);
  const pulseRingsRef = useRef<Mesh>(null);

  // Subtle rotating micro-animation on outer executive legal circuit
  useFrame((_, delta) => {
    if (outerCircuitRef.current) {
      outerCircuitRef.current.rotation.y += delta * 0.03;
    }
    if (pulseRingsRef.current) {
      const scale = 1 + Math.sin(Date.now() * 0.0012) * 0.015;
      pulseRingsRef.current.scale.set(scale, scale, 1);
    }
  });

  const pylonCount = 8;
  const pylonRadius = 14.5;
  const pylons = Array.from({ length: pylonCount }).map((_, i) => {
    const angle = (i * 2 * Math.PI) / pylonCount;
    const x = Math.cos(angle) * pylonRadius;
    const z = Math.sin(angle) * pylonRadius;
    return { x, z, angle, id: i };
  });

  return (
    <group name="ContractRoomFloor">
      {/* 1. Heavy Executive Slate Base */}
      <mesh position={[0, -0.2, 0]} receiveShadow>
        <cylinderGeometry args={[16.0, 16.4, 0.4, 64]} />
        <meshStandardMaterial
          color={CONTRACT_LAYOUT.theme.slateDark}
          metalness={0.9}
          roughness={0.2}
        />
      </mesh>

      {/* 2. Inner Polished Dark Obsidian Deck */}
      <mesh position={[0, 0.01, 0]} receiveShadow rotation={[-Math.PI / 2, 0, 0]}>
        <circleGeometry args={[14.8, 64]} />
        <meshStandardMaterial
          color={CONTRACT_LAYOUT.theme.slateSurface}
          metalness={0.8}
          roughness={0.25}
        />
      </mesh>

      {/* 3. Rotating Outer Legal Circuit Teeth */}
      <group ref={outerCircuitRef} position={[0, 0.02, 0]}>
        {Array.from({ length: 24 }).map((_, i) => {
          const angle = (i * 2 * Math.PI) / 24;
          const x = Math.cos(angle) * 15.5;
          const z = Math.sin(angle) * 15.5;
          return (
            <mesh key={i} position={[x, 0.06, z]} rotation={[0, -angle, 0]}>
              <boxGeometry args={[0.4, 0.12, 0.7]} />
              <meshStandardMaterial
                color="#1e293b"
                metalness={0.9}
                roughness={0.2}
              />
            </mesh>
          );
        })}
      </group>

      {/* 4. Concentric Glowing Sapphire Circuit Lines */}
      <mesh
        ref={pulseRingsRef}
        position={[0, 0.03, 0]}
        rotation={[-Math.PI / 2, 0, 0]}
      >
        <ringGeometry args={[13.2, 13.35, 64]} />
        <meshBasicMaterial
          color={CONTRACT_LAYOUT.theme.sapphirePrimary}
          transparent
          opacity={0.65}
        />
      </mesh>

      <mesh position={[0, 0.03, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[8.8, 8.92, 64]} />
        <meshBasicMaterial
          color={CONTRACT_LAYOUT.theme.ceruleanNeon}
          transparent
          opacity={0.45}
        />
      </mesh>

      <mesh position={[0, 0.03, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[4.4, 4.5, 48]} />
        <meshBasicMaterial
          color={CONTRACT_LAYOUT.theme.sapphirePrimary}
          transparent
          opacity={0.35}
        />
      </mesh>

      {/* 5. 8 Perimeter Executive Slate Pylons with Blue Beacons */}
      <group name="PerimeterLegalPylons">
        {pylons.map((p) => (
          <group key={p.id} position={[p.x, 0, p.z]} rotation={[0, -p.angle, 0]}>
            {/* Pylon Column */}
            <mesh position={[0, 1.2, 0]} castShadow receiveShadow>
              <cylinderGeometry args={[0.25, 0.38, 2.4, 16]} />
              <meshStandardMaterial
                color="#0f172a"
                metalness={0.95}
                roughness={0.15}
              />
            </mesh>

            {/* Glowing Sapphire Beacon Ring */}
            <mesh position={[0, 2.45, 0]}>
              <cylinderGeometry args={[0.2, 0.22, 0.1, 16]} />
              <meshBasicMaterial color={CONTRACT_LAYOUT.theme.sapphirePrimary} />
            </mesh>

            {/* Pylon Head Cap */}
            <mesh position={[0, 2.54, 0]}>
              <coneGeometry args={[0.24, 0.18, 16]} />
              <meshStandardMaterial color="#1e293b" metalness={0.9} roughness={0.2} />
            </mesh>

            {/* Subtle Point Light for Atmospheric Legal Ambience */}
            <pointLight
              color={CONTRACT_LAYOUT.theme.sapphirePrimary}
              intensity={0.7}
              distance={7}
              decay={2}
              position={[0, 2.6, 0]}
            />
          </group>
        ))}
      </group>

      {/* 6. Inscribed Floor Markings */}
      <group position={[0, 0.04, 7.5]} rotation={[-Math.PI / 2, 0, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.42}
          color={CONTRACT_LAYOUT.theme.sapphirePrimary}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          EXECUTIVE LEGAL SUITE • DIGITAL CONTRACT ROOM
        </Text>
        <Text
          position={[0, -0.38, 0]}
          fontSize={0.2}
          color={CONTRACT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          LAW ON ELECTRONIC TRANSACTIONS COMPLIANT • SHA-256 MULTI-SIG RATIFIED
        </Text>
      </group>
    </group>
  );
};
