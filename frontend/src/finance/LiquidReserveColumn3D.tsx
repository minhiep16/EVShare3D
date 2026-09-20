import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useFinanceStore } from './useFinanceStore';
import { FINANCE_LAYOUT } from './financeLayout';
import { formatCurrencyVND } from '@/i18n';

export const LiquidReserveColumn3D: React.FC = () => {
  const sharedFund = useFinanceStore((state) => state.sharedFund);
  const beaconRef = useRef<THREE.Mesh>(null);
  const liquidRef = useRef<THREE.Mesh>(null);

  // Scale parameters
  const maxCapacityVnd = 120000000; // 120M VND max visual column
  const columnTotalHeight = 3.6;
  const radius = 0.85;

  const balanceRatio = Math.max(0.05, Math.min(1.0, sharedFund.currentBalance / maxCapacityVnd));
  const liquidHeight = columnTotalHeight * balanceRatio;
  const reserveRatio = Math.max(0.05, Math.min(1.0, sharedFund.minimumReserve / maxCapacityVnd));
  const reserveLineHeight = columnTotalHeight * reserveRatio;

  const isLow = sharedFund.currentBalance < sharedFund.minimumReserve;

  // Animate warning beacon if low liquidity
  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (beaconRef.current) {
      if (isLow) {
        beaconRef.current.rotation.y = t * 4.0;
        const pulse = 0.5 + Math.sin(t * 8) * 0.5;
        (beaconRef.current.material as THREE.MeshBasicMaterial).opacity = pulse;
      } else {
        beaconRef.current.rotation.y = t * 0.5;
        (beaconRef.current.material as THREE.MeshBasicMaterial).opacity = 0.3;
      }
    }

    if (liquidRef.current) {
      // Subtle liquid wave surface
      liquidRef.current.rotation.y = t * 0.2;
    }
  });

  return (
    <group position={FINANCE_LAYOUT.liquidReservePosition}>
      {/* 1. Heavy Titanium Base */}
      <mesh position={[0, 0.2, 0]}>
        <cylinderGeometry args={[radius + 0.35, radius + 0.45, 0.4, 32]} />
        <meshStandardMaterial color="#091424" metalness={0.9} roughness={0.2} />
      </mesh>
      <mesh position={[0, 0.41, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[radius, radius + 0.3, 32]} />
        <meshBasicMaterial
          color={isLow ? '#ff1744' : '#00e676'}
          transparent
          opacity={0.6}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 2. Transparent Outer Glass Cylinder */}
      <mesh position={[0, 0.4 + columnTotalHeight / 2, 0]}>
        <cylinderGeometry args={[radius, radius, columnTotalHeight, 32, 1, true]} />
        <meshPhysicalMaterial
          color="#00e5ff"
          transmission={0.9}
          opacity={0.35}
          transparent
          roughness={0.05}
          ior={1.4}
          thickness={0.2}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 3. Glowing Internal Liquid Fluid Column */}
      <mesh
        ref={liquidRef}
        position={[0, 0.4 + liquidHeight / 2, 0]}
      >
        <cylinderGeometry args={[radius - 0.05, radius - 0.05, liquidHeight, 24]} />
        <meshStandardMaterial
          color={isLow ? '#ff1744' : '#00e676'}
          emissive={isLow ? '#ff1744' : '#00e676'}
          emissiveIntensity={0.6}
          roughness={0.1}
          metalness={0.8}
          transparent
          opacity={0.85}
        />
      </mesh>

      {/* 4. Minimum Safety Reserve Horizontal Guideline (BR-FIN-03) */}
      <group position={[0, 0.4 + reserveLineHeight, 0]}>
        <mesh rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[radius - 0.02, radius + 0.12, 32]} />
          <meshBasicMaterial color="#ffab00" transparent opacity={0.85} side={THREE.DoubleSide} />
        </mesh>
        <Text
          position={[radius + 0.35, 0, 0]}
          fontSize={0.08}
          color="#ffab00"
          anchorX="left"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          {`◄ HẠN MỨC DỰ PHÒNG TỐI THIỂU (${(sharedFund.minimumReserve / 1000000).toFixed(0)}Tr - BR-FIN-03)`}
        </Text>
      </group>

      {/* 5. Titanium Top Cap */}
      <mesh position={[0, 0.4 + columnTotalHeight + 0.15, 0]}>
        <cylinderGeometry args={[radius + 0.35, radius + 0.15, 0.3, 32]} />
        <meshStandardMaterial color="#091424" metalness={0.9} roughness={0.2} />
      </mesh>

      {/* 6. Liquid Health Alarm Beacon */}
      <mesh
        ref={beaconRef}
        position={[0, 0.4 + columnTotalHeight + 0.42, 0]}
      >
        <octahedronGeometry args={[0.22, 0]} />
        <meshBasicMaterial
          color={isLow ? '#ff1744' : '#00e676'}
          wireframe
          transparent
          opacity={0.5}
        />
      </mesh>

      {/* 7. Floating Readout Stela Beside Column */}
      <group position={[-1.6, 2.5, 0.3]}>
        <mesh position={[0, 0, -0.01]}>
          <planeGeometry args={[2.0, 1.4]} />
          <meshBasicMaterial color="#030914" transparent opacity={0.85} side={THREE.DoubleSide} />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[2.04, 1.44]} />
          <meshBasicMaterial
            color={isLow ? '#ff1744' : '#00e676'}
            transparent
            opacity={0.5}
            side={THREE.DoubleSide}
          />
        </mesh>

        <Text
          position={[0, 0.52, 0]}
          fontSize={0.105}
          color="#00e5ff"
          anchorX="center"
          anchorY="middle"
          font="/fonts/Orbitron-Bold.ttf"
        >
          KHO QUỸ DỰ PHÒNG CHUNG
        </Text>

        <Text
          position={[0, 0.28, 0]}
          fontSize={0.14}
          color={isLow ? '#ff1744' : '#00e676'}
          anchorX="center"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          {formatCurrencyVND(sharedFund.currentBalance)}
        </Text>

        <Text
          position={[0, 0.08, 0]}
          fontSize={0.08}
          color="#8a94a6"
          anchorX="center"
          anchorY="middle"
        >
          Tỷ lệ thanh khoản dự phòng:
        </Text>

        <Text
          position={[0, -0.1, 0]}
          fontSize={0.11}
          color={isLow ? '#ff1744' : '#00e676'}
          anchorX="center"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          {`${Math.round((sharedFund.currentBalance / sharedFund.minimumReserve) * 100)}%`}
        </Text>

        <Text
          position={[0, -0.32, 0]}
          fontSize={0.075}
          color={isLow ? '#ff1744' : '#00e676'}
          anchorX="center"
          anchorY="middle"
          font="/fonts/Orbitron-Bold.ttf"
        >
          {isLow ? '⚠️ CẢNH BÁO THÂM HỤT QUỸ' : '✓ THANH KHOẢN AN TOÀN'}
        </Text>

        {isLow && (
          <Text
            position={[0, -0.5, 0]}
            fontSize={0.065}
            color="#ff1744"
            anchorX="center"
            anchorY="middle"
          >
            {`Thâm hụt: ${formatCurrencyVND(sharedFund.safetyDeficitAmount)}`}
          </Text>
        )}
      </group>
    </group>
  );
};
