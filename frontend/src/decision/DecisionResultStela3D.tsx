import React from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';
import { formatPercentageVN, formatStatusVN } from '@/i18n';

interface DecisionResultStela3DProps {
  position?: [number, number, number];
}

export const DecisionResultStela3D: React.FC<DecisionResultStela3DProps> = ({
  position = DECISION_LAYOUT.resultStelaPosition,
}) => {
  const results = useDecisionStore((s) => s.results);
  const activeProposal = useDecisionStore((s) => s.activeProposal);

  const isPassed = results?.passed ?? false;
  const quorumReached = results?.quorumReached ?? false;
  const finalDecision = results?.finalDecision ?? (quorumReached ? 'ACTIVE' : 'QUORUM_NOT_MET');

  const statusColor = isPassed
    ? '#10b981' // Emerald
    : finalDecision === 'REJECTED'
    ? '#ef4444' // Ruby
    : '#f59e0b'; // Amber

  return (
    <group name="DecisionResultStela" position={position}>
      {/* Monolithic Slate Stela Body */}
      <mesh castShadow receiveShadow position={[0, 2.4, 0]}>
        <boxGeometry args={[3.2, 4.8, 0.25]} />
        <meshStandardMaterial
          color="#090d16"
          roughness={0.3}
          metalness={0.8}
        />
      </mesh>

      {/* Outer Halo Wireframe */}
      <lineSegments position={[0, 2.4, 0.13]}>
        <edgesGeometry args={[new THREE.BoxGeometry(3.2, 4.8, 0.25)]} />
        <lineBasicMaterial color={statusColor} linewidth={1} />
      </lineSegments>

      {/* Top Header Badge */}
      <group position={[0, 4.25, 0.15]}>
        <Text
          fontSize={0.16}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          PHÁN QUYẾT NGHỊ QUYẾT QUẢN TRỊ
        </Text>
        <Text
          position={[0, -0.22, 0]}
          fontSize={0.105}
          color={DECISION_LAYOUT.colors.indigoLight}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.04}
        >
          ĐỘNG CƠ BIỂU QUYẾT TẬP TRUNG • NỀN TẢNG EVSHARE
        </Text>
      </group>

      {/* Active Proposal Title */}
      <group position={[0, 3.45, 0.15]}>
        <Text
          fontSize={0.12}
          color={DECISION_LAYOUT.colors.textMuted}
          anchorX="center"
          anchorY="middle"
        >
          ĐỀ XUẤT ĐƯỢC XEM XÉT
        </Text>
        <Text
          position={[0, -0.2, 0]}
          fontSize={0.135}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          maxWidth={2.9}
          textAlign="center"
        >
          {activeProposal ? activeProposal.title : 'Chưa chọn đề xuất'}
        </Text>
      </group>

      {/* Decision Status Plaque */}
      <group position={[0, 2.5, 0.15]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.8, 0.72]} />
          <meshBasicMaterial color="#020617" opacity={0.9} transparent side={THREE.DoubleSide} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.8, 0.72)]} />
          <lineBasicMaterial color={statusColor} />
        </lineSegments>

        <Text
          position={[0, 0.12, 0.02]}
          fontSize={0.2}
          color={statusColor}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          {formatStatusVN(finalDecision)}
        </Text>

        <Text
          position={[0, -0.16, 0.02]}
          fontSize={0.105}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
        >
          {isPassed
            ? '✓ ĐÃ THỎA MÃN TOÀN BỘ ĐIỀU KIỆN BIỂU QUYẾT'
            : quorumReached
            ? 'ĐANG TIẾN HÀNH THẢO LUẬN / BỎ PHIẾU'
            : '⚠ CHƯA ĐẠT HẠN NGẠCH TỐI THIỂU (<60,00%)'}
        </Text>
      </group>

      {/* Mathematical Breakdown & Authority Justification */}
      <group position={[0, 1.4, 0.15]}>
        <Text
          position={[0, 0.35, 0]}
          fontSize={0.11}
          color={DECISION_LAYOUT.colors.cyanQuorum}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.04}
        >
          TỶ LỆ THAM GIA BIỂU QUYẾT
        </Text>

        <Text
          position={[0, 0.12, 0]}
          fontSize={0.11}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
        >
          {results
            ? `Tham gia: ${formatPercentageVN(results.participatingEquity)} • Hạn ngạch: 60,00%`
            : `Tham gia: ${formatPercentageVN(45.0)} • Hạn ngạch: 60,00%`}
        </Text>

        <Text
          position={[0, -0.12, 0]}
          fontSize={0.105}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
        >
          {results
            ? `Tán thành: ${formatPercentageVN(results.approveWeight)} | Bác bỏ: ${formatPercentageVN(results.rejectWeight)} | Không biểu quyết: ${formatPercentageVN(results.abstainWeight)}`
            : `Tán thành: ${formatPercentageVN(45.0)} | Bác bỏ: ${formatPercentageVN(0.0)} | Không biểu quyết: ${formatPercentageVN(0.0)}`}
        </Text>

        <Text
          position={[0, -0.32, 0]}
          fontSize={0.10}
          color={DECISION_LAYOUT.colors.goldThreshold}
          anchorX="center"
          anchorY="middle"
        >
          {`QUY TẮC NGƯỠNG: ${results?.thresholdDescription || '> 50,00% tổng cổ phần tham gia'}`}
        </Text>

        <Text
          position={[0, -0.52, 0]}
          fontSize={0.095}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          maxWidth={2.8}
          textAlign="center"
        >
          {results?.decisionReason || 'Đang chờ các thành viên bỏ phiếu để tổng hợp kết quả.'}
        </Text>
      </group>

      {/* Security Footer Seal */}
      <group position={[0, 0.45, 0.15]}>
        <Text
          fontSize={0.09}
          color={DECISION_LAYOUT.colors.textMuted}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          BẢO CHỨNG BỞI GIAO THỨC ĐỒNG THUẬN NHÓM SỞ HỮU
        </Text>
      </group>
    </group>
  );
};
