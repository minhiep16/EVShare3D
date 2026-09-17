import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group, Mesh } from 'three';
import { VAULT_LAYOUT } from './vaultLayout';

export const VaultChamberFloor3D: React.FC = () => {
  const outerRingRef = useRef<Group>(null);
  const pulseRingsRef = useRef<Mesh>(null);
  const pylonBeaconsRef = useRef<Group>(null);

  // Gentle rotating micro-animation on heavy vault locking gears
  useFrame((_, delta) => {
    if (outerRingRef.current) {
      outerRingRef.current.rotation.y += delta * 0.04;
    }
    if (pulseRingsRef.current) {
      const scale = 1 + Math.sin(Date.now() * 0.0015) * 0.015;
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

  const gearTeethCount = 24;
  const gearTeeth = Array.from({ length: gearTeethCount }).map((_, i) => {
    const angle = (i * 2 * Math.PI) / gearTeethCount;
    const x = Math.cos(angle) * 15.6;
    const z = Math.sin(angle) * 15.6;
    return { x, z, angle, id: i };
  });

  return (
    <group name="VaultChamberFloor">
      {/* 1. Main Heavy Titanium Foundation */}
      <mesh position={[0, -0.2, 0]} receiveShadow>
        <cylinderGeometry args={[16.0, 16.4, 0.4, 64]} />
        <meshStandardMaterial
          color={VAULT_LAYOUT.theme.titaniumDark}
          metalness={0.9}
          roughness={0.2}
        />
      </mesh>

      {/* 2. Inner Brushed Dark Steel Radial Deck */}
      <mesh position={[0, 0.01, 0]} receiveShadow rotation={[-Math.PI / 2, 0, 0]}>
        <circleGeometry args={[14.8, 64]} />
        <meshStandardMaterial
          color={VAULT_LAYOUT.theme.titaniumLight}
          metalness={0.8}
          roughness={0.3}
        />
      </mesh>

      {/* 3. Rotating Outer Locking Gear Ring */}
      <group ref={outerRingRef} position={[0, 0.02, 0]}>
        {gearTeeth.map((tooth) => (
          <mesh
            key={tooth.id}
            position={[tooth.x, 0.08, tooth.z]}
            rotation={[0, -tooth.angle, 0]}
          >
            <boxGeometry args={[0.5, 0.16, 0.8]} />
            <meshStandardMaterial
              color="#2a384c"
              metalness={0.9}
              roughness={0.2}
            />
          </mesh>
        ))}
      </group>

      {/* 4. Concentric Glowing Amber Circuit Rings */}
      <mesh
        ref={pulseRingsRef}
        position={[0, 0.03, 0]}
        rotation={[-Math.PI / 2, 0, 0]}
      >
        <ringGeometry args={[13.2, 13.35, 64]} />
        <meshBasicMaterial
          color={VAULT_LAYOUT.theme.goldPrimary}
          transparent
          opacity={0.7}
        />
      </mesh>

      <mesh position={[0, 0.03, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[8.8, 8.92, 64]} />
        <meshBasicMaterial
          color={VAULT_LAYOUT.theme.amberAccent}
          transparent
          opacity={0.5}
        />
      </mesh>

      <mesh position={[0, 0.03, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[4.4, 4.5, 48]} />
        <meshBasicMaterial
          color={VAULT_LAYOUT.theme.goldPrimary}
          transparent
          opacity={0.4}
        />
      </mesh>

      {/* 5. 8 Perimeter Heavy Security Pylons */}
      <group ref={pylonBeaconsRef} name="SecurityPylons">
        {pylons.map((p) => (
          <group key={p.id} position={[p.x, 0, p.z]} rotation={[0, -p.angle, 0]}>
            {/* Pylon Column Base */}
            <mesh position={[0, 1.2, 0]} castShadow receiveShadow>
              <cylinderGeometry args={[0.3, 0.45, 2.4, 16]} />
              <meshStandardMaterial
                color="#0f172a"
                metalness={0.95}
                roughness={0.15}
              />
            </mesh>

            {/* Glowing Amber Beacon Ring on Pylon */}
            <mesh position={[0, 2.45, 0]}>
              <cylinderGeometry args={[0.22, 0.25, 0.12, 16]} />
              <meshBasicMaterial color={VAULT_LAYOUT.theme.goldPrimary} />
            </mesh>

            {/* Pylon Head Cap */}
            <mesh position={[0, 2.55, 0]}>
              <coneGeometry args={[0.28, 0.2, 16]} />
              <meshStandardMaterial color="#1e293b" metalness={0.9} roughness={0.2} />
            </mesh>

            {/* Subtle Point Light for Vault Ambience */}
            <pointLight
              color={VAULT_LAYOUT.theme.goldPrimary}
              intensity={0.6}
              distance={7}
              decay={2}
              position={[0, 2.6, 0]}
            />
          </group>
        ))}
      </group>

      {/* 6. Inscribed In-World Spatial Sector Markings */}
      <group position={[0, 0.04, 7.5]} rotation={[-Math.PI / 2, 0, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.45}
          color={VAULT_LAYOUT.theme.goldPrimary}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          SYNDICATE TREASURY • SECURE VAULT
        </Text>
        <Text
          position={[0, -0.4, 0]}
          fontSize={0.22}
          color={VAULT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          CRYPTOGRAPHIC MULTI-SIG RESERVE • BR-FIN-03 PROTECTED
        </Text>
      </group>
    </group>
  );
};
