import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAdminStore } from './useAdminStore';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';

export const BookingCore3D: React.FC = () => {
  const {
    conflicts,
    userRole,
    resolveBookingConflict,
    preemptReservation,
    purgeExpiredBookingHolds,
    isExecuting,
  } = useAdminStore();

  const helixRef = useRef<THREE.Group>(null);
  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  useFrame(({ clock }) => {
    if (helixRef.current) {
      helixRef.current.rotation.y = clock.getElapsedTime() * 0.75;
    }
  });

  const config = ADMIN_CORES_CONFIG.BOOKING_CORE;
  const isAuthorized = userRole === 'ROLE_ADMIN';
  const conflict = conflicts[0];

  return (
    <group name="BookingCoreStation" position={config.relativePosition}>
      {/* Title */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.22}
        color={config.primaryColor}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        CHRONO-SPATIAL BOOKING CORE
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.11}
        color={COMMAND_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Autonomous Conflict Resolution • Priority Preemption • Expired Hold Purge
      </Text>

      {/* Floating Chrono-Spatial Calendar Helix */}
      <group ref={helixRef} position={[0, 2.2, 0]}>
        {[-0.25, 0, 0.25].map((yOff, idx) => (
          <mesh key={idx} position={[0, yOff, 0]} rotation={[0, idx * 0.8, 0]}>
            <torusGeometry args={[0.48 - Math.abs(yOff) * 0.2, 0.03, 16, 32]} />
            <meshStandardMaterial
              color={config.primaryColor}
              emissive={config.primaryColor}
              emissiveIntensity={isAuthorized ? 0.8 : 0.2}
              wireframe
            />
          </mesh>
        ))}
      </group>

      {/* Terminal Board */}
      <group position={[0, 1.25, -0.4]} rotation={[0.15, Math.PI, 0]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[2.8, 1.6]} />
          <meshStandardMaterial
            color="#1a1205"
            roughness={0.3}
            metalness={0.9}
            transparent
            opacity={0.94}
          />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[2.84, 1.64]} />
          <meshBasicMaterial
            color={isAuthorized ? config.primaryColor : COMMAND_THEME.alertRed}
            wireframe
          />
        </mesh>

        {/* Conflict Data */}
        <group position={[-1.25, 0.55, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.072}
            color={config.primaryColor}
            anchorX="left"
            anchorY="middle"
          >
            DISPUTED SLOT #{conflict.id}: {conflict.vehicleName}
          </Text>
          <Text
            position={[0, -0.14, 0]}
            fontSize={0.06}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            Contenders: {conflict.conflictingUsers.join(' vs ')} ({conflict.slotTime})
          </Text>
          <Text
            position={[0, -0.28, 0]}
            fontSize={0.062}
            color={
              conflict.status === 'RESOLVED_BY_ADMIN' ? '#34d399' : '#fbbf24'
            }
            anchorX="left"
            anchorY="middle"
          >
            Status: {conflict.status.replace(/_/g, ' ')}
          </Text>
        </group>

        {/* 3D Action Controls */}
        <group position={[0, -0.25, 0.02]}>
          {/* Action 1: Resolve Conflict */}
          <group
            position={[0, 0.22, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                resolveBookingConflict(conflict.id);
            }}
            onPointerOver={() => {
              setHoveredBtn('resolve');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[2.5, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'resolve' ? '#b45309' : '#451a03'}
                emissive={config.primaryColor}
                emissiveIntensity={hoveredBtn === 'resolve' ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.062}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ ⏱ ARBITRATE SCHEDULE CONFLICT (FAVOR ALICE) ]
            </Text>
          </group>

          {/* Action 2: Preempt Reservation */}
          <group
            position={[-0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                preemptReservation(105, 'Platform Priority Maintenance');
            }}
            onPointerOver={() => {
              setHoveredBtn('preempt');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'preempt' ? '#78350f' : '#271007'}
                emissive={COMMAND_THEME.accentGold}
                emissiveIntensity={hoveredBtn === 'preempt' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.055}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ ⛔ PREEMPT SLOT ]
            </Text>
          </group>

          {/* Action 3: Purge Holds */}
          <group
            position={[0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) purgeExpiredBookingHolds();
            }}
            onPointerOver={() => {
              setHoveredBtn('purge');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'purge' ? '#9a3412' : '#431407'}
                emissive="#f97316"
                emissiveIntensity={hoveredBtn === 'purge' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.055}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ ⚡ PURGE HOLDS ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
