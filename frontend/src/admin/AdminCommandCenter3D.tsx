import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { AdminOrbitalFloor3D } from './AdminOrbitalFloor3D';
import { UserCore3D } from './UserCore3D';
import { VehicleCore3D } from './VehicleCore3D';
import { OwnershipCore3D } from './OwnershipCore3D';
import { BookingCore3D } from './BookingCore3D';
import { FinanceCore3D } from './FinanceCore3D';
import { DisputeCore3D } from './DisputeCore3D';
import { SystemCore3D } from './SystemCore3D';
import {
  ADMIN_CAMERA_PRESETS,
  ADMIN_SECTOR_CENTER,
  COMMAND_THEME,
} from './adminLayout';
import { useAdminStore } from './useAdminStore';
import { useCameraStore } from '@/stores/useCameraStore';
import type { UserRole } from '../world/worldTypes';

interface AdminCommandCenter3DProps {
  position?: [number, number, number];
}

export const AdminCommandCenter3D: React.FC<AdminCommandCenter3DProps> = ({
  position = ADMIN_SECTOR_CENTER,
}) => {
  const {
    userRole,
    setUserRole,
    feedbackNotice,
    rbacViolationNotice,
    errorMessage,
  } = useAdminStore();

  const cameraButtons = [
    {
      key: 'ORBITAL_OVERVIEW',
      label: 'OVERVIEW',
      preset: ADMIN_CAMERA_PRESETS.ORBITAL_OVERVIEW,
    },
    {
      key: 'USER_CORE_FOCUS',
      label: 'USER CORE',
      preset: ADMIN_CAMERA_PRESETS.USER_CORE_FOCUS,
    },
    {
      key: 'VEHICLE_CORE_FOCUS',
      label: 'VEHICLE CORE',
      preset: ADMIN_CAMERA_PRESETS.VEHICLE_CORE_FOCUS,
    },
    {
      key: 'OWNERSHIP_CORE_FOCUS',
      label: 'EQUITY CORE',
      preset: ADMIN_CAMERA_PRESETS.OWNERSHIP_CORE_FOCUS,
    },
    {
      key: 'BOOKING_CORE_FOCUS',
      label: 'BOOKING CORE',
      preset: ADMIN_CAMERA_PRESETS.BOOKING_CORE_FOCUS,
    },
    {
      key: 'FINANCE_CORE_FOCUS',
      label: 'FINANCE CORE',
      preset: ADMIN_CAMERA_PRESETS.FINANCE_CORE_FOCUS,
    },
    {
      key: 'DISPUTE_CORE_FOCUS',
      label: 'DISPUTE CORE',
      preset: ADMIN_CAMERA_PRESETS.DISPUTE_CORE_FOCUS,
    },
    {
      key: 'SYSTEM_CORE_FOCUS',
      label: 'ZENITH CORE',
      preset: ADMIN_CAMERA_PRESETS.SYSTEM_CORE_FOCUS,
    },
  ];

  const handleCameraChange = (
    preset: (typeof ADMIN_CAMERA_PRESETS)['ORBITAL_OVERVIEW']
  ) => {
    const targetPos: [number, number, number] = [
      preset.position[0] + position[0] - ADMIN_SECTOR_CENTER[0],
      preset.position[1] + position[1] - ADMIN_SECTOR_CENTER[1],
      preset.position[2] + position[2] - ADMIN_SECTOR_CENTER[2],
    ];
    const targetLook: [number, number, number] = [
      preset.target[0] + position[0] - ADMIN_SECTOR_CENTER[0],
      preset.target[1] + position[1] - ADMIN_SECTOR_CENTER[1],
      preset.target[2] + position[2] - ADMIN_SECTOR_CENTER[2],
    ];

    useCameraStore.getState().transitionTo(targetPos, targetLook, 1.2);
  };

  const activeNotice = rbacViolationNotice || errorMessage || feedbackNotice;
  const isAuthorized = userRole === 'ROLE_ADMIN';

  return (
    <group name="AdminCommandCenterSector" position={position}>
      {/* 1. Sector Dramatic Atmospheric & Orbital Lighting */}
      <ambientLight color="#020617" intensity={0.9} />
      <directionalLight
        position={[0, 45, 0]}
        intensity={2.4}
        color="#e0f2fe"
        castShadow
      />
      {/* Point lights for each core */}
      <pointLight position={[0, 4.0, 0]} intensity={2.6} color="#38bdf8" distance={16} />
      <pointLight position={[0, 3.0, -5.4]} intensity={1.6} color="#38bdf8" distance={8} />
      <pointLight position={[4.8, 3.0, -2.6]} intensity={1.6} color="#06b6d4" distance={8} />
      <pointLight position={[4.8, 3.0, 2.6]} intensity={1.6} color="#a855f7" distance={8} />
      <pointLight position={[0, 3.0, 5.4]} intensity={1.6} color="#f59e0b" distance={8} />
      <pointLight position={[-4.8, 3.0, 2.6]} intensity={1.6} color="#10b981" distance={8} />
      <pointLight position={[-4.8, 3.0, -2.6]} intensity={1.6} color="#ff1744" distance={8} />

      {/* 2. Elevated Translucent Obsidian Glass Floor & Pedestals */}
      <AdminOrbitalFloor3D />

      {/* 3. The 7 Conceptual Command Cores */}
      <UserCore3D />
      <VehicleCore3D />
      <OwnershipCore3D />
      <BookingCore3D />
      <FinanceCore3D />
      <DisputeCore3D />
      <SystemCore3D />

      {/* 4. Floating Overhead Sovereign Notice Ribbon */}
      {activeNotice && (
        <group position={[0, 6.4, 0]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[8.8, 0.48]} />
            <meshBasicMaterial
              color={
                rbacViolationNotice
                  ? '#7f1d1d'
                  : errorMessage
                  ? '#831843'
                  : '#0c4a6e'
              }
              transparent
              opacity={0.92}
            />
          </mesh>
          <mesh position={[0, 0, 0.005]}>
            <planeGeometry args={[8.84, 0.52]} />
            <meshBasicMaterial
              color={
                rbacViolationNotice
                  ? COMMAND_THEME.alertRed
                  : COMMAND_THEME.primary
              }
              wireframe
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.11}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
            maxWidth={8.5}
          >
            {activeNotice}
          </Text>
        </group>
      )}

      {/* 5. In-Scene 3D Control Rail: Camera Quick-Teleport & RBAC Identity Simulator */}
      <group position={[0, 0.5, 9.2]} rotation={[-Math.PI * 0.14, 0, 0]}>
        {/* Navigation Rail Backing Plate */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[10.2, 1.25]} />
          <meshBasicMaterial
            color="#020617"
            opacity={0.94}
            transparent
            side={THREE.DoubleSide}
          />
        </mesh>
        <mesh position={[0, 0, 0.005]}>
          <planeGeometry args={[10.24, 1.29]} />
          <meshBasicMaterial
            color={
              isAuthorized ? COMMAND_THEME.primary : COMMAND_THEME.alertRed
            }
            wireframe
          />
        </mesh>

        {/* Row 1: Camera Quick Teleport Buttons */}
        <group position={[0, 0.3, 0.02]}>
          <Text
            position={[-4.8, 0, 0]}
            fontSize={0.075}
            color={COMMAND_THEME.primary}
            anchorX="left"
            anchorY="middle"
          >
            CORE TELEPORT:
          </Text>

          {cameraButtons.map((btn, idx) => {
            const spacing = 1.05;
            const xPos = idx * spacing - 2.8;
            return (
              <StationButton3D
                key={btn.key}
                position={[xPos, 0, 0]}
                label={btn.label}
                onClick={() => handleCameraChange(btn.preset)}
              />
            );
          })}
        </group>

        {/* Row 2: In-Scene RBAC Role Simulation Selector */}
        <group position={[0, -0.3, 0.02]}>
          <Text
            position={[-4.8, 0, 0]}
            fontSize={0.075}
            color={COMMAND_THEME.accentGold}
            anchorX="left"
            anchorY="middle"
          >
            RBAC SIMULATED IDENTITY:
          </Text>

          {(
            [
              { role: 'ROLE_CO_OWNER', label: '👤 CO-OWNER (ALICE)' },
              { role: 'ROLE_STAFF', label: '🛠 PLATFORM STAFF' },
              { role: 'ROLE_ADMIN', label: '👑 SUPREME ADMIN' },
            ] as const
          ).map((item, idx) => {
            const isCurrent = userRole === item.role;
            const xPos = idx * 2.3 - 1.2;
            return (
              <RoleButton3D
                key={item.role}
                position={[xPos, 0, 0]}
                label={item.label}
                isActive={isCurrent}
                onClick={() => setUserRole(item.role as UserRole)}
              />
            );
          })}
        </group>
      </group>
    </group>
  );
};

