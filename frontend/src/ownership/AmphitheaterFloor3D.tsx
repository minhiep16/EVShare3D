import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import type { Group } from 'three';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';

export const AmphitheaterFloor3D: React.FC = () => {
  const haloRef = useRef<Group>(null);

  useFrame((_, delta) => {
    if (haloRef.current) {
      haloRef.current.rotation.y += delta * 0.15;
    }
  });

  const {
    HALL_RADIUS,
    CENTRAL_DAIS_RADIUS,
    CENTRAL_DAIS_HEIGHT,
    THEME,
  } = CO_OWNERSHIP_HALL_LAYOUT;

  return (
    <group name="AmphitheaterFloor3D">
      {/* 1. Base Dark Obsidian Circular Amphitheater Slab */}
      <mesh position={[0, 0.05, 0]} receiveShadow>
        <cylinderGeometry args={[HALL_RADIUS, HALL_RADIUS + 0.5, 0.1, 64]} />
        <meshStandardMaterial
          color={THEME.FLOOR_OBSIDIAN}
          roughness={0.2}
          metalness={0.85}
        />
      </mesh>

      {/* 2. Outer Tier Risers (Stepped Amphitheater Rings) */}
      {/* Tier 1 - Outer */}
      <mesh position={[0, 0.15, 0]} receiveShadow>
        <cylinderGeometry args={[HALL_RADIUS - 0.8, HALL_RADIUS - 0.8, 0.12, 64]} />
        <meshStandardMaterial
          color="#0f172a"
          roughness={0.3}
          metalness={0.7}
        />
      </mesh>

      {/* Tier 2 - Mid Ring */}
      <mesh position={[0, 0.25, 0]} receiveShadow>
        <cylinderGeometry args={[HALL_RADIUS - 3.5, HALL_RADIUS - 3.5, 0.12, 64]} />
        <meshStandardMaterial
          color="#0b0f19"
          roughness={0.3}
          metalness={0.75}
        />
      </mesh>

      {/* 3. Perimeter Radiant Gold Boundary Ring */}
      <mesh position={[0, 0.11, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[HALL_RADIUS - 0.25, HALL_RADIUS, 64]} />
        <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} transparent opacity={0.85} />
      </mesh>

      {/* 4. Tier Step Glow Accent Ring */}
      <mesh position={[0, 0.22, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[HALL_RADIUS - 3.6, HALL_RADIUS - 3.45, 64]} />
        <meshBasicMaterial color={THEME.AMBER_DARK} transparent opacity={0.6} />
      </mesh>

      {/* 5. Radial Inlaid Golden Floor Runways (Pointing inwards from perimeter) */}
      {[0, Math.PI / 4, Math.PI / 2, (3 * Math.PI) / 4, Math.PI, (5 * Math.PI) / 4, (3 * Math.PI) / 2, (7 * Math.PI) / 4].map(
        (angle, idx) => {
          const length = HALL_RADIUS - CENTRAL_DAIS_RADIUS - 0.6;
          const midDist = CENTRAL_DAIS_RADIUS + length / 2;
          const x = Math.cos(angle) * midDist;
          const z = Math.sin(angle) * midDist;

          return (
            <mesh
              key={idx}
              position={[x, 0.26, z]}
              rotation={[-Math.PI / 2, 0, -angle]}
            >
              <planeGeometry args={[0.08, length]} />
              <meshBasicMaterial color={THEME.GOLD_ACCENT_GLOW} transparent opacity={0.4} />
            </mesh>
          );
        }
      )}

      {/* 6. Central Elevated Syndicate Dais */}
      <mesh position={[0, CENTRAL_DAIS_HEIGHT / 2, 0]} receiveShadow>
        <cylinderGeometry args={[CENTRAL_DAIS_RADIUS, CENTRAL_DAIS_RADIUS + 0.2, CENTRAL_DAIS_HEIGHT, 48]} />
        <meshStandardMaterial
          color="#030712"
          roughness={0.15}
          metalness={0.9}
        />
      </mesh>

      {/* Dais Golden Edge Ring */}
      <mesh position={[0, CENTRAL_DAIS_HEIGHT + 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[CENTRAL_DAIS_RADIUS - 0.15, CENTRAL_DAIS_RADIUS, 48]} />
        <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} />
      </mesh>

      {/* 7. Suspended Overhead Golden Illumination Halo */}
      <group ref={haloRef} position={[0, 7.5, 0]}>
        <mesh rotation={[Math.PI / 2, 0, 0]}>
          <torusGeometry args={[CENTRAL_DAIS_RADIUS + 1.2, 0.05, 16, 64]} />
          <meshBasicMaterial color={THEME.NEON_GOLD} />
        </mesh>
        <pointLight
          color={THEME.GOLD_ACCENT_PRIMARY}
          intensity={1.2}
          distance={16}
          decay={2}
          position={[0, -0.5, 0]}
        />
      </group>
    </group>
  );
};
