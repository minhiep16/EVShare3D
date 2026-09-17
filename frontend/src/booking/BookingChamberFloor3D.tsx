import React, { useMemo } from 'react';
import { BOOKING_CHAMBER_LAYOUT, BOOKING_CHAMBER_THEME } from './bookingLayout';

export const BookingChamberFloor3D: React.FC = () => {
  const { CHAMBER_RADIUS, TURNTABLE_RADIUS } = BOOKING_CHAMBER_LAYOUT;
  const { CHRONO_CYAN, DARK_OBSIDIAN, BUFFER_PURPLE } = BOOKING_CHAMBER_THEME;

  // Chronometer radial tick marks (12 major hours, 48 minor ticks)
  const dialTicks = useMemo(() => {
    const ticks: { x: number; z: number; length: number; rotY: number; isMajor: boolean }[] = [];
    const total = 60;
    const dialRadius = 11.2;

    for (let i = 0; i < total; i++) {
      const angle = (i / total) * Math.PI * 2;
      const isMajor = i % 5 === 0;
      const length = isMajor ? 0.8 : 0.4;
      ticks.push({
        x: Math.cos(angle) * dialRadius,
        z: Math.sin(angle) * dialRadius,
        length,
        rotY: -angle,
        isMajor,
      });
    }
    return ticks;
  }, []);

  // 8 Perimeter atmospheric energy pillars
  const perimeterPillars = useMemo(() => {
    const pillars: { x: number; z: number }[] = [];
    const count = 8;
    const radius = 13.0;
    for (let i = 0; i < count; i++) {
      const angle = (i / count) * Math.PI * 2;
      pillars.push({
        x: Math.cos(angle) * radius,
        z: Math.sin(angle) * radius,
      });
    }
    return pillars;
  }, []);

  return (
    <group name="BookingChamberFloor3D">
      {/* 1. Main Obsidian Floor Disc */}
      <mesh position={[0, -0.08, 0]} receiveShadow>
        <cylinderGeometry args={[CHAMBER_RADIUS, CHAMBER_RADIUS + 0.5, 0.16, 64]} />
        <meshStandardMaterial
          color={DARK_OBSIDIAN}
          roughness={0.35}
          metalness={0.8}
        />
      </mesh>

      {/* 2. Outer Luminous Neon Perimeter Ring */}
      <mesh position={[0, 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[CHAMBER_RADIUS - 0.25, CHAMBER_RADIUS, 64]} />
        <meshBasicMaterial color={CHRONO_CYAN} transparent opacity={0.65} />
      </mesh>

      {/* 3. Chronometer Dial Ring Accent */}
      <mesh position={[0, 0.012, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[11.0, 11.06, 64]} />
        <meshBasicMaterial color={CHRONO_CYAN} transparent opacity={0.4} />
      </mesh>

      {/* 4. Chronometer Hour & Minute Ticks */}
      {dialTicks.map((tick, idx) => (
        <mesh
          key={idx}
          position={[tick.x, 0.015, tick.z]}
          rotation={[0, tick.rotY, 0]}
        >
          <boxGeometry args={[0.08, 0.01, tick.length]} />
          <meshBasicMaterial
            color={tick.isMajor ? CHRONO_CYAN : BUFFER_PURPLE}
            transparent
            opacity={tick.isMajor ? 0.85 : 0.45}
          />
        </mesh>
      ))}

      {/* 5. Inner Turntable Guide Ring */}
      <mesh position={[0, 0.012, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[TURNTABLE_RADIUS + 0.4, TURNTABLE_RADIUS + 0.55, 48]} />
        <meshBasicMaterial color={CHRONO_CYAN} transparent opacity={0.5} />
      </mesh>

      {/* 6. Perimeter Ambient Energy Pylons */}
      {perimeterPillars.map((p, idx) => (
        <group key={idx} position={[p.x, 0, p.z]}>
          {/* Base Mount */}
          <mesh position={[0, 0.25, 0]} castShadow>
            <cylinderGeometry args={[0.3, 0.45, 0.5, 16]} />
            <meshStandardMaterial color="#0f172a" metalness={0.9} roughness={0.2} />
          </mesh>
          {/* Vertical Glowing Light Core */}
          <mesh position={[0, 1.8, 0]}>
            <cylinderGeometry args={[0.06, 0.06, 2.6, 12]} />
            <meshBasicMaterial color={CHRONO_CYAN} transparent opacity={0.7} />
          </mesh>
          {/* Pylon Top Fin */}
          <mesh position={[0, 3.2, 0]}>
            <coneGeometry args={[0.2, 0.4, 16]} />
            <meshStandardMaterial color="#1e293b" metalness={0.8} />
          </mesh>
        </group>
      ))}
    </group>
  );
};