interface StationButton3DProps {
  position: [number, number, number];
  label: string;
  onClick: () => void;
}

const StationButton3D: React.FC<StationButton3DProps> = ({
  position,
  label,
  onClick,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
      onPointerOver={() => {
        setHovered(true);
        document.body.style.cursor = 'pointer';
      }}
      onPointerOut={() => {
        setHovered(false);
        document.body.style.cursor = 'auto';
      }}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[0.96, 0.28]} />
        <meshStandardMaterial
          color={hovered ? '#0369a1' : '#082f49'}
          emissive={COMMAND_THEME.primary}
          emissiveIntensity={hovered ? 0.8 : 0.2}
        />
      </mesh>
      <Text
        position={[0, 0, 0.01]}
        fontSize={0.058}
        color="#ffffff"
        anchorX="center"
        anchorY="middle"
      >
        {label}
      </Text>
    </group>
  );
};

interface RoleButton3DProps {
  position: [number, number, number];
  label: string;
  isActive: boolean;
  onClick: () => void;
}

const RoleButton3D: React.FC<RoleButton3DProps> = ({
  position,
  label,
  isActive,
  onClick,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
      onPointerOver={() => {
        setHovered(true);
        document.body.style.cursor = 'pointer';
      }}
      onPointerOut={() => {
        setHovered(false);
        document.body.style.cursor = 'auto';
      }}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[2.05, 0.32]} />
        <meshStandardMaterial
          color={
            isActive
              ? '#78350f'
              : hovered
              ? '#451a03'
              : '#0f172a'
          }
          emissive={COMMAND_THEME.accentGold}
          emissiveIntensity={isActive ? 0.8 : hovered ? 0.5 : 0.15}
        />
      </mesh>
      <Text
        position={[0, 0, 0.01]}
        fontSize={0.068}
        color={isActive ? '#ffffff' : '#cbd5e1'}
        anchorX="center"
        anchorY="middle"
      >
        {label}
      </Text>
    </group>
  );
};
