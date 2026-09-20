import React, { useEffect, useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';
import { DecisionChamberFloor3D } from './DecisionChamberFloor3D';
import { ProposalHoloPod3D } from './ProposalHoloPod3D';
import { VotingTerminal3D } from './VotingTerminal3D';
import { EquityAllocationPillars3D } from './EquityAllocationPillars3D';
import { QuorumLiquidColumn3D } from './QuorumLiquidColumn3D';
import { DecisionResultStela3D } from './DecisionResultStela3D';
import { useCameraStore } from '@/stores/useCameraStore';

interface DecisionChamber3DProps {
  position?: [number, number, number];
}

export const DecisionChamber3D: React.FC<DecisionChamber3DProps> = ({
  position = [0, 0, 0],
}) => {
  const fetchProposals = useDecisionStore((s) => s.fetchProposals);
  const activeStation = useDecisionStore((s) => s.activeStation);
  const setActiveStation = useDecisionStore((s) => s.setActiveStation);

  useEffect(() => {
    fetchProposals();
  }, [fetchProposals]);

  const cameraButtons = [
    { key: 'ARENA_OVERVIEW', label: 'TỔNG QUAN', preset: DECISION_LAYOUT.cameraPresets.ARENA_OVERVIEW },
    { key: 'TERMINAL_FOCUS', label: 'BÀN BIỂU QUYẾT', preset: DECISION_LAYOUT.cameraPresets.TERMINAL_FOCUS },
    { key: 'PROPOSAL_PODS_FOCUS', label: 'ĐỀ XUẤT', preset: DECISION_LAYOUT.cameraPresets.PROPOSAL_PODS_FOCUS },
    { key: 'EQUITY_FOCUS', label: 'CỘT BIỂU QUYẾT', preset: DECISION_LAYOUT.cameraPresets.EQUITY_FOCUS },
    { key: 'QUORUM_RESULTS_FOCUS', label: 'CỘT ĐẠT CHUẨN', preset: DECISION_LAYOUT.cameraPresets.QUORUM_RESULTS_FOCUS },
    { key: 'STELA_FOCUS', label: 'BIA KẾT QUẢ', preset: DECISION_LAYOUT.cameraPresets.STELA_FOCUS },
  ];

  const handleCameraChange = (preset: typeof DECISION_LAYOUT.cameraPresets.ARENA_OVERVIEW) => {
    // Add sector offset to world coordinates
    const targetPos: [number, number, number] = [
      preset.position[0] + position[0],
      preset.position[1] + position[1],
      preset.position[2] + position[2],
    ];
    const targetLook: [number, number, number] = [
      preset.target[0] + position[0],
      preset.target[1] + position[1],
      preset.target[2] + position[2],
    ];

    useCameraStore.getState().transitionTo(targetPos, targetLook, 1.2);
  };

  return (
    <group name="DecisionChamberSector" position={position}>
      {/* Parliament Indigo Environment Lighting */}
      <ambientLight color="#1e1b4b" intensity={0.9} />
      <directionalLight
        position={[8, 14, 10]}
        intensity={1.3}
        color="#a5b4fc"
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
      />
      <pointLight position={[0, 4, 1.2]} intensity={1.2} color="#06b6d4" distance={10} />
      <pointLight position={[0, 4, -6.5]} intensity={1.4} color="#8b5cf6" distance={12} />

      {/* 1. Parliamentary Arena Floor & Peripheral Pylons */}
      <DecisionChamberFloor3D />

      {/* 2. Floating 3D Proposal Polyhedron Pods */}
      <ProposalHoloPod3D />

      {/* 3. Central Angled Voting Terminal (-25°) */}
      <VotingTerminal3D />

      {/* 4. Syndicate Member Equity Pillars (35%, 25%, 20%, 20%) */}
      <EquityAllocationPillars3D />

      {/* 5. Volumetric Liquid Quorum Column (60% Laser Line) */}
      <QuorumLiquidColumn3D />

      {/* 6. Authoritative Governance Verdict Stela */}
      <DecisionResultStela3D />

      {/* 7. Floating 3D Pure Navigation Bar (Station Quick-Teleport) */}
      <group position={[0, 0.4, 4.8]} rotation={[-Math.PI * 0.15, 0, 0]}>
        {/* Navigation Bar Base */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[6.8, 0.65]} />
          <meshBasicMaterial color="#030712" opacity={0.88} transparent side={THREE.DoubleSide} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(6.8, 0.65)]} />
          <lineBasicMaterial color={DECISION_LAYOUT.colors.indigoLight} />
        </lineSegments>

        {cameraButtons.map((btn, idx) => {
          const spacing = 1.1;
          const xPos = (idx - 2.5) * spacing;
          return (
            <StationNavButton3D
              key={btn.key}
              position={[xPos, 0, 0.02]}
              label={btn.label}
              onClick={() => {
                setActiveStation(btn.key as any);
                handleCameraChange(btn.preset);
              }}
            />
          );
        })}
      </group>
    </group>
  );
};

interface StationNavButton3DProps {
  position: [number, number, number];
  label: string;
  onClick: () => void;
}

const StationNavButton3D: React.FC<StationNavButton3DProps> = ({
  position,
  label,
  onClick,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        setHovered(true);
        document.body.style.cursor = 'pointer';
      }}
      onPointerOut={() => {
        setHovered(false);
        document.body.style.cursor = 'auto';
      }}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[0.98, 0.38]} />
        <meshStandardMaterial
          color={hovered ? '#312e81' : '#0f172a'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(0.98, 0.38)]} />
        <lineBasicMaterial color={hovered ? '#818cf8' : '#4f46e5'} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.08}
        color={hovered ? '#ffffff' : '#c7d2fe'}
        anchorX="center"
        anchorY="middle"
        letterSpacing={0.04}
      >
        {label}
      </Text>
    </group>
  );
};
