import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useVaultStore } from './useVaultStore';
import { VAULT_LAYOUT } from './vaultLayout';
import type { VaultTab } from './vaultTypes';
import type { PaymentMethod } from '@/api/financeApi';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import { formatCurrencyVND } from '@/i18n';

export const VaultTerminal3D: React.FC = () => {
  const activeTab = useVaultStore((state) => state.activeTab);
  const setActiveTab = useVaultStore((state) => state.setActiveTab);
  const sharedFund = useVaultStore((state) => state.sharedFund);
  const transactions = useVaultStore((state) => state.transactions);
  const auditHistory = useVaultStore((state) => state.auditHistory);
  const operationStatus = useVaultStore((state) => state.operationStatus);
  const resetOperationStatus = useVaultStore((state) => state.resetOperationStatus);

  // Form states
  const depositAmountVnd = useVaultStore((state) => state.depositAmountVnd);
  const setDepositAmount = useVaultStore((state) => state.setDepositAmount);
  const depositPaymentMethod = useVaultStore((state) => state.depositPaymentMethod);
  const setDepositPaymentMethod = useVaultStore((state) => state.setDepositPaymentMethod);
  const contribute = useVaultStore((state) => state.contribute);

  const withdrawalAmountVnd = useVaultStore((state) => state.withdrawalAmountVnd);
  const setWithdrawalAmount = useVaultStore((state) => state.setWithdrawalAmount);
  const withdrawalAllowOverdraft = useVaultStore((state) => state.withdrawalAllowOverdraft);
  const setWithdrawalAllowOverdraft = useVaultStore((state) => state.setWithdrawalAllowOverdraft);
  const withdraw = useVaultStore((state) => state.withdraw);

  const fetchVaultData = useVaultStore((state) => state.fetchVaultData);

  const [hoveredButton, setHoveredButton] = useState<string | null>(null);

  const tabs: { id: VaultTab; label: string }[] = [
    { id: 'OVERVIEW', label: 'TỔNG QUAN' },
    { id: 'CONTRIBUTE', label: 'NẠP TIỀN' },
    { id: 'WITHDRAW', label: 'RÚT TIỀN' },
    { id: 'TRANSACTIONS', label: 'SỔ CÁI' },
    { id: 'AUDIT_RECONCILIATION', label: 'KIỂM TOÁN' },
  ];

  const currentBalance = sharedFund?.currentBalance ?? 45000000;
  const minimumReserve = sharedFund?.minimumReserve ?? 15000000;
  const isBelowReserve = currentBalance < minimumReserve;

  const paymentMethods: PaymentMethod[] = ['VNPAY', 'MOMO', 'BANK_TRANSFER', 'SYNDICATE_WALLET'];

  return (
    <group position={VAULT_LAYOUT.terminalPosition} name="MasterVaultTerminal">
      {/* 1. Pedestal Base */}
      <mesh position={[0, 0.45, 0]} castShadow receiveShadow>
        <cylinderGeometry args={[0.45, 0.65, 0.9, 16]} />
        <meshStandardMaterial
          color={VAULT_LAYOUT.theme.titaniumDark}
          metalness={0.9}
          roughness={0.2}
        />
      </mesh>

      {/* Glowing base collar */}
      <mesh position={[0, 0.91, 0]}>
        <cylinderGeometry args={[0.48, 0.48, 0.04, 16]} />
        <meshBasicMaterial color={VAULT_LAYOUT.theme.goldPrimary} />
      </mesh>

      {/* 2. Angled Console Mainframe (-25 degrees tilt) */}
      <group position={[0, 1.35, 0]} rotation={[-0.45, 0, 0]}>
        {/* Terminal Screen Backing Plate */}
        <mesh position={[0, 0, -0.04]} castShadow>
          <boxGeometry args={[3.8, 2.4, 0.08]} />
          <meshStandardMaterial
            color="#050a14"
            metalness={0.9}
            roughness={0.2}
          />
        </mesh>

        {/* Console Screen Glass */}
        <mesh position={[0, 0, 0.01]}>
          <planeGeometry args={[3.65, 2.25]} />
          <meshStandardMaterial
            color="#08101d"
            roughness={0.2}
            metalness={0.5}
          />
        </mesh>

        {/* Screen Bezel Edge */}
        <lineSegments position={[0, 0, 0.02]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(3.65, 2.25)]} />
          <lineBasicMaterial color={VAULT_LAYOUT.theme.goldPrimary} />
        </lineSegments>

        {/* Header Title Bar */}
        <group position={[0, 0.95, 0.03]}>
          <Text
            position={[-1.7, 0, 0]}
            fontSize={0.13}
            color={VAULT_LAYOUT.theme.goldPrimary}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            HỆ THỐNG KHO QUỸ EVSHARE v4.2 • QUỸ DỰ PHÒNG ĐỒNG SỞ HỮU
          </Text>

          {/* Sync Button */}
          <group
            position={[1.45, 0, 0]}
            onPointerOver={() => setHoveredButton('SYNC')}
            onPointerOut={() => setHoveredButton(null)}
            onClick={(e) => {
              e.stopPropagation();
              AudioEngine.play('UI_CLICK');
              fetchVaultData();
            }}
          >
            <mesh>
              <planeGeometry args={[0.65, 0.22]} />
              <meshBasicMaterial color={hoveredButton === 'SYNC' ? VAULT_LAYOUT.theme.amberAccent : '#1e293b'} />
            </mesh>
            <Text
              fontSize={0.095}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
            >
              ĐỒNG BỘ
            </Text>
          </group>
        </group>

        {/* 3. 3D Navigation Tab Strip */}
        <group position={[0, 0.72, 0.03]}>
          {tabs.map((tab, idx) => {
            const tabWidth = 0.68;
            const xPos = -1.36 + idx * (tabWidth + 0.04);
            const isActive = activeTab === tab.id;
            const isHovered = hoveredButton === `TAB_${tab.id}`;

            return (
              <group
                key={tab.id}
                position={[xPos, 0, 0]}
                onPointerOver={() => {
                  setHoveredButton(`TAB_${tab.id}`);
                  AudioEngine.play('UI_HOVER');
                }}
                onPointerOut={() => setHoveredButton(null)}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveTab(tab.id);
                  resetOperationStatus();
                }}
              >
                <mesh>
                  <planeGeometry args={[tabWidth, 0.22]} />
                  <meshBasicMaterial
                    color={
                      isActive
                        ? VAULT_LAYOUT.theme.goldPrimary
                        : isHovered
                          ? '#1e2d46'
                          : '#0d1829'
                    }
                  />
                </mesh>
                <Text
                  fontSize={0.095}
                  color={isActive ? '#000000' : '#ffffff'}
                  anchorX="center"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  {tab.label}
                </Text>
              </group>
            );
          })}
        </group>

        {/* 4. Tab Content Area */}
        <group position={[0, -0.15, 0.03]}>
          {/* TAB 1: OVERVIEW */}
          {activeTab === 'OVERVIEW' && (
            <group>
              {/* Balance Box */}
              <mesh position={[-0.85, 0.28, 0]}>
                <planeGeometry args={[1.7, 0.75]} />
                <meshBasicMaterial color="#0c172a" />
              </mesh>
              <Text
                position={[-1.6, 0.52, 0.01]}
                fontSize={0.11}
                color={VAULT_LAYOUT.theme.textMuted}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                TỔNG THANH KHOẢN QUỸ
              </Text>
              <Text
                position={[-1.6, 0.28, 0.01]}
                fontSize={0.19}
                color={VAULT_LAYOUT.theme.goldPrimary}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                {formatCurrencyVND(currentBalance)}
              </Text>
              <Text
                position={[-1.6, 0.06, 0.01]}
                fontSize={0.095}
                color={isBelowReserve ? '#ff5252' : VAULT_LAYOUT.theme.solvencyGreen}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                {isBelowReserve ? '⚠️ DƯỚI HẠN MỨC DỰ PHÒNG' : '✓ THANH KHOẢN AN TOÀN 100%'}
              </Text>

              {/* Reserve Box */}
              <mesh position={[0.85, 0.28, 0]}>
                <planeGeometry args={[1.7, 0.75]} />
                <meshBasicMaterial color="#0c172a" />
              </mesh>
              <Text
                position={[0.1, 0.52, 0.01]}
                fontSize={0.11}
                color={VAULT_LAYOUT.theme.textMuted}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                DỰ PHÒNG TỐI THIỂU BR-FIN-03
              </Text>
              <Text
                position={[0.1, 0.28, 0.01]}
                fontSize={0.19}
                color="#ffffff"
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                {formatCurrencyVND(minimumReserve)}
              </Text>
              <Text
                position={[0.1, 0.06, 0.01]}
                fontSize={0.095}
                color={VAULT_LAYOUT.theme.textMuted}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                CHỐNG THẤU CHI ĐANG HOẠT ĐỘNG
              </Text>

              {/* Quick Actions Pedestals */}
              <group position={[0, -0.42, 0]}>
                {/* Deposit Shortcut */}
                <group
                  position={[-0.9, 0, 0]}
                  onPointerOver={() => setHoveredButton('GOTO_DEP')}
                  onPointerOut={() => setHoveredButton(null)}
                  onClick={(e) => {
                    e.stopPropagation();
                    setActiveTab('CONTRIBUTE');
                  }}
                >
                  <mesh>
                    <planeGeometry args={[1.55, 0.35]} />
                    <meshBasicMaterial color={hoveredButton === 'GOTO_DEP' ? VAULT_LAYOUT.theme.goldPrimary : '#132338'} />
                  </mesh>
                  <Text
                    fontSize={0.12}
                    color={hoveredButton === 'GOTO_DEP' ? '#000000' : '#ffffff'}
                    anchorX="center"
                    anchorY="middle"
                    font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                  >
                    + NẠP VỐN VÀO QUỸ
                  </Text>
                </group>

                {/* Withdraw Shortcut */}
                <group
                  position={[0.9, 0, 0]}
                  onPointerOver={() => setHoveredButton('GOTO_WTH')}
                  onPointerOut={() => setHoveredButton(null)}
                  onClick={(e) => {
                    e.stopPropagation();
                    setActiveTab('WITHDRAW');
                  }}
                >
                  <mesh>
                    <planeGeometry args={[1.55, 0.35]} />
                    <meshBasicMaterial color={hoveredButton === 'GOTO_WTH' ? VAULT_LAYOUT.theme.dangerRed : '#24141c'} />
                  </mesh>
                  <Text
                    fontSize={0.12}
                    color="#ffffff"
                    anchorX="center"
                    anchorY="middle"
                    font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                  >
                    - CHI TIỀN TỪ QUỸ
                  </Text>
                </group>
              </group>
            </group>
          )}

          {/* TAB 2: CONTRIBUTE */}
          {activeTab === 'CONTRIBUTE' && (
            <group>
              <Text
                position={[-1.7, 0.58, 0]}
                fontSize={0.13}
                color={VAULT_LAYOUT.theme.goldPrimary}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                CHỌN SỐ TIỀN ĐÓNG GÓP VỐN:
              </Text>

              {/* Amount Chips */}
              <group position={[0, 0.34, 0]}>
                {[1000000, 5000000, 10000000, 25000000].map((amt, idx) => {
                  const x = -1.25 + idx * 0.83;
                  const isSel = depositAmountVnd === amt;
                  return (
                    <group
                      key={amt}
                      position={[x, 0, 0]}
                      onPointerOver={() => {
                        setHoveredButton(`DEP_AMT_${amt}`);
                        AudioEngine.play('UI_HOVER');
                      }}
                      onPointerOut={() => setHoveredButton(null)}
                      onClick={(e) => {
                        e.stopPropagation();
                        AudioEngine.play('UI_CLICK');
                        setDepositAmount(amt);
                      }}
                    >
                      <mesh>
                        <planeGeometry args={[0.76, 0.28]} />
                        <meshBasicMaterial
                          color={isSel ? VAULT_LAYOUT.theme.goldPrimary : '#122033'}
                        />
                      </mesh>
                      <Text
                        fontSize={0.11}
                        color={isSel ? '#000000' : '#ffffff'}
                        anchorX="center"
                        anchorY="middle"
                        font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                      >
                        +{(amt / 1000000).toFixed(0)} Tr ₫
                      </Text>
                    </group>
                  );
                })}
              </group>

              {/* Selected Amount Display */}
              <Text
                position={[0, 0.06, 0]}
                fontSize={0.19}
                color={VAULT_LAYOUT.theme.goldPrimary}
                anchorX="center"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                SỐ TIỀN: {formatCurrencyVND(depositAmountVnd)}
              </Text>

              {/* Payment Method Chips */}
              <group position={[0, -0.22, 0]}>
                {paymentMethods.map((m, idx) => {
                  const x = -1.25 + idx * 0.83;
                  const isSel = depositPaymentMethod === m;
                  const methodLabels: Record<PaymentMethod, string> = {
                    VNPAY: 'VNPAY',
                    MOMO: 'MOMO',
                    BANK_TRANSFER: 'CHUYỂN KHOẢN',
                    SYNDICATE_WALLET: 'VÍ NHÓM',
                  };
                  return (
                    <group
                      key={m}
                      position={[x, 0, 0]}
                      onPointerOver={() => setHoveredButton(`DEP_MET_${m}`)}
                      onPointerOut={() => setHoveredButton(null)}
                      onClick={(e) => {
                        e.stopPropagation();
                        setDepositPaymentMethod(m);
                      }}
                    >
                      <mesh>
                        <planeGeometry args={[0.76, 0.24]} />
                        <meshBasicMaterial
                          color={isSel ? '#00e5ff' : '#0c1626'}
                        />
                      </mesh>
                      <Text
                        fontSize={0.08}
                        color={isSel ? '#000000' : '#94a3b8'}
                        anchorX="center"
                        anchorY="middle"
                        font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                      >
                        {methodLabels[m] || m}
                      </Text>
                    </group>
                  );
                })}
              </group>

              {/* Submit Button */}
              <group
                position={[0, -0.56, 0]}
                onPointerOver={() => setHoveredButton('SUBMIT_DEP')}
                onPointerOut={() => setHoveredButton(null)}
                onClick={(e) => {
                  e.stopPropagation();
                  contribute();
                }}
              >
                <mesh>
                  <planeGeometry args={[2.8, 0.35]} />
                  <meshBasicMaterial
                    color={hoveredButton === 'SUBMIT_DEP' ? VAULT_LAYOUT.theme.goldPrimary : '#1c3452'}
                  />
                </mesh>
                <Text
                  fontSize={0.12}
                  color={hoveredButton === 'SUBMIT_DEP' ? '#000000' : '#ffffff'}
                  anchorX="center"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  GỬI YÊU CẦU NẠP TIỀN VÀO QUỸ
                </Text>
              </group>
            </group>
          )}

          {/* TAB 3: WITHDRAW (BR-FIN-03 STRICT ENFORCEMENT) */}
          {activeTab === 'WITHDRAW' && (
            <group>
              <Text
                position={[-1.7, 0.58, 0]}
                fontSize={0.12}
                color={VAULT_LAYOUT.theme.dangerRed}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                CHI TIỀN TỪ QUỸ • BẢO VỆ HẠN MỨC DỰ PHÒNG BR-FIN-03:
              </Text>

              {/* Amount Chips (Includes 35M to demonstrate real BR-FIN-03 rejection!) */}
              <group position={[0, 0.34, 0]}>
                {[1000000, 3500000, 5000000, 35000000].map((amt, idx) => {
                  const x = -1.25 + idx * 0.83;
                  const isSel = withdrawalAmountVnd === amt;
                  return (
                    <group
                      key={amt}
                      position={[x, 0, 0]}
                      onPointerOver={() => {
                        setHoveredButton(`WTH_AMT_${amt}`);
                        AudioEngine.play('UI_HOVER');
                      }}
                      onPointerOut={() => setHoveredButton(null)}
                      onClick={(e) => {
                        e.stopPropagation();
                        AudioEngine.play('UI_CLICK');
                        setWithdrawalAmount(amt);
                      }}
                    >
                      <mesh>
                        <planeGeometry args={[0.76, 0.28]} />
                        <meshBasicMaterial
                          color={isSel ? VAULT_LAYOUT.theme.dangerRed : '#24121b'}
                        />
                      </mesh>
                      <Text
                        fontSize={0.1}
                        color="#ffffff"
                        anchorX="center"
                        anchorY="middle"
                        font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                      >
                        -{(amt / 1000000).toFixed(1)} Tr ₫
                      </Text>
                    </group>
                  );
                })}
              </group>

              {/* Amount & Projected Balance */}
              <group position={[0, 0.05, 0]}>
                <Text
                  position={[-1.6, 0, 0]}
                  fontSize={0.14}
                  color={VAULT_LAYOUT.theme.dangerRed}
                  anchorX="left"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  SỐ TIỀN: {formatCurrencyVND(withdrawalAmountVnd)}
                </Text>
                <Text
                  position={[1.6, 0, 0]}
                  fontSize={0.12}
                  color={
                    currentBalance - withdrawalAmountVnd < minimumReserve
                      ? '#ff5252'
                      : VAULT_LAYOUT.theme.solvencyGreen
                  }
                  anchorX="right"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                >
                  DỰ PHÓNG SAU CHI: {formatCurrencyVND(currentBalance - withdrawalAmountVnd)}
                </Text>
              </group>

              {/* Overdraft Checkbox Toggle */}
              <group
                position={[0, -0.22, 0]}
                onPointerOver={() => setHoveredButton('TOGGLE_OVERDRAFT')}
                onPointerOut={() => setHoveredButton(null)}
                onClick={(e) => {
                  e.stopPropagation();
                  setWithdrawalAllowOverdraft(!withdrawalAllowOverdraft);
                }}
              >
                <mesh position={[-1.3, 0, 0]}>
                  <boxGeometry args={[0.22, 0.22, 0.02]} />
                  <meshBasicMaterial
                    color={withdrawalAllowOverdraft ? VAULT_LAYOUT.theme.goldPrimary : '#1e293b'}
                  />
                </mesh>
                <Text
                  position={[-1.1, 0, 0]}
                  fontSize={0.11}
                  color="#ffffff"
                  anchorX="left"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                >
                  CHO PHÉP THẤU CHI (MIỄN TRỪ CẦN BIỂU QUYẾT ĐẶC BIỆT)
                </Text>
                <Text
                  position={[1.6, 0, 0]}
                  fontSize={0.1}
                  color={withdrawalAllowOverdraft ? VAULT_LAYOUT.theme.goldPrimary : VAULT_LAYOUT.theme.textMuted}
                  anchorX="right"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  [{withdrawalAllowOverdraft ? 'THẤU CHI: ĐÃ BẬT' : 'THẤU CHI: ĐÃ CHẶN'}]
                </Text>
              </group>

              {/* Submit Disbursement Button */}
              <group
                position={[0, -0.56, 0]}
                onPointerOver={() => setHoveredButton('SUBMIT_WTH')}
                onPointerOut={() => setHoveredButton(null)}
                onClick={(e) => {
                  e.stopPropagation();
                  withdraw();
                }}
              >
                <mesh>
                  <planeGeometry args={[2.8, 0.35]} />
                  <meshBasicMaterial
                    color={hoveredButton === 'SUBMIT_WTH' ? VAULT_LAYOUT.theme.dangerRed : '#3f1522'}
                  />
                </mesh>
                <Text
                  fontSize={0.12}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  THỰC HIỆN LỆNH CHI TIỀN TỪ QUỸ
                </Text>
              </group>
            </group>
          )}

          {/* TAB 4: TRANSACTIONS / LEDGER */}
          {activeTab === 'TRANSACTIONS' && (
            <group>
              <Text
                position={[-1.7, 0.58, 0]}
                fontSize={0.13}
                color={VAULT_LAYOUT.theme.goldPrimary}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                GIAO DỊCH QUỸ GẦN ĐÂY:
              </Text>

              {transactions.slice(0, 4).map((tx, idx) => {
                const y = 0.35 - idx * 0.28;
                const isDeposit = tx.type === 'DEPOSIT';
                return (
                  <group key={tx.id} position={[0, y, 0]}>
                    <mesh position={[0, 0, 0]}>
                      <planeGeometry args={[3.4, 0.24]} />
                      <meshBasicMaterial color="#0c172a" />
                    </mesh>
                    <Text
                      position={[-1.6, 0, 0.01]}
                      fontSize={0.11}
                      color="#ffffff"
                      anchorX="left"
                      anchorY="middle"
                      font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                    >
                      {tx.reference} • {tx.actorName}
                    </Text>
                    <Text
                      position={[1.6, 0, 0.01]}
                      fontSize={0.11}
                      color={isDeposit ? VAULT_LAYOUT.theme.solvencyGreen : VAULT_LAYOUT.theme.dangerRed}
                      anchorX="right"
                      anchorY="middle"
                      font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                    >
                      {isDeposit ? '+' : '-'}{formatCurrencyVND(tx.amountVnd)}
                    </Text>
                  </group>
                );
              })}
            </group>
          )}

          {/* TAB 5: AUDIT */}
          {activeTab === 'AUDIT_RECONCILIATION' && (
            <group>
              <Text
                position={[-1.7, 0.58, 0]}
                fontSize={0.13}
                color={VAULT_LAYOUT.theme.goldPrimary}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                CHUỖI BẰNG CHỨNG KIỂM TOÁN MẬT MÃ:
              </Text>

              {auditHistory.slice(0, 3).map((item, idx) => {
                const y = 0.32 - idx * 0.36;
                return (
                  <group key={item.id} position={[0, y, 0]}>
                    <mesh position={[0, 0, 0]}>
                      <planeGeometry args={[3.4, 0.32]} />
                      <meshBasicMaterial color="#091424" />
                    </mesh>
                    <Text
                      position={[-1.6, 0.08, 0.01]}
                      fontSize={0.1}
                      color={VAULT_LAYOUT.theme.goldPrimary}
                      anchorX="left"
                      anchorY="middle"
                      font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                    >
                      [{item.action}] BỞI {item.actorName} ({item.ipAddress})
                    </Text>
                    <Text
                      position={[-1.6, -0.08, 0.01]}
                      fontSize={0.085}
                      color={VAULT_LAYOUT.theme.textMuted}
                      anchorX="left"
                      anchorY="middle"
                      maxWidth={3.2}
                      font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                    >
                      {item.details}
                    </Text>
                  </group>
                );
              })}
            </group>
          )}
        </group>

        {/* 5. Bottom Status / Error Feedback Bar */}
        {operationStatus.status !== 'IDLE' && (
          <group position={[0, -0.92, 0.04]}>
            <mesh>
              <planeGeometry args={[3.6, 0.28]} />
              <meshBasicMaterial
                color={
                  operationStatus.status === 'SUCCESS'
                    ? '#052e16'
                    : operationStatus.status === 'ERROR'
                      ? '#450a0a'
                      : '#1e293b'
                }
              />
            </mesh>
            <Text
              position={[-1.7, 0, 0.01]}
              fontSize={0.095}
              color={
                operationStatus.status === 'SUCCESS'
                  ? VAULT_LAYOUT.theme.solvencyGreen
                  : operationStatus.status === 'ERROR'
                    ? '#ff5252'
                    : '#ffffff'
              }
              anchorX="left"
              anchorY="middle"
              maxWidth={3.4}
              font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
            >
              {operationStatus.message}
            </Text>
          </group>
        )}
      </group>
    </group>
  );
};
