import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';
import { useAdminStore } from './useAdminStore';
import type { AdminCoreId } from './adminTypes';

export const AdminOrbitalFloor3D: React.FC = () => {
  const { activeCore, setActiveCore, userRole } = useAdminStore();
  const ringRotRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    if (ringRotRef.current) {
      ringRotRef.current.rotation.z = clock.getElapsedTime() * 0.15;
    }
  });

  const isLockedOut = userRole !== 'ROLE_ADMIN';

  return (
    <group name="AdminOrbitalDeck">
      {/* 1. Translucent Obsidian Glass Floor Platform (Elevated at Y=25) */}
      <mesh position={[0, -0.1, 0]} receiveShadow>
        <cylinderGeometry args={[13.5, 14.0, 0.4, 48]} />
        <meshStandardMaterial
          color={isLockedOut ? '#1a0505' : COMMAND_THEME.deckGlass}
          roughness={0.15}
          metalness={0.9}
          transparent
          opacity={0.88}
        />
      </mesh>

      {/* 2. Concentric Runic Circuitry Rings */}
      <mesh
        ref={ringRotRef}
        position={[0, 0.12, 0]}
        rotation={[-Math.PI / 2, 0, 0]}
      >
        <ringGeometry args={[2.5, 12.8, 48]} />
        <meshBasicMaterial
          color={isLockedOut ? COMMAND_THEME.alertRed : COMMAND_THEME.primary}
          wireframe
          transparent
          opacity={0.25}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 3. Outer Perimeter Balustrade / Guardrail Ring */}
      <mesh position={[0, 0.6, 0]}>
        <cylinderGeometry args={[13.6, 13.6, 1.2, 48, 1, true]} />
        <meshStandardMaterial
          color="#0f172a"
          roughness={0.3}
          metalness={0.8}
          transparent
          opacity={0.4}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* Outer Cyan Glowing Rail Trim */}
      <mesh position={[0, 1.21, 0]}>
        <torusGeometry args={[13.6, 0.04, 16, 48]} />
        <meshStandardMaterial
          color={isLockedOut ? COMMAND_THEME.alertRed : COMMAND_THEME.primary}
          emissive={
            isLockedOut ? COMMAND_THEME.alertRed : COMMAND_THEME.primary
          }
          emissiveIntensity={0.8}
        />
      </mesh>

      {/* 4. Radial Energy Conduit Buslines to each of the 6 outer cores */}
      {Object.entries(ADMIN_CORES_CONFIG)
        .filter(([id]) => id !== 'SYSTEM_CORE')
        .map(([id, cfg]) => {
          const [cx, , cz] = cfg.relativePosition;
          const angle = Math.atan2(cx, cz);
          const length = Math.sqrt(cx * cx + cz * cz);
          const isSelected = activeCore === id;

          return (
            <group key={id} rotation={[0, angle, 0]}>
              <mesh position={[0, 0.11, length / 2]}>
                <boxGeometry args={[0.22, 0.02, length]} />
                <meshStandardMaterial
                  color={isSelected ? cfg.primaryColor : '#1e293b'}
                  emissive={isSelected ? cfg.primaryColor : '#0f172a'}
                  emissiveIntensity={isSelected ? 0.7 : 0.2}
                />
              </mesh>

              {/* Pedestal Pad under core */}
              <mesh
                position={[0, 0.15, length]}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveCore(id as AdminCoreId);
                }}
              >
                <cylinderGeometry args={[1.5, 1.6, 0.2, 24]} />
                <meshStandardMaterial
                  color="#0f172a"
                  roughness={0.5}
                  metalness={0.8}
                />
              </mesh>

              {/* Pedestal Status Ring */}
              <mesh
                position={[0, 0.26, length]}
                rotation={[-Math.PI / 2, 0, 0]}
              >
                <ringGeometry args={[1.1, 1.48, 24]} />
                <meshBasicMaterial
                  color={cfg.primaryColor}
                  side={THREE.DoubleSide}
                />
              </mesh>
            </group>
          );
        })}

      {/* 5. Central Pedestal for Zenith System Core */}
      <mesh position={[0, 0.3, 0]}>
        <cylinderGeometry args={[2.2, 2.4, 0.5, 32]} />
        <meshStandardMaterial color="#0b0f19" roughness={0.4} metalness={0.9} />
      </mesh>
      <mesh position={[0, 0.56, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[1.6, 2.18, 32]} />
        <meshStandardMaterial
          color={COMMAND_THEME.primary}
          emissive={COMMAND_THEME.primary}
          emissiveIntensity={0.6}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 6. Sky-Bridge Entrance Waypoint from Elevator Arrival [0, 0, 12] */}
      <group position={[0, 0, 10.5]}>
        <mesh position={[0, 0.1, 0]}>
          <boxGeometry args={[3.2, 0.2, 3.8]} />
          <meshStandardMaterial color="#090d16" roughness={0.4} metalness={0.8} />
        </mesh>
        <Text
          position={[0, 0.22, 0]}
          rotation={[-Math.PI / 2, 0, 0]}
          fontSize={0.2}
          color={isLockedOut ? COMMAND_THEME.alertRed : COMMAND_THEME.primary}
          anchorX="center"
          anchorY="middle"
        >
          {isLockedOut
            ? 'VÙNG HẠN CHẾ — CẤM TRUY CẬP'
            : 'SÀN CHỈ HUY TỐI CAO APEX'}
        </Text>
      </group>
    </group>
  );
};
