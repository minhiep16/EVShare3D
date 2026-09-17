import React, { useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useFinanceStore } from './useFinanceStore';
import { FINANCE_LAYOUT } from './financeLayout';
import type { PaymentMethod } from '@/api/financeApi';

export const PaymentKiosk3D: React.FC = () => {
  const session = useFinanceStore((state) => state.paymentSession);
  const initiatePayment = useFinanceStore((state) => state.initiatePaymentAction);
  const verifyPayment = useFinanceStore((state) => state.verifyPaymentAction);
  const contributeFund = useFinanceStore((state) => state.contributeFundAction);
  const resetSession = useFinanceStore((state) => state.resetPaymentSession);

  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('BANK_TRANSFER');
  const [payMode, setPayMode] = useState<'DUE_SETTLEMENT' | 'VAULT_CONTRIBUTION'>('DUE_SETTLEMENT');
  const [customAmount, setCustomAmount] = useState<number>(5880000);

  const methods: { id: PaymentMethod; label: string; icon: string }[] = [
    { id: 'BANK_TRANSFER', label: 'VIETQR', icon: '🏦' },
    { id: 'E_WALLET', label: 'E-WALLET', icon: '📱' },
    { id: 'CREDIT_CARD', label: 'NFC CARD', icon: '💳' },
    { id: 'MOCK', label: 'DEV SANDBOX', icon: '🧪' },
  ];

  const handleExecute = () => {
    if (payMode === 'VAULT_CONTRIBUTION') {
      contributeFund(customAmount, selectedMethod, 'Syndicate Capital Contribution');
    } else {
      initiatePayment(customAmount, selectedMethod);
    }
  };

  return (
    <group position={FINANCE_LAYOUT.paymentKioskPosition}>
      {/* 1. Kiosk Pedestal Base */}
      <mesh position={[0, 0.45, 0]}>
        <cylinderGeometry args={[0.3, 0.45, 0.9, 24]} />
        <meshStandardMaterial color="#081424" metalness={0.9} roughness={0.2} />
      </mesh>

      {/* Floor Guidance Ring */}
      <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.7, 0.85, 32]} />
        <meshBasicMaterial
          color={
            session.status === 'FAILED'
              ? '#ff1744'
              : session.status === 'COMPLETED'
              ? '#00e676'
              : '#00e5ff'
          }
          transparent
          opacity={0.6}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 2. Angled Master Payment Console Display (-20 deg tilt) */}
      <group position={[0, 1.25, 0]} rotation={[-0.35, 0, 0]}>
        {/* Terminal frame & bevel */}
        <mesh position={[0, 0, -0.02]}>
          <boxGeometry args={[1.65, 1.35, 0.08]} />
          <meshStandardMaterial color="#040b17" metalness={0.9} roughness={0.2} />
        </mesh>
        <mesh position={[0, 0, 0.025]}>
          <planeGeometry args={[1.58, 1.28]} />
          <meshBasicMaterial
            color={
              session.status === 'FAILED'
                ? '#120407'
                : session.status === 'COMPLETED'
                ? '#04140b'
                : '#040d1c'
            }
          />
        </mesh>

        {/* LED Header bar */}
        <mesh position={[0, 0.58, 0.03]}>
          <planeGeometry args={[1.5, 0.04]} />
          <meshBasicMaterial
            color={
              session.status === 'FAILED'
                ? '#ff1744'
                : session.status === 'COMPLETED'
                ? '#00e676'
                : session.status === 'PROCESSING'
                ? '#ffab00'
                : '#00e5ff'
            }
          />
        </mesh>

        {/* Header Title */}
        <Text
          position={[0, 0.5, 0.04]}
          fontSize={0.08}
          color="#00e5ff"
          anchorX="center"
          anchorY="middle"
          font="/fonts/Orbitron-Bold.ttf"
        >
          CONTACTLESS 3D PAYMENT TERMINAL
        </Text>

        {/* Mode Selector: DUE SETTLEMENT vs VAULT TOPUP */}
        {session.status === 'IDLE' && (
          <group position={[0, 0.36, 0.04]}>
            <group
              position={[-0.38, 0, 0]}
              onClick={(e) => {
                e.stopPropagation();
                setPayMode('DUE_SETTLEMENT');
                setCustomAmount(5880000);
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[0.7, 0.16]} />
                <meshBasicMaterial
                  color={payMode === 'DUE_SETTLEMENT' ? '#00e676' : '#081729'}
                  transparent
                  opacity={0.8}
                  side={THREE.DoubleSide}
                />
              </mesh>
              <Text
                position={[0, 0, 0.01]}
                fontSize={0.055}
                color={payMode === 'DUE_SETTLEMENT' ? '#040b17' : '#f0f4fc'}
                anchorX="center"
                anchorY="middle"
                font="/fonts/Orbitron-Bold.ttf"
              >
                SETTLE DUES (5.88M)
              </Text>
            </group>

            <group
              position={[0.38, 0, 0]}
              onClick={(e) => {
                e.stopPropagation();
                setPayMode('VAULT_CONTRIBUTION');
                setCustomAmount(2000000);
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[0.7, 0.16]} />
                <meshBasicMaterial
                  color={payMode === 'VAULT_CONTRIBUTION' ? '#00e676' : '#081729'}
                  transparent
                  opacity={0.8}
                  side={THREE.DoubleSide}
                />
              </mesh>
              <Text
                position={[0, 0, 0.01]}
                fontSize={0.055}
                color={payMode === 'VAULT_CONTRIBUTION' ? '#040b17' : '#f0f4fc'}
                anchorX="center"
                anchorY="middle"
                font="/fonts/Orbitron-Bold.ttf"
              >
                VAULT DEPOSIT (2.0M)
              </Text>
            </group>
          </group>
        )}

        {/* Payment Methods */}
        {session.status === 'IDLE' && (
          <group position={[0, 0.18, 0.04]}>
            {methods.map((m, idx) => {
              const isSelected = selectedMethod === m.id;
              const x = (idx - 1.5) * 0.36;
              return (
                <group
                  key={m.id}
                  position={[x, 0, 0]}
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedMethod(m.id);
                  }}
                >
                  <mesh position={[0, 0, 0]}>
                    <planeGeometry args={[0.34, 0.14]} />
                    <meshBasicMaterial
                      color={isSelected ? '#00e5ff' : '#081729'}
                      transparent
                      opacity={isSelected ? 0.8 : 0.5}
                      side={THREE.DoubleSide}
                    />
                  </mesh>
                  <Text
                    position={[0, 0, 0.01]}
                    fontSize={0.045}
                    color={isSelected ? '#040b17' : '#8a94a6'}
                    anchorX="center"
                    anchorY="middle"
                    font="/fonts/Orbitron-Bold.ttf"
                  >
                    {`${m.icon} ${m.label}`}
                  </Text>
                </group>
              );
            })}
          </group>
        )}

        {/* Active Session Content */}
        {session.status === 'IDLE' && (
          <group position={[0, -0.1, 0.04]}>
            {/* Amount readout */}
            <Text
              fontSize={0.07}
              color="#8a94a6"
              anchorX="center"
              anchorY="middle"
            >
              Amount to authorize:
            </Text>
            <Text
              position={[0, -0.12, 0]}
              fontSize={0.13}
              color="#00e676"
              anchorX="center"
              anchorY="middle"
              font="/fonts/JetBrainsMono-Bold.ttf"
            >
              {`${customAmount.toLocaleString()} VND`}
            </Text>

            {/* Simulated NFC Card Tap Pad */}
            {selectedMethod === 'CREDIT_CARD' ? (
              <group
                position={[0, -0.32, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  handleExecute();
                }}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[0.9, 0.18]} />
                  <meshBasicMaterial color="#00e5ff" transparent opacity={0.3} side={THREE.DoubleSide} />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.07}
                  color="#00e5ff"
                  anchorX="center"
                  anchorY="middle"
                  font="/fonts/Orbitron-Bold.ttf"
                >
                  ((💳)) TAP NFC CARD HERE
                </Text>
              </group>
            ) : (
              <group
                position={[0, -0.32, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  handleExecute();
                }}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[1.05, 0.18]} />
                  <meshBasicMaterial color="#00e676" transparent opacity={0.35} side={THREE.DoubleSide} />
                </mesh>
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={0.07}
                  color="#00e676"
                  anchorX="center"
                  anchorY="middle"
                  font="/fonts/Orbitron-Bold.ttf"
                >
                  ⚡ TRANSMIT TO BACKEND
                </Text>
              </group>
            )}
          </group>
        )}

        {/* PROCESSING STATE */}
        {session.status === 'PROCESSING' && (
          <group position={[0, -0.05, 0.04]}>
            <Text
              fontSize={0.09}
              color="#ffab00"
              anchorX="center"
              anchorY="middle"
              font="/fonts/Orbitron-Bold.ttf"
            >
              CONNECTING TO SETTLEMENT ENGINE...
            </Text>
            <Text
              position={[0, -0.15, 0]}
              fontSize={0.065}
              color="#8a94a6"
              anchorX="center"
              anchorY="middle"
            >
              Contacting Spring Boot backend (:8080) for real transaction ledger verification
            </Text>
          </group>
        )}

        {/* AWAITING SETTLEMENT (VietQR) */}
        {session.status === 'AWAITING_SETTLEMENT' && (
          <group position={[0, -0.05, 0.04]}>
            <Text
              fontSize={0.075}
              color="#00e5ff"
              anchorX="center"
              anchorY="middle"
              font="/fonts/Orbitron-Bold.ttf"
            >
              VIETQR SETTLEMENT READY
            </Text>
            <Text
              position={[0, -0.12, 0]}
              fontSize={0.08}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
              font="/fonts/JetBrainsMono-Bold.ttf"
            >
              {`REF: ${session.reference}`}
            </Text>
            <Text
              position={[0, -0.22, 0]}
              fontSize={0.06}
              color="#8a94a6"
              anchorX="center"
              anchorY="middle"
            >
              {session.providerInstructions || 'Scan with Banking App to clear transaction'}
            </Text>

            <group
              position={[0, -0.38, 0]}
              onClick={(e) => {
                e.stopPropagation();
                verifyPayment();
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[1.1, 0.16]} />
                <meshBasicMaterial color="#00e676" transparent opacity={0.4} side={THREE.DoubleSide} />
              </mesh>
              <Text
                position={[0, 0, 0.01]}
                fontSize={0.065}
                color="#00e676"
                anchorX="center"
                anchorY="middle"
                font="/fonts/Orbitron-Bold.ttf"
              >
                ✓ VERIFY SETTLEMENT RESULT
              </Text>
            </group>
          </group>
        )}

        {/* STRICT NO FAKE SUCCESS: FAILED STATE */}
        {session.status === 'FAILED' && (
          <group position={[0, -0.05, 0.04]}>
            <Text
              fontSize={0.09}
              color="#ff1744"
              anchorX="center"
              anchorY="middle"
              font="/fonts/Orbitron-Bold.ttf"
            >
              TRANSACTION FAILED / REJECTED
            </Text>
            <Text
              position={[0, -0.14, 0]}
              fontSize={0.065}
              color="#ff8a80"
              anchorX="center"
              anchorY="middle"
              maxWidth={1.4}
            >
              {session.errorMessage || 'Settlement declined by real financial backend.'}
            </Text>
            <Text
              position={[0, -0.26, 0]}
              fontSize={0.06}
              color="#8a94a6"
              anchorX="center"
              anchorY="middle"
            >
              No fake success permitted. Ledger preserved without mutation.
            </Text>

            <group
              position={[0, -0.38, 0]}
              onClick={(e) => {
                e.stopPropagation();
                resetSession();
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[0.9, 0.16]} />
                <meshBasicMaterial color="#ff1744" transparent opacity={0.3} side={THREE.DoubleSide} />
              </mesh>
              <Text
                position={[0, 0, 0.01]}
                fontSize={0.065}
                color="#ff1744"
                anchorX="center"
                anchorY="middle"
                font="/fonts/Orbitron-Bold.ttf"
              >
                ↻ RETRY PAYMENT
              </Text>
            </group>
          </group>
        )}

        {/* COMPLETED SUCCESS STATE */}
        {session.status === 'COMPLETED' && (
          <group position={[0, -0.05, 0.04]}>
            <Text
              fontSize={0.09}
              color="#00e676"
              anchorX="center"
              anchorY="middle"
              font="/fonts/Orbitron-Bold.ttf"
            >
              SETTLEMENT VERIFIED &amp; CONFIRMED
            </Text>
            <Text
              position={[0, -0.12, 0]}
              fontSize={0.075}
              color="#f0f4fc"
              anchorX="center"
              anchorY="middle"
              font="/fonts/JetBrainsMono-Bold.ttf"
            >
              {session.receiptId || 'REC-VERIFIED-ESCROW'}
            </Text>
            <Text
              position={[0, -0.22, 0]}
              fontSize={0.065}
              color="#8a94a6"
              anchorX="center"
              anchorY="middle"
            >
              {`Authorized: ${session.amountVnd.toLocaleString()} VND via ${session.method}`}
            </Text>

            <group
              position={[0, -0.38, 0]}
              onClick={(e) => {
                e.stopPropagation();
                resetSession();
              }}
            >
              <mesh position={[0, 0, 0]}>
                <planeGeometry args={[0.9, 0.16]} />
                <meshBasicMaterial color="#00e676" transparent opacity={0.3} side={THREE.DoubleSide} />
              </mesh>
              <Text
                position={[0, 0, 0.01]}
                fontSize={0.065}
                color="#00e676"
                anchorX="center"
                anchorY="middle"
                font="/fonts/Orbitron-Bold.ttf"
              >
                + NEW TRANSACTION
              </Text>
            </group>
          </group>
        )}
      </group>
    </group>
  );
};
