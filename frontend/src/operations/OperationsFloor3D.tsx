import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { OPERATIONS_THEME, SERVICE_BAYS } from './operationsLayout';

export const OperationsFloor3D: React.FC = () => {
  const beaconRef = useRef<THREE.Group>(null);
  const ringRef = useRef<THREE.Mesh>(null);

  useFrame((_, delta) => {
    if (ringRef.current) {
      ringRef.current.rotation.z += delta * 0.08;
    }
  });

  // 8 Perimeter Industrial Pylons
  const pylons = Array.from({ length: 8 }, (_, i) => {
    const angle = (i * Math.PI) / 4;
    const radius = 10.5;
    return {
      id: `pylon-${i}`,
      x: Math.cos(angle) * radius,
      z: Math.sin(angle) * radius,
    };
  });

  return (
    <group name="OperationsFloor">
      {/* 1. Base Hangar Slab */}
      <mesh position={[0, -0.2, 0]} receiveShadow>
        <cylinderGeometry args={[11.5, 11.8, 0.4, 48]} />
        <meshStandardMaterial
          color={OPERATIONS_THEME.darkBase}
          roughness={0.7}
          metalness={0.4}
        />
      </mesh>

      {/* 2. Top Metallic Floor Deck */}
      <mesh position={[0, 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <ringGeometry args={[0, 11.2, 48]} />
        <meshStandardMaterial
          color="#0b0f19"
          roughness={0.5}
          metalness={0.5}
        />
      </mesh>

      {/* 3. Perimeter Hazard Caution Stripes Ring */}
      <mesh position={[0, 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[10.6, 11.1, 48]} />
        <meshStandardMaterial
          color={OPERATIONS_THEME.hazardYellow}
          roughness={0.4}
          metalness={0.3}
          emissive={OPERATIONS_THEME.hazardYellow}
          emissiveIntensity={0.25}
        />
      </mesh>

      {/* 4. Rotating Tech Guide Ring in Center */}
      <mesh ref={ringRef} position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[4.4, 4.55, 64]} />
        <meshBasicMaterial
          color={OPERATIONS_THEME.primary}
          transparent
          opacity={0.7}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 5. Overhead Hangar Header Sign */}
      <group position={[0, 4.8, -4.8]}>
        <Text
          fontSize={0.38}
          color={OPERATIONS_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.12}
        >
          OPERATIONS & LOGISTICS HANGAR // SECTOR 08
        </Text>
        <Text
          position={[0, -0.4, 0]}
          fontSize={0.16}
          color={OPERATIONS_THEME.secondary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          VEHICLE TELEMATICS • OPTICAL QR DISPATCH • RETURN RECONCILIATION
        </Text>
      </group>

      {/* 6. Station Conduit Lines */}
      {/* To QR Desk */}
      <mesh position={[-1.7, 0.018, 0.6]} rotation={[-Math.PI / 2, 0, -0.34]}>
        <planeGeometry args={[0.08, 3.8]} />
        <meshBasicMaterial color={OPERATIONS_THEME.primary} opacity={0.65} transparent />
      </mesh>
      {/* To Dispatch Console */}
      <mesh position={[0, 0.018, 1.25]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[0.08, 2.5]} />
        <meshBasicMaterial color={OPERATIONS_THEME.primary} opacity={0.65} transparent />
      </mesh>
      {/* To Notification Board */}
      <mesh position={[1.8, 0.018, 0.6]} rotation={[-Math.PI / 2, 0, 0.34]}>
        <planeGeometry args={[0.08, 3.8]} />
        <meshBasicMaterial color={OPERATIONS_THEME.primary} opacity={0.65} transparent />
      </mesh>
      {/* To Fleet Stela */}
      <mesh position={[0, 0.018, -2.4]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[0.08, 4.8]} />
        <meshBasicMaterial color={OPERATIONS_THEME.primary} opacity={0.65} transparent />
      </mesh>

      {/* 7. Service Bay Markings on Floor */}
      {SERVICE_BAYS.map((bay) => (
        <group key={bay.id} position={bay.offset}>
          {/* Bay boundary box line */}
          <lineSegments position={[0, 0.02, 0]}>
            <edgesGeometry args={[new THREE.BoxGeometry(3.0, 0.02, 4.6)]} />
            <lineBasicMaterial color={OPERATIONS_THEME.primary} linewidth={2} />
          </lineSegments>

          {/* Bay corner accent brackets */}
          <mesh position={[-1.4, 0.025, -2.2]}>
            <boxGeometry args={[0.3, 0.03, 0.3]} />
            <meshBasicMaterial color={OPERATIONS_THEME.secondary} />
          </mesh>
          <mesh position={[1.4, 0.025, -2.2]}>
            <boxGeometry args={[0.3, 0.03, 0.3]} />
            <meshBasicMaterial color={OPERATIONS_THEME.secondary} />
          </mesh>
          <mesh position={[-1.4, 0.025, 2.2]}>
            <boxGeometry args={[0.3, 0.03, 0.3]} />
            <meshBasicMaterial color={OPERATIONS_THEME.secondary} />
          </mesh>
          <mesh position={[1.4, 0.025, 2.2]}>
            <boxGeometry args={[0.3, 0.03, 0.3]} />
            <meshBasicMaterial color={OPERATIONS_THEME.secondary} />
          </mesh>

          {/* Bay Label Text */}
          <Text
            position={[0, 0.03, 2.0]}
            rotation={[-Math.PI / 2, 0, 0]}
            fontSize={0.16}
            color={OPERATIONS_THEME.secondary}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.06}
          >
            {bay.name}
          </Text>
        </group>
      ))}

      {/* 8. Perimeter Industrial Pylons & Warning Beacons */}
      <group ref={beaconRef}>
        {pylons.map((p) => (
          <group key={p.id} position={[p.x, 0, p.z]}>
            {/* Pylon Main Truss */}
            <mesh position={[0, 2.5, 0]}>
              <boxGeometry args={[0.45, 5.0, 0.45]} />
              <meshStandardMaterial
                color="#1e293b"
                metalness={0.7}
                roughness={0.3}
              />
            </mesh>
            {/* Accent bands */}
            <mesh position={[0, 1.2, 0]}>
              <boxGeometry args={[0.5, 0.15, 0.5]} />
              <meshStandardMaterial color={OPERATIONS_THEME.primary} emissive={OPERATIONS_THEME.primary} emissiveIntensity={0.6} />
            </mesh>
            <mesh position={[0, 3.8, 0]}>
              <boxGeometry args={[0.5, 0.15, 0.5]} />
              <meshStandardMaterial color={OPERATIONS_THEME.primary} emissive={OPERATIONS_THEME.primary} emissiveIntensity={0.6} />
            </mesh>
            {/* Top Amber Warning Beacon Cap */}
            <mesh position={[0, 5.15, 0]}>
              <cylinderGeometry args={[0.18, 0.22, 0.3, 16]} />
              <meshStandardMaterial
                color={OPERATIONS_THEME.secondary}
                emissive={OPERATIONS_THEME.secondary}
                emissiveIntensity={1.2}
              />
            </mesh>
            <pointLight position={[0, 5.2, 0]} color={OPERATIONS_THEME.secondary} intensity={0.5} distance={6} />
          </group>
        ))}
      </group>
    </group>
  );
};
