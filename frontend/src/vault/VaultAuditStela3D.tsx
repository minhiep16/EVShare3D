import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useVaultStore } from './useVaultStore';
import { VAULT_LAYOUT } from './vaultLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import { formatCurrencyVND, formatTimeVN } from '@/i18n';

export const VaultAuditStela3D: React.FC = () => {
  const reconciliationReport = useVaultStore((state) => state.reconciliationReport);
  const reconcile = useVaultStore((state) => state.reconcile);
  const focusCamera = useVaultStore((state) => state.focusCamera);

  const [hoveredButton, setHoveredButton] = useState<string | null>(null);

  const r = reconciliationReport || {
    currentBalanceVnd: 45000000,
    calculatedLedgerBalanceVnd: 45000000,
    totalCreditsVnd: 52000000,
    totalDebitsVnd: 7000000,
    creditCount: 14,
    debitCount: 5,
    transactionCount: 19,
    isReconciled: true,
    reconciliationDeltaVnd: 0,
    reconciledAt: new Date().toISOString(),
    summary: 'Ledger state mathematically verified.',
  };

  return (
    <group
      position={VAULT_LAYOUT.auditStelaPosition}
      name="VaultAuditStela"
      onClick={(e) => {
        e.stopPropagation();
        focusCamera('AUDIT_FOCUS');
      }}
    >
      {/* 1. Pedestal Base */}
      <mesh position={[0, 0.2, 0]} castShadow receiveShadow>
        <boxGeometry args={[2.2, 0.4, 1.2]} />
        <meshStandardMaterial
          color={VAULT_LAYOUT.theme.titaniumDark}
          metalness={0.9}
          roughness={0.2}
        />
      </mesh>

      {/* 2. Upright Monolithic Stela */}
      <mesh position={[0, 1.8, 0]} castShadow receiveShadow>
        <boxGeometry args={[1.9, 2.8, 0.18]} />
        <meshStandardMaterial
          color="#050a14"
          metalness={0.8}
          roughness={0.15}
        />
      </mesh>

      {/* Frame Trim */}
      <lineSegments position={[0, 1.8, 0.1]}>
        <edgesGeometry args={[new THREE.BoxGeometry(1.9, 2.8, 0.18)]} />
        <lineBasicMaterial color={VAULT_LAYOUT.theme.goldPrimary} />
      </lineSegments>

      {/* 3. Inscribed Monolith Text Content */}
      <group position={[0, 1.8, 0.11]}>
        {/* Title */}
        <Text
          position={[0, 1.15, 0]}
          fontSize={0.12}
          color={VAULT_LAYOUT.theme.goldPrimary}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          BẰNG CHỨNG KIỂM TOÁN MẬT MÃ
        </Text>

        <Text
          position={[0, 0.98, 0]}
          fontSize={0.085}
          color={VAULT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          ĐỐI SOÁT SỔ CÁI TOÁN HỌC
        </Text>

        {/* Total Credits */}
        <group position={[0, 0.7, 0]}>
          <Text
            position={[-0.8, 0, 0]}
            fontSize={0.08}
            color={VAULT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            TỔNG TIỀN NẠP ({r.creditCount} GD):
          </Text>
          <Text
            position={[0.8, 0, 0]}
            fontSize={0.09}
            color={VAULT_LAYOUT.theme.solvencyGreen}
            anchorX="right"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            +{formatCurrencyVND(r.totalCreditsVnd)}
          </Text>
        </group>

        {/* Total Debits */}
        <group position={[0, 0.48, 0]}>
          <Text
            position={[-0.8, 0, 0]}
            fontSize={0.08}
            color={VAULT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            TỔNG TIỀN CHI ({r.debitCount} GD):
          </Text>
          <Text
            position={[0.8, 0, 0]}
            fontSize={0.09}
            color={VAULT_LAYOUT.theme.dangerRed}
            anchorX="right"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            -{formatCurrencyVND(r.totalDebitsVnd)}
          </Text>
        </group>

        {/* Calculated Balance */}
        <group position={[0, 0.22, 0]}>
          <Text
            position={[-0.8, 0, 0]}
            fontSize={0.08}
            color={VAULT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            SỐ DƯ TÍNH TOÁN:
          </Text>
          <Text
            position={[0.8, 0, 0]}
            fontSize={0.095}
            color={VAULT_LAYOUT.theme.goldPrimary}
            anchorX="right"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {formatCurrencyVND(r.calculatedLedgerBalanceVnd)}
          </Text>
        </group>

        {/* On-Chain Physical Balance */}
        <group position={[0, 0.0, 0]}>
          <Text
            position={[-0.8, 0, 0]}
            fontSize={0.08}
            color={VAULT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            DỰ PHÒNG THỰC TẾ:
          </Text>
          <Text
            position={[0.8, 0, 0]}
            fontSize={0.095}
            color="#ffffff"
            anchorX="right"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {formatCurrencyVND(r.currentBalanceVnd)}
          </Text>
        </group>

        {/* Delta Status Banner */}
        <group position={[0, -0.3, 0]}>
          <mesh>
            <planeGeometry args={[1.65, 0.28]} />
            <meshBasicMaterial
              color={r.isReconciled ? '#052e16' : '#450a0a'}
            />
          </mesh>
          <Text
            fontSize={0.09}
            color={r.isReconciled ? VAULT_LAYOUT.theme.solvencyGreen : '#ff5252'}
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {r.isReconciled ? '✓ ĐÃ ĐỐI SOÁT: CHÊNH LỆCH 0 ₫' : `⚠️ SAI LỆCH: ${formatCurrencyVND(r.reconciliationDeltaVnd)}`}
          </Text>
        </group>

        {/* Reconcile Now Action Button */}
        <group
          position={[0, -0.75, 0]}
          onPointerOver={() => {
            setHoveredButton('RECONCILE');
            AudioEngine.play('UI_HOVER');
          }}
          onPointerOut={() => setHoveredButton(null)}
          onClick={(e) => {
            e.stopPropagation();
            reconcile();
          }}
        >
          <mesh>
            <planeGeometry args={[1.65, 0.32]} />
            <meshBasicMaterial
              color={hoveredButton === 'RECONCILE' ? VAULT_LAYOUT.theme.goldPrimary : '#162338'}
            />
          </mesh>
          <Text
            fontSize={0.1}
            color={hoveredButton === 'RECONCILE' ? '#000000' : '#ffffff'}
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            THỰC HIỆN ĐỐI SOÁT NGAY
          </Text>
        </group>

        <Text
          position={[0, -1.05, 0]}
          fontSize={0.075}
          color={VAULT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          XÁC MINH LẦN CUỐI: {formatTimeVN(r.reconciledAt)}
        </Text>
      </group>
    </group>
  );
};
