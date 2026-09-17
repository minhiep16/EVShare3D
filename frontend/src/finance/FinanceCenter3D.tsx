import React, { useEffect } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { FinanceCenterFloor3D } from './FinanceCenterFloor3D';
import { ExpenseCluster3D } from './ExpenseCluster3D';
import { CostAllocationBarGraph3D } from './CostAllocationBarGraph3D';
import { LiquidReserveColumn3D } from './LiquidReserveColumn3D';
import { PaymentKiosk3D } from './PaymentKiosk3D';
import { FinanceTerminal3D } from './FinanceTerminal3D';
import { useFinanceStore } from './useFinanceStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { FINANCE_LAYOUT } from './financeLayout';

export const FinanceCenter3D: React.FC = () => {
  const loadFinanceData = useFinanceStore((state) => state.loadGroupFinanceData);
  const isTransitioning = useCameraStore((state) => state.isTransitioning);
  const currentPos = useCameraStore((state) => state.currentPosition);
  const currentTarget = useCameraStore((state) => state.target);

  // Initialize camera and hydrate backend data on mount
  useEffect(() => {
    loadFinanceData();

    const overview = FINANCE_LAYOUT.cameras.FINANCE_OVERVIEW;
    useCameraStore.getState().moveTo(overview.position, overview.target, { durationSeconds: 1.2 });
  }, [loadFinanceData]);

  // Smooth lerp camera framing
  useFrame(({ camera }) => {
    if (isTransitioning) {
      camera.position.lerp(new THREE.Vector3(...currentPos), 0.08);
      const targetVec = new THREE.Vector3(...currentTarget);
      camera.lookAt(targetVec);
    }
  });

  return (
    <group name="EnergyFinanceCenterSector">
      {/* 1. Surgical Cleanroom Floor */}
      <FinanceCenterFloor3D />

      {/* 2. Floating 3D Expense Crystals Cluster */}
      <ExpenseCluster3D />

      {/* 3. 3D Cost Allocation Bar Graphs */}
      <CostAllocationBarGraph3D />

      {/* 4. Liquid Reserve Glass Column (BR-FIN-03) */}
      <LiquidReserveColumn3D />

      {/* 5. Contactless 3D Payment Kiosk */}
      <PaymentKiosk3D />

      {/* 6. Master Finance Terminal Console */}
      <FinanceTerminal3D />
    </group>
  );
};
