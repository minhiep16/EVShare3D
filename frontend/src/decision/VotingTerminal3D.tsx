import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';
import { useAuthStore } from '@/auth/useAuthStore';
import type { VoteOptionKey } from '../api/proposalsApi';
import { formatPercentageVN, formatStatusVN } from '@/i18n';

interface VotingTerminal3DProps {
  position?: [number, number, number];
}

export const VotingTerminal3D: React.FC<VotingTerminal3DProps> = ({
  position = DECISION_LAYOUT.votingTerminalPosition,
}) => {
  const activeProposal = useDecisionStore((s) => s.activeProposal);
  const myVote = useDecisionStore((s) => s.myVote);
  const isSubmittingVote = useDecisionStore((s) => s.isSubmittingVote);
  const voteError = useDecisionStore((s) => s.voteError);
  const castVote = useDecisionStore((s) => s.castVote);
  const currentUser = useAuthStore((s) => s.currentUser);

  const voterName = myVote?.voterName || currentUser?.fullName || 'Minh Hiep';
  const voterWeight = myVote?.equityWeight ?? 35.0;
  const isVotingClosed = activeProposal ? activeProposal.status !== 'ACTIVE' : false;

  // Voting options
  const choices: { key: VoteOptionKey; label: string; sub: string; color: string; hoverColor: string }[] = [
    {
      key: 'APPROVE',
      label: 'TÁN THÀNH',
      sub: `+${formatPercentageVN(voterWeight)} ĐỒNG Ý`,
      color: '#10b981', // Emerald
      hoverColor: '#34d399',
    },
    {
      key: 'REJECT',
      label: 'KHÔNG TÁN THÀNH',
      sub: `-${formatPercentageVN(voterWeight)} BÁC BỎ`,
      color: '#ef4444', // Ruby
      hoverColor: '#f87171',
    },
    {
      key: 'ABSTAIN',
      label: 'KHÔNG BIỂU QUYẾT',
      sub: 'TRUNG LẬP',
      color: '#f59e0b', // Amber
      hoverColor: '#fbbf24',
    },
  ];

  return (
    <group name="VotingTerminal" position={position}>
      {/* 1. Terminal Console Plinth */}
      <mesh castShadow receiveShadow position={[0, 0.5, 0]}>
        <cylinderGeometry args={[1.5, 1.8, 1.0, 32]} />
        <meshStandardMaterial
          color="#0f172a"
          roughness={0.4}
          metalness={0.7}
        />
      </mesh>

      {/* Decorative Cyan Floor Ring at Base */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[1.75, 1.85, 32]} />
        <meshBasicMaterial color={DECISION_LAYOUT.colors.cyanQuorum} opacity={0.7} transparent />
      </mesh>

      {/* 2. Angled Console Faceplate (-25 degrees toward user) */}
      <group position={[0, 1.05, 0]} rotation={[-Math.PI * 0.14, 0, 0]}>
        {/* Main interactive panel plane */}
        <mesh receiveShadow position={[0, 0, 0]}>
          <boxGeometry args={[3.2, 1.9, 0.1]} />
          <meshStandardMaterial
            color="#080c16"
            roughness={0.3}
            metalness={0.8}
          />
        </mesh>

        {/* Outer glowing border */}
        <lineSegments position={[0, 0, 0.055]}>
          <edgesGeometry args={[new THREE.BoxGeometry(3.2, 1.9, 0.1)]} />
          <lineBasicMaterial color={DECISION_LAYOUT.colors.indigoLight} linewidth={1} />
        </lineSegments>

        {/* Panel Header */}
        <group position={[0, 0.72, 0.06]}>
          <Text
            fontSize={0.13}
            color={DECISION_LAYOUT.colors.cyanQuorum}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.08}
          >
            BÀN BIỂU QUYẾT NGHỊ VIỆN • GÓC NGHIÊNG: -25°
          </Text>
          <Text
            position={[0, -0.16, 0]}
            fontSize={0.14}
            color={DECISION_LAYOUT.colors.textWhite}
            anchorX="center"
            anchorY="middle"
            maxWidth={2.9}
            textAlign="center"
          >
            {activeProposal
              ? activeProposal.title.length > 44
                ? `${activeProposal.title.substring(0, 42)}...`
                : activeProposal.title
              : 'CHƯA CHỌN ĐỀ XUẤT'}
          </Text>
        </group>

        {/* Voter Equity Weight Readout */}
        <group position={[0, 0.36, 0.06]}>
          <Text
            fontSize={0.105}
            color={DECISION_LAYOUT.colors.textMuted}
            anchorX="center"
            anchorY="middle"
          >
            {`NGƯỜI BẦU: ${voterName.toUpperCase()} • TRỌNG SỐ BIỂU QUYẾT: ${formatPercentageVN(voterWeight)}`}
          </Text>
        </group>

        {/* Three Vote Choice Buttons */}
        <group position={[0, -0.12, 0.08]}>
          {choices.map((choice, i) => {
            const spacing = 0.96;
            const xPos = (i - 1) * spacing;
            const isMyChoice = myVote?.optionKey === choice.key;
            const hasVoted = !!myVote;

            return (
              <VoteButton3D
                key={choice.key}
                position={[xPos, 0, 0]}
                choice={choice}
                isSelected={isMyChoice}
                disabled={hasVoted || isSubmittingVote || isVotingClosed}
                onVote={() => castVote(choice.key)}
              />
            );
          })}
        </group>

        {/* Status / Feedback Footer Bar */}
        <group position={[0, -0.68, 0.06]}>
          {voteError ? (
            <Text
              fontSize={0.12}
              color="#f87171"
              anchorX="center"
              anchorY="middle"
            >
              {`LỖI: ${voteError}`}
            </Text>
          ) : isSubmittingVote ? (
            <Text
              fontSize={0.12}
              color={DECISION_LAYOUT.colors.goldThreshold}
              anchorX="center"
              anchorY="middle"
            >
              ĐANG GỬI PHIẾU BẦU VỀ HỆ THỐNG QUẢN TRỊ...
            </Text>
          ) : myVote ? (
            <Text
              fontSize={0.11}
              color="#34d399"
              anchorX="center"
              anchorY="middle"
            >
              {`ĐÃ GHI NHẬN PHIẾU BẦU: ${myVote.optionKey === 'APPROVE' ? 'TÁN THÀNH' : myVote.optionKey === 'REJECT' ? 'KHÔNG TÁN THÀNH' : 'KHÔNG BIỂU QUYẾT'} (${formatPercentageVN(voterWeight)}) • ĐÃ LƯU SỔ`}
            </Text>
          ) : isVotingClosed ? (
            <Text
              fontSize={0.11}
              color={DECISION_LAYOUT.colors.goldThreshold}
              anchorX="center"
              anchorY="middle"
            >
              {`TRẠNG THÁI: ${formatStatusVN(activeProposal?.status || '')} • PHIÊN THẢO LUẬN ĐÃ ĐÓNG`}
            </Text>
          ) : (
            <Text
              fontSize={0.105}
              color={DECISION_LAYOUT.colors.textMuted}
              anchorX="center"
              anchorY="middle"
            >
              {`CHỌN NÚT ĐỂ BỎ PHIẾU VỚI ${formatPercentageVN(voterWeight)} TRỌNG SỐ ĐỒNG SỞ HỮU`}
            </Text>
          )}
        </group>
      </group>
    </group>
  );
};

