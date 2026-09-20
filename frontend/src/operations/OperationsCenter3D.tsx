import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { OperationsFloor3D } from './OperationsFloor3D';
import { QRScannerStation3D } from './QRScannerStation3D';
import { DispatchConsole3D } from './DispatchConsole3D';
import { FleetStatusStela3D } from './FleetStatusStela3D';
import { OperationalNotifications3D } from './OperationalNotifications3D';
import { VehicleInspectionBay3D } from './VehicleInspectionBay3D';
import {
  OPERATIONS_CAMERA_PRESETS,
  OPERATIONS_THEME,
} from './operationsLayout';
import { useCameraStore } from '@/stores/useCameraStore';

interface OperationsCenter3DProps {
  position?: [number, number, number];
}

export const OperationsCenter3D: React.FC<OperationsCenter3DProps> = ({
  position = [0, 0, 0],
}) => {
  const cameraButtons = [
    { key: 'HANGAR_OVERVIEW', label: 'TỔNG QUAN', preset: OPERATIONS_CAMERA_PRESETS.HANGAR_OVERVIEW },
    { key: 'QR_STATION_FOCUS', label: 'TRẠM QUÉT QR', preset: OPERATIONS_CAMERA_PRESETS.QR_STATION_FOCUS },
    { key: 'DISPATCH_CONSOLE_FOCUS', label: 'BÀN ĐIỀU PHỐI', preset: OPERATIONS_CAMERA_PRESETS.DISPATCH_CONSOLE_FOCUS },
    { key: 'FLEET_STATUS_FOCUS', label: 'MA TRẬN ĐỘI XE', preset: OPERATIONS_CAMERA_PRESETS.FLEET_STATUS_FOCUS },
    { key: 'NOTIFICATION_BOARD_FOCUS', label: 'LUỒNG CẢNH BÁO', preset: OPERATIONS_CAMERA_PRESETS.NOTIFICATION_BOARD_FOCUS },
    { key: 'INSPECTION_BAY_FOCUS', label: 'KHU KIỂM TRA', preset: OPERATIONS_CAMERA_PRESETS.INSPECTION_BAY_FOCUS },
  ];

  const handleCameraChange = (preset: typeof OPERATIONS_CAMERA_PRESETS.HANGAR_OVERVIEW) => {
    const targetPos: [number, number, number] = [
      preset.position[0] + position[0] - (-40),
      preset.position[1] + position[1],
      preset.position[2] + position[2],
    ];
    const targetLook: [number, number, number] = [
      preset.target[0] + position[0] - (-40),
      preset.target[1] + position[1],
      preset.target[2] + position[2],
    ];

    useCameraStore.getState().transitionTo(targetPos, targetLook, 1.2);
  };

  return (
    <group name="OperationsCenterSector" position={position}>
      {/* 1. Industrial Environmental Lighting */}
      <ambientLight color="#18181b" intensity={0.9} />
      <directionalLight
        position={[8, 16, 12]}
        intensity={1.4}
        color="#fb923c"
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
      />
      {/* Station Local Point Lights */}
      <pointLight position={[-3.4, 2.5, 1.2]} intensity={1.5} color="#06b6d4" distance={8} />
      <pointLight position={[0, 2.5, 2.5]} intensity={1.8} color="#f97316" distance={10} />
      <pointLight position={[0, 3.2, -4.8]} intensity={1.8} color="#fbbf24" distance={12} />
      <pointLight position={[3.6, 2.5, 1.2]} intensity={1.5} color="#ef4444" distance={8} />

      {/* 2. Base Hangar Floor & Structural Pylons */}
      <OperationsFloor3D />

      {/* 3. Optical QR Scanner Kiosk */}
      <QRScannerStation3D />

      {/* 4. Master Operations & Dispatch Console */}
      <DispatchConsole3D />

      {/* 5. Curved Fleet Telematics Stela */}
      <FleetStatusStela3D />

      {/* 6. Operational Incident & Alert Hologram */}
      <OperationalNotifications3D />

      {/* 7. Physical Inspection & Staging Pad */}
      <VehicleInspectionBay3D />

      {/* 8. Floating 3D Navigation Bar for Station Quick-Teleport */}
      <group position={[0, 0.4, 5.2]} rotation={[-Math.PI * 0.15, 0, 0]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[7.4, 0.65]} />
          <meshBasicMaterial color="#020309" opacity={0.88} transparent side={THREE.DoubleSide} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(7.4, 0.65)]} />
          <lineBasicMaterial color={OPERATIONS_THEME.primary} />
        </lineSegments>

        {cameraButtons.map((btn, idx) => {
          const spacing = 1.18;
          const xPos = (idx - 2.5) * spacing;
          return (
            <StationNavButton3D
              key={btn.key}
              position={[xPos, 0, 0.02]}
              label={btn.label}
              onClick={() => handleCameraChange(btn.preset)}
            />
          );
        })}
      </group>
    </group>
  );
};

interface StationNavButton3DProps {
  position: [number, number, number];
  label: string;
  onClick: () => void;
}

const StationNavButton3D: React.FC<StationNavButton3DProps> = ({
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
      onPointerOver={(e) => {
        e.stopPropagation();
        setHovered(true);
        document.body.style.cursor = 'pointer';
      }}
      onPointerOut={() => {
        setHovered(false);
        document.body.style.cursor = 'auto';
      }}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[1.1, 0.38]} />
        <meshStandardMaterial
          color={hovered ? '#ea580c' : '#0f172a'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(1.1, 0.38)]} />
        <lineBasicMaterial color={hovered ? '#fdba74' : OPERATIONS_THEME.primary} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.075}
        color={hovered ? '#ffffff' : '#fed7aa'}
        anchorX="center"
        anchorY="middle"
        letterSpacing={0.04}
      >
        {label}
      </Text>
    </group>
  );
};
