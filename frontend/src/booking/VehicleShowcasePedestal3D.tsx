import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group } from 'three';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_LAYOUT, BOOKING_CHAMBER_THEME } from './bookingLayout';

export const VehicleShowcasePedestal3D: React.FC = () => {
  const { TURNTABLE_RADIUS, TURNTABLE_HEIGHT } = BOOKING_CHAMBER_LAYOUT;
  const { CHRONO_CYAN, DARK_OBSIDIAN } = BOOKING_CHAMBER_THEME;

  const vehicles = useBookingStore((s) => s.vehicles);
  const selectedVehicleId = useBookingStore((s) => s.selectedVehicleId);
  const selectVehicle = useBookingStore((s) => s.selectVehicle);

  const selectedVehicle = vehicles.find((v) => v.id === selectedVehicleId) || vehicles[0];

  const turntableRef = useRef<Group>(null);

  // Slow continuous rotation of turntable
  useFrame((_, delta) => {
    if (turntableRef.current) {
      turntableRef.current.rotation.y += delta * 0.15;
    }
  });

  return (
    <group name="VehicleShowcasePedestal3D" position={[0, 0, 0]}>
      {/* 1. Base Circular Turntable Mesh */}
      <mesh position={[0, TURNTABLE_HEIGHT / 2, 0]} receiveShadow castShadow>
        <cylinderGeometry args={[TURNTABLE_RADIUS, TURNTABLE_RADIUS + 0.3, TURNTABLE_HEIGHT, 48]} />
        <meshStandardMaterial
          color={DARK_OBSIDIAN}
          metalness={0.85}
          roughness={0.2}
        />
      </mesh>

      {/* Turntable Luminous Border */}
      <mesh position={[0, TURNTABLE_HEIGHT + 0.005, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[TURNTABLE_RADIUS - 0.2, TURNTABLE_RADIUS, 48]} />
        <meshBasicMaterial color={CHRONO_CYAN} transparent opacity={0.6} />
      </mesh>

      {/* 2. Rotating Turntable Content */}
      <group ref={turntableRef} position={[0, TURNTABLE_HEIGHT, 0]}>
        {/* Procedural Digital Twin Vehicle Representation */}
        <group position={[0, 0.4, 0]}>
          {/* Main Aerodynamic Chassis */}
          <mesh position={[0, 0.25, 0]} castShadow receiveShadow>
            <boxGeometry args={[1.95, 0.4, 4.3]} />
            <meshStandardMaterial
              color={selectedVehicle.bodyColor}
              metalness={0.85}
              roughness={0.2}
            />
          </mesh>

          {/* Aerodynamic Cabin & Glass Roof */}
          <mesh position={[0, 0.65, -0.2]} castShadow>
            <boxGeometry args={[1.5, 0.45, 2.3]} />
            <meshStandardMaterial
              color="#020617"
              metalness={0.95}
              roughness={0.1}
              transparent
              opacity={0.85}
            />
          </mesh>

          {/* Front Cyber LED Lightbar */}
          <mesh position={[0, 0.25, 2.16]}>
            <boxGeometry args={[1.7, 0.08, 0.04]} />
            <meshBasicMaterial color={CHRONO_CYAN} />
          </mesh>

          {/* Rear Crimson LED Lightbar */}
          <mesh position={[0, 0.32, -2.16]}>
            <boxGeometry args={[1.7, 0.08, 0.04]} />
            <meshBasicMaterial color="#ef4444" />
          </mesh>

          {/* 4 Wheels */}
          {[-0.95, 0.95].map((wx) =>
            [-1.35, 1.35].map((wz) => (
              <mesh
                key={`${wx}_${wz}`}
                position={[wx, 0.1, wz]}
                rotation={[0, 0, Math.PI / 2]}
                castShadow
              >
                <cylinderGeometry args={[0.34, 0.34, 0.24, 24]} />
                <meshStandardMaterial color="#0f172a" metalness={0.9} roughness={0.3} />
              </mesh>
            ))
          )}
        </group>
      </group>

      {/* 3. Floating Vehicle Identity Badge (Stationary above pedestal) */}
      <group position={[0, 2.6, 0]}>
        {/* Hologram plate */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[3.2, 0.9]} />
          <meshStandardMaterial
            color="#050b14"
            roughness={0.2}
            metalness={0.9}
            transparent
            opacity={0.88}
          />
        </mesh>
        <mesh position={[0, 0, 0.01]}>
          <planeGeometry args={[3.24, 0.94]} />
          <meshBasicMaterial color={CHRONO_CYAN} wireframe />
        </mesh>

        <Text
          position={[-1.45, 0.22, 0.02]}
          fontSize={0.15}
          color="#ffffff"
          anchorX="left"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {selectedVehicle.modelName.toUpperCase()}
        </Text>

        <Text
          position={[-1.45, -0.02, 0.02]}
          fontSize={0.11}
          color={CHRONO_CYAN}
          anchorX="left"
          anchorY="middle"
        >
          {`PLATE: ${selectedVehicle.licensePlate} • BATTERY: ${selectedVehicle.batteryLevel}%`}
        </Text>

        <Text
          position={[-1.45, -0.24, 0.02]}
          fontSize={0.095}
          color="#94a3b8"
          anchorX="left"
          anchorY="middle"
        >
          {`RATE: ${(selectedVehicle?.hourlyRateVnd || 150000).toLocaleString()} VND/h • STALL: ${selectedVehicle?.stallLocationCode || 'BAY-01'}`}
        </Text>
      </group>

      {/* 4. Quick Vehicle Switcher Pedestals on Rim */}
      <group position={[0, 0.15, TURNTABLE_RADIUS + 0.8]}>
        {vehicles.map((v, i) => {
          const isSelected = v.id === selectedVehicleId;
          const xOffset = (i - (vehicles.length - 1) / 2) * 1.5;

          return (
            <group
              key={v.id}
              position={[xOffset, 0, 0]}
              onClick={(e) => {
                e.stopPropagation();
                selectVehicle(v.id);
              }}
            >
              <mesh position={[0, 0.15, 0]} castShadow>
                <boxGeometry args={[1.3, 0.3, 0.4]} />
                <meshStandardMaterial
                  color={isSelected ? '#0284c7' : '#0f172a'}
                  emissive={isSelected ? CHRONO_CYAN : '#000000'}
                  emissiveIntensity={isSelected ? 0.5 : 0}
                  metalness={0.8}
                  roughness={0.2}
                />
              </mesh>
              <Text
                position={[0, 0.18, 0.22]}
                fontSize={0.08}
                color={isSelected ? '#ffffff' : '#94a3b8'}
                anchorX="center"
                anchorY="middle"
              >
                {v.modelName}
              </Text>
            </group>
          );
        })}
      </group>
    </group>
  );
};
