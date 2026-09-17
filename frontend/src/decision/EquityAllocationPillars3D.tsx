import React from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';

interface EquityAllocationPillars3DProps {
  position?: [number, number, number];
}

export const EquityAllocationPillars3D: React.FC<EquityAllocationPillars3DProps> = ({
  position = DECISION_LAYOUT.equityPillarsPosition,
}) => {
  const members = useDecisionStore((s) => s.members);

  return (
    <group name="EquityAllocationPillars" position={position}>
      {/* Station Header */}
      <group position={[0, 4.2, 0]}>
        <Text
          fontSize={0.24}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          MEMBER EQUITY ALLOCATION
        </Text>
        <Text
          position={[0, -0.28, 0]}
          fontSize={0.14}
          color={DECISION_LAYOUT.colors.indigoLight}
          anchorX="center"
          anchorY="middle"
        >
          WEIGHTED VOTING SHARES (TOTAL: 100.00%)
        </Text>
      </group>

      {/* Base Plinth */}
      <mesh position={[0, 0.15, 0]}>
        <boxGeometry args={[4.8, 0.3, 1.6]} />
        <meshStandardMaterial color="#0f172a" roughness={0.5} metalness={0.6} />
      </mesh>
      <lineSegments position={[0, 0.15, 0]}>
        <edgesGeometry args={[new THREE.BoxGeometry(4.8, 0.3, 1.6)]} />
        <lineBasicMaterial color={DECISION_LAYOUT.colors.indigoLight} />
      </lineSegments>

      {/* 4 Pillars representing syndicate co-owners */}
      {members.map((member, i) => {
        const spacing = 1.1;
        const xPos = (i - 1.5) * spacing;
        // Height scaled by equity percentage (e.g., 35% -> 2.45m, 20% -> 1.4m)
        const pillarHeight = Math.max(0.6, (member.equityPercentage / 100) * 6.5);
        const yCenter = 0.3 + pillarHeight / 2;

        const voteColor = member.hasVoted
          ? member.voteChoice === 'APPROVE'
            ? '#10b981'
            : member.voteChoice === 'REJECT'
            ? '#ef4444'
            : '#f59e0b'
          : '#64748b';

        return (
          <group key={member.userId} position={[xPos, 0, 0]}>
            {/* Column Column Mesh */}
            <mesh castShadow receiveShadow position={[0, yCenter, 0]}>
              <cylinderGeometry args={[0.26, 0.3, pillarHeight, 20]} />
              <meshStandardMaterial
                color={member.avatarColor}
                emissive={member.avatarColor}
                emissiveIntensity={0.25}
                roughness={0.2}
                metalness={0.7}
                transparent
                opacity={0.88}
              />
            </mesh>

            {/* Glowing Wireframe Cage */}
            <mesh position={[0, yCenter, 0]}>
              <cylinderGeometry args={[0.28, 0.32, pillarHeight + 0.05, 12]} />
              <meshBasicMaterial
                color={member.avatarColor}
                wireframe
                transparent
                opacity={0.35}
              />
            </mesh>

            {/* Base socket ring */}
            <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.31, 0]}>
              <ringGeometry args={[0.3, 0.4, 24]} />
              <meshBasicMaterial color={member.avatarColor} />
            </mesh>

            {/* Floating Info Above Pillar */}
            <group position={[0, pillarHeight + 0.6, 0]}>
              {/* Member Name */}
              <Text
                position={[0, 0.32, 0]}
                fontSize={0.13}
                color={DECISION_LAYOUT.colors.textWhite}
                anchorX="center"
                anchorY="middle"
              >
                {member.memberName}
              </Text>

              {/* Equity Percentage */}
              <Text
                position={[0, 0.15, 0]}
                fontSize={0.16}
                color={member.avatarColor}
                anchorX="center"
                anchorY="middle"
                letterSpacing={0.04}
              >
                {`${member.equityPercentage.toFixed(1)}%`}
              </Text>

              {/* Ballot Badge */}
              <group position={[0, -0.08, 0]}>
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[0.9, 0.22]} />
                  <meshBasicMaterial color="#020617" opacity={0.85} transparent side={THREE.DoubleSide} />
                </mesh>
                <lineSegments position={[0, 0, 0.005]}>
                  <edgesGeometry args={[new THREE.PlaneGeometry(0.9, 0.22)]} />
                  <lineBasicMaterial color={voteColor} />
                </lineSegments>
                <Text
                  position={[0, 0, 0.02]}
                  fontSize={0.09}
                  color={voteColor}
                  anchorX="center"
                  anchorY="middle"
                >
                  {member.hasVoted ? `✓ ${member.voteChoice}` : 'PENDING'}
                </Text>
              </group>
            </group>
          </group>
        );
      })}
    </group>
  );
};
