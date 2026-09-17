import React, { useMemo } from 'react';
import * as THREE from 'three';
import { FINANCE_LAYOUT } from './financeLayout';

export const FinanceCenterFloor3D: React.FC = () => {
  const { floorRadius, theme } = FINANCE_LAYOUT;

  // Concentric guideline rings
  const ringGeometries = useMemo(() => {
    return [4.0, 8.0, 12.0, 15.8].map((radius) => {
      const pts: THREE.Vector3[] = [];
      const segments = 64;
      for (let i = 0; i <= segments; i++) {
        const theta = (i / segments) * Math.PI * 2;
        pts.push(new THREE.Vector3(Math.cos(theta) * radius, 0.02, Math.sin(theta) * radius));
      }
      return new THREE.BufferGeometry().setFromPoints(pts);
    });
  }, []);

  // Perimeter atmospheric pylons
  const pylons = useMemo(() => {
    const list: { pos: [number, number, number]; angle: number }[] = [];
    const count = 10;
    for (let i = 0; i < count; i++) {
      const theta = (i / count) * Math.PI * 2;
      const x = Math.cos(theta) * (floorRadius - 0.6);
      const z = Math.sin(theta) * (floorRadius - 0.6);
      list.push({ pos: [x, 0, z], angle: theta });
    }
    return list;
  }, [floorRadius]);

  return (
    <group name="FinanceCenterFloor">
      {/* 1. Base Cleanroom Floor Disc */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.05, 0]} receiveShadow>
        <cylinderGeometry args={[floorRadius, floorRadius + 0.5, 0.1, 64]} />
        <meshStandardMaterial
          color="#050c18"
          roughness={0.25}
          metalness={0.8}
        />
      </mesh>

      {/* 2. Concentric Emerald & Cyan Guideline Rings */}
      {ringGeometries.map((geom, idx) => (
        <primitive
          key={`ring_${idx}`}
          object={new THREE.Line(
            geom,
            new THREE.LineBasicMaterial({
              color: idx % 2 === 0 ? theme.emeraldPrimary : theme.cyanAccent,
              transparent: true,
              opacity: 0.35 + idx * 0.1,
            })
          )}
        />
      ))}

      {/* 3. Outer Neon Curb Ring */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.03, 0]}>
        <ringGeometry args={[floorRadius - 0.3, floorRadius, 64]} />
        <meshBasicMaterial
          color={theme.emeraldPrimary}
          transparent
          opacity={0.7}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 4. Radial Data Conduits linking stations */}
      <mesh rotation={[-Math.PI / 2, 0, Math.PI / 4]} position={[0, 0.015, 0]}>
        <planeGeometry args={[0.08, floorRadius * 1.9]} />
        <meshBasicMaterial color={theme.cyanAccent} transparent opacity={0.3} />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, -Math.PI / 4]} position={[0, 0.015, 0]}>
        <planeGeometry args={[0.08, floorRadius * 1.9]} />
        <meshBasicMaterial color={theme.emeraldPrimary} transparent opacity={0.3} />
      </mesh>

      {/* 5. Perimeter Data Pylons with Emerald Beacons */}
      {pylons.map((p, idx) => (
        <group key={`pylon_${idx}`} position={p.pos}>
          {/* Base stalk */}
          <mesh position={[0, 1.2, 0]}>
            <cylinderGeometry args={[0.12, 0.16, 2.4, 16]} />
            <meshStandardMaterial color="#0c1726" metalness={0.9} roughness={0.3} />
          </mesh>
          {/* Glowing Beacon Head */}
          <mesh position={[0, 2.45, 0]}>
            <octahedronGeometry args={[0.22, 0]} />
            <meshStandardMaterial
              color={theme.emeraldPrimary}
              emissive={theme.emeraldPrimary}
              emissiveIntensity={0.8}
              wireframe
            />
          </mesh>
          {/* Vertical Light Strip */}
          <mesh position={[0, 1.2, 0.13]}>
            <planeGeometry args={[0.04, 2.0]} />
            <meshBasicMaterial color={theme.emeraldPrimary} />
          </mesh>
        </group>
      ))}

      {/* 6. Suspended Overhead Cyber Luminance Halo */}
      <mesh position={[0, 7.5, 0]} rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[8.5, 0.08, 16, 64]} />
        <meshBasicMaterial color={theme.emeraldPrimary} transparent opacity={0.5} />
      </mesh>
      <mesh position={[0, 7.5, 0]} rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[12.5, 0.05, 16, 64]} />
        <meshBasicMaterial color={theme.cyanAccent} transparent opacity={0.3} />
      </mesh>
    </group>
  );
};
