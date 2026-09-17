import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { DisputeChamberFloor3D } from './DisputeChamberFloor3D';
import { FloatingDisputeCrystal3D } from './FloatingDisputeCrystal3D';
import { DefectHolotank3D } from './DefectHolotank3D';
import { EvidenceDisplay3D } from './EvidenceDisplay3D';
import { StaffMediationConsole3D } from './StaffMediationConsole3D';
import { AdminArbitrationDais3D } from './AdminArbitrationDais3D';
import { DisputeStatusStela3D } from './DisputeStatusStela3D';
import {
  DISPUTE_CAMERA_PRESETS,
  DISPUTE_THEME,
  DISPUTE_SECTOR_CENTER,
} from './disputeLayout';
import { useDisputeStore } from './useDisputeStore';
import { useCameraStore } from '@/stores/useCameraStore';
import type { UserRole } from '../world/worldTypes';

interface DisputeRoom3DProps {
  position?: [number, number, number];
}

export const DisputeRoom3D: React.FC<DisputeRoom3DProps> = ({
  position = DISPUTE_SECTOR_CENTER,
}) => {
  const {
    userRole,
    setUserRole,
    feedbackMessage,
    rbacViolationNotice,
    errorMessage,
  } = useDisputeStore();

  const cameraButtons = [
    {
      key: 'CHAMBER_OVERVIEW',
      label: 'CHAMBER OVERVIEW',
      preset: DISPUTE_CAMERA_PRESETS.CHAMBER_OVERVIEW,
    },
    {
      key: 'CRYSTAL_FOCUS',
      label: 'DISPUTE CRYSTAL',
      preset: DISPUTE_CAMERA_PRESETS.CRYSTAL_FOCUS,
    },
    {
      key: 'HOLOTANK_FOCUS',
      label: 'DEFECT HOLOTANK',
      preset: DISPUTE_CAMERA_PRESETS.HOLOTANK_FOCUS,
    },
    {
      key: 'EVIDENCE_FOCUS',
      label: 'EVIDENCE CAROUSEL',
      preset: DISPUTE_CAMERA_PRESETS.EVIDENCE_FOCUS,
    },
    {
      key: 'STAFF_CONSOLE_FOCUS',
      label: 'STAFF MEDIATION',
      preset: DISPUTE_CAMERA_PRESETS.STAFF_CONSOLE_FOCUS,
    },
    {
      key: 'ADMIN_DAIS_FOCUS',
      label: 'ADMIN DAIS',
      preset: DISPUTE_CAMERA_PRESETS.ADMIN_DAIS_FOCUS,
    },
  ];

  const handleCameraChange = (
    preset: (typeof DISPUTE_CAMERA_PRESETS)['CHAMBER_OVERVIEW']
  ) => {
    const targetPos: [number, number, number] = [
      preset.position[0] + position[0] - DISPUTE_SECTOR_CENTER[0],
      preset.position[1] + position[1] - DISPUTE_SECTOR_CENTER[1],
      preset.position[2] + position[2] - DISPUTE_SECTOR_CENTER[2],
    ];
    const targetLook: [number, number, number] = [
      preset.target[0] + position[0] - DISPUTE_SECTOR_CENTER[0],
      preset.target[1] + position[1] - DISPUTE_SECTOR_CENTER[1],
      preset.target[2] + position[2] - DISPUTE_SECTOR_CENTER[2],
    ];

    useCameraStore.getState().transitionTo(targetPos, targetLook, 1.2);
  };

  const activeNotice = rbacViolationNotice || errorMessage || feedbackMessage;

  return (
    <group name="DisputeRoomSector" position={position}>
      {/* 1. Sector Dramatic Lighting */}
      <ambientLight color="#180404" intensity={1.0} />
      <directionalLight
        position={[-10, 18, 15]}
        intensity={1.8}
        color="#ffffff"
        castShadow
      />
      {/* Dynamic Key Lights for Stations */}
      <pointLight
        position={[0, 4.5, 0]}
        intensity={2.2}
        color={DISPUTE_THEME.primary}
        distance={16}
      />
      <pointLight
        position={[0, 2.8, -1.8]}
        intensity={1.8}
        color={DISPUTE_THEME.cyberCyan}
        distance={10}
      />
      <pointLight
        position={[-3.6, 2.8, 1.8]}
        intensity={1.6}
        color={DISPUTE_THEME.cyberCyan}
        distance={10}
      />
      <pointLight
        position={[3.6, 2.8, 1.8]}
        intensity={1.6}
        color={DISPUTE_THEME.secondary}
        distance={10}
      />
      <pointLight
        position={[0, 3.2, 4.2]}
        intensity={2.0}
        color={DISPUTE_THEME.secondary}
        distance={12}
      />

      {/* 2. Heavy Obsidian Chamber Floor & Monolith Ring */}
      <DisputeChamberFloor3D />

      {/* 3. Central Floating Polyhedral Dispute Crystal */}
      <FloatingDisputeCrystal3D />

      {/* 4. 3D Defect Coordinate Holotank & Holographic EV Twin */}
      <DefectHolotank3D />

      {/* 5. Immutable Evidence Carousel Staging */}
      <EvidenceDisplay3D />

      {/* 6. Staff Mediation & Review Console */}
      <StaffMediationConsole3D />

      {/* 7. Admin Arbitration Dais of Finality */}
      <AdminArbitrationDais3D />

      {/* 8. Chronological Audit & Status Stela */}
      <DisputeStatusStela3D />

      {/* 9. Floating Overhead Status / Notice Ribbon */}
      {activeNotice && (
        <group position={[0, 5.2, 0]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[7.2, 0.45]} />
            <meshBasicMaterial
              color={
                rbacViolationNotice
                  ? '#7f1d1d'
                  : errorMessage
                  ? '#831843'
                  : '#14532d'
              }
              transparent
              opacity={0.92}
            />
          </mesh>
          <mesh position={[0, 0, 0.005]}>
            <planeGeometry args={[7.24, 0.49]} />
            <meshBasicMaterial
              color={
                rbacViolationNotice
                  ? DISPUTE_THEME.alertRed
                  : DISPUTE_THEME.secondary
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
            maxWidth={6.9}
          >
            {activeNotice}
          </Text>
        </group>
      )}

      {/* 10. Floating In-Scene 3D Control Rail: Camera Presets & RBAC Role Simulation */}
      <group position={[0, 0.5, 7.8]} rotation={[-Math.PI * 0.14, 0, 0]}>
        {/* Navigation Rail Backing Plate */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[8.8, 1.2]} />
          <meshBasicMaterial
            color="#090101"
            opacity={0.94}
            transparent
            side={THREE.DoubleSide}
          />
        </mesh>
        <mesh position={[0, 0, 0.005]}>
          <planeGeometry args={[8.84, 1.24]} />
          <meshBasicMaterial color={DISPUTE_THEME.primary} wireframe />
        </mesh>

        {/* Row 1: Camera Quick Teleport Buttons */}
        <group position={[0, 0.28, 0.02]}>
          <Text
            position={[-4.1, 0, 0]}
            fontSize={0.075}
            color={DISPUTE_THEME.primary}
            anchorX="left"
            anchorY="middle"
          >
            STATION TELEPORT:
          </Text>

          {cameraButtons.map((btn, idx) => {
            const spacing = 1.15;
            const xPos = idx * spacing - 1.8;
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
        <group position={[0, -0.28, 0.02]}>
          <Text
            position={[-4.1, 0, 0]}
            fontSize={0.075}
            color={DISPUTE_THEME.secondary}
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
            const xPos = idx * 2.1 - 0.9;
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
        <planeGeometry args={[1.05, 0.28]} />
        <meshStandardMaterial
          color={hovered ? '#4c0d0d' : '#1e0505'}
          emissive={DISPUTE_THEME.primary}
          emissiveIntensity={hovered ? 0.8 : 0.2}
        />
      </mesh>
      <Text
        position={[0, 0, 0.01]}
        fontSize={0.065}
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
        <planeGeometry args={[1.9, 0.32]} />
        <meshStandardMaterial
          color={
            isActive
              ? '#78350f'
              : hovered
              ? '#451a03'
              : '#1a0808'
          }
          emissive={DISPUTE_THEME.secondary}
          emissiveIntensity={isActive ? 0.8 : hovered ? 0.5 : 0.15}
        />
      </mesh>
      <Text
        position={[0, 0, 0.01]}
        fontSize={0.07}
        color={isActive ? '#ffffff' : '#e2e8f0'}
        anchorX="center"
        anchorY="middle"
      >
        {label}
      </Text>
    </group>
  );
};
