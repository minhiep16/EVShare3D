import React from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';

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
          AUTHORITATIVE GOVERNANCE VERDICT
        </Text>
        <Text
          position={[0, -0.22, 0]}
          fontSize={0.105}
          color={DECISION_LAYOUT.colors.indigoLight}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.04}
        >
          BACKEND DECISION ENGINE • SPRING BOOT VOTING SERVICE
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
          SUBJECT PROPOSAL
        </Text>
        <Text
          position={[0, -0.2, 0]}
          fontSize={0.14}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          maxWidth={2.9}
          textAlign="center"
        >
          {activeProposal ? activeProposal.title : 'No Proposal Selected'}
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
          fontSize={0.22}
          color={statusColor}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          {finalDecision}
        </Text>

        <Text
          position={[0, -0.16, 0.02]}
          fontSize={0.105}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
        >
          {isPassed
            ? '✓ ALL GOVERNANCE CONDITIONS SATISFIED'
            : quorumReached
            ? 'DELIBERATION IN PROGRESS / ACTIVE'
            : '⚠ QUORUM REQUIREMENT PENDING (<60.00%)'}
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
          DETERMINISTIC EQUITY PARTICIPATION
        </Text>

        <Text
          position={[0, 0.12, 0]}
          fontSize={0.11}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
        >
          {results
            ? `Participating: ${results.participatingEquity.toFixed(1)}% • Required Quorum: 60.00%`
            : 'Participating: 45.0% • Quorum: 60.00%'}
        </Text>

        <Text
          position={[0, -0.12, 0]}
          fontSize={0.11}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
        >
          {results
            ? `Approve: ${results.approveWeight.toFixed(1)}% | Reject: ${results.rejectWeight.toFixed(1)}% | Abstain: ${results.abstainWeight.toFixed(1)}%`
            : 'Approve: 45.0% | Reject: 0.0% | Abstain: 0.0%'}
        </Text>

        <Text
          position={[0, -0.32, 0]}
          fontSize={0.10}
          color={DECISION_LAYOUT.colors.goldThreshold}
          anchorX="center"
          anchorY="middle"
        >
          {`THRESHOLD RULE: ${results?.thresholdDescription || '> 50.00% of participating equity'}`}
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
          {results?.decisionReason || 'Awaiting co-owner ballots to evaluate passing criteria.'}
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
          SECURED BY SYNDICATE CONSENSUS PROTOCOL
        </Text>
      </group>
    </group>
  );
};
