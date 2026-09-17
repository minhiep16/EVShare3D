import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';
import type { AISafetyActionType } from './aiTypes';

export const AITerminal3D: React.FC = () => {
  const isAnalyzing = useAIStore((s) => s.isAnalyzing);
  const activePrompt = useAIStore((s) => s.activePrompt);
  const queryResult = useAIStore((s) => s.queryResult);
  const runInteractiveQuery = useAIStore((s) => s.runInteractiveQuery);
  const triggerSafetyAttempt = useAIStore((s) => s.triggerSafetyAttempt);
  const lastBlockedAttempt = useAIStore((s) => s.lastBlockedAttempt);
  const clearBlockedAttempt = useAIStore((s) => s.clearBlockedAttempt);

  const queryPrompts = [
    { label: 'ANALYZE FAIRNESS', prompt: 'ANALYZE SYNDICATE FAIRNESS' },
    { label: 'BATTERY TELEMETRY', prompt: 'PREDICT BATTERY HEALTH' },
    { label: 'TARIFF OPTIMIZATION', prompt: 'OPTIMIZE CHARGING TARIFFS' },
    { label: 'ANOMALY SCAN', prompt: 'SCAN TELEMETRY ANOMALIES' },
  ];

  const disclosureNotice = useAIStore((s) => s.disclosureNotice);
  const modelStatus = useAIStore((s) => s.modelStatus);

  const safetyActionsRow1: { label: string; action: AISafetyActionType }[] = [
    { label: 'TEST AUTH BYPASS', action: 'BYPASS_AUTHENTICATION' },
    { label: 'TEST RBAC BYPASS', action: 'BYPASS_RBAC' },
    { label: 'TEST ALTER EQUITY', action: 'ALTER_OWNERSHIP' },
  ];

  const safetyActionsRow2: { label: string; action: AISafetyActionType }[] = [
    { label: 'TEST AUTO-PAYMENT', action: 'AUTHORIZE_PAYMENT' },
    { label: 'TEST AUTO-SIGN', action: 'APPROVE_CONTRACT' },
    { label: 'TEST VOTE OVERRIDE', action: 'OVERRIDE_VOTING_RULES' },
  ];

  return (
    <group name="AITerminal" position={AI_LAYOUT.interactionTerminalPosition}>
      {/* 1. Terminal Console Base Plinth */}
      <mesh castShadow receiveShadow position={[0, 0.45, 0]}>
        <cylinderGeometry args={[1.6, 1.9, 0.9, 32]} />
        <meshStandardMaterial color="#0b0d1e" roughness={0.4} metalness={0.7} />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[1.85, 1.95, 32]} />
        <meshBasicMaterial color={AI_LAYOUT.colors.neuralViolet} opacity={0.6} transparent />
      </mesh>

      {/* 2. Angled Console Faceplate (-22° toward user) */}
      <group position={[0, 1.0, 0]} rotation={[-Math.PI * 0.12, 0, 0]}>
        {/* Main console slab */}
        <mesh receiveShadow position={[0, 0, 0]}>
          <boxGeometry args={[4.0, 2.5, 0.1]} />
          <meshStandardMaterial color="#060814" roughness={0.3} metalness={0.8} />
        </mesh>
        <lineSegments position={[0, 0, 0.055]}>
          <edgesGeometry args={[new THREE.BoxGeometry(4.0, 2.5, 0.1)]} />
          <lineBasicMaterial color={AI_LAYOUT.colors.neuralViolet} />
        </lineSegments>

        {/* Panel Header */}
        <group position={[0, 1.02, 0.06]}>
          <Text
            fontSize={0.14}
            color={AI_LAYOUT.colors.neuralViolet}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.08}
          >
            AI INTERACTION &amp; SAFETY BOUNDARY CONSOLE
          </Text>
          <Text
            position={[0, -0.16, 0]}
            fontSize={0.09}
            color={modelStatus === 'NOT_AVAILABLE' ? '#f59e0b' : AI_LAYOUT.colors.goldWarning}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.03}
            maxWidth={3.8}
            textAlign="center"
          >
            {disclosureNotice}
          </Text>
        </group>

        {/* Query Prompt Chips */}
        <group position={[0, 0.58, 0.06]}>
          <Text
            position={[0, 0.14, 0]}
            fontSize={0.085}
            color={AI_LAYOUT.colors.textMuted}
            anchorX="center"
            anchorY="middle"
          >
            SELECT ADVISORY QUERY SYNTHESIS:
          </Text>
          <group position={[0, -0.12, 0]}>
            {queryPrompts.map((p, idx) => {
              const spacing = 0.92;
              const xPos = (idx - 1.5) * spacing;
              const isSelected = activePrompt === p.prompt;

              return (
                <PromptChipButton3D
                  key={p.label}
                  position={[xPos, 0, 0]}
                  label={p.label}
                  isSelected={isSelected}
                  disabled={isAnalyzing}
                  onClick={() => runInteractiveQuery(p.prompt)}
                />
              );
            })}
          </group>
        </group>

        {/* Query Result / Telemetry Readout Feed */}
        <group position={[0, 0.08, 0.06]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[3.7, 0.48]} />
            <meshBasicMaterial color="#02030a" opacity={0.92} transparent side={THREE.DoubleSide} />
          </mesh>
          <lineSegments position={[0, 0, 0.005]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(3.7, 0.48)]} />
            <lineBasicMaterial color="#1e293b" />
          </lineSegments>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.095}
            color={isAnalyzing ? AI_LAYOUT.colors.goldWarning : AI_LAYOUT.colors.textWhite}
            anchorX="center"
            anchorY="middle"
            maxWidth={3.5}
            textAlign="center"
            lineHeight={1.3}
          >
            {isAnalyzing
              ? 'SYNTHESIZING MOBILITY DATA NODES...'
              : queryResult || 'SELECT A QUERY CHIP ABOVE TO RUN ADVISORY SYNTHESIS'}
          </Text>
        </group>

        {/* Safety Boundary Test Actions Header */}
        <group position={[0, -0.42, 0.06]}>
          <Text
            position={[0, 0.1, 0]}
            fontSize={0.085}
            color={AI_LAYOUT.colors.anomalyRed}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.04}
          >
            BR-AI-SAFE-01 BOUNDARY TEST (PROHIBITED AUTONOMOUS ACTIONS):
          </Text>

          {/* Row 1: Auth, RBAC, Ownership */}
          <group position={[0, -0.14, 0]}>
            {safetyActionsRow1.map((act, idx) => {
              const spacing = 1.22;
              const xPos = (idx - 1.0) * spacing;

              return (
                <SafetyTestButton3D
                  key={act.action}
                  position={[xPos, 0, 0]}
                  label={act.label}
                  onClick={() => triggerSafetyAttempt(act.action)}
                />
              );
            })}
          </group>

          {/* Row 2: Payment, Contract, Voting */}
          <group position={[0, -0.46, 0]}>
            {safetyActionsRow2.map((act, idx) => {
              const spacing = 1.22;
              const xPos = (idx - 1.0) * spacing;

              return (
                <SafetyTestButton3D
                  key={act.action}
                  position={[xPos, 0, 0]}
                  label={act.label}
                  onClick={() => triggerSafetyAttempt(act.action)}
                />
              );
            })}
          </group>
        </group>

        {/* Active Blocked Safety Modal Popup in 3D */}
        {lastBlockedAttempt && (
          <group position={[0, 0, 0.22]}>
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[3.6, 1.8]} />
              <meshBasicMaterial color="#050103" opacity={0.98} transparent side={THREE.DoubleSide} />
            </mesh>
            <lineSegments position={[0, 0, 0.005]}>
              <edgesGeometry args={[new THREE.PlaneGeometry(3.6, 1.8)]} />
              <lineBasicMaterial color={AI_LAYOUT.colors.safetyCrimson} linewidth={2} />
            </lineSegments>

            <Text
              position={[0, 0.65, 0.02]}
              fontSize={0.13}
              color={AI_LAYOUT.colors.safetyCrimson}
              anchorX="center"
              anchorY="middle"
              letterSpacing={0.06}
            >
              ⚠ HARD SAFETY INTERLOCK ACTIVATED (BR-AI-SAFE-01)
            </Text>

            <Text
              position={[0, 0.38, 0.02]}
              fontSize={0.105}
              color={AI_LAYOUT.colors.textWhite}
              anchorX="center"
              anchorY="middle"
            >
              {`ATTEMPTED: ${lastBlockedAttempt.actionLabel}`}
            </Text>

            <Text
              position={[0, 0.05, 0.02]}
              fontSize={0.09}
              color="#fca5a5"
              anchorX="center"
              anchorY="middle"
              maxWidth={3.2}
              textAlign="center"
              lineHeight={1.3}
            >
              {lastBlockedAttempt.blockedReason}
            </Text>

            <Text
              position={[0, -0.32, 0.02]}
              fontSize={0.08}
              color={AI_LAYOUT.colors.goldWarning}
              anchorX="center"
              anchorY="middle"
            >
              {`REQUIRED AUTHORITY: ${lastBlockedAttempt.requiredAuthority}`}
            </Text>

            {/* Dismiss Button in Pure 3D */}
            <group
              position={[0, -0.62, 0.04]}
              onClick={(e) => {
                e.stopPropagation();
                clearBlockedAttempt();
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[1.8, 0.25]} />
                <meshStandardMaterial color="#2d0a0e" roughness={0.4} metalness={0.6} />
              </mesh>
              <lineSegments position={[0, 0, 0.005]}>
                <edgesGeometry args={[new THREE.PlaneGeometry(1.8, 0.25)]} />
                <lineBasicMaterial color={AI_LAYOUT.colors.safetyCrimson} />
              </lineSegments>
              <Text
                position={[0, 0, 0.02]}
                fontSize={0.085}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
              >
                [ ACKNOWLEDGE SAFETY BOUNDARY ]
              </Text>
            </group>
          </group>
        )}
      </group>
    </group>
  );
};

