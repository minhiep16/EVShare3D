import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group } from 'three';
import { useBookingStore } from './useBookingStore';
import { BOOKING_CHAMBER_LAYOUT, BOOKING_CHAMBER_THEME } from './bookingLayout';

export const FairUsageHoloPillar3D: React.FC = () => {
  const fairUsage = useBookingStore((s) => s.fairUsage);
  const { FAIR_USAGE_PILLAR } = BOOKING_CHAMBER_LAYOUT.POSITIONS;
  const { CHRONO_CYAN, AVAILABLE_GREEN } = BOOKING_CHAMBER_THEME;

  const holoRef = useRef<Group>(null);

  useFrame(({ clock }) => {
    if (holoRef.current) {
      const t = clock.getElapsedTime();
      holoRef.current.position.y = 1.6 + Math.sin(t * 1.5) * 0.04;
    }
  });

  const score = fairUsage?.score || 96;
  const fairnessRatio = fairUsage?.fairnessRatio || 1.05;
  const tier = fairUsage?.priorityTier || 'BALANCED';

  return (
    <group name="FairUsageHoloPillar3D" position={FAIR_USAGE_PILLAR} rotation={[0, Math.PI / 6, 0]}>
      {/* 1. Pedestal Base Mount */}
      <mesh position={[0, 0.45, 0]} castShadow receiveShadow>
        <cylinderGeometry args={[0.6, 0.75, 0.9, 24]} />
        <meshStandardMaterial color="#0f172a" metalness={0.9} roughness={0.2} />
      </mesh>

      {/* Glowing Pedestal Rim */}
      <mesh position={[0, 0.91, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.52, 0.6, 24]} />
        <meshBasicMaterial color={CHRONO_CYAN} transparent opacity={0.7} />
      </mesh>

      {/* 2. Floating Holographic Quota Stela */}
      <group ref={holoRef} position={[0, 1.6, 0]}>
        {/* Hologram Glass Plate */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[1.5, 1.6]} />
          <meshStandardMaterial
            color="#040d1a"
            metalness={0.9}
            roughness={0.15}
            transparent
            opacity={0.9}
          />
        </mesh>
        <mesh position={[0, 0, 0.01]}>
          <planeGeometry args={[1.54, 1.64]} />
          <meshBasicMaterial color={CHRONO_CYAN} wireframe />
        </mesh>

        {/* Title */}
        <Text
          position={[0, 0.65, 0.02]}
          fontSize={0.075}
          color={CHRONO_CYAN}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          FAIR USAGE QUOTA
        </Text>

        <Text
          position={[0, 0.52, 0.02]}
          fontSize={0.055}
          color="#94a3b8"
          anchorX="center"
          anchorY="middle"
        >
          BR-FAIR-01..03 MOBILITY STANDING
        </Text>

        {/* Score Ring Display */}
        <group position={[0, 0.22, 0.02]}>
          <mesh>
            <ringGeometry args={[0.18, 0.22, 32]} />
            <meshBasicMaterial color={AVAILABLE_GREEN} />
          </mesh>
          <Text
            position={[0, 0, 0.01]}
            fontSize={0.1}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {score.toString()}
          </Text>
        </group>

        {/* Metrics Readout */}
        <group position={[-0.65, -0.15, 0.02]}>
          <Text position={[0, 0, 0]} fontSize={0.06} color="#64748b" anchorX="left" anchorY="middle">
            FAIRNESS RATIO:
          </Text>
          <Text position={[0.75, 0, 0]} fontSize={0.065} color="#38bdf8" anchorX="left" anchorY="middle">
            {`${fairnessRatio.toFixed(2)}x`}
          </Text>

          <Text position={[0, -0.14, 0]} fontSize={0.06} color="#64748b" anchorX="left" anchorY="middle">
            EQUITY TIER:
          </Text>
          <Text position={[0.75, -0.14, 0]} fontSize={0.065} color={AVAILABLE_GREEN} anchorX="left" anchorY="middle">
            {tier}
          </Text>

          <Text position={[0, -0.28, 0]} fontSize={0.06} color="#64748b" anchorX="left" anchorY="middle">
            PRIORITY STATUS:
          </Text>
          <Text position={[0.75, -0.28, 0]} fontSize={0.065} color="#ffffff" anchorX="left" anchorY="middle">
            ACTIVE QUOTA
          </Text>
        </group>

        {/* Advisory Box */}
        <group position={[0, -0.62, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.05}
            color="#94a3b8"
            anchorX="center"
            anchorY="middle"
            maxWidth={1.35}
            lineHeight={1.3}
          >
            {fairUsage?.recommendation || 'Syndicate quota in balance. Immediate reservation approved.'}
          </Text>
        </group>
      </group>
    </group>
  );
};
