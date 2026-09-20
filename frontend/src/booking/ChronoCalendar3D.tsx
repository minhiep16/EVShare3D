import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_THEME } from './bookingLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const ChronoCalendar3D: React.FC = () => {
  const calendarDays = useBookingStore((s) => s.calendarDays);
  const selectedDate = useBookingStore((s) => s.selectedDate);
  const selectDate = useBookingStore((s) => s.selectDate);
  const weekOffset = useBookingStore((s) => s.weekOffset);
  const setWeekOffset = useBookingStore((s) => s.setWeekOffset);
  const setActiveStep = useBookingStore((s) => s.setActiveStep);

  const [hoveredDate, setHoveredDate] = useState<string | null>(null);

  const { CHRONO_CYAN, OCCUPIED_AMBER, AVAILABLE_GREEN, CONFLICT_RED } = BOOKING_CHAMBER_THEME;

  return (
    <group name="ChronoCalendar3D" position={[0, 0, 0]}>
      {/* 1. Header Bar */}
      <group position={[0, 1.25, 0]}>
        <Text
          position={[0, 0, 0]}
          fontSize={0.13}
          color={CHRONO_CYAN}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          LỊCH ĐẶT XE KHÔNG GIAN 3D
        </Text>
        <Text
          position={[0, -0.16, 0]}
          fontSize={0.075}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          Chọn ngày đặt xe để kiểm tra tiến trình 24 giờ và khung giờ trống
        </Text>
      </group>

      {/* 2. Week Navigation Controls */}
      <group position={[0, 0.95, 0]}>
        {/* Prev Week Button */}
        <group
          position={[-1.6, 0, 0.02]}
          onClick={(e) => {
            e.stopPropagation();
            if (weekOffset > 0) {
              AudioEngine.play('UI_CLICK');
              setWeekOffset(weekOffset - 1);
            }
          }}
        >
          <mesh>
            <boxGeometry args={[0.9, 0.18, 0.03]} />
            <meshStandardMaterial
              color={weekOffset > 0 ? '#1e293b' : '#0f172a'}
              emissive={weekOffset > 0 ? CHRONO_CYAN : '#000000'}
              emissiveIntensity={weekOffset > 0 ? 0.3 : 0}
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.07}
            color={weekOffset > 0 ? '#ffffff' : '#64748b'}
            anchorX="center"
            anchorY="middle"
          >
            {'◄ TUẦN TRƯỚC'}
          </Text>
        </group>

        {/* Current Window Indicator */}
        <Text
          position={[0, 0, 0.02]}
          fontSize={0.075}
          color="#e2e8f0"
          anchorX="center"
          anchorY="middle"
        >
          {`HIỂN THỊ 14 NGÀY (LỆCH: +${weekOffset * 7} NGÀY)`}
        </Text>

        {/* Next Week Button */}
        <group
          position={[1.6, 0, 0.02]}
          onClick={(e) => {
            e.stopPropagation();
            if (weekOffset < 3) {
              AudioEngine.play('UI_CLICK');
              setWeekOffset(weekOffset + 1);
            }
          }}
        >
          <mesh>
            <boxGeometry args={[0.9, 0.18, 0.03]} />
            <meshStandardMaterial
              color={weekOffset < 3 ? '#1e293b' : '#0f172a'}
              emissive={weekOffset < 3 ? CHRONO_CYAN : '#000000'}
              emissiveIntensity={weekOffset < 3 ? 0.3 : 0}
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.07}
            color={weekOffset < 3 ? '#ffffff' : '#64748b'}
            anchorX="center"
            anchorY="middle"
          >
            {'TUẦN TIẾP ►'}
          </Text>
        </group>
      </group>

      {/* 3. 14 Days Grid (2 rows x 7 days) */}
      <group position={[0, 0.25, 0]}>
        {calendarDays.map((day, idx) => {
          const row = Math.floor(idx / 7); // 0 or 1
          const col = idx % 7; // 0 to 6

          const x = (col - 3) * 0.65;
          const y = (1 - row) * 0.72 - 0.2;
          const isSelected = day.dateStr === selectedDate;
          const isHovered = day.dateStr === hoveredDate;

          const statusColor =
            day.occupancyStatus === 'AVAILABLE'
              ? AVAILABLE_GREEN
              : day.occupancyStatus === 'PARTIALLY_BOOKED'
              ? OCCUPIED_AMBER
              : CONFLICT_RED;

          const zElev = isSelected ? 0.05 : isHovered ? 0.025 : 0;

          return (
            <group
              key={day.dateStr}
              position={[x, y, zElev]}
              onClick={(e) => {
                e.stopPropagation();
                AudioEngine.play('UI_CLICK');
                selectDate(day.dateStr);
              }}
              onPointerOver={(e) => {
                e.stopPropagation();
                setHoveredDate(day.dateStr);
              }}
              onPointerOut={() => setHoveredDate(null)}
            >
              {/* Day Card Backplate */}
              <mesh castShadow>
                <boxGeometry args={[0.58, 0.62, 0.03]} />
                <meshStandardMaterial
                  color={isSelected ? '#0c2238' : isHovered ? '#1e293b' : '#0a101d'}
                  metalness={0.8}
                  roughness={0.2}
                  emissive={isSelected ? CHRONO_CYAN : isHovered ? '#38bdf8' : '#000000'}
                  emissiveIntensity={isSelected ? 0.45 : isHovered ? 0.2 : 0}
                />
              </mesh>

              {/* Glowing Wireframe Rim on Selected */}
              {isSelected && (
                <mesh position={[0, 0, 0.02]}>
                  <planeGeometry args={[0.59, 0.63]} />
                  <meshBasicMaterial color={CHRONO_CYAN} wireframe />
                </mesh>
              )}

              {/* Day of Week */}
              <Text
                position={[0, 0.2, 0.02]}
                fontSize={0.065}
                color={isSelected ? CHRONO_CYAN : '#94a3b8'}
                anchorX="center"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                {day.dayOfWeek === 'MON' ? 'T2' : day.dayOfWeek === 'TUE' ? 'T3' : day.dayOfWeek === 'WED' ? 'T4' : day.dayOfWeek === 'THU' ? 'T5' : day.dayOfWeek === 'FRI' ? 'T6' : day.dayOfWeek === 'SAT' ? 'T7' : 'CN'}
              </Text>

              {/* Day Number */}
              <Text
                position={[0, 0.02, 0.02]}
                fontSize={0.16}
                color={isSelected ? '#ffffff' : '#f1f5f9'}
                anchorX="center"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                {day.dayNumber.toString()}
              </Text>

              {/* Month */}
              <Text
                position={[0, -0.14, 0.02]}
                fontSize={0.055}
                color="#64748b"
                anchorX="center"
                anchorY="middle"
              >
                {`Tháng ${new Date(day.dateStr).getMonth() + 1}`}
              </Text>

              {/* Occupancy Indicator Dot */}
              <group position={[0, -0.23, 0.02]}>
                <mesh>
                  <circleGeometry args={[0.035, 16]} />
                  <meshBasicMaterial color={statusColor} />
                </mesh>
              </group>
            </group>
          );
        })}
      </group>

      {/* 4. Action Button: Advance to Timeline Step */}
      <group
        position={[0, -0.72, 0.02]}
        onClick={(e) => {
          e.stopPropagation();
          AudioEngine.getInstance().playClickSnap();
          setActiveStep('TIMELINE');
        }}
      >
        <mesh>
          <boxGeometry args={[2.4, 0.24, 0.04]} />
          <meshStandardMaterial
            color="#0284c7"
            emissive={CHRONO_CYAN}
            emissiveIntensity={0.6}
          />
        </mesh>
        <Text
          position={[0, 0, 0.03]}
          fontSize={0.08}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {`XEM TIẾN TRÌNH NGÀY ${selectedDate} ►`}
        </Text>
      </group>
    </group>
  );
};
