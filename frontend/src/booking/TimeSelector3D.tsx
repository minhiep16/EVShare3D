import React from 'react';
import { Text } from '@react-three/drei';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_THEME } from './bookingLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const TimeSelector3D: React.FC = () => {
  const startHour = useBookingStore((s) => s.startHour);
  const endHour = useBookingStore((s) => s.endHour);
  const durationHours = useBookingStore((s) => s.durationHours);
  const estimatedCostVnd = useBookingStore((s) => s.estimatedCostVnd);
  const isSlotAvailable = useBookingStore((s) => s.isSlotAvailable);
  const conflictReason = useBookingStore((s) => s.conflictReason);
  const isCheckingAvailability = useBookingStore((s) => s.isCheckingAvailability);

  const setTimeRange = useBookingStore((s) => s.setTimeRange);
  const setDurationPreset = useBookingStore((s) => s.setDurationPreset);
  const setActiveStep = useBookingStore((s) => s.setActiveStep);

  const { CHRONO_CYAN, AVAILABLE_GREEN, CONFLICT_RED } = BOOKING_CHAMBER_THEME;

  const presets = [
    { label: '2 HOURS', hours: 2 },
    { label: '4 HOURS', hours: 4 },
    { label: '8 HOURS', hours: 8 },
    { label: 'FULL DAY', hours: 14 },
  ];

  return (
    <group name="TimeSelector3D" position={[0, 0, 0]}>
      {/* 1. Steppers Row (Start Hour & End Hour) */}
      <group position={[0, 0.48, 0]}>
        {/* Start Hour Stepper */}
        <group position={[-1.25, 0, 0]}>
          <Text position={[0, 0.16, 0.02]} fontSize={0.075} color="#94a3b8" anchorX="center" anchorY="middle">
            START TIME:
          </Text>
          <group position={[0, -0.05, 0]}>
            {/* Dec button */}
            <group
              position={[-0.45, 0, 0.02]}
              onClick={(e) => {
                e.stopPropagation();
                if (startHour > 0) {
                  AudioEngine.play('UI_CLICK');
                  setTimeRange(startHour - 1, endHour);
                }
              }}
            >
              <mesh>
                <boxGeometry args={[0.26, 0.22, 0.03]} />
                <meshStandardMaterial color="#1e293b" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.12} color="#ffffff" anchorX="center" anchorY="middle">
                -
              </Text>
            </group>

            {/* Time Display */}
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.13}
              color={CHRONO_CYAN}
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              {`${startHour.toString().padStart(2, '0')}:00`}
            </Text>

            {/* Inc button */}
            <group
              position={[0.45, 0, 0.02]}
              onClick={(e) => {
                e.stopPropagation();
                if (startHour < endHour - 1) {
                  AudioEngine.play('UI_CLICK');
                  setTimeRange(startHour + 1, endHour);
                }
              }}
            >
              <mesh>
                <boxGeometry args={[0.26, 0.22, 0.03]} />
                <meshStandardMaterial color="#1e293b" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.12} color="#ffffff" anchorX="center" anchorY="middle">
                +
              </Text>
            </group>
          </group>
        </group>

        {/* End Hour Stepper */}
        <group position={[1.25, 0, 0]}>
          <Text position={[0, 0.16, 0.02]} fontSize={0.075} color="#94a3b8" anchorX="center" anchorY="middle">
            RETURN TIME:
          </Text>
          <group position={[0, -0.05, 0]}>
            {/* Dec button */}
            <group
              position={[-0.45, 0, 0.02]}
              onClick={(e) => {
                e.stopPropagation();
                if (endHour > startHour + 1) {
                  AudioEngine.play('UI_CLICK');
                  setTimeRange(startHour, endHour - 1);
                }
              }}
            >
              <mesh>
                <boxGeometry args={[0.26, 0.22, 0.03]} />
                <meshStandardMaterial color="#1e293b" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.12} color="#ffffff" anchorX="center" anchorY="middle">
                -
              </Text>
            </group>

            {/* Time Display */}
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.13}
              color={CHRONO_CYAN}
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              {`${endHour.toString().padStart(2, '0')}:00`}
            </Text>

            {/* Inc button */}
            <group
              position={[0.45, 0, 0.02]}
              onClick={(e) => {
                e.stopPropagation();
                if (endHour < 24) {
                  AudioEngine.play('UI_CLICK');
                  setTimeRange(startHour, endHour + 1);
                }
              }}
            >
              <mesh>
                <boxGeometry args={[0.26, 0.22, 0.03]} />
                <meshStandardMaterial color="#1e293b" />
              </mesh>
              <Text position={[0, 0, 0.02]} fontSize={0.12} color="#ffffff" anchorX="center" anchorY="middle">
                +
              </Text>
            </group>
          </group>
        </group>
      </group>

      {/* 2. Quick Duration Presets */}
      <group position={[0, 0.18, 0]}>
        {presets.map((p, idx) => {
          const x = (idx - 1.5) * 0.85;
          const isSelected = durationHours === p.hours;
          return (
            <group
              key={p.label}
              position={[x, 0, 0.02]}
              onClick={(e) => {
                e.stopPropagation();
                AudioEngine.play('UI_CLICK');
                setDurationPreset(p.hours);
              }}
            >
              <mesh>
                <boxGeometry args={[0.78, 0.18, 0.03]} />
                <meshStandardMaterial
                  color={isSelected ? '#0284c7' : '#0f172a'}
                  emissive={isSelected ? CHRONO_CYAN : '#000000'}
                  emissiveIntensity={isSelected ? 0.4 : 0}
                />
              </mesh>
              <Text
                position={[0, 0, 0.02]}
                fontSize={0.065}
                color={isSelected ? '#ffffff' : '#94a3b8'}
                anchorX="center"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                {p.label}
              </Text>
            </group>
          );
        })}
      </group>

      {/* 3. Real-Time Availability & Quota Status Feedback */}
      <group position={[0, -0.1, 0.02]}>
        {isCheckingAvailability ? (
          <Text fontSize={0.08} color="#f59e0b" anchorX="center" anchorY="middle">
            {'VERIFYING AVAILABILITY & TURNAROUND BUFFER...'}
          </Text>
        ) : isSlotAvailable ? (
          <Text fontSize={0.085} color={AVAILABLE_GREEN} anchorX="center" anchorY="middle">
            {`SLOT VERIFIED AVAILABLE • ${durationHours}H TOTAL • EST: ${(estimatedCostVnd || 0).toLocaleString()} VND`}
          </Text>
        ) : (
          <Text fontSize={0.075} color={CONFLICT_RED} anchorX="center" anchorY="middle">
            {conflictReason || 'UNAVAILABLE: Turnaround buffer conflict or existing booking'}
          </Text>
        )}
      </group>

      {/* 4. Action Buttons: Back to Calendar & Proceed to Confirmation */}
      <group position={[0, -0.36, 0.02]}>
        {/* Back Button */}
        <group
          position={[-1.1, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            AudioEngine.play('UI_CLICK');
            setActiveStep('CALENDAR');
          }}
        >
          <mesh>
            <boxGeometry args={[1.1, 0.22, 0.03]} />
            <meshStandardMaterial color="#334155" />
          </mesh>
          <Text position={[0, 0, 0.02]} fontSize={0.075} color="#cbd5e1" anchorX="center" anchorY="middle">
            {'◄ CALENDAR'}
          </Text>
        </group>

        {/* Proceed Button */}
        <group
          position={[1.1, 0, 0]}
          onClick={(e) => {
            e.stopPropagation();
            if (isSlotAvailable) {
              AudioEngine.getInstance().playClickSnap();
              setActiveStep('CONFIRMATION');
            }
          }}
        >
          <mesh>
            <boxGeometry args={[1.5, 0.22, 0.03]} />
            <meshStandardMaterial
              color={isSlotAvailable ? '#0284c7' : '#1e293b'}
              emissive={isSlotAvailable ? CHRONO_CYAN : '#000000'}
              emissiveIntensity={isSlotAvailable ? 0.6 : 0}
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.08}
            color={isSlotAvailable ? '#ffffff' : '#64748b'}
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {'REVIEW & CONFIRM ►'}
          </Text>
        </group>
      </group>
    </group>
  );
};
