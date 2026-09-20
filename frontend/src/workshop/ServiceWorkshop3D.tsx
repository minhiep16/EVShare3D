import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { WorkshopFloor3D } from './WorkshopFloor3D';
import { HydraulicLift3D } from './HydraulicLift3D';
import { DiagnosticCart3D } from './DiagnosticCart3D';
import { PartsRack3D } from './PartsRack3D';
import { WorkOrderStela3D } from './WorkOrderStela3D';
import {
  WORKSHOP_CAMERA_PRESETS,
  WORKSHOP_THEME,
} from './workshopLayout';
import { useCameraStore } from '@/stores/useCameraStore';

interface ServiceWorkshop3DProps {
  position?: [number, number, number];
}

export const ServiceWorkshop3D: React.FC<ServiceWorkshop3DProps> = ({
  position = [0, 0, 0],
}) => {
  const cameraButtons = [
    { key: 'WORKSHOP_OVERVIEW', label: 'TỔNG QUAN', preset: WORKSHOP_CAMERA_PRESETS.WORKSHOP_OVERVIEW },
    { key: 'HYDRAULIC_LIFT_FOCUS', label: 'CẦU NÂNG', preset: WORKSHOP_CAMERA_PRESETS.HYDRAULIC_LIFT_FOCUS },
    { key: 'DIAGNOSTIC_CART_FOCUS', label: 'XE CHẨN ĐOÁN', preset: WORKSHOP_CAMERA_PRESETS.DIAGNOSTIC_CART_FOCUS },
    { key: 'PARTS_RACK_FOCUS', label: 'KỆ PHỤ TÙNG', preset: WORKSHOP_CAMERA_PRESETS.PARTS_RACK_FOCUS },
    { key: 'WORK_ORDER_STELA_FOCUS', label: 'LỆNH SỬA CHỮA', preset: WORKSHOP_CAMERA_PRESETS.WORK_ORDER_STELA_FOCUS },
    { key: 'UNDERCARRIAGE_INSPECTION', label: 'GẦM XE', preset: WORKSHOP_CAMERA_PRESETS.UNDERCARRIAGE_INSPECTION },
  ];

  const handleCameraChange = (preset: typeof WORKSHOP_CAMERA_PRESETS.WORKSHOP_OVERVIEW) => {
    const targetPos: [number, number, number] = [
      preset.position[0] + position[0] - (-40),
      preset.position[1] + position[1],
      preset.position[2] + position[2] - 40,
    ];
    const targetLook: [number, number, number] = [
      preset.target[0] + position[0] - (-40),
      preset.target[1] + position[1],
      preset.target[2] + position[2] - 40,
    ];

    useCameraStore.getState().transitionTo(targetPos, targetLook, 1.2);
  };

  return (
    <group name="ServiceWorkshopSector" position={position}>
      {/* 1. Industrial Environmental Lighting */}
      <ambientLight color="#18181b" intensity={0.9} />
      <directionalLight
        position={[8, 16, 12]}
        intensity={1.5}
        color="#ffffff"
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
      />
      {/* Station Local Lights */}
      <pointLight position={[0, 2.5, 0]} intensity={1.8} color="#38bdf8" distance={10} />
      <pointLight position={[-3.6, 2.5, 2.4]} intensity={1.5} color="#06b6d4" distance={8} />
      <pointLight position={[3.8, 2.5, 2.4]} intensity={1.5} color="#f59e0b" distance={8} />
      <pointLight position={[0, 3.2, -4.8]} intensity={1.8} color="#38bdf8" distance={12} />

      {/* 2. Base Steel Floor & Heavy Trusses */}
      <WorkshopFloor3D />

      {/* 3. Dual-Column Hydraulic Lift & Staged EV */}
      <HydraulicLift3D />

      {/* 4. Mobile OBD-II Diagnostic Tool Cart */}
      <DiagnosticCart3D />

      {/* 5. Modular Spare Parts Staging Rack */}
      <PartsRack3D />

      {/* 6. Work Order Dispatch & Safety Stela */}
      <WorkOrderStela3D />

      {/* 7. Floating 3D Navigation Bar for Station Quick-Teleport */}
      <group position={[0, 0.4, 5.2]} rotation={[-Math.PI * 0.15, 0, 0]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[7.4, 0.65]} />
          <meshBasicMaterial color="#020309" opacity={0.88} transparent side={THREE.DoubleSide} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(7.4, 0.65)]} />
          <lineBasicMaterial color={WORKSHOP_THEME.primary} />
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
          color={hovered ? '#0284c7' : '#0f172a'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(1.1, 0.38)]} />
        <lineBasicMaterial color={hovered ? '#7dd3fc' : WORKSHOP_THEME.primary} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.075}
        color={hovered ? '#ffffff' : '#bae6fd'}
        anchorX="center"
        anchorY="middle"
        letterSpacing={0.04}
      >
        {label}
      </Text>
    </group>
  );
};
