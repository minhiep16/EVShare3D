import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAdminStore } from './useAdminStore';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';

export const VehicleCore3D: React.FC = () => {
  const {
    fleet,
    userRole,
    toggleVehicleLockdown,
    syncFleetTelematics,
    dispatchVehicleToWorkshop,
    isExecuting,
  } = useAdminStore();

  const prismRef = useRef<THREE.Mesh>(null);
  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  useFrame(({ clock }) => {
    if (prismRef.current) {
      prismRef.current.rotation.y = clock.getElapsedTime() * 0.6;
    }
  });

  const config = ADMIN_CORES_CONFIG.VEHICLE_CORE;
  const isAuthorized = userRole === 'ROLE_ADMIN';
  const targetVehicle = fleet[0]; // VF8 Plus

  return (
    <group name="VehicleCoreStation" position={config.relativePosition}>
      {/* Title */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.22}
        color={config.primaryColor}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        FLEET TELEMATICS & LOCKDOWN CORE
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.11}
        color={COMMAND_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        CAN-Bus Synchronization • Remote Anti-Theft Lock • Workshop Dispatch
      </Text>

      {/* Floating Wireframe Fleet Prism */}
      <mesh ref={prismRef} position={[0, 2.2, 0]}>
        <octahedronGeometry args={[0.48, 0]} />
        <meshStandardMaterial
          color={config.primaryColor}
          emissive={config.primaryColor}
          emissiveIntensity={isAuthorized ? 0.9 : 0.2}
          wireframe
        />
      </mesh>

      {/* Terminal Board */}
      <group position={[0, 1.25, 0.4]} rotation={[-0.15, 0, 0]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[2.8, 1.6]} />
          <meshStandardMaterial
            color="#04121a"
            roughness={0.3}
            metalness={0.9}
            transparent
            opacity={0.94}
          />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[2.84, 1.64]} />
          <meshBasicMaterial
            color={isAuthorized ? config.primaryColor : COMMAND_THEME.alertRed}
            wireframe
          />
        </mesh>

        {/* Telemetry Data */}
        <group position={[-1.25, 0.55, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.075}
            color={config.primaryColor}
            anchorX="left"
            anchorY="middle"
          >
            FLAGSHIP: {targetVehicle.model} ({targetVehicle.licensePlate})
          </Text>
          <Text
            position={[0, -0.14, 0]}
            fontSize={0.062}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            Battery SoC: {targetVehicle.batterySoc}% • Health:{' '}
            {targetVehicle.diagnosticHealth}%
          </Text>
          <Text
            position={[0, -0.28, 0]}
            fontSize={0.062}
            color={
              targetVehicle.lockdownState === 'LOCKED_SECURE'
                ? COMMAND_THEME.alertRed
                : '#34d399'
            }
            anchorX="left"
            anchorY="middle"
          >
            Lockdown:{' '}
            {targetVehicle.lockdownState === 'LOCKED_SECURE'
              ? '🚨 LOCKED & SECURED'
              : '✓ NOMINAL (UNLOCKED)'}{' '}
            • Sector: {targetVehicle.assignedSector}
          </Text>
        </group>

        {/* 3D Action Controls */}
        <group position={[0, -0.25, 0.02]}>
          {/* Action 1: Lockdown */}
          <group
            position={[0, 0.22, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                toggleVehicleLockdown(targetVehicle.id);
            }}
            onPointerOver={() => {
              setHoveredBtn('lock');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[2.5, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'lock' ? '#7f1d1d' : '#220b12'}
                emissive={COMMAND_THEME.alertRed}
                emissiveIntensity={hoveredBtn === 'lock' ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.065}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {targetVehicle.lockdownState === 'LOCKED_SECURE'
                ? '[ 🔓 RELEASE VEHICLE LOCKDOWN ]'
                : '[ 🚨 REMOTE EMERGENCY VEHICLE LOCKDOWN ]'}
            </Text>
          </group>

          {/* Action 2: Sync Telematics */}
          <group
            position={[-0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) syncFleetTelematics();
            }}
            onPointerOver={() => {
              setHoveredBtn('sync');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'sync' ? '#0369a1' : '#082f49'}
                emissive={config.primaryColor}
                emissiveIntensity={hoveredBtn === 'sync' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.055}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 🔄 SYNC TELEMATICS ]
            </Text>
          </group>

          {/* Action 3: Dispatch to Workshop */}
          <group
            position={[0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                dispatchVehicleToWorkshop(targetVehicle.id);
            }}
            onPointerOver={() => {
              setHoveredBtn('workshop');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'workshop' ? '#0f766e' : '#042f2e'}
                emissive="#14b8a6"
                emissiveIntensity={hoveredBtn === 'workshop' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.055}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 🛠 DISPATCH WORKSHOP ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