interface VoteButton3DProps {
  position: [number, number, number];
  choice: { key: VoteOptionKey; label: string; sub: string; color: string; hoverColor: string };
  isSelected: boolean;
  disabled: boolean;
  onVote: () => void;
}

const VoteButton3D: React.FC<VoteButton3DProps> = ({
  position,
  choice,
  isSelected,
  disabled,
  onVote,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        if (!disabled) {
          onVote();
        }
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        if (!disabled) {
          setHovered(true);
          document.body.style.cursor = 'pointer';
        }
      }}
      onPointerOut={() => {
        setHovered(false);
        document.body.style.cursor = 'auto';
      }}
    >
      {/* Button Plinth Mesh */}
      <mesh
        castShadow
        position={[0, 0, isSelected ? 0.06 : hovered && !disabled ? 0.04 : 0.02]}
      >
        <boxGeometry args={[0.82, 0.58, 0.08]} />
        <meshStandardMaterial
          color={
            isSelected
              ? choice.color
              : hovered && !disabled
              ? '#1e293b'
              : '#0f172a'
          }
          emissive={isSelected ? choice.color : hovered && !disabled ? choice.hoverColor : '#000000'}
          emissiveIntensity={isSelected ? 0.8 : hovered && !disabled ? 0.4 : 0}
          roughness={0.3}
          metalness={0.7}
        />
      </mesh>

      {/* Button Glowing Edge Wireframe */}
      <lineSegments position={[0, 0, isSelected ? 0.1 : 0.06]}>
        <edgesGeometry args={[new THREE.BoxGeometry(0.82, 0.58, 0.08)]} />
        <lineBasicMaterial
          color={isSelected ? '#ffffff' : hovered && !disabled ? choice.hoverColor : choice.color}
          linewidth={isSelected ? 2 : 1}
        />
      </lineSegments>

      {/* Button Labels */}
      <Text
        position={[0, 0.08, isSelected ? 0.12 : 0.08]}
        fontSize={0.11}
        color={isSelected ? '#ffffff' : hovered && !disabled ? '#ffffff' : choice.color}
        anchorX="center"
        anchorY="middle"
        letterSpacing={0.06}
      >
        {choice.label}
      </Text>
      <Text
        position={[0, -0.1, isSelected ? 0.12 : 0.08]}
        fontSize={0.085}
        color={isSelected ? '#ffffff' : DECISION_LAYOUT.colors.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        {isSelected ? '✓ ĐÃ GHI NHẬN' : choice.sub}
      </Text>
    </group>
  );
};
