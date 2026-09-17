import React from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { AI_LAYOUT } from './aiLayout';

export const AIResultVisualization3D: React.FC = () => {
  const memberFairness = [
    { name: 'Minh Hiep', ratio: 1.05, equity: '35.0%', color: '#06b6d4', status: 'BALANCED' },
    { name: 'Tran Duc', ratio: 1.12, equity: '25.0%', color: '#f59e0b', status: 'PEAK HEAVY' },
    { name: 'Le Hoang', ratio: 0.78, equity: '20.0%', color: '#10b981', status: 'PRIORITY BONUS' },
    { name: 'Pham Mai', ratio: 0.96, equity: '20.0%', color: '#a855f7', status: 'BALANCED' },
  ];

  const cells = [
    { id: 'CELL #1', temp: '33.2°C', status: 'NOMINAL', color: '#10b981' },
    { id: 'CELL #2', temp: '33.8°C', status: 'NOMINAL', color: '#10b981' },
    { id: 'CELL #3', temp: '34.0°C', status: 'NOMINAL', color: '#10b981' },
    { id: 'CELL #4', temp: '41.8°C', status: 'THERMAL DRIFT', color: '#ef4444' },
  ];

  return (
    <group name="AIResultVisualization" position={AI_LAYOUT.resultVisualizationPosition}>
      {/* 1. Monolithic Visualization Stela */}
      <mesh castShadow receiveShadow position={[0, 2.5, 0]}>
        <boxGeometry args={[6.8, 5.0, 0.2]} />
        <meshStandardMaterial color="#060814" roughness={0.4} metalness={0.7} />
      </mesh>
      <lineSegments position={[0, 2.5, 0.105]}>
        <edgesGeometry args={[new THREE.BoxGeometry(6.8, 5.0, 0.2)]} />
        <lineBasicMaterial color={AI_LAYOUT.colors.neuralViolet} />
      </lineSegments>

      {/* Stela Header */}
      <group position={[0, 4.55, 0.12]}>
        <Text
          fontSize={0.2}
          color={AI_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          AI MOBILITY ANALYTICS &amp; ANOMALY MATRIX
        </Text>
        <Text
          position={[0, -0.24, 0]}
          fontSize={0.11}
          color={AI_LAYOUT.colors.cyanLight}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.04}
        >
          REAL-TIME TELEMETRY SYNTHESIS • ADVISORY ONLY
        </Text>
      </group>

      {/* 2. Left Section: Fairness Ratio Graph */}
      <group position={[-1.7, 2.5, 0.12]}>
        <Text
          position={[0, 1.4, 0]}
          fontSize={0.13}
          color={AI_LAYOUT.colors.neuralViolet}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.05}
        >
          MEMBER FAIR USAGE RATIO
        </Text>

        {/* Parity baseline line (1.00) */}
        <group position={[0, 0.25, 0]}>
          <lineSegments position={[0, 0, 0]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(2.8, 0.02)]} />
            <lineBasicMaterial color="#64748b" />
          </lineSegments>
          <Text
            position={[1.45, 0, 0]}
            fontSize={0.08}
            color="#94a3b8"
            anchorX="left"
            anchorY="middle"
          >
            1.0 PARITY
          </Text>
        </group>

        {/* 4 Member Bars */}
        {memberFairness.map((m, idx) => {
          const spacing = 0.65;
          const xPos = (idx - 1.5) * spacing;
          const barHeight = m.ratio * 1.5;
          const yCenter = -0.7 + barHeight / 2;

          return (
            <group key={m.name} position={[xPos, 0, 0]}>
              <mesh position={[0, yCenter, 0]}>
                <boxGeometry args={[0.38, barHeight, 0.05]} />
                <meshStandardMaterial
                  color={m.color}
                  emissive={m.color}
                  emissiveIntensity={0.3}
                  roughness={0.3}
                  metalness={0.7}
                />
              </mesh>
              <Text
                position={[0, -0.85, 0]}
                fontSize={0.08}
                color={AI_LAYOUT.colors.textWhite}
                anchorX="center"
                anchorY="middle"
              >
                {m.name.split(' ')[0]}
              </Text>
              <Text
                position={[0, yCenter + barHeight / 2 + 0.12, 0]}
                fontSize={0.08}
                color={m.color}
                anchorX="center"
                anchorY="middle"
              >
                {m.ratio.toFixed(2)}
              </Text>
            </group>
          );
        })}
      </group>

      {/* 3. Right Section: Battery Health & Cell Thermal Matrix */}
      <group position={[1.7, 2.5, 0.12]}>
        <Text
          position={[0, 1.4, 0]}
          fontSize={0.13}
          color={AI_LAYOUT.colors.cyberCyan}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.05}
        >
          BATTERY PACK THERMAL MATRIX
        </Text>

        {/* 4 Cell Cards */}
        {cells.map((cell, idx) => {
          const row = Math.floor(idx / 2);
          const col = idx % 2;
          const xPos = (col - 0.5) * 1.35;
          const yPos = 0.6 - row * 0.95;

          return (
            <group key={cell.id} position={[xPos, yPos, 0]}>
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[1.2, 0.72]} />
                <meshBasicMaterial color="#02040b" opacity={0.9} transparent side={THREE.DoubleSide} />
              </mesh>
              <lineSegments position={[0, 0, 0.005]}>
                <edgesGeometry args={[new THREE.PlaneGeometry(1.2, 0.72)]} />
                <lineBasicMaterial color={cell.color} />
              </lineSegments>
              <Text
                position={[0, 0.2, 0.02]}
                fontSize={0.095}
                color={AI_LAYOUT.colors.textWhite}
                anchorX="center"
                anchorY="middle"
              >
                {cell.id}
              </Text>
              <Text
                position={[0, 0.02, 0.02]}
                fontSize={0.12}
                color={cell.color}
                anchorX="center"
                anchorY="middle"
              >
                {cell.temp}
              </Text>
              <Text
                position={[0, -0.2, 0.02]}
                fontSize={0.075}
                color={cell.color}
                anchorX="center"
                anchorY="middle"
                letterSpacing={0.04}
              >
                {cell.status}
              </Text>
            </group>
          );
        })}
      </group>

      {/* 4. Footer Safety Certification Seal */}
      <group position={[0, 0.5, 0.12]}>
        <Text
          fontSize={0.095}
          color={AI_LAYOUT.colors.textMuted}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          AI AUTONOMY: 0.0% (STRICTLY ADVISORY) • HUMAN RBAC CONSENSUS MANDATORY (BR-AI-SAFE-01)
        </Text>
      </group>
    </group>
  );
};
