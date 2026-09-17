import React from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { FloatingExpenseCrystal3D } from './FloatingExpenseCrystal3D';
import { useFinanceStore } from './useFinanceStore';
import { FINANCE_LAYOUT } from './financeLayout';

export const ExpenseCluster3D: React.FC = () => {
  const expenses = useFinanceStore((state) => state.expenses);
  const totalBurn = expenses.reduce((acc, e) => acc + e.amountVnd, 0);

  // Arrange crystals in a semi-circular arc around the dais
  const crystalPositions: [number, number, number][] = [
    [-1.6, 1.3, -0.4],
    [-0.8, 1.4, 0.5],
    [0.0, 1.5, 0.8],
    [0.8, 1.4, 0.5],
    [1.6, 1.3, -0.4],
  ];

  return (
    <group position={FINANCE_LAYOUT.expenseClusterPosition}>
      {/* 1. Cluster Plinth Base */}
      <mesh position={[0, 0.1, 0]}>
        <cylinderGeometry args={[2.8, 3.1, 0.2, 32]} />
        <meshStandardMaterial color="#071220" metalness={0.8} roughness={0.3} />
      </mesh>

      {/* Glowing boundary rings */}
      <mesh position={[0, 0.22, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[2.5, 2.7, 32]} />
        <meshBasicMaterial
          color="#00e5ff"
          transparent
          opacity={0.4}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 2. Floating Header Stela */}
      <group position={[0, 2.8, -1.2]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[3.2, 0.8]} />
          <meshBasicMaterial color="#020814" transparent opacity={0.85} side={THREE.DoubleSide} />
        </mesh>
        <mesh position={[0, 0, -0.025]}>
          <planeGeometry args={[3.24, 0.84]} />
          <meshBasicMaterial color="#00e5ff" transparent opacity={0.4} side={THREE.DoubleSide} />
        </mesh>

        <Text
          position={[0, 0.22, 0]}
          fontSize={0.14}
          color="#00e5ff"
          anchorX="center"
          anchorY="middle"
          font="/fonts/Orbitron-Bold.ttf"
        >
          PHYSICAL EXPENSE OBJECTS
        </Text>

        <Text
          position={[0, 0.02, 0]}
          fontSize={0.105}
          color="#f0f4fc"
          anchorX="center"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          {`MONTHLY POOL TOTAL: ${totalBurn.toLocaleString()} VND`}
        </Text>

        <Text
          position={[0, -0.2, 0]}
          fontSize={0.08}
          color="#8a94a6"
          anchorX="center"
          anchorY="middle"
        >
          Click crystal polyhedron to expand co-owner volumetric allocation
        </Text>
      </group>

      {/* 3. Render 3D Expense Crystals */}
      {expenses.slice(0, 5).map((exp, idx) => (
        <FloatingExpenseCrystal3D
          key={`exp_${exp.id}`}
          expense={exp}
          position={crystalPositions[idx] || [0, 1.2, 0]}
        />
      ))}
    </group>
  );
};
