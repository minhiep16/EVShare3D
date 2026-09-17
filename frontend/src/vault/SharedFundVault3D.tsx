import React, { useEffect } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { VaultChamberFloor3D } from './VaultChamberFloor3D';
import { VaultLiquidColumn3D } from './VaultLiquidColumn3D';
import { TransactionRibbon3D } from './TransactionRibbon3D';
import { VaultTerminal3D } from './VaultTerminal3D';
import { VaultAuditStela3D } from './VaultAuditStela3D';
import { useVaultStore } from './useVaultStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { VAULT_LAYOUT } from './vaultLayout';

export const SharedFundVault3D: React.FC = () => {
  const fetchVaultData = useVaultStore((state) => state.fetchVaultData);
  const isTransitioning = useCameraStore((state) => state.isTransitioning);
  const currentPos = useCameraStore((state) => state.currentPosition);
  const currentTarget = useCameraStore((state) => state.target);

  // Initialize data hydration and overview camera framing on mount
  useEffect(() => {
    fetchVaultData();

    const overview = VAULT_LAYOUT.cameras.VAULT_OVERVIEW;
    useCameraStore.getState().moveTo(overview.position, overview.target, { duration: 1.2 });
  }, [fetchVaultData]);

  // Smooth lerp camera framing
  useFrame(({ camera }) => {
    if (isTransitioning) {
      camera.position.lerp(new THREE.Vector3(...currentPos), 0.08);
      const targetVec = new THREE.Vector3(...currentTarget);
      camera.lookAt(targetVec);
    }
  });

  return (
    <group name="SharedFundVaultSector">
      {/* 1. Chamber Floor & Heavy Titanium Locking Ring */}
      <VaultChamberFloor3D />

      {/* 2. Liquid Reserve Column with BR-FIN-03 Laser Line & Balance HUD */}
      <VaultLiquidColumn3D />

      {/* 3. Sweeping 3D Holographic Transaction Chrono-Ribbon */}
      <TransactionRibbon3D />

      {/* 4. Master 3D Console Terminal */}
      <VaultTerminal3D />

      {/* 5. Cryptographic Ledger Audit Proof Monolith */}
      <VaultAuditStela3D />
    </group>
  );
};
