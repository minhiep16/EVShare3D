import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { OPERATIONS_STATIONS, OPERATIONS_THEME } from './operationsLayout';
import { useOperationsStore } from './useOperationsStore';

export const VehicleInspectionBay3D: React.FC = () => {
  const laserRef = useRef<THREE.Mesh>(null);
  const { hasDamageReported } = useOperationsStore();

  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (laserRef.current) {
      // Oscillate scan plane along vehicle length z = -2.1 to +2.1
      laserRef.current.position.z = Math.sin(t * 1.5) * 2.1;
    }
  });

  const sensorPoints = [
    { name: 'FRONT LIDAR', pos: [0, 0.45, -1.9] as [number, number, number] },
    { name: 'LEFT HUB', pos: [-1.2, 0.3, 0] as [number, number, number] },
    { name: 'RIGHT HUB', pos: [1.2, 0.3, 0] as [number, number, number] },
    { name: 'BATTERY TRAY', pos: [0, 0.18, 0] as [number, number, number] },
    { name: 'CHARGE PORT', pos: [0, 0.45, 1.9] as [number, number, number] },
  ];

  return (
    <group
      name="VehicleInspectionBay"
      position={OPERATIONS_STATIONS.INSPECTION_BAY.relativePosition}
    >
      {/* 1. Staging Pad Raised Base */}
      <mesh position={[0, 0.05, 0]}>
        <boxGeometry args={[3.2, 0.1, 4.8]} />
        <meshStandardMaterial
          color="#0a0f1d"
          metalness={0.6}
          roughness={0.4}
        />
      </mesh>

      {/* Boundary Grid */}
      <lineSegments position={[0, 0.105, 0]}>
        <edgesGeometry args={[new THREE.BoxGeometry(3.2, 0.01, 4.8)]} />
        <lineBasicMaterial color={OPERATIONS_THEME.secondary} />
      </lineSegments>

      {/* 2. Sweeping Laser Inspection Plane */}
      <mesh ref={laserRef} position={[0, 0.45, 0]}>
        <boxGeometry args={[2.8, 0.8, 0.04]} />
        <meshBasicMaterial
          color={hasDamageReported ? '#ef4444' : OPERATIONS_THEME.cyberCyan}
          transparent
          opacity={0.35}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 3. Holographic Ghost EV Wireframe Chassis */}
      <group position={[0, 0.65, 0]}>
        {/* Main Body Wireframe */}
        <lineSegments>
          <edgesGeometry args={[new THREE.BoxGeometry(2.1, 0.7, 4.2)]} />
          <lineBasicMaterial
            color={hasDamageReported ? '#ef4444' : OPERATIONS_THEME.cyberCyan}
            transparent
            opacity={0.6}
          />
        </lineSegments>

        {/* Cabin Glass Wireframe */}
        <lineSegments position={[0, 0.55, -0.2]}>
          <edgesGeometry args={[new THREE.BoxGeometry(1.7, 0.5, 2.4)]} />
          <lineBasicMaterial
            color={OPERATIONS_THEME.primary}
            transparent
            opacity={0.4}
          />
        </lineSegments>
      </group>

      {/* 4. Multi-Point Diagnostic Sensors */}
      {sensorPoints.map((sp) => (
        <group key={sp.name} position={sp.pos}>
          <mesh>
            <sphereGeometry args={[0.08, 16, 16]} />
            <meshStandardMaterial
              color={hasDamageReported ? '#ef4444' : '#10b981'}
              emissive={hasDamageReported ? '#ef4444' : '#10b981'}
              emissiveIntensity={0.8}
            />
          </mesh>
          <Text
            position={[0, 0.18, 0]}
            fontSize={0.065}
            color="#e2e8f0"
            anchorX="center"
            anchorY="middle"
          >
            {sp.name}
          </Text>
        </group>
      ))}

      {/* 5. Overhead Sensor HUD Readout */}
      <group position={[0, 2.2, 0]}>
        <Text
          position={[0, 0.16, 0]}
          fontSize={0.12}
          color={OPERATIONS_THEME.secondary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          BAY 02 // AUTOMATED CHASSIS INSPECTION PAD
        </Text>
        <Text
          position={[0, -0.05, 0]}
          fontSize={0.075}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          TIRES: 2.4 BAR • PACK TEMP: 28°C • BRAKE LINING: NOMINAL
        </Text>
      </group>
    </group>
  );
};
