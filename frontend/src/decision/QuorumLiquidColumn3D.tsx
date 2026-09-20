import React from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';
import { formatPercentageVN } from '@/i18n';

interface QuorumLiquidColumn3DProps {
  position?: [number, number, number];
}

export const QuorumLiquidColumn3D: React.FC<QuorumLiquidColumn3DProps> = ({
  position = DECISION_LAYOUT.quorumColumnPosition,
}) => {
  const tally = useDecisionStore((s) => s.tally);
  const results = useDecisionStore((s) => s.results);

  const columnMaxHeight = 3.8;
  const participationRate = results?.participationRatePercentage ?? tally?.participationRatePercentage ?? 45.0;
  const quorumThreshold = results?.quorumPercentage ?? tally?.quorumPercentage ?? 60.0;
  const quorumReached = results?.quorumReached ?? tally?.quorumReached ?? false;

  // Liquid height based on participating equity
  const liquidHeight = Math.max(0.1, (participationRate / 100) * columnMaxHeight);
  const liquidCenterY = 0.3 + liquidHeight / 2;

  // Laser line height for quorum threshold (60%)
  const thresholdY = 0.3 + (quorumThreshold / 100) * columnMaxHeight;

  return (
    <group name="QuorumLiquidColumn" position={position}>
      {/* Station Header */}
      <group position={[0, 4.8, 0]}>
        <Text
          fontSize={0.24}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          GIÁM SÁT HẠN NGẠCH BIỂU QUYẾT
        </Text>
        <Text
          position={[0, -0.28, 0]}
          fontSize={0.14}
          color={DECISION_LAYOUT.colors.cyanQuorum}
          anchorX="center"
          anchorY="middle"
        >
          QUY ĐỊNH THAM GIA TỐI THIỂU 60,00%
        </Text>
      </group>

      {/* Base Plinth */}
      <mesh position={[0, 0.15, 0]}>
        <cylinderGeometry args={[1.3, 1.5, 0.3, 32]} />
        <meshStandardMaterial color="#0f172a" roughness={0.4} metalness={0.7} />
      </mesh>

      {/* Outer Transparent Glass Cylinder Tube */}
      <mesh position={[0, 0.3 + columnMaxHeight / 2, 0]}>
        <cylinderGeometry args={[0.7, 0.7, columnMaxHeight, 32]} />
        <meshStandardMaterial
          color="#38bdf8"
          roughness={0.1}
          metalness={0.1}
          transparent
          opacity={0.25}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* Volumetric Liquid Core (Participating Equity) */}
      <mesh position={[0, liquidCenterY, 0]}>
        <cylinderGeometry args={[0.66, 0.66, liquidHeight, 32]} />
        <meshStandardMaterial
          color={quorumReached ? '#10b981' : '#06b6d4'}
          emissive={quorumReached ? '#059669' : '#0891b2'}
          emissiveIntensity={0.5}
          roughness={0.3}
          metalness={0.4}
          transparent
          opacity={0.8}
        />
      </mesh>

      {/* Laser Quorum Threshold Ring at 60.00% */}
      <group position={[0, thresholdY, 0]}>
        {/* Glowing ring */}
        <mesh rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[0.67, 0.76, 32]} />
          <meshBasicMaterial color={DECISION_LAYOUT.colors.goldThreshold} side={THREE.DoubleSide} />
        </mesh>

        {/* Laser indicator tag */}
        <group position={[1.1, 0, 0]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[1.8, 0.28]} />
            <meshBasicMaterial color="#020617" opacity={0.85} transparent side={THREE.DoubleSide} />
          </mesh>
          <lineSegments position={[0, 0, 0.005]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(1.8, 0.28)]} />
            <lineBasicMaterial color={DECISION_LAYOUT.colors.goldThreshold} />
          </lineSegments>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.095}
            color={DECISION_LAYOUT.colors.goldThreshold}
            anchorX="center"
            anchorY="middle"
          >
            {`HẠN MỨC: ${formatPercentageVN(quorumThreshold)}`}
          </Text>
        </group>
      </group>

      {/* Quorum Readout Panel on front */}
      <group position={[0, 2.2, 0.85]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.0, 0.9]} />
          <meshBasicMaterial color="#030712" opacity={0.9} transparent side={THREE.DoubleSide} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.0, 0.9)]} />
          <lineBasicMaterial color={quorumReached ? '#10b981' : '#f59e0b'} />
        </lineSegments>

        {/* Rate Text */}
        <Text
          position={[0, 0.28, 0.02]}
          fontSize={0.15}
          color={quorumReached ? '#34d399' : '#fbbf24'}
          anchorX="center"
          anchorY="middle"
        >
          {`${formatPercentageVN(participationRate)} THAM GIA`}
        </Text>

        {/* Quorum status badge */}
        <Text
          position={[0, 0.04, 0.02]}
          fontSize={0.11}
          color={quorumReached ? '#10b981' : '#f59e0b'}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.04}
        >
          {quorumReached ? '✓ ĐÃ ĐẠT ĐỦ ĐIỀU KIỆN' : '⚠ CHƯA ĐỦ ĐIỀU KIỆN (<60%)'}
        </Text>

        {/* Breakdown of participating votes */}
        <Text
          position={[0, -0.24, 0.02]}
          fontSize={0.095}
          color={DECISION_LAYOUT.colors.textMuted}
          anchorX="center"
          anchorY="middle"
        >
          {tally
            ? `Tán thành: ${formatPercentageVN(tally.approveEquity)} • Bác bỏ: ${formatPercentageVN(tally.rejectEquity)}`
            : `Tán thành: ${formatPercentageVN(45.0)} • Bác bỏ: ${formatPercentageVN(0.0)}`}
        </Text>
      </group>
    </group>
  );
};
