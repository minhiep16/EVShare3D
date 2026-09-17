import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';
import type { ProposalDTO } from '../api/proposalsApi';

interface ProposalHoloPod3DProps {
  position?: [number, number, number];
}

export const ProposalHoloPod3D: React.FC<ProposalHoloPod3DProps> = ({
  position = DECISION_LAYOUT.proposalPodClusterPosition,
}) => {
  const proposals = useDecisionStore((s) => s.proposals);
  const selectedProposalId = useDecisionStore((s) => s.selectedProposalId);
  const selectProposal = useDecisionStore((s) => s.selectProposal);

  return (
    <group name="ProposalPodCluster" position={position}>
      {/* Cluster Header */}
      <group position={[0, 3.8, 0]}>
        <Text
          fontSize={0.28}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          ACTIVE GOVERNANCE PROPOSALS
        </Text>
        <Text
          position={[0, -0.32, 0]}
          fontSize={0.16}
          color={DECISION_LAYOUT.colors.indigoLight}
          anchorX="center"
          anchorY="middle"
        >
          SELECT PROPOSAL POD TO DELIBERATE & CAST BALLOT
        </Text>
      </group>

      {/* Semicircular Pod Array */}
      {proposals.map((proposal, idx) => {
        // Distribute along an arc of 120 degrees (-60 to +60)
        const total = Math.max(proposals.length, 1);
        const arcSpread = Math.PI * 0.55;
        const angle = total === 1 ? 0 : -arcSpread / 2 + (idx / (total - 1)) * arcSpread;
        const radius = DECISION_LAYOUT.podArcRadius;
        const px = Math.sin(angle) * radius;
        const pz = -Math.cos(angle) * radius + 1.8;

        const isSelected = selectedProposalId === proposal.id;

        return (
          <SingleProposalPod
            key={proposal.id}
            proposal={proposal}
            position={[px, 0, pz]}
            isSelected={isSelected}
            onSelect={() => selectProposal(proposal.id)}
          />
        );
      })}
    </group>
  );
};

interface SingleProposalPodProps {
  proposal: ProposalDTO;
  position: [number, number, number];
  isSelected: boolean;
  onSelect: () => void;
}

const SingleProposalPod: React.FC<SingleProposalPodProps> = ({
  proposal,
  position,
  isSelected,
  onSelect,
}) => {
  const crystalRef = useRef<THREE.Mesh>(null);
  const [hovered, setHovered] = useState(false);

  // Determine geometry & colors based on type
  const getPodTheme = (type: string) => {
    switch (type) {
      case 'ROUTINE_EXPENSE':
        return {
          primary: '#10b981',
          accent: '#34d399',
          label: 'ROUTINE EXPENSE',
        };
      case 'ASSET_UPGRADE':
        return {
          primary: '#38bdf8',
          accent: '#7dd3fc',
          label: 'ASSET UPGRADE',
        };
      case 'AMENDMENT':
        return {
          primary: '#a855f7',
          accent: '#c084fc',
          label: 'CHARTER AMENDMENT',
        };
      default:
        return {
          primary: '#f59e0b',
          accent: '#fbbf24',
          label: 'GENERAL GOVERNANCE',
        };
    }
  };

  const theme = getPodTheme(proposal.proposalType);

  useFrame((_, delta) => {
    if (crystalRef.current) {
      // Gentle floating and rotation
      crystalRef.current.rotation.y += delta * (isSelected ? 1.2 : 0.5);
      crystalRef.current.rotation.x = Math.sin(Date.now() * 0.002) * 0.12;
      const hoverElev = isSelected ? 0.35 : hovered ? 0.2 : 0;
      crystalRef.current.position.y = 1.45 + hoverElev + Math.sin(Date.now() * 0.003) * 0.08;
    }
  });

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        onSelect();
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
      {/* Base Pedestal */}
      <mesh position={[0, 0.3, 0]}>
        <cylinderGeometry args={[0.7, 0.85, 0.6, 24]} />
        <meshStandardMaterial
          color="#0b1120"
          roughness={0.4}
          metalness={0.8}
        />
      </mesh>

      {/* Halo Ring on pedestal */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.61, 0]}>
        <ringGeometry args={[0.55, 0.68, 32]} />
        <meshBasicMaterial
          color={isSelected ? '#fbbf24' : theme.primary}
          opacity={isSelected ? 0.95 : 0.6}
          transparent
        />
      </mesh>

      {/* Floating 3D Polyhedron Crystal */}
      <mesh
        ref={crystalRef}
        castShadow
        position={[0, 1.45, 0]}
        scale={isSelected ? 1.25 : hovered ? 1.1 : 0.95}
      >
        {proposal.proposalType === 'ROUTINE_EXPENSE' ? (
          <octahedronGeometry args={[0.42, 0]} />
        ) : proposal.proposalType === 'ASSET_UPGRADE' ? (
          <dodecahedronGeometry args={[0.38, 0]} />
        ) : (
          <icosahedronGeometry args={[0.4, 0]} />
        )}
        <meshStandardMaterial
          color={isSelected ? '#fef08a' : theme.primary}
          emissive={isSelected ? '#f59e0b' : theme.primary}
          emissiveIntensity={isSelected ? 1.4 : hovered ? 0.9 : 0.4}
          roughness={0.2}
          metalness={0.8}
          wireframe={false}
        />
      </mesh>

      {/* Wireframe outer shell for holographic look */}
      <mesh
        position={[0, 1.45, 0]}
        scale={isSelected ? 1.45 : hovered ? 1.25 : 1.1}
      >
        <octahedronGeometry args={[0.44, 0]} />
        <meshBasicMaterial
          color={isSelected ? '#fbbf24' : theme.accent}
          wireframe
          transparent
          opacity={isSelected ? 0.7 : 0.3}
        />
      </mesh>

      {/* 3D Holo Title Card */}
      <group position={[0, 2.5, 0]}>
        {/* Background plaque */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[2.4, 0.95]} />
          <meshBasicMaterial
            color="#020617"
            opacity={isSelected ? 0.9 : 0.75}
            transparent
            side={THREE.DoubleSide}
          />
        </mesh>
        {/* Border outline */}
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.4, 0.95)]} />
          <lineBasicMaterial
            color={isSelected ? '#fbbf24' : hovered ? '#38bdf8' : '#334155'}
            linewidth={1}
          />
        </lineSegments>

        {/* Category Pill */}
        <Text
          position={[0, 0.32, 0.02]}
          fontSize={0.11}
          color={isSelected ? '#f59e0b' : theme.accent}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          {theme.label}
        </Text>

        {/* Truncated Title */}
        <Text
          position={[0, 0.06, 0.02]}
          fontSize={0.13}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          maxWidth={2.2}
          textAlign="center"
        >
          {proposal.title.length > 36 ? `${proposal.title.substring(0, 34)}...` : proposal.title}
        </Text>

        {/* Status / Proposer line */}
        <Text
          position={[0, -0.28, 0.02]}
          fontSize={0.1}
          color={DECISION_LAYOUT.colors.textMuted}
          anchorX="center"
          anchorY="middle"
        >
          {`ID: #${proposal.id} • ${proposal.status} • By: ${proposal.proposerFullName || 'Syndicate'}`}
        </Text>
      </group>
    </group>
  );
};
