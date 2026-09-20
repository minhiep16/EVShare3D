import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group } from 'three';
import type { ChargingStation } from './garageTypes';
import { useGarageStore } from './useGarageStore';

interface ChargingStation3DProps {
  station: ChargingStation;
}

export const ChargingStation3D: React.FC<ChargingStation3DProps> = ({ station }) => {
  const pulseRef = useRef<Group>(null);
  const toggleCharging = useGarageStore((s) => s.toggleCharging);

  const isCharging = station.status === 'CHARGING';
  const statusColor = isCharging ? '#00e5ff' : '#10b981';

  useFrame(({ clock }) => {
    if (pulseRef.current && isCharging) {
      const t = clock.getElapsedTime();
      const s = 1.0 + Math.sin(t * 3.5) * 0.05;
      pulseRef.current.scale.set(s, s, s);
    }
  });

  const handleClick = () => {
    if (station.connectedVehicleId) {
      toggleCharging(station.connectedVehicleId);
    }
  };

  return (
    <group position={station.position} rotation={station.rotation} onClick={handleClick}>
      {/* 1. Base Foundation */}
      <mesh position={[0, 0.08, 0]}>
        <boxGeometry args={[0.8, 0.16, 0.6]} />
        <meshStandardMaterial color="#0f172a" roughness={0.4} metalness={0.7} />
      </mesh>

      {/* 2. Main Futuristic Charger Tower */}
      <mesh position={[0, 1.15, 0]} castShadow>
        <boxGeometry args={[0.55, 2.0, 0.35]} />
        <meshStandardMaterial color="#1e293b" roughness={0.25} metalness={0.85} />
      </mesh>

      {/* 3. Front Glass Inset Display Panel */}
      <mesh position={[0, 1.3, 0.18]}>
        <planeGeometry args={[0.42, 0.7]} />
        <meshStandardMaterial
          color="#060913"
          roughness={0.1}
          metalness={0.9}
        />
      </mesh>

      {/* 4. Glowing Vertical Neon Status Lightstrip */}
      <mesh position={[0.26, 1.15, 0]}>
        <boxGeometry args={[0.04, 1.9, 0.32]} />
        <meshStandardMaterial
          color={statusColor}
          emissive={statusColor}
          emissiveIntensity={isCharging ? 2.5 : 1.2}
        />
      </mesh>
      <mesh position={[-0.26, 1.15, 0]}>
        <boxGeometry args={[0.04, 1.9, 0.32]} />
        <meshStandardMaterial
          color={statusColor}
          emissive={statusColor}
          emissiveIntensity={isCharging ? 2.5 : 1.2}
        />
      </mesh>

      {/* 5. Holographic Readout on Display Screen */}
      <group position={[0, 1.35, 0.19]}>
        <Text
          position={[0, 0.22, 0]}
          fontSize={0.065}
          color="#00e5ff"
          anchorX="center"
          anchorY="middle"
        >
          TRẠM SẠC NHANH V4
        </Text>
        <Text
          position={[0, 0.12, 0]}
          fontSize={0.05}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          {station.maxPowerKw} kW DC SIÊU NHANH
        </Text>
        <Text
          position={[0, -0.04, 0]}
          fontSize={0.08}
          color={statusColor}
          anchorX="center"
          anchorY="middle"
        >
          {isCharging ? `${station.currentPowerKw} kW` : 'SẴN SÀNG'}
        </Text>
        <Text
          position={[0, -0.16, 0]}
          fontSize={0.05}
          color={isCharging ? '#38bdf8' : '#64748b'}
          anchorX="center"
          anchorY="middle"
        >
          {isCharging ? '⚡ ĐANG SẠC NHANH' : 'KẾT NỐI XE ĐỂ BẮT ĐẦU'}
        </Text>
      </group>

      {/* 6. Heavy-Duty Charging Cable & Nozzle */}
      <group position={[0, 0.6, 0.2]}>
        {/* Cable Curvature */}
        <mesh position={[0, 0, 0.15]} rotation={[0.4, 0, 0]}>
          <cylinderGeometry args={[0.035, 0.035, 0.8, 16]} />
          <meshStandardMaterial color="#020617" roughness={0.7} />
        </mesh>
        {/* Nozzle Handle */}
        <mesh position={[0, -0.3, 0.3]} rotation={[0, 0, Math.PI / 2]}>
          <cylinderGeometry args={[0.05, 0.05, 0.2, 16]} />
          <meshStandardMaterial
            color={statusColor}
            emissive={statusColor}
            emissiveIntensity={1.0}
          />
        </mesh>
      </group>

      {/* 7. Active Charging Glow Aura */}
      {isCharging && (
        <group ref={pulseRef} position={[0, 1.2, 0.3]}>
          <pointLight color="#00e5ff" intensity={8} distance={6} decay={2} />
        </group>
      )}
    </group>
  );
};
