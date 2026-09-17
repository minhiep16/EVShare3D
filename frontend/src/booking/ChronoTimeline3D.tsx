import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_THEME } from './bookingLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const ChronoTimeline3D: React.FC = () => {
  const selectedDate = useBookingStore((s) => s.selectedDate);
  const timelineHours = useBookingStore((s) => s.timelineHours);
  const startHour = useBookingStore((s) => s.startHour);
  const endHour = useBookingStore((s) => s.endHour);
  const setTimeRange = useBookingStore((s) => s.setTimeRange);

  const [hoveredHour, setHoveredHour] = useState<number | null>(null);

  const {
    CHRONO_CYAN,
    BUFFER_PURPLE,
    OCCUPIED_AMBER,
    AVAILABLE_GREEN,
  } = BOOKING_CHAMBER_THEME;

  const totalHours = 24;
  const slotWidth = 0.165;
  const ribbonWidth = totalHours * slotWidth;

  const hoveredSlot = hoveredHour !== null ? timelineHours[hoveredHour] : null;

  return (
    <group name="ChronoTimeline3D" position={[0, 0, 0]}>
      {/* 1. Ribbon Title & Date Header */}
      <group position={[0, 0.45, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.11}
          color={CHRONO_CYAN}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {`24-HOUR CHRONO-TIMELINE (${selectedDate})`}
        </Text>
      </group>

      {/* 2. Main 24-Hour Timeline Blocks */}
      <group position={[-(ribbonWidth / 2) + slotWidth / 2, 0.12, 0]}>
        {timelineHours.map((slot) => {
          const isSelectedRange = slot.hour >= startHour && slot.hour < endHour;
          const isHovered = slot.hour === hoveredHour;

          // Color classification
          let blockColor = '#1e293b';
          let emissiveColor = '#000000';
          let emissiveInt = 0;

          if (isSelectedRange) {
            blockColor = '#0284c7';
            emissiveColor = CHRONO_CYAN;
            emissiveInt = 0.7;
          } else if (slot.isMyBooking) {
            blockColor = '#0369a1';
            emissiveColor = '#38bdf8';
            emissiveInt = 0.5;
          } else if (slot.isBooked) {
            blockColor = '#78350f';
            emissiveColor = OCCUPIED_AMBER;
            emissiveInt = 0.5;
          } else if (slot.isBuffer) {
            blockColor = '#581c87';
            emissiveColor = BUFFER_PURPLE;
            emissiveInt = 0.5;
          } else if (slot.isAvailable) {
            blockColor = '#064e3b';
            emissiveColor = AVAILABLE_GREEN;
            emissiveInt = 0.25;
          }

          const height = isSelectedRange ? 0.38 : isHovered ? 0.35 : 0.3;
          const zOffset = isSelectedRange ? 0.04 : isHovered ? 0.02 : 0;

          return (
            <group
              key={slot.hour}
              position={[slot.hour * slotWidth, 0, zOffset]}
              onClick={(e) => {
                e.stopPropagation();
                AudioEngine.play('UI_CLICK');
                // Set as start hour, preserving 4h duration if valid
                const newStart = slot.hour;
                const newEnd = Math.min(24, newStart + (endHour - startHour || 4));
                setTimeRange(newStart, newEnd);
              }}
              onPointerOver={(e) => {
                e.stopPropagation();
                setHoveredHour(slot.hour);
              }}
              onPointerOut={() => setHoveredHour(null)}
            >
              {/* Hour Segment Mesh */}
              <mesh castShadow>
                <boxGeometry args={[slotWidth - 0.015, height, 0.03]} />
                <meshStandardMaterial
                  color={blockColor}
                  emissive={emissiveColor}
                  emissiveIntensity={emissiveInt}
                  metalness={0.8}
                  roughness={0.2}
                />
              </mesh>

              {/* Hour Label */}
              <Text
                position={[0, -0.24, 0.02]}
                fontSize={0.055}
                color={isSelectedRange ? CHRONO_CYAN : '#94a3b8'}
                anchorX="center"
                anchorY="middle"
              >
                {slot.hour.toString().padStart(2, '0')}
              </Text>
            </group>
          );
        })}
      </group>

      {/* 3. Dynamic Inspection Tooltip / Readout */}
      <group position={[0, -0.32, 0.02]}>
        {hoveredSlot ? (
          <Text fontSize={0.08} color="#f8fafc" anchorX="center" anchorY="middle">
            {`SLOT ${hoveredSlot.label}: ${
              hoveredSlot.isBooked
                ? '⛔ RESERVED (UNAVAILABLE)'
                : hoveredSlot.isBuffer
                ? '⚡ 30-MIN TURNAROUND BUFFER (BR-BKG-02)'
                : '✅ AVAILABLE FOR INSTANT BOOKING'
            }`}
          </Text>
        ) : (
          <Text fontSize={0.075} color="#94a3b8" anchorX="center" anchorY="middle">
            {`SELECTED WINDOW: ${startHour.toString().padStart(2, '0')}:00 – ${endHour
              .toString()
              .padStart(2, '0')}:00 (${endHour - startHour} Hours)`}
          </Text>
        )}
      </group>

      {/* 4. Color Legend Bar */}
      <group position={[0, -0.48, 0.02]}>
        <group position={[-1.6, 0, 0]}>
          <mesh position={[-0.1, 0, 0]}>
            <boxGeometry args={[0.12, 0.08, 0.01]} />
            <meshBasicMaterial color={AVAILABLE_GREEN} />
          </mesh>
          <Text position={[0.02, 0, 0]} fontSize={0.065} color="#94a3b8" anchorX="left" anchorY="middle">
            Available
          </Text>
        </group>

        <group position={[-0.55, 0, 0]}>
          <mesh position={[-0.1, 0, 0]}>
            <boxGeometry args={[0.12, 0.08, 0.01]} />
            <meshBasicMaterial color={OCCUPIED_AMBER} />
          </mesh>
          <Text position={[0.02, 0, 0]} fontSize={0.065} color="#94a3b8" anchorX="left" anchorY="middle">
            Booked
          </Text>
        </group>

        <group position={[0.45, 0, 0]}>
          <mesh position={[-0.1, 0, 0]}>
            <boxGeometry args={[0.12, 0.08, 0.01]} />
            <meshBasicMaterial color={BUFFER_PURPLE} />
          </mesh>
          <Text position={[0.02, 0, 0]} fontSize={0.065} color="#94a3b8" anchorX="left" anchorY="middle">
            Buffer (+30m)
          </Text>
        </group>

        <group position={[1.65, 0, 0]}>
          <mesh position={[-0.1, 0, 0]}>
            <boxGeometry args={[0.12, 0.08, 0.01]} />
            <meshBasicMaterial color={CHRONO_CYAN} />
          </mesh>
          <Text position={[0.02, 0, 0]} fontSize={0.065} color="#94a3b8" anchorX="left" anchorY="middle">
            Your Selection
          </Text>
        </group>
      </group>
    </group>
  );
};
