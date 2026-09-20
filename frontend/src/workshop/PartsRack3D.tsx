import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useWorkshopStore } from './useWorkshopStore';
import { WORKSHOP_STATIONS, WORKSHOP_THEME } from './workshopLayout';
import type { SubsystemId } from './workshopTypes';
import { formatCurrencyVND } from '@/i18n';

export const PartsRack3D: React.FC = () => {
  const { repairSubsystem, subsystems } = useWorkshopStore();
  const [hoveredPart, setHoveredPart] = useState<string | null>(null);

  const parts = [
    {
      id: 'BRAKE_SYSTEM' as SubsystemId,
      name: 'BỘ ĐĨA PHANH GỐM & MÁ PHANH',
      category: 'KHUNG GẦM',
      price: formatCurrencyVND(4200000),
      shelfY: 1.6,
      stock: 4,
    },
    {
      id: 'LIDAR_ADAS' as SubsystemId,
      name: 'CỤM CẢM BIẾN LIDAR ADAS',
      category: 'ĐIỆN TỬ',
      price: formatCurrencyVND(1800000),
      shelfY: 1.05,
      stock: 2,
    },
    {
      id: 'THERMAL_COOLANT' as SubsystemId,
      name: 'BƠM DUNG DỊCH LÀM MÁT INVERTER',
      category: 'HỆ NHIỆT',
      price: formatCurrencyVND(2100000),
      shelfY: 0.5,
      stock: 3,
    },
  ];

  return (
    <group
      name="PartsRack"
      position={WORKSHOP_STATIONS.PARTS_RACK.relativePosition}
    >
      {/* 1. Heavy Industrial Steel Shelving Unit */}
      {/* Side Upright Posts */}
      {[-1.1, 1.1].map((x) => (
        <group key={`post-${x}`} position={[x, 1.25, 0]}>
          <mesh castShadow>
            <boxGeometry args={[0.08, 2.5, 0.7]} />
            <meshStandardMaterial color="#18181b" metalness={0.8} roughness={0.3} />
          </mesh>
        </group>
      ))}

      {/* 3 Horizontal Shelves */}
      {[0.4, 0.95, 1.5, 2.05].map((y, idx) => (
        <mesh key={`shelf-${idx}`} position={[0, y, 0]}>
          <boxGeometry args={[2.3, 0.04, 0.75]} />
          <meshStandardMaterial color="#27272a" metalness={0.7} roughness={0.3} />
        </mesh>
      ))}

      {/* Header Plaque */}
      <group position={[0, 2.35, 0]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.2, 0.35]} />
          <meshBasicMaterial color="#09090b" />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.2, 0.35)]} />
          <lineBasicMaterial color={WORKSHOP_THEME.secondary} />
        </lineSegments>
        <Text
          position={[0, 0.05, 0.01]}
          fontSize={0.068}
          color={WORKSHOP_THEME.secondary}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          KHO PHỤ TÙNG CHÍNH HÃNG OEM
        </Text>
        <Text
          position={[0, -0.08, 0.01]}
          fontSize={0.046}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          PHỤ TÙNG VINFAST ĐẠT CHUẨN • ISO-9001
        </Text>
      </group>

      {/* 2. Parts Bins & Clickable Replacement Boxes */}
      {parts.map((p) => {
        const isHovered = hoveredPart === p.id;
        const currentSubsystem = subsystems[p.id];
        const isNeeded = currentSubsystem && currentSubsystem.faultCode !== null;

        return (
          <group
            key={p.id}
            position={[0, p.shelfY, 0.15]}
            onClick={(e) => {
              e.stopPropagation();
              repairSubsystem(p.id);
            }}
            onPointerOver={(e) => {
              e.stopPropagation();
              setHoveredPart(p.id);
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredPart(null);
              document.body.style.cursor = 'auto';
            }}
          >
            {/* 3D Part Container Box */}
            <mesh castShadow>
              <boxGeometry args={[1.8, 0.32, 0.45]} />
              <meshStandardMaterial
                color={isHovered ? '#0284c7' : isNeeded ? '#1e293b' : '#0f172a'}
                emissive={isNeeded ? '#ef4444' : isHovered ? WORKSHOP_THEME.primary : '#000000'}
                emissiveIntensity={isNeeded ? 0.3 : isHovered ? 0.5 : 0}
                roughness={0.3}
                metalness={0.7}
              />
            </mesh>

            {/* Label Plaque on Box Front */}
            <Text
              position={[0, 0.06, 0.23]}
              fontSize={0.052}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {p.name}
            </Text>
            <Text
              position={[0, -0.06, 0.23]}
              fontSize={0.044}
              color={isNeeded ? '#f87171' : '#38bdf8'}
              anchorX="center"
              anchorY="middle"
            >
              {isNeeded
                ? `⚠ YÊU CẦU ĐỂ SỬA CHỮA • ${p.price}`
                : `TỒN KHO: ${p.stock} BỘ • ${p.price}`}
            </Text>
          </group>
        );
      })}
    </group>
  );
};
