import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useFinanceStore } from './useFinanceStore';
import { FINANCE_LAYOUT } from './financeLayout';
import type { AllocationStrategy } from './financeTypes';

export const CostAllocationBarGraph3D: React.FC = () => {
  const memberSummaries = useFinanceStore((state) => state.memberSummaries);
  const strategy = useFinanceStore((state) => state.allocationStrategy);
  const isCalculatingAllocation = useFinanceStore((state) => state.isCalculatingAllocation);
  const setStrategy = useFinanceStore((state) => state.setAllocationStrategy);
  const setActiveTab = useFinanceStore((state) => state.setActiveTab);

  // Maximum scale reference for bar height (e.g. 15,000,000 VND = 2.4m height)
  const maxRefAmount = 14000000;
  const maxBarHeight = 2.4;

  const strategies: { id: AllocationStrategy; label: string; desc: string }[] = [
    { id: 'OWNERSHIP_BASED', label: 'EQUITY %', desc: 'Strictly proportional to fractional ownership' },
    { id: 'USAGE_BASED', label: 'USAGE %', desc: 'Pro-rata by logged kilometers & hours' },
    { id: 'HYBRID', label: 'HYBRID', desc: 'Fixed costs by equity %, variable by usage %' },
  ];

  return (
    <group position={FINANCE_LAYOUT.allocationBarPosition}>
      {/* 1. Base Platform */}
      <mesh position={[0, 0.1, 0]}>
        <cylinderGeometry args={[2.8, 3.1, 0.2, 32]} />
        <meshStandardMaterial color="#071220" metalness={0.8} roughness={0.3} />
      </mesh>

      <mesh position={[0, 0.22, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[2.5, 2.7, 32]} />
        <meshBasicMaterial color="#00e676" transparent opacity={0.4} side={THREE.DoubleSide} />
      </mesh>

      {/* 2. Strategy Switcher Header Bar */}
      <group position={[0, 3.25, -1.2]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[3.8, 1.2]} />
          <meshBasicMaterial color="#020814" transparent opacity={0.88} side={THREE.DoubleSide} />
        </mesh>
        <mesh position={[0, 0, -0.025]}>
          <planeGeometry args={[3.84, 1.24]} />
          <meshBasicMaterial color="#00e676" transparent opacity={0.4} side={THREE.DoubleSide} />
        </mesh>

        <Text
          position={[0, 0.42, 0]}
          fontSize={0.13}
          color="#00e676"
          anchorX="center"
          anchorY="middle"
          font="/fonts/Orbitron-Bold.ttf"
        >
          COST ALLOCATION ENGINE
        </Text>

        <Text
          position={[0, 0.24, 0]}
          fontSize={0.075}
          color={isCalculatingAllocation ? '#ffab00' : '#00e5ff'}
          anchorX="center"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          {isCalculatingAllocation
            ? '⚡ RECALCULATING VIA SPRING BOOT (BR-FIN-02)...'
            : '✓ AUTHORITATIVE BACKEND RECONCILED (BR-FIN-02)'}
        </Text>

        <Text
          position={[0, 0.08, 0]}
          fontSize={0.075}
          color="#8a94a6"
          anchorX="center"
          anchorY="middle"
        >
          Select allocation rule to dynamically recalculate member liability:
        </Text>

        {/* 3D Strategy Buttons */}
        <group position={[0, -0.16, 0]}>
          {strategies.map((s, idx) => {
            const isSelected = strategy === s.id;
            const x = (idx - 1) * 1.05;
            return (
              <group
                key={s.id}
                position={[x, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setStrategy(s.id);
                }}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[0.95, 0.32]} />
                  <meshBasicMaterial
                    color={isSelected ? '#00e676' : '#0a192f'}
                    transparent
                    opacity={isSelected ? 0.85 : 0.6}
                    side={THREE.DoubleSide}
                  />
                </mesh>
                <mesh position={[0, 0, -0.005]}>
                  <planeGeometry args={[0.97, 0.34]} />
                  <meshBasicMaterial
                    color={isSelected ? '#00e676' : 'rgba(255,255,255,0.2)'}
                    transparent
                    opacity={0.7}
                    side={THREE.DoubleSide}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.02]}
                  fontSize={0.08}
                  color={isSelected ? '#040b17' : '#f0f4fc'}
                  anchorX="center"
                  anchorY="middle"
                  font="/fonts/Orbitron-Bold.ttf"
                >
                  {s.label}
                </Text>
              </group>
            );
          })}
        </group>
      </group>

      {/* 3. 3D Bar Columns for each member */}
      <group position={[0, 0.2, 0]}>
        {memberSummaries.map((m, idx) => {
          const count = memberSummaries.length;
          const x = (idx - (count - 1) / 2) * 1.25;

          const height = Math.max(0.3, Math.min(maxBarHeight, (m.allocatedTotalVnd / maxRefAmount) * maxBarHeight));
          const paidRatio = m.allocatedTotalVnd > 0 ? m.paidTotalVnd / m.allocatedTotalVnd : 1;
          const paidHeight = height * paidRatio;
          const dueHeight = height - paidHeight;

          return (
            <group
              key={`bar_${m.userId}`}
              position={[x, 0, 0]}
              onClick={(e) => {
                e.stopPropagation();
                if (m.outstandingDueVnd > 0) {
                  useFinanceStore.setState((prev) => ({
                    paymentSession: {
                      ...prev.paymentSession,
                      amountVnd: m.outstandingDueVnd,
                      status: 'IDLE',
                    },
                  }));
                  setActiveTab('PAYMENT');
                }
              }}
            >
              {/* Pillar Base Ring */}
              <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
                <ringGeometry args={[0.26, 0.36, 24]} />
                <meshBasicMaterial
                  color={m.outstandingDueVnd === 0 ? '#00e676' : '#ffab00'}
                  transparent
                  opacity={0.6}
                  side={THREE.DoubleSide}
                />
              </mesh>

              {/* Paid Segment (Emerald) */}
              {paidHeight > 0.05 && (
                <mesh position={[0, paidHeight / 2, 0]}>
                  <cylinderGeometry args={[0.26, 0.26, paidHeight, 24]} />
                  <meshStandardMaterial
                    color="#00e676"
                    emissive="#00e676"
                    emissiveIntensity={0.5}
                    roughness={0.2}
                    metalness={0.7}
                    transparent
                    opacity={0.88}
                  />
                </mesh>
              )}

              {/* Outstanding Due Segment (Amber/Red) */}
              {dueHeight > 0.05 && (
                <mesh position={[0, paidHeight + dueHeight / 2, 0]}>
                  <cylinderGeometry args={[0.26, 0.26, dueHeight, 24]} />
                  <meshStandardMaterial
                    color="#ffab00"
                    emissive="#ffab00"
                    emissiveIntensity={0.6}
                    roughness={0.2}
                    metalness={0.7}
                    transparent
                    opacity={0.88}
                  />
                </mesh>
              )}

              {/* Top Cap */}
              <mesh position={[0, height + 0.03, 0]}>
                <cylinderGeometry args={[0.28, 0.28, 0.04, 24]} />
                <meshStandardMaterial
                  color={m.outstandingDueVnd === 0 ? '#00e676' : '#ffab00'}
                  emissive={m.outstandingDueVnd === 0 ? '#00e676' : '#ffab00'}
                  emissiveIntensity={0.8}
                />
              </mesh>

              {/* Floating Labels above Column */}
              <group position={[0, height + 0.45, 0]}>
                {/* Member Name */}
                <Text
                  fontSize={0.11}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                  font="/fonts/Orbitron-Bold.ttf"
                >
                  {m.userName.split(' ')[0]}
                </Text>

                {/* Equity & Usage ratio */}
                <Text
                  position={[0, -0.14, 0]}
                  fontSize={0.075}
                  color="#8a94a6"
                  anchorX="center"
                  anchorY="middle"
                >
                  {`EQ: ${m.equityPercentage}% | USE: ${m.usagePercentage}%`}
                </Text>

                {/* Total Allocated */}
                <Text
                  position={[0, -0.27, 0]}
                  fontSize={0.095}
                  color="#00e5ff"
                  anchorX="center"
                  anchorY="middle"
                  font="/fonts/JetBrainsMono-Bold.ttf"
                >
                  {`${(m.allocatedTotalVnd / 1000000).toFixed(2)}M VND`}
                </Text>

                {/* Settlement badge */}
                <Text
                  position={[0, -0.4, 0]}
                  fontSize={0.075}
                  color={m.outstandingDueVnd === 0 ? '#00e676' : '#ff1744'}
                  anchorX="center"
                  anchorY="middle"
                  font="/fonts/JetBrainsMono-Bold.ttf"
                >
                  {m.outstandingDueVnd === 0
                    ? '● FULLY SETTLED'
                    : `● DUE: ${(m.outstandingDueVnd / 1000000).toFixed(2)}M`}
                </Text>
              </group>
            </group>
          );
        })}
      </group>
    </group>
  );
};