interface PromptChipButton3DProps {
  position: [number, number, number];
  label: string;
  isSelected: boolean;
  disabled: boolean;
  onClick: () => void;
}

const PromptChipButton3D: React.FC<PromptChipButton3DProps> = ({
  position,
  label,
  isSelected,
  disabled,
  onClick,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        if (!disabled) onClick();
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        if (!disabled) setHovered(true);
      }}
      onPointerOut={() => setHovered(false)}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[0.82, 0.25]} />
        <meshStandardMaterial
          color={isSelected ? '#4c1d95' : hovered ? '#2e1065' : '#0f172a'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(0.82, 0.25)]} />
        <lineBasicMaterial color={isSelected ? '#c084fc' : hovered ? '#a855f7' : '#475569'} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.068}
        color={isSelected ? '#ffffff' : hovered ? '#ffffff' : '#94a3b8'}
        anchorX="center"
        anchorY="middle"
      >
        {label}
      </Text>
    </group>
  );
};

interface SafetyTestButton3DProps {
  position: [number, number, number];
  label: string;
  onClick: () => void;
}

const SafetyTestButton3D: React.FC<SafetyTestButton3DProps> = ({
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
      }}
      onPointerOut={() => setHovered(false)}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[1.1, 0.22]} />
        <meshStandardMaterial
          color={hovered ? '#450a0a' : '#1e0507'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(1.1, 0.22)]} />
        <lineBasicMaterial color={hovered ? '#ef4444' : '#991b1b'} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.065}
        color={hovered ? '#fecaca' : '#f87171'}
        anchorX="center"
        anchorY="middle"
      >
        {label}
      </Text>
    </group>
  );
};
