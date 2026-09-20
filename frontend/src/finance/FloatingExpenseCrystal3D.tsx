import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import type { ExpenseItemModel } from './financeTypes';
import { CATEGORY_CONFIGS } from './financeTypes';
import { useFinanceStore } from './useFinanceStore';
import { formatCurrencyVND } from '@/i18n';

interface FloatingExpenseCrystal3DProps {
  expense: ExpenseItemModel;
  position: [number, number, number];
}

export const FloatingExpenseCrystal3D: React.FC<FloatingExpenseCrystal3DProps> = ({
  expense,
  position,
}) => {
  const crystalGroupRef = useRef<THREE.Group>(null);
  const coreRef = useRef<THREE.Mesh>(null);
  const [isHovered, setIsHovered] = useState(false);

  const selectedExpenseId = useFinanceStore((state) => state.selectedExpenseId);
  const selectExpense = useFinanceStore((state) => state.selectExpense);
  const isSelected = selectedExpenseId === expense.id;

  const config = CATEGORY_CONFIGS[expense.category] || CATEGORY_CONFIGS.OTHER;

  // Gentle float bob and rotation
  useFrame(({ clock }) => {
    if (crystalGroupRef.current) {
      const t = clock.getElapsedTime() + expense.id * 0.7;
      const bob = Math.sin(t * 1.8) * 0.08;
      crystalGroupRef.current.position.y = position[1] + bob;

      if (!isSelected && coreRef.current) {
        coreRef.current.rotation.y = t * 0.6;
        coreRef.current.rotation.x = Math.sin(t * 0.4) * 0.2;
      }
    }
  });

  // Category geometry
  const renderGeometry = () => {
    switch (expense.category) {
      case 'CHARGING':
        return <octahedronGeometry args={[0.38, 0]} />;
      case 'MAINTENANCE':
        return <dodecahedronGeometry args={[0.36, 0]} />;
      case 'INSURANCE':
        return <icosahedronGeometry args={[0.38, 0]} />;
      case 'CLEANING':
        return <sphereGeometry args={[0.34, 16, 16]} />;
      case 'PARKING':
        return <boxGeometry args={[0.48, 0.48, 0.48]} />;
      default:
        return <octahedronGeometry args={[0.35, 0]} />;
    }
  };

  return (
    <group
      ref={crystalGroupRef}
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        selectExpense(isSelected ? null : expense.id);
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        setIsHovered(true);
      }}
      onPointerOut={(e) => {
        e.stopPropagation();
        setIsHovered(false);
      }}
    >
      {/* 1. Base Float Glow & Radial Shadow */}
      <mesh position={[0, -0.7, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.2, 0.5, 32]} />
        <meshBasicMaterial
          color={config.color}
          transparent
          opacity={isSelected ? 0.8 : isHovered ? 0.5 : 0.2}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 2. Core Polyhedron Crystal */}
      {!isSelected ? (
        <group>
          {/* Main solid crystal */}
          <mesh ref={coreRef} scale={isHovered ? 1.15 : 1.0}>
            {renderGeometry()}
            <meshStandardMaterial
              color={config.color}
              emissive={config.emissive}
              emissiveIntensity={isHovered ? 0.9 : 0.4}
              roughness={0.15}
              metalness={0.85}
              transparent
              opacity={0.92}
            />
          </mesh>

          {/* Wireframe outer shell */}
          <mesh scale={isHovered ? 1.25 : 1.08}>
            {renderGeometry()}
            <meshBasicMaterial
              color={config.color}
              wireframe
              transparent
              opacity={isHovered ? 0.7 : 0.3}
            />
          </mesh>
        </group>
      ) : (
        /* 3. Volumetric Slice Expansion when selected (showing member allocation shards) */
        <group name="VolumetricAllocationSlices">
          {expense.allocations.map((alloc, idx) => {
            const angle = (idx / expense.allocations.length) * Math.PI * 2;
            const spread = 0.55;
            const sx = Math.cos(angle) * spread;
            const sz = Math.sin(angle) * spread;

            return (
              <group key={`shard_${alloc.userId}`} position={[sx, 0, sz]}>
                <mesh scale={[0.45, 0.45, 0.45]}>
                  {renderGeometry()}
                  <meshStandardMaterial
                    color={alloc.isPaid ? '#00e676' : '#ffab00'}
                    emissive={alloc.isPaid ? '#00e676' : '#ffab00'}
                    emissiveIntensity={0.6}
                    roughness={0.2}
                    metalness={0.8}
                  />
                </mesh>

                {/* Shard member share badge */}
                <group position={[0, 0.45, 0]}>
                  <Text
                    fontSize={0.09}
                    color="#ffffff"
                    anchorX="center"
                    anchorY="middle"
                    font="/fonts/Orbitron-Bold.ttf"
                  >
                    {alloc.userName.split(' ')[0]}
                  </Text>
                  <Text
                    position={[0, -0.12, 0]}
                    fontSize={0.075}
                    color={alloc.isPaid ? '#00e676' : '#ffab00'}
                    anchorX="center"
                    anchorY="middle"
                    font="/fonts/JetBrainsMono-Bold.ttf"
                  >
                    {formatCurrencyVND(alloc.allocatedAmountVnd)}
                  </Text>
                  <Text
                    position={[0, -0.22, 0]}
                    fontSize={0.065}
                    color={alloc.isPaid ? '#00e676' : '#ff1744'}
                    anchorX="center"
                    anchorY="middle"
                  >
                    {alloc.isPaid ? 'ĐÃ TRẢ' : 'CÒN NỢ'}
                  </Text>
                </group>
              </group>
            );
          })}
        </group>
      )}

      {/* 4. Floating Holographic Info Badge */}
      <group position={[0, 0.72, 0]}>
        {/* Background plaque */}
        <mesh position={[0, 0, -0.01]}>
          <planeGeometry args={[1.3, 0.45]} />
          <meshBasicMaterial
            color="#040a16"
            transparent
            opacity={0.88}
            side={THREE.DoubleSide}
          />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[1.34, 0.49]} />
          <meshBasicMaterial
            color={isSelected ? '#00e676' : config.color}
            transparent
            opacity={0.6}
            side={THREE.DoubleSide}
          />
        </mesh>

        {/* Title */}
        <Text
          position={[0, 0.12, 0]}
          fontSize={0.09}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
          font="/fonts/Orbitron-Bold.ttf"
        >
          {`${config.icon} ${expense.title.length > 20 ? expense.title.substring(0, 18) + '...' : expense.title}`}
        </Text>

        {/* Amount */}
        <Text
          position={[0, -0.04, 0]}
          fontSize={0.105}
          color={config.color}
          anchorX="center"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          {formatCurrencyVND(expense.amountVnd)}
        </Text>

        {/* Settlement status tag */}
        <Text
          position={[0, -0.16, 0]}
          fontSize={0.068}
          color={expense.isSettled ? '#00e676' : '#ffab00'}
          anchorX="center"
          anchorY="middle"
          font="/fonts/JetBrainsMono-Bold.ttf"
        >
          {expense.isSettled ? '● ĐÃ THANH TOÁN ĐỦ' : isSelected ? '▼ XEM PHÂN BỔ (BẤM ĐÓNG)' : '● CHƯA THANH TOÁN (BẤM XEM)'}
        </Text>
      </group>
    </group>
  );
};
