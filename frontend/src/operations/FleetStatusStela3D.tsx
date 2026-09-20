import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useOperationsStore } from './useOperationsStore';
import { OPERATIONS_STATIONS, OPERATIONS_THEME } from './operationsLayout';
import type { VehicleOperationStatus } from './operationsTypes';
import { formatNumberVN, formatStatusVN } from '@/i18n';

export const FleetStatusStela3D: React.FC = () => {
  const {
    fleet,
    selectedVehicleId,
    selectVehicle,
    updateVehicleOperationalStatus,
  } = useOperationsStore();

  const [hoveredCard, setHoveredCard] = useState<number | null>(null);

  // Status color mapper
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'AVAILABLE':
        return '#10b981'; // green
      case 'IN_USE':
        return '#06b6d4'; // cyan
      case 'MAINTENANCE':
        return '#ef4444'; // red
      case 'RESERVED':
        return '#f59e0b'; // amber
      default:
        return '#94a3b8';
    }
  };

  const getSocColor = (soc: number) => {
    if (soc > 60) return '#10b981';
    if (soc > 30) return '#f59e0b';
    return '#ef4444';
  };

  return (
    <group
      name="FleetStatusStela"
      position={OPERATIONS_STATIONS.FLEET_STELA.relativePosition}
    >
      {/* 1. Stela Structural Frame & Backing */}
      <mesh position={[0, 2.2, 0]}>
        <boxGeometry args={[7.8, 3.2, 0.12]} />
        <meshStandardMaterial
          color="#050811"
          metalness={0.8}
          roughness={0.2}
        />
      </mesh>
      <lineSegments position={[0, 2.2, 0.065]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(7.8, 3.2)]} />
        <lineBasicMaterial color={OPERATIONS_THEME.primary} />
      </lineSegments>

      {/* Top Header */}
      <group position={[0, 3.45, 0.08]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.15}
          color={OPERATIONS_THEME.primary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.1}
        >
          MA TRẬN ĐỘI XE &amp; DỮ LIỆU ĐIỀU PHỐI TỪ XA
        </Text>
        <Text
          position={[0, -0.22, 0]}
          fontSize={0.09}
          color={OPERATIONS_THEME.secondary}
          anchorX="center"
          anchorY="middle"
        >
          KẾT NỐI TỪ XA THỜI GIAN THỰC • SỨC KHỎE ĐỘI XE: 100% • BUS IOT
        </Text>
      </group>

      {/* 2. Three Fleet Cards Side-by-Side */}
      {fleet.map((vehicle: VehicleOperationStatus, index: number) => {
        const xOffset = -2.4 + index * 2.4;
        const isSelected = vehicle.vehicleId === selectedVehicleId;
        const isHovered = hoveredCard === vehicle.vehicleId;
        const statusColor = getStatusColor(vehicle.status);
        const socColor = getSocColor(vehicle.batterySoc);

        return (
          <group
            key={vehicle.vehicleId}
            position={[xOffset, 1.9, 0.08]}
          >
            {/* Card Background Plate */}
            <mesh
              position={[0, 0, 0]}
              onClick={(e) => {
                e.stopPropagation();
                selectVehicle(vehicle.vehicleId);
              }}
              onPointerOver={(e) => {
                e.stopPropagation();
                setHoveredCard(vehicle.vehicleId);
                document.body.style.cursor = 'pointer';
              }}
              onPointerOut={() => {
                setHoveredCard(null);
                document.body.style.cursor = 'auto';
              }}
            >
              <planeGeometry args={[2.2, 2.3]} />
              <meshStandardMaterial
                color={isSelected ? '#1e293b' : isHovered ? '#0f172a' : '#090d18'}
                roughness={0.4}
                metalness={0.6}
              />
            </mesh>

            {/* Card Border Wire */}
            <lineSegments position={[0, 0, 0.005]}>
              <edgesGeometry args={[new THREE.PlaneGeometry(2.2, 2.3)]} />
              <lineBasicMaterial
                color={isSelected ? OPERATIONS_THEME.primary : '#334155'}
                linewidth={isSelected ? 2 : 1}
              />
            </lineSegments>

            {/* Bay Badge */}
            <mesh position={[0, 0.95, 0.01]}>
              <planeGeometry args={[1.9, 0.18]} />
              <meshStandardMaterial color="#1e1b4b" />
            </mesh>
            <Text
              position={[0, 0.95, 0.02]}
              fontSize={0.075}
              color={OPERATIONS_THEME.secondary}
              anchorX="center"
              anchorY="middle"
              letterSpacing={0.05}
            >
              {vehicle.currentBay}
            </Text>

            {/* Model Name & Plate */}
            <Text
              position={[0, 0.72, 0.02]}
              fontSize={0.105}
              color="#f8fafc"
              anchorX="center"
              anchorY="middle"
            >
              {vehicle.modelName}
            </Text>
            <Text
              position={[0, 0.55, 0.02]}
              fontSize={0.08}
              color="#94a3b8"
              anchorX="center"
              anchorY="middle"
            >
              BIỂN SỐ: {vehicle.licensePlate}
            </Text>

            {/* Status Badge */}
            <mesh position={[0, 0.35, 0.01]}>
              <planeGeometry args={[1.5, 0.18]} />
              <meshStandardMaterial
                color={statusColor}
                emissive={statusColor}
                emissiveIntensity={0.4}
              />
            </mesh>
            <Text
              position={[0, 0.35, 0.02]}
              fontSize={0.07}
              color="#000000"
              anchorX="center"
              anchorY="middle"
            >
              {formatStatusVN(vehicle.status)}
            </Text>

            {/* Battery SoC Gauge */}
            <group position={[0, 0.1, 0.01]}>
              <Text
                position={[-0.9, 0, 0.01]}
                fontSize={0.065}
                color="#94a3b8"
                anchorX="left"
                anchorY="middle"
              >
                Pin: {vehicle.batterySoc}%
              </Text>
              {/* SoC Bar Background */}
              <mesh position={[0.2, 0, 0]}>
                <planeGeometry args={[1.2, 0.12]} />
                <meshStandardMaterial color="#1e293b" />
              </mesh>
              {/* SoC Bar Fill */}
              <mesh
                position={[-0.4 + (1.2 * (vehicle.batterySoc / 100)) / 2, 0, 0.005]}
              >
                <planeGeometry args={[1.2 * (vehicle.batterySoc / 100), 0.1]} />
                <meshStandardMaterial
                  color={socColor}
                  emissive={socColor}
                  emissiveIntensity={0.5}
                />
              </mesh>
            </group>

            {/* Odometer readout */}
            <Text
              position={[0, -0.15, 0.02]}
              fontSize={0.075}
              color="#e2e8f0"
              anchorX="center"
              anchorY="middle"
            >
              CÔNG TƠ MÉT: {formatNumberVN(vehicle.odometerKm)} KM
            </Text>

            {/* Active Driver / User */}
            <Text
              position={[0, -0.32, 0.02]}
              fontSize={0.065}
              color={vehicle.activeUser ? '#38bdf8' : '#64748b'}
              anchorX="center"
              anchorY="middle"
            >
              {vehicle.activeUser
                ? `TÀI XẾ: ${vehicle.activeUser}`
                : 'KHÔNG CÓ TÀI XẾ (ĐANG ĐỖ)'}
            </Text>

            {/* Interactive 3D Button: Focus in Dispatch */}
            <group
              position={[-0.52, -0.65, 0.02]}
              onClick={(e) => {
                e.stopPropagation();
                selectVehicle(vehicle.vehicleId);
              }}
            >
              <mesh>
                <planeGeometry args={[0.95, 0.22]} />
                <meshStandardMaterial
                  color={isSelected ? '#ea580c' : '#1e293b'}
                />
              </mesh>
              <Text
                position={[0, 0, 0.01]}
                fontSize={0.06}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
              >
                {isSelected ? '✓ ĐÃ CHỌN' : 'CHỌN XE'}
              </Text>
            </group>

            {/* Interactive 3D Button: Toggle Service / Maintenance */}
            <group
              position={[0.52, -0.65, 0.02]}
              onClick={(e) => {
                e.stopPropagation();
                const nextStatus =
                  vehicle.status === 'MAINTENANCE' ? 'AVAILABLE' : 'MAINTENANCE';
                updateVehicleOperationalStatus(vehicle.vehicleId, nextStatus);
              }}
            >
              <mesh>
                <planeGeometry args={[0.95, 0.22]} />
                <meshStandardMaterial
                  color={vehicle.status === 'MAINTENANCE' ? '#15803d' : '#991b1b'}
                />
              </mesh>
              <Text
                position={[0, 0, 0.01]}
                fontSize={0.052}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
              >
                {vehicle.status === 'MAINTENANCE' ? 'MỞ LẠI' : 'BẢO DƯỠNG'}
              </Text>
            </group>
          </group>
        );
      })}

      {/* 3. Base Ground Anchor */}
      <mesh position={[0, 0.3, 0]}>
        <boxGeometry args={[8.2, 0.6, 0.8]} />
        <meshStandardMaterial
          color="#0b0f19"
          metalness={0.7}
          roughness={0.3}
        />
      </mesh>
    </group>
  );
};
