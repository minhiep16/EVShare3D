import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAdminStore } from './useAdminStore';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';

export const FinanceCore3D: React.FC = () => {
  const {
    treasury,
    userRole,
    injectTreasuryReserve,
    toggleDisbursementFreeze,
    auditVaultLedger,
    isExecuting,
  } = useAdminStore();

  const octahedronRef = useRef<THREE.Mesh>(null);
  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  useFrame(({ clock }) => {
    if (octahedronRef.current) {
      const t = clock.getElapsedTime();
      octahedronRef.current.rotation.y = t * 0.7;
      octahedronRef.current.position.y = 2.2 + Math.sin(t * 1.8) * 0.08;
    }
  });

  const config = ADMIN_CORES_CONFIG.FINANCE_CORE;
  const isAuthorized = userRole === 'ROLE_ADMIN';

  return (
    <group name="FinanceCoreStation" position={config.relativePosition}>
      {/* Title */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.22}
        color={config.primaryColor}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        SHAREDFUND TREASURY & LIQUIDITY CORE
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.11}
        color={COMMAND_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Vault Liquidity Injection • Emergency Outflow Freeze • Ledger Verification
      </Text>

      {/* Levitating Golden Treasury Octahedron */}
      <mesh ref={octahedronRef} position={[0, 2.2, 0]}>
        <octahedronGeometry args={[0.46, 0]} />
        <meshStandardMaterial
          color={config.primaryColor}
          emissive={config.primaryColor}
          emissiveIntensity={isAuthorized ? 0.9 : 0.2}
          roughness={0.2}
          metalness={0.8}
        />
      </mesh>

      {/* Terminal Board */}
      <group position={[0, 1.25, 0.4]} rotation={[-0.15, 0, 0]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[2.8, 1.6]} />
          <meshStandardMaterial
            color="#041a12"
            roughness={0.3}
            metalness={0.9}
            transparent
            opacity={0.94}
          />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[2.84, 1.64]} />
          <meshBasicMaterial
            color={isAuthorized ? config.primaryColor : COMMAND_THEME.alertRed}
            wireframe
          />
        </mesh>

        {/* Treasury Data */}
        <group position={[-1.25, 0.55, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.075}
            color={config.primaryColor}
            anchorX="left"
            anchorY="middle"
          >
            VAULT BALANCE: {treasury.vaultBalance.toLocaleString()} VND
          </Text>
          <Text
            position={[0, -0.14, 0]}
            fontSize={0.062}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            Reserve Liquidity: {treasury.reserveLiquidity.toLocaleString()} VND •
            Claims: {treasury.pendingExpenseClaims}
          </Text>
          <Text
            position={[0, -0.28, 0]}
            fontSize={0.062}
            color={treasury.disbursementsFrozen ? COMMAND_THEME.alertRed : '#34d399'}
            anchorX="left"
            anchorY="middle"
          >
            Disbursements:{' '}
            {treasury.disbursementsFrozen ? '🔒 FROZEN BY ADMIN' : '✓ UNRESTRICTED'} • Hash: {treasury.ledgerHash.slice(0, 14)}...
          </Text>
        </group>

        {/* 3D Action Controls */}
        <group position={[0, -0.25, 0.02]}>
          {/* Action 1: Inject Liquidity */}
          <group
            position={[0, 0.22, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) injectTreasuryReserve(2000000);
            }}
            onPointerOver={() => {
              setHoveredBtn('inject');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[2.5, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'inject' ? '#047857' : '#064e3b'}
                emissive={config.primaryColor}
                emissiveIntensity={hoveredBtn === 'inject' ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.065}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 💰 INJECT LIQUIDITY (+2,000,000 VND) ]
            </Text>
          </group>

          {/* Action 2: Freeze Disbursements */}
          <group
            position={[-0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) toggleDisbursementFreeze();
            }}
            onPointerOver={() => {
              setHoveredBtn('freeze');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'freeze' ? '#7f1d1d' : '#220b12'}
                emissive={COMMAND_THEME.alertRed}
                emissiveIntensity={hoveredBtn === 'freeze' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.052}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {treasury.disbursementsFrozen
                ? '[ 🔓 UNFREEZE ]'
                : '[ 🛑 FREEZE OUTFLOW ]'}
            </Text>
          </group>

          {/* Action 3: Audit Vault */}
          <group
            position={[0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) auditVaultLedger();
            }}
            onPointerOver={() => {
              setHoveredBtn('audit');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'audit' ? '#0f766e' : '#042f2e'}
                emissive="#14b8a6"
                emissiveIntensity={hoveredBtn === 'audit' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.055}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 🔍 AUDIT VAULT ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
