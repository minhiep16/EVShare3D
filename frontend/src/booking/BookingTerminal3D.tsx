import React from 'react';
import { Text } from '@react-three/drei';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_LAYOUT, BOOKING_CHAMBER_THEME } from './bookingLayout';
import { ChronoCalendar3D } from './ChronoCalendar3D';
import { ChronoTimeline3D } from './ChronoTimeline3D';
import { TimeSelector3D } from './TimeSelector3D';
import { BookingConfirmation3D } from './BookingConfirmation3D';
import { BookingResult3D } from './BookingResult3D';
import { BookingHistory3D } from './BookingHistory3D';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import type { BookingStep } from './bookingTypes';

export const BookingTerminal3D: React.FC = () => {
  const activeStep = useBookingStore((s) => s.activeStep);
  const setActiveStep = useBookingStore((s) => s.setActiveStep);
  const isTerminalFocused = useBookingStore((s) => s.isTerminalFocused);
  const setIsTerminalFocused = useBookingStore((s) => s.setIsTerminalFocused);
  const setCameraPreset = useBookingStore((s) => s.setCameraPreset);

  const { TERMINAL } = BOOKING_CHAMBER_LAYOUT.POSITIONS;
  const { CHRONO_CYAN, PANEL_BG } = BOOKING_CHAMBER_THEME;

  const steps: { id: BookingStep; label: string }[] = [
    { id: 'CALENDAR', label: '1. CALENDAR' },
    { id: 'TIMELINE', label: '2. TIMELINE' },
    { id: 'TIME_SELECT', label: '3. TIME SELECT' },
    { id: 'CONFIRMATION', label: '4. CONFIRM' },
    { id: 'HISTORY', label: '5. HISTORY' },
  ];

  return (
    <group
      name="BookingTerminal3D"
      position={TERMINAL}
      rotation={[-Math.PI * 0.05, 0, 0]} // -9 degrees tilted back
    >
      {/* 1. Physical Console Pedestal Mount */}
      <group position={[0, -0.85, 0]}>
        {/* Base Floor Plate */}
        <mesh position={[0, 0.05, 0]} castShadow receiveShadow>
          <boxGeometry args={[2.4, 0.1, 1.2]} />
          <meshStandardMaterial color="#0f172a" metalness={0.9} roughness={0.2} />
        </mesh>
        {/* Twin Vertical Support Struts */}
        {[-0.8, 0.8].map((x) => (
          <mesh key={x} position={[x, 0.7, 0]} castShadow>
            <boxGeometry args={[0.15, 1.3, 0.25]} />
            <meshStandardMaterial color="#1e293b" metalness={0.8} roughness={0.3} />
          </mesh>
        ))}
      </group>

      {/* 2. Main Terminal Holographic Console Display Box */}
      <group position={[0, 1.5, 0]}>
        {/* Dark Glass Screen */}
        <mesh position={[0, 0, 0]}>
          <boxGeometry args={[4.4, 3.2, 0.06]} />
          <meshStandardMaterial
            color={PANEL_BG}
            metalness={0.85}
            roughness={0.2}
            transparent
            opacity={0.92}
          />
        </mesh>

        {/* Outer Glowing Wireframe Rim */}
        <mesh position={[0, 0, 0.035]}>
          <planeGeometry args={[4.38, 3.18]} />
          <meshBasicMaterial color={CHRONO_CYAN} wireframe />
        </mesh>

        {/* 3. Top Header Bar & Step Breadcrumbs */}
        <group position={[0, 1.35, 0.04]}>
          {/* Breadcrumb Steps */}
          <group position={[-1.5, 0, 0]}>
            {steps.map((st, i) => {
              const isCurrent = activeStep === st.id;
              const xPos = i * 0.76;

              return (
                <group
                  key={st.id}
                  position={[xPos, 0, 0]}
                  onClick={(e) => {
                    e.stopPropagation();
                    AudioEngine.play('UI_CLICK');
                    setActiveStep(st.id);
                  }}
                >
                  <mesh>
                    <boxGeometry args={[0.72, 0.18, 0.02]} />
                    <meshStandardMaterial
                      color={isCurrent ? '#0284c7' : '#1e293b'}
                      emissive={isCurrent ? CHRONO_CYAN : '#000000'}
                      emissiveIntensity={isCurrent ? 0.5 : 0}
                    />
                  </mesh>
                  <Text
                    position={[0, 0, 0.015]}
                    fontSize={0.052}
                    color={isCurrent ? '#ffffff' : '#94a3b8'}
                    anchorX="center"
                    anchorY="middle"
                    font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                  >
                    {st.label}
                  </Text>
                </group>
              );
            })}
          </group>

          {/* Camera Focus Toggle Button */}
          <group
            position={[1.85, 0, 0]}
            onClick={(e) => {
              e.stopPropagation();
              AudioEngine.getInstance().playClickSnap();
              if (isTerminalFocused) {
                setIsTerminalFocused(false);
                setCameraPreset('CHAMBER_OVERVIEW');
              } else {
                setIsTerminalFocused(true);
                setCameraPreset('TERMINAL_FOCUS');
              }
            }}
          >
            <mesh>
              <boxGeometry args={[0.55, 0.18, 0.02]} />
              <meshStandardMaterial color="#334155" />
            </mesh>
            <Text
              position={[0, 0, 0.015]}
              fontSize={0.06}
              color="#cbd5e1"
              anchorX="center"
              anchorY="middle"
            >
              {isTerminalFocused ? 'WIDE CAM' : 'FOCUS'}
            </Text>
          </group>
        </group>

        {/* 4. Active Workflow Stage Rendering */}
        <group position={[0, -0.05, 0.04]}>
          {activeStep === 'CALENDAR' && <ChronoCalendar3D />}

          {(activeStep === 'TIMELINE' || activeStep === 'TIME_SELECT') && (
            <group position={[0, 0, 0]}>
              <group position={[0, 0.5, 0]}>
                <ChronoTimeline3D />
              </group>
              <group position={[0, -0.55, 0]}>
                <TimeSelector3D />
              </group>
            </group>
          )}

          {activeStep === 'CONFIRMATION' && <BookingConfirmation3D />}

          {activeStep === 'RESULT' && <BookingResult3D />}

          {activeStep === 'HISTORY' && <BookingHistory3D />}
        </group>
      </group>
    </group>
  );
};
