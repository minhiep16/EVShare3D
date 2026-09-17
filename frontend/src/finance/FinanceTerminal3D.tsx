import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useFinanceStore } from './useFinanceStore';
import { FINANCE_LAYOUT } from './financeLayout';
import type { FinanceTab } from './financeTypes';
import type { ExpenseCategory } from '@/api/financeApi';

export const FinanceTerminal3D: React.FC = () => {
  const activeTab = useFinanceStore((state) => state.activeTab);
  const setActiveTab = useFinanceStore((state) => state.setActiveTab);
  const expenses = useFinanceStore((state) => state.expenses);
  const sharedFund = useFinanceStore((state) => state.sharedFund);
  const memberSummaries = useFinanceStore((state) => state.memberSummaries);
  const focusStation = useFinanceStore((state) => state.focusStation);
  const createNewExpense = useFinanceStore((state) => state.createNewExpenseAction);

  const [isLoggingExpense, setIsLoggingExpense] = useState(false);
  const [newExpCategory, setNewExpCategory] = useState<ExpenseCategory>('CHARGING');
  const [newExpAmount, setNewExpAmount] = useState(1500000);
  const [newExpTitle, setNewExpTitle] = useState('VinFast DC Supercharge');

  const mySummary = memberSummaries.find((m) => m.userId === 1) || memberSummaries[0];
  const totalBurn = expenses.reduce((acc, e) => acc + e.amountVnd, 0);

  const tabs: { id: FinanceTab; label: string }[] = [
    { id: 'OVERVIEW', label: 'TREASURY' },
    { id: 'EXPENSES', label: 'EXPENSES' },
    { id: 'ALLOCATION', label: 'ALLOCATION' },
    { id: 'PAYMENT', label: 'PAY KIOSK' },
    { id: 'AUDIT', label: 'AUDIT' },
  ];

  const handlePostExpense = async () => {
    await createNewExpense({
      groupId: 1,
      vehicleId: 1,
      category: newExpCategory,
      title: newExpTitle,
      amount: newExpAmount,
      incurredDate: new Date().toISOString().split('T')[0],
      description: 'Logged via 3D Finance Terminal Console',
    });
    setIsLoggingExpense(false);
  };

  return (
    <group position={FINANCE_LAYOUT.terminalPosition}>
      {/* 1. Terminal Base Plinth */}
      <mesh position={[0, 0.45, 0]}>
        <cylinderGeometry args={[0.6, 0.8, 0.9, 32]} />
        <meshStandardMaterial color="#050e1c" metalness={0.9} roughness={0.2} />
      </mesh>

      {/* 2. Angled Master Console Display (-18 deg tilt) */}
      <group position={[0, 1.35, 0]} rotation={[-0.31, 0, 0]}>
        {/* Outer chassis */}
        <mesh position={[0, 0, -0.02]}>
          <boxGeometry args={[2.8, 1.85, 0.08]} />
          <meshStandardMaterial color="#040b17" metalness={0.9} roughness={0.2} />
        </mesh>
        <mesh position={[0, 0, 0.025]}>
          <planeGeometry args={[2.72, 1.76]} />
          <meshBasicMaterial color="#020814" />
        </mesh>

        {/* Top Glowing Cyan/Emerald Strip */}
        <mesh position={[0, 0.82, 0.03]}>
          <planeGeometry args={[2.65, 0.03]} />
          <meshBasicMaterial color="#00e676" />
        </mesh>

        {/* Terminal Header */}
        <Text
          position={[-1.25, 0.73, 0.04]}
          fontSize={0.08}
          color="#00e5ff"
          anchorX="left"
          anchorY="middle"
          font="/fonts/Orbitron-Bold.ttf"
        >
          FINANCE &amp; COST ALLOCATION TERMINAL
        </Text>

        <Text
          position={[1.25, 0.73, 0.04]}
          fontSize={0.065}
          color="#00e676"
          anchorX="right"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          ● BACKEND API CONNECTED
        </Text>

        {/* 3D Tab Breadcrumbs */}
        <group position={[0, 0.58, 0.04]}>
          {tabs.map((tab, idx) => {
            const isSelected = activeTab === tab.id;
            const x = (idx - 2) * 0.52;
            return (
              <group
                key={tab.id}
                position={[x, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveTab(tab.id);
                }}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[0.48, 0.16]} />
                  <meshBasicMaterial
                    color={isSelected ? '#00e676' : '#081729'}
                    transparent
                    opacity={isSelected ? 0.85 : 0.5}
                    side={THREE.DoubleSide}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.06}
                  color={isSelected ? '#040b17' : '#f0f4fc'}
                  anchorX="center"
                  anchorY="middle"
                  font="/fonts/Orbitron-Bold.ttf"
                >
                  {tab.label}
                </Text>
              </group>
            );
          })}
        </group>

        {/* TAB 1: OVERVIEW */}
        {activeTab === 'OVERVIEW' && !isLoggingExpense && (
          <group position={[0, -0.05, 0.04]}>
            {/* KPI Cards Row */}
            <group position={[0, 0.32, 0]}>
              {/* Total Monthly Pool */}
              <group position={[-0.85, 0, 0]}>
                <mesh>
                  <planeGeometry args={[0.8, 0.34]} />
                  <meshBasicMaterial color="#071526" />
                </mesh>
                <Text position={[0, 0.08, 0.01]} fontSize={0.05} color="#8a94a6" anchorX="center">
                  MONTHLY EXPENSE POOL
                </Text>
                <Text position={[0, -0.05, 0.01]} fontSize={0.085} color="#00e5ff" anchorX="center" font="/fonts/JetBrainsMono-Bold.ttf">
                  {`${(totalBurn / 1000000).toFixed(1)}M VND`}
                </Text>
              </group>

              {/* Shared Fund Liquidity */}
              <group position={[0, 0, 0]}>
                <mesh>
                  <planeGeometry args={[0.8, 0.34]} />
                  <meshBasicMaterial color="#071526" />
                </mesh>
                <Text position={[0, 0.08, 0.01]} fontSize={0.05} color="#8a94a6" anchorX="center">
                  VAULT LIQUIDITY (BR-FIN-03)
                </Text>
                <Text position={[0, -0.05, 0.01]} fontSize={0.085} color="#00e676" anchorX="center" font="/fonts/JetBrainsMono-Bold.ttf">
                  {`${(sharedFund.currentBalance / 1000000).toFixed(1)}M VND`}
                </Text>
              </group>

              {/* My Outstanding Due */}
              <group position={[0.85, 0, 0]}>
                <mesh>
                  <planeGeometry args={[0.8, 0.34]} />
                  <meshBasicMaterial color="#071526" />
                </mesh>
                <Text position={[0, 0.08, 0.01]} fontSize={0.05} color="#8a94a6" anchorX="center">
                  YOUR PENDING DUES
                </Text>
                <Text position={[0, -0.05, 0.01]} fontSize={0.085} color={mySummary.outstandingDueVnd > 0 ? '#ff1744' : '#00e676'} anchorX="center" font="/fonts/JetBrainsMono-Bold.ttf">
                  {`${(mySummary.outstandingDueVnd / 1000000).toFixed(2)}M VND`}
                </Text>
              </group>
            </group>

            {/* Quick Actions */}
            <group position={[0, -0.08, 0]}>
              <group
                position={[-0.65, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setIsLoggingExpense(true);
                }}
              >
                <mesh>
                  <planeGeometry args={[0.6, 0.18]} />
                  <meshBasicMaterial color="#ffab00" transparent opacity={0.3} side={THREE.DoubleSide} />
                </mesh>
                <Text position={[0, 0, 0.01]} fontSize={0.065} color="#ffab00" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                  + LOG EXPENSE
                </Text>
              </group>

              <group
                position={[0, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveTab('PAYMENT');
                }}
              >
                <mesh>
                  <planeGeometry args={[0.6, 0.18]} />
                  <meshBasicMaterial color="#00e676" transparent opacity={0.3} side={THREE.DoubleSide} />
                </mesh>
                <Text position={[0, 0, 0.01]} fontSize={0.065} color="#00e676" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                  ⚡ PAY DUES NOW
                </Text>
              </group>

              <group
                position={[0.65, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  focusStation('ALLOCATION');
                }}
              >
                <mesh>
                  <planeGeometry args={[0.6, 0.18]} />
                  <meshBasicMaterial color="#00e5ff" transparent opacity={0.3} side={THREE.DoubleSide} />
                </mesh>
                <Text position={[0, 0, 0.01]} fontSize={0.065} color="#00e5ff" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                  📊 VIEW BARS
                </Text>
              </group>
            </group>

            {/* Camera View Navigation Row */}
            <group position={[0, -0.32, 0]}>
              <Text position={[-1.2, 0, 0]} fontSize={0.055} color="#8a94a6" anchorX="left">
                CAMERA SHORTCUTS:
              </Text>
              {[
                { label: 'OVERVIEW', st: 'OVERVIEW' },
                { label: 'CRYSTALS', st: 'CRYSTALS' },
                { label: 'BARS', st: 'ALLOCATION' },
                { label: 'VAULT', st: 'COLUMN' },
                { label: 'KIOSK', st: 'KIOSK' },
              ].map((c, idx) => (
                <group
                  key={c.st}
                  position={[-0.4 + idx * 0.42, 0, 0]}
                  onClick={(e) => {
                    e.stopPropagation();
                    focusStation(c.st as 'OVERVIEW' | 'TERMINAL' | 'CRYSTALS' | 'ALLOCATION' | 'KIOSK' | 'COLUMN');
                  }}
                >
                  <mesh>
                    <planeGeometry args={[0.38, 0.14]} />
                    <meshBasicMaterial color="#0b1e36" transparent opacity={0.6} side={THREE.DoubleSide} />
                  </mesh>
                  <Text position={[0, 0, 0.01]} fontSize={0.05} color="#00e5ff" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                    {c.label}
                  </Text>
                </group>
              ))}
            </group>
          </group>
        )}

        {/* LOG NEW EXPENSE FORM (In pure 3D) */}
        {isLoggingExpense && (
          <group position={[0, -0.05, 0.04]}>
            <Text position={[0, 0.35, 0]} fontSize={0.09} color="#ffab00" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
              LOG NEW SYNDICATE EXPENSE (BACKEND POST)
            </Text>

            {/* Category selection */}
            <group position={[0, 0.18, 0]}>
              {(['CHARGING', 'MAINTENANCE', 'CLEANING', 'PARKING'] as ExpenseCategory[]).map((cat, idx) => {
                const isSel = newExpCategory === cat;
                const x = (idx - 1.5) * 0.6;
                return (
                  <group
                    key={cat}
                    position={[x, 0, 0]}
                    onClick={(e) => {
                      e.stopPropagation();
                      setNewExpCategory(cat);
                    }}
                  >
                    <mesh>
                      <planeGeometry args={[0.55, 0.14]} />
                      <meshBasicMaterial color={isSel ? '#00e5ff' : '#081729'} transparent opacity={isSel ? 0.8 : 0.4} side={THREE.DoubleSide} />
                    </mesh>
                    <Text position={[0, 0, 0.01]} fontSize={0.05} color={isSel ? '#040b17' : '#f0f4fc'} anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                      {cat}
                    </Text>
                  </group>
                );
              })}
            </group>

            {/* Amount stepper */}
            <group position={[0, -0.02, 0]}>
              <Text position={[-0.8, 0, 0]} fontSize={0.07} color="#8a94a6" anchorX="left">
                Amount:
              </Text>
              <Text position={[0, 0, 0]} fontSize={0.11} color="#00e676" anchorX="center" font="/fonts/JetBrainsMono-Bold.ttf">
                {`${newExpAmount.toLocaleString()} VND`}
              </Text>
              <group
                position={[0.55, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setNewExpAmount(newExpAmount + 500000);
                }}
              >
                <mesh>
                  <planeGeometry args={[0.2, 0.14]} />
                  <meshBasicMaterial color="#00e676" transparent opacity={0.3} side={THREE.DoubleSide} />
                </mesh>
                <Text position={[0, 0, 0.01]} fontSize={0.08} color="#00e676" anchorX="center">
                  +
                </Text>
              </group>
              <group
                position={[0.85, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setNewExpAmount(Math.max(500000, newExpAmount - 500000));
                }}
              >
                <mesh>
                  <planeGeometry args={[0.2, 0.14]} />
                  <meshBasicMaterial color="#ff1744" transparent opacity={0.3} side={THREE.DoubleSide} />
                </mesh>
                <Text position={[0, 0, 0.01]} fontSize={0.08} color="#ff1744" anchorX="center">
                  -
                </Text>
              </group>
            </group>

            {/* Submit / Cancel buttons */}
            <group position={[0, -0.25, 0]}>
              <group
                position={[-0.45, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  handlePostExpense();
                }}
              >
                <mesh>
                  <planeGeometry args={[0.7, 0.16]} />
                  <meshBasicMaterial color="#00e676" transparent opacity={0.5} side={THREE.DoubleSide} />
                </mesh>
                <Text position={[0, 0, 0.01]} fontSize={0.065} color="#00e676" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                  ✓ SUBMIT TO LEDGER
                </Text>
              </group>

              <group
                position={[0.45, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setIsLoggingExpense(false);
                }}
              >
                <mesh>
                  <planeGeometry args={[0.6, 0.16]} />
                  <meshBasicMaterial color="#ff1744" transparent opacity={0.3} side={THREE.DoubleSide} />
                </mesh>
                <Text position={[0, 0, 0.01]} fontSize={0.065} color="#ff1744" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                  CANCEL
                </Text>
              </group>
            </group>
          </group>
        )}

        {/* TAB 2: EXPENSES */}
        {activeTab === 'EXPENSES' && (
          <group position={[0, -0.05, 0.04]}>
            <Text position={[0, 0.35, 0]} fontSize={0.085} color="#00e5ff" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
              ACTIVE SYNDICATE EXPENDITURE LEDGER
            </Text>
            {expenses.slice(0, 4).map((exp, idx) => (
              <group key={exp.id} position={[0, 0.15 - idx * 0.18, 0]}>
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[2.5, 0.14]} />
                  <meshBasicMaterial color="#071526" transparent opacity={0.6} />
                </mesh>
                <Text position={[-1.15, 0, 0.01]} fontSize={0.06} color="#ffffff" anchorX="left">
                  {exp.title}
                </Text>
                <Text position={[0.4, 0, 0.01]} fontSize={0.065} color="#00e5ff" anchorX="left" font="/fonts/JetBrainsMono-Bold.ttf">
                  {`${exp.amountVnd.toLocaleString()} VND`}
                </Text>
                <Text position={[1.15, 0, 0.01]} fontSize={0.055} color={exp.isSettled ? '#00e676' : '#ffab00'} anchorX="right">
                  {exp.isSettled ? 'SETTLED' : 'DUE'}
                </Text>
              </group>
            ))}
          </group>
        )}

        {/* TAB 3: ALLOCATION */}
        {activeTab === 'ALLOCATION' && (
          <group position={[0, -0.05, 0.04]}>
            <Text position={[0, 0.35, 0]} fontSize={0.085} color="#00e676" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
              MEMBER ALLOCATION BALANCES
            </Text>
            {memberSummaries.map((m, idx) => (
              <group key={m.userId} position={[0, 0.15 - idx * 0.18, 0]}>
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[2.5, 0.14]} />
                  <meshBasicMaterial color="#071526" transparent opacity={0.6} />
                </mesh>
                <Text position={[-1.15, 0, 0.01]} fontSize={0.065} color="#ffffff" anchorX="left">
                  {m.userName}
                </Text>
                <Text position={[-0.2, 0, 0.01]} fontSize={0.055} color="#8a94a6" anchorX="left">
                  {`Share: ${m.equityPercentage}%`}
                </Text>
                <Text position={[0.4, 0, 0.01]} fontSize={0.065} color="#00e5ff" anchorX="left" font="/fonts/JetBrainsMono-Bold.ttf">
                  {`${(m.allocatedTotalVnd / 1000000).toFixed(2)}M`}
                </Text>
                <Text position={[1.15, 0, 0.01]} fontSize={0.06} color={m.outstandingDueVnd === 0 ? '#00e676' : '#ff1744'} anchorX="right">
                  {m.outstandingDueVnd === 0 ? 'SETTLED' : `DUE: ${(m.outstandingDueVnd / 1000000).toFixed(2)}M`}
                </Text>
              </group>
            ))}
          </group>
        )}

        {/* TAB 4: PAYMENT LINK */}
        {activeTab === 'PAYMENT' && (
          <group position={[0, -0.05, 0.04]}>
            <Text position={[0, 0.2, 0]} fontSize={0.09} color="#00e5ff" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
              3D PAYMENT KIOSK ACTIVE
            </Text>
            <Text position={[0, 0.05, 0]} fontSize={0.07} color="#8a94a6" anchorX="center">
              Please step up to the Contactless 3D Payment Kiosk on your right to settle dues.
            </Text>
            <group
              position={[0, -0.2, 0]}
              onClick={(e) => {
                e.stopPropagation();
                focusStation('KIOSK');
              }}
            >
              <mesh>
                <planeGeometry args={[0.9, 0.18]} />
                <meshBasicMaterial color="#00e676" transparent opacity={0.4} side={THREE.DoubleSide} />
              </mesh>
              <Text position={[0, 0, 0.01]} fontSize={0.07} color="#00e676" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
                FOCUS PAYMENT KIOSK ➔
              </Text>
            </group>
          </group>
        )}

        {/* TAB 5: AUDIT */}
        {activeTab === 'AUDIT' && (
          <group position={[0, -0.05, 0.04]}>
            <Text position={[0, 0.35, 0]} fontSize={0.085} color="#00e5ff" anchorX="center" font="/fonts/Orbitron-Bold.ttf">
              IMMUTABLE FINANCIAL AUDIT TRAIL
            </Text>
            {[
              { id: 'TX-1092', action: 'PAYMENT_SETTLED', user: 'Minh Hiep', amount: '2,975,000 VND', status: 'VERIFIED' },
              { id: 'TX-1091', action: 'FUND_TOPUP', user: 'Tran Duc', amount: '10,000,000 VND', status: 'VERIFIED' },
              { id: 'TX-1090', action: 'EXPENSE_RECORDED', user: 'System', amount: '4,200,000 VND', status: 'COMMITTED' },
            ].map((log, idx) => (
              <group key={log.id} position={[0, 0.15 - idx * 0.18, 0]}>
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[2.5, 0.14]} />
                  <meshBasicMaterial color="#071526" transparent opacity={0.6} />
                </mesh>
                <Text position={[-1.15, 0, 0.01]} fontSize={0.06} color="#8a94a6" anchorX="left" font="/fonts/JetBrainsMono-Bold.ttf">
                  {log.id}
                </Text>
                <Text position={[-0.6, 0, 0.01]} fontSize={0.06} color="#ffffff" anchorX="left">
                  {log.action}
                </Text>
                <Text position={[0.4, 0, 0.01]} fontSize={0.06} color="#00e5ff" anchorX="left" font="/fonts/JetBrainsMono-Bold.ttf">
                  {log.amount}
                </Text>
                <Text position={[1.15, 0, 0.01]} fontSize={0.055} color="#00e676" anchorX="right">
                  {log.status}
                </Text>
              </group>
            ))}
          </group>
        )}
      </group>
    </group>
  );
};
