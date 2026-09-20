import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useWorkshopStore } from './useWorkshopStore';
import { WORKSHOP_THEME } from './workshopLayout';
import type { SubsystemId, VehicleConditionSubsystem } from './workshopTypes';

export const VehicleConditionDisplay3D: React.FC = () => {
  const {
    subsystems,
    selectedSubsystemId,
    selectSubsystem,
    repairSubsystem,
  } = useWorkshopStore();

  const [hoveredNode, setHoveredNode] = useState<SubsystemId | null>(null);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CRITICAL_FAULT':
        return '#ef4444'; // Red
      case 'WARNING':
        return '#f59e0b'; // Amber
      case 'REPAIRED':
        return '#38bdf8'; // Electric Cyan
      case 'NOMINAL':
      default:
        return '#10b981'; // Green
    }
  };

  return (
    <group name="VehicleConditionDisplay">
      {Object.values(subsystems).map((subsystem: VehicleConditionSubsystem) => {
        const isSelected = subsystem.subsystemId === selectedSubsystemId;
        const isHovered = hoveredNode === subsystem.subsystemId;
        const statusColor = getStatusColor(subsystem.status);

        return (
          <DiagnosticSubsystemNode3D
            key={subsystem.subsystemId}
            subsystem={subsystem}
            isSelected={isSelected}
            isHovered={isHovered}
            statusColor={statusColor}
            onSelect={() => selectSubsystem(subsystem.subsystemId)}
            onHover={(h) => setHoveredNode(h ? subsystem.subsystemId : null)}
            onQuickRepair={() => repairSubsystem(subsystem.subsystemId)}
          />
        );
      })}
    </group>
  );
};

import { formatStatusVN } from '@/i18n';

interface SubsystemNodeProps {
  subsystem: VehicleConditionSubsystem;
  isSelected: boolean;
  isHovered: boolean;
  statusColor: string;
  onSelect: () => void;
  onHover: (hovered: boolean) => void;
  onQuickRepair: () => void;
}

const getSubsystemVNName = (id: string, defaultName: string): string => {
  switch (id) {
    case 'BRAKE_SYSTEM':
      return 'HỆ THỐNG PHANH GỐM';
    case 'LIDAR_ADAS':
      return 'CẢM BIẾN LIDAR & ADAS';
    case 'HIGH_VOLTAGE_BATTERY':
      return 'PIN CAO ÁP 800V';
    case 'ELECTRIC_DRIVE_MOTOR':
      return 'ĐỘNG CƠ ĐIỆN ĐỒNG BỘ';
    case 'THERMAL_MANAGEMENT':
      return 'HỆ THỐNG QUẢN LÝ NHIỆT';
    default:
      return defaultName.toUpperCase();
  }
};

const DiagnosticSubsystemNode3D: React.FC<SubsystemNodeProps> = ({
  subsystem,
  isSelected,
  isHovered,
  statusColor,
  onSelect,
  onHover,
  onQuickRepair,
}) => {
  const pulseRingRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (pulseRingRef.current) {
      pulseRingRef.current.scale.setScalar(1 + Math.sin(t * 3.0) * 0.15);
      pulseRingRef.current.rotation.z += 0.02;
    }
  });

  return (
    <group position={subsystem.positionOffset}>
      {/* 1. Interactive Pulsing Diagnostic Core Sphere */}
      <mesh
        onClick={(e) => {
          e.stopPropagation();
          onSelect();
        }}
        onPointerOver={(e) => {
          e.stopPropagation();
          onHover(true);
          document.body.style.cursor = 'pointer';
        }}
        onPointerOut={() => {
          onHover(false);
          document.body.style.cursor = 'auto';
        }}
      >
        <sphereGeometry args={[0.12, 16, 16]} />
        <meshStandardMaterial
          color={statusColor}
          emissive={statusColor}
          emissiveIntensity={isHovered || isSelected ? 1.2 : 0.6}
        />
      </mesh>

      {/* Pulsing Guide Ring */}
      <mesh ref={pulseRingRef} rotation={[Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.16, 0.2, 24]} />
        <meshBasicMaterial
          color={statusColor}
          side={THREE.DoubleSide}
          transparent
          opacity={0.8}
        />
      </mesh>

      {/* Vertical Stalk to Floating HUD Card */}
      <mesh position={[0, 0.35, 0]}>
        <cylinderGeometry args={[0.015, 0.015, 0.45, 8]} />
        <meshBasicMaterial color={statusColor} opacity={0.6} transparent />
      </mesh>

      {/* 2. Floating Holographic Info HUD Card */}
      <group position={[0, 0.65, 0]}>
        {/* Card Backplate */}
        <mesh
          position={[0, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            onSelect();
          }}
        >
          <planeGeometry args={[1.6, 0.55]} />
          <meshBasicMaterial
            color="#09090b"
            opacity={0.92}
            transparent
            side={THREE.DoubleSide}
          />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(1.6, 0.55)]} />
          <lineBasicMaterial
            color={isSelected ? '#ffffff' : statusColor}
            linewidth={isSelected ? 2 : 1}
          />
        </lineSegments>

        {/* Subsystem Name */}
        <Text
          position={[0, 0.16, 0.01]}
          fontSize={0.054}
          color="#f8fafc"
          anchorX="center"
          anchorY="middle"
        >
          {getSubsystemVNName(subsystem.subsystemId, subsystem.name)}
        </Text>

        {/* Health & Fault Line */}
        <Text
          position={[0, 0.02, 0.01]}
          fontSize={0.05}
          color={statusColor}
          anchorX="center"
          anchorY="middle"
        >
          {subsystem.faultCode
            ? `LỖI: ${subsystem.faultCode} • SỨC KHỎE: ${subsystem.healthPercentage}%`
            : `SỨC KHỎE: ${subsystem.healthPercentage}% • ${formatStatusVN(subsystem.status)}`}
        </Text>

        {/* Quick Action Button */}
        {subsystem.faultCode ? (
          <group
            position={[0, -0.15, 0.01]}
            onClick={(e) => {
              e.stopPropagation();
              onQuickRepair();
            }}
          >
            <mesh>
              <planeGeometry args={[1.3, 0.15]} />
              <meshStandardMaterial color="#0284c7" />
            </mesh>
            <Text
              position={[0, 0, 0.01]}
              fontSize={0.042}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              🛠 THAY: {subsystem.replacementPartName.slice(0, 20)}...
            </Text>
          </group>
        ) : (
          <Text
            position={[0, -0.15, 0.01]}
            fontSize={0.046}
            color="#10b981"
            anchorX="center"
            anchorY="middle"
          >
            ✓ THÔNG SỐ ĐẠT CHUẨN
          </Text>
        )}
      </group>
    </group>
  );
};
