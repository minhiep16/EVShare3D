import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useVaultStore } from './useVaultStore';
import { VAULT_LAYOUT } from './vaultLayout';
import type { VaultTransactionNode } from './vaultTypes';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const TransactionRibbon3D: React.FC = () => {
  const transactions = useVaultStore((state) => state.transactions);
  const selectedTransaction = useVaultStore((state) => state.selectedTransaction);
  const selectTransaction = useVaultStore((state) => state.selectTransaction);
  const focusCamera = useVaultStore((state) => state.focusCamera);

  const [hoveredNodeId, setHoveredNodeId] = useState<number | null>(null);
  const ribbonArcRef = useRef<THREE.Group>(null);

  // Subtle undulation micro-animation
  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (ribbonArcRef.current) {
      ribbonArcRef.current.position.y = VAULT_LAYOUT.ribbonCenterPosition[1] + Math.sin(t * 0.8) * 0.05;
    }
  });

  // Arc curve parameters: Semi-circle around the ribbon center
  const arcRadius = 5.2;
  const maxNodes = Math.min(8, transactions.length);
  const visibleTxs = transactions.slice(0, maxNodes);

  const getNodePosition = (index: number, total: number): [number, number, number] => {
    if (total <= 1) return [0, 0, 0];
    const spanAngle = Math.PI * 0.75; // 135 degree panoramic arc
    const startAngle = -spanAngle / 2;
    const step = spanAngle / (total - 1);
    const angle = startAngle + index * step;
    const x = Math.sin(angle) * arcRadius;
    const z = -Math.cos(angle) * arcRadius + arcRadius * 0.6;
    const y = Math.sin(index * 0.7) * 0.35;
    return [x, y, z];
  };

  const handleNodeClick = (tx: VaultTransactionNode, e: { stopPropagation: () => void }) => {
    e.stopPropagation();
    selectTransaction(tx);
    focusCamera('RIBBON_FOCUS');
  };

  return (
    <group ref={ribbonArcRef} position={VAULT_LAYOUT.ribbonCenterPosition} name="TransactionRibbonArc">
      {/* 1. Header Hologram Plaque */}
      <group position={[0, 1.6, -1.0]}>
        <Text
          position={[0, 0.25, 0]}
          fontSize={0.28}
          color={VAULT_LAYOUT.theme.goldPrimary}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          IMMUTABLE TRANSACTION CHRONO-RIBBON
        </Text>
        <Text
          position={[0, -0.05, 0]}
          fontSize={0.14}
          color={VAULT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          INTERACTIVE LEDGER NODES • CLICK TO EXPAND CRYPTOGRAPHIC RECEIPT
        </Text>
      </group>

      {/* 2. Ribbon Arc Guiderail Line */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0, arcRadius * 0.6]}>
        <ringGeometry args={[arcRadius - 0.04, arcRadius + 0.04, 64, 1, Math.PI * 0.62, Math.PI * 0.76]} />
        <meshBasicMaterial
          color={VAULT_LAYOUT.theme.goldPrimary}
          transparent
          opacity={0.35}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 3. Transaction Nodes along the Arc */}
      {visibleTxs.map((tx, idx) => {
        const [nx, ny, nz] = getNodePosition(idx, visibleTxs.length);
        const isHovered = hoveredNodeId === tx.id;
        const isSelected = selectedTransaction?.id === tx.id;
        const isDeposit = tx.type === 'DEPOSIT';
        const isWithdrawal = tx.type === 'WITHDRAWAL';

        const nodeColor = isDeposit
          ? VAULT_LAYOUT.theme.solvencyGreen
          : isWithdrawal
            ? VAULT_LAYOUT.theme.dangerRed
            : VAULT_LAYOUT.theme.goldPrimary;

        return (
          <group key={tx.id} position={[nx, ny, nz]}>
            {/* Interactive Node Mesh */}
            <mesh
              scale={isHovered || isSelected ? 1.35 : 1.0}
              onPointerOver={(e) => {
                e.stopPropagation();
                setHoveredNodeId(tx.id);
                AudioEngine.play('UI_HOVER');
              }}
              onPointerOut={() => setHoveredNodeId(null)}
              onClick={(e) => handleNodeClick(tx, e)}
            >
              <octahedronGeometry args={[0.26, 0]} />
              <meshStandardMaterial
                color={nodeColor}
                emissive={nodeColor}
                emissiveIntensity={isHovered || isSelected ? 0.9 : 0.4}
                metalness={0.7}
                roughness={0.2}
              />
            </mesh>

            {/* Glowing Selection Halo */}
            {isSelected && (
              <mesh rotation={[-Math.PI / 2, 0, 0]}>
                <ringGeometry args={[0.38, 0.45, 24]} />
                <meshBasicMaterial color="#ffffff" transparent opacity={0.8} />
              </mesh>
            )}

            {/* Node Quick Badge Label */}
            <group position={[0, -0.42, 0]}>
              <Text
                position={[0, 0.06, 0]}
                fontSize={0.13}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                {isDeposit ? '+' : '-'}{(tx.amountVnd / 1000000).toFixed(1)}M
              </Text>
              <Text
                position={[0, -0.1, 0]}
                fontSize={0.1}
                color={nodeColor}
                anchorX="center"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                {tx.type}
              </Text>
            </group>
          </group>
        );
      })}

      {/* 4. Expanded In-World 3D Holographic Receipt Card */}
      {selectedTransaction && (
        <group position={[0, -0.2, 0.8]} name="ExpandedReceiptCard">
          {/* Hologram Card Panel */}
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[3.8, 2.2]} />
            <meshStandardMaterial
              color="#07101e"
              transparent
              opacity={0.92}
              roughness={0.3}
              metalness={0.6}
            />
          </mesh>

          {/* Border Outline */}
          <lineSegments position={[0, 0, 0.01]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(3.8, 2.2)]} />
            <lineBasicMaterial
              color={
                selectedTransaction.type === 'DEPOSIT'
                  ? VAULT_LAYOUT.theme.solvencyGreen
                  : selectedTransaction.type === 'WITHDRAWAL'
                    ? VAULT_LAYOUT.theme.dangerRed
                    : VAULT_LAYOUT.theme.goldPrimary
              }
            />
          </lineSegments>

          {/* Close button [X] */}
          <group
            position={[1.6, 0.85, 0.02]}
            onClick={(e) => {
              e.stopPropagation();
              selectTransaction(null);
            }}
          >
            <mesh>
              <planeGeometry args={[0.3, 0.3]} />
              <meshBasicMaterial color="#1e293b" />
            </mesh>
            <Text
              fontSize={0.16}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              ✕
            </Text>
          </group>

          {/* Receipt Header */}
          <Text
            position={[-1.65, 0.85, 0.02]}
            fontSize={0.16}
            color={VAULT_LAYOUT.theme.goldPrimary}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            RECEIPT: {selectedTransaction.receiptCode}
          </Text>

          {/* Reference & Type */}
          <Text
            position={[-1.65, 0.55, 0.02]}
            fontSize={0.13}
            color={VAULT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            REF: {selectedTransaction.reference} • TYPE: {selectedTransaction.type}
          </Text>

          {/* Amount Badge */}
          <Text
            position={[-1.65, 0.2, 0.02]}
            fontSize={0.28}
            color={
              selectedTransaction.type === 'DEPOSIT'
                ? VAULT_LAYOUT.theme.solvencyGreen
                : selectedTransaction.type === 'WITHDRAWAL'
                  ? VAULT_LAYOUT.theme.dangerRed
                  : VAULT_LAYOUT.theme.goldPrimary
            }
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {selectedTransaction.type === 'DEPOSIT' ? '+' : '-'}
            {selectedTransaction.amountVnd.toLocaleString('vi-VN')} VND
          </Text>

          {/* Initiator / Actor */}
          <Text
            position={[-1.65, -0.15, 0.02]}
            fontSize={0.14}
            color="#ffffff"
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            INITIATOR: {selectedTransaction.actorName}
          </Text>

          {/* Description */}
          <Text
            position={[-1.65, -0.4, 0.02]}
            fontSize={0.12}
            color={VAULT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            maxWidth={3.3}
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            DESC: {selectedTransaction.description}
          </Text>

          {/* Balance After */}
          <Text
            position={[-1.65, -0.72, 0.02]}
            fontSize={0.13}
            color={VAULT_LAYOUT.theme.goldPrimary}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            VAULT BALANCE AFTER: {selectedTransaction.balanceAfterVnd.toLocaleString('vi-VN')} VND
          </Text>

          {/* Timestamp */}
          <Text
            position={[1.65, -0.72, 0.02]}
            fontSize={0.11}
            color={VAULT_LAYOUT.theme.textMuted}
            anchorX="right"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            {new Date(selectedTransaction.timestamp).toLocaleDateString('vi-VN')}
          </Text>
        </group>
      )}
    </group>
  );
};
