import React from 'react';
import { Text } from '@react-three/drei';
import type { ParkingBay } from './garageTypes';

interface ParkingBay3DProps {
  bay: ParkingBay;
}

export const ParkingBay3D: React.FC<ParkingBay3DProps> = ({ bay }) => {
  const isCharging = bay.type === 'CHARGING';
  const baseColor = isCharging ? '#10b981' : '#00e5ff';
  const accentColor = bay.isOccupied ? '#f59e0b' : baseColor;

  return (
    <group position={bay.position} rotation={bay.rotation}>
      {/* 1. Stall Floor Pad */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.015, 0]}>
        <planeGeometry args={[3.2, 6.0]} />
        <meshStandardMaterial
          color="#0a101f"
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>

      {/* 2. U-shaped Bay Markings */}
      {/* Left Stripe */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[-1.5, 0.02, 0]}>
        <planeGeometry args={[0.08, 5.8]} />
        <meshStandardMaterial
          color={accentColor}
          emissive={accentColor}
          emissiveIntensity={1.2}
        />
      </mesh>
      {/* Right Stripe */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[1.5, 0.02, 0]}>
        <planeGeometry args={[0.08, 5.8]} />
        <meshStandardMaterial
          color={accentColor}
          emissive={accentColor}
          emissiveIntensity={1.2}
        />
      </mesh>
      {/* Rear Bumper Line */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, -2.85]}>
        <planeGeometry args={[3.08, 0.08]} />
        <meshStandardMaterial
          color={accentColor}
          emissive={accentColor}
          emissiveIntensity={1.2}
        />
      </mesh>

      {/* 3. Rear Curb Wheel Stop */}
      <group position={[0, 0.08, -2.3]}>
        <mesh castShadow>
          <boxGeometry args={[2.4, 0.12, 0.2]} />
          <meshStandardMaterial
            color="#1e293b"
            roughness={0.6}
            metalness={0.4}
          />
        </mesh>
        {/* Glow indicator on curb */}
        <mesh position={[0, 0.065, 0]}>
          <boxGeometry args={[2.3, 0.02, 0.18]} />
          <meshStandardMaterial
            color={accentColor}
            emissive={accentColor}
            emissiveIntensity={1.5}
          />
        </mesh>
      </group>

      {/* 4. Floor SDF Text Information */}
      <group position={[0, 0.025, 2.4]} rotation={[-Math.PI / 2, 0, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.28}
          color={accentColor}
          anchorX="center"
          anchorY="middle"
        >
          {bay.label}
        </Text>
        <Text
          position={[0, -0.25, 0]}
          fontSize={0.14}
          color="#64748b"
          anchorX="center"
          anchorY="middle"
        >
          {isCharging ? '⚡ TRẠM SẠC SIÊU TỐC' : '🅿 VỊ TRÍ ĐỖ XE ĐỒNG SỞ HỮU'}
        </Text>
      </group>

      {/* 5. Center Occupancy Sensor Ring */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[0.5, 0.58, 24]} />
        <meshStandardMaterial
          color={bay.isOccupied ? '#f59e0b' : '#10b981'}
          emissive={bay.isOccupied ? '#f59e0b' : '#10b981'}
          emissiveIntensity={1.8}
        />
      </mesh>
    </group>
  );
};
