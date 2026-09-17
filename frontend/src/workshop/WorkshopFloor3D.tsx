import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { WORKSHOP_THEME } from './workshopLayout';

export const WorkshopFloor3D: React.FC = () => {
  const guideRingRef = useRef<THREE.Mesh>(null);

  useFrame((_, delta) => {
    if (guideRingRef.current) {
      guideRingRef.current.rotation.z += delta * 0.06;
    }
  });

  // 8 Heavy Structural Trusses at perimeter
  const trusses = Array.from({ length: 8 }, (_, i) => {
    const angle = (i * Math.PI) / 4;
    const radius = 10.5;
    return {
      id: `truss-${i}`,
      x: Math.cos(angle) * radius,
      z: Math.sin(angle) * radius,
    };
  });

  return (
    <group name="WorkshopFloor">
      {/* 1. Heavy Base Workshop Slab */}
      <mesh position={[0, -0.2, 0]} receiveShadow>
        <cylinderGeometry args={[11.5, 11.8, 0.4, 48]} />
        <meshStandardMaterial
          color={WORKSHOP_THEME.darkBase}
          roughness={0.7}
          metalness={0.5}
        />
      </mesh>

      {/* 2. Top Steel Diamond Plate Deck */}
      <mesh position={[0, 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <ringGeometry args={[0, 11.2, 48]} />
        <meshStandardMaterial
          color="#18181b"
          roughness={0.4}
          metalness={0.7}
        />
      </mesh>

      {/* 3. Perimeter Caution Stripes Ring */}
      <mesh position={[0, 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[10.6, 11.1, 48]} />
        <meshStandardMaterial
          color={WORKSHOP_THEME.hazardYellow}
          roughness={0.3}
          metalness={0.3}
          emissive={WORKSHOP_THEME.hazardYellow}
          emissiveIntensity={0.25}
        />
      </mesh>

      {/* 4. Central Diagnostic Guide Ring */}
      <mesh ref={guideRingRef} position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[4.8, 4.95, 64]} />
        <meshBasicMaterial
          color={WORKSHOP_THEME.primary}
          transparent
          opacity={0.65}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 5. Oil Drainage Grates around Central Lift */}
      <group position={[0, 0.02, 0]}>
        <mesh position={[-2.4, 0, 0]}>
          <planeGeometry args={[0.3, 5.4]} />
          <meshStandardMaterial color="#09090b" roughness={0.8} />
        </mesh>
        <mesh position={[2.4, 0, 0]}>
          <planeGeometry args={[0.3, 5.4]} />
          <meshStandardMaterial color="#09090b" roughness={0.8} />
        </mesh>
      </group>

      {/* 6. Overhead Workshop Sign */}
      <group position={[0, 5.2, -4.8]}>
        <Text
          fontSize={0.38}
          color={WORKSHOP_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.12}
        >
          SERVICE WORKSHOP // SECTOR 09
        </Text>
        <Text
          position={[0, -0.42, 0]}
          fontSize={0.16}
          color={WORKSHOP_THEME.secondary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          HYDRAULIC LIFT • MULTI-SYSTEM DIAGNOSTICS • REAL EXPENSE LEDGER AUDIT
        </Text>
      </group>

      {/* 7. Station Cable Conduits on Floor */}
      {/* Conduit to Diagnostic Cart */}
      <mesh position={[-1.8, 0.018, 1.2]} rotation={[-Math.PI / 2, 0, -0.58]}>
        <planeGeometry args={[0.08, 4.2]} />
        <meshBasicMaterial color={WORKSHOP_THEME.primary} opacity={0.6} transparent />
      </mesh>
      {/* Conduit to Parts Rack */}
      <mesh position={[1.9, 0.018, 1.2]} rotation={[-Math.PI / 2, 0, 0.58]}>
        <planeGeometry args={[0.08, 4.4]} />
        <meshBasicMaterial color={WORKSHOP_THEME.primary} opacity={0.6} transparent />
      </mesh>
      {/* Conduit to Work Order Stela */}
      <mesh position={[0, 0.018, -2.4]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[0.08, 4.8]} />
        <meshBasicMaterial color={WORKSHOP_THEME.primary} opacity={0.6} transparent />
      </mesh>

      {/* 8. Perimeter Industrial Steel Trusses & Cyan Fluorescent Lights */}
      {trusses.map((truss) => (
        <group key={truss.id} position={[truss.x, 0, truss.z]}>
          {/* Main Heavy Pillar */}
          <mesh position={[0, 2.5, 0]}>
            <boxGeometry args={[0.5, 5.0, 0.5]} />
            <meshStandardMaterial
              color="#27272a"
              metalness={0.8}
              roughness={0.2}
            />
          </mesh>
          {/* Neon accent rings */}
          <mesh position={[0, 1.5, 0]}>
            <boxGeometry args={[0.55, 0.15, 0.55]} />
            <meshStandardMaterial
              color={WORKSHOP_THEME.primary}
              emissive={WORKSHOP_THEME.primary}
              emissiveIntensity={0.6}
            />
          </mesh>
          <mesh position={[0, 3.8, 0]}>
            <boxGeometry args={[0.55, 0.15, 0.55]} />
            <meshStandardMaterial
              color={WORKSHOP_THEME.primary}
              emissive={WORKSHOP_THEME.primary}
              emissiveIntensity={0.6}
            />
          </mesh>
          {/* Top Inspection Work Light */}
          <mesh position={[0, 5.1, 0]}>
            <cylinderGeometry args={[0.2, 0.25, 0.25, 16]} />
            <meshStandardMaterial
              color={WORKSHOP_THEME.primary}
              emissive={WORKSHOP_THEME.primary}
              emissiveIntensity={1.2}
            />
          </mesh>
          <pointLight
            position={[0, 5.0, 0]}
            color={WORKSHOP_THEME.primary}
            intensity={0.6}
            distance={8}
          />
        </group>
      ))}
    </group>
  );
};
