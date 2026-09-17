import React, { useEffect } from 'react';
import * as THREE from 'three';
import { ContractRoomFloor3D } from './ContractRoomFloor3D';
import { ContractDocument3D } from './ContractDocument3D';
import { SignaturePedestal3D } from './SignaturePedestal3D';
import { ContractVersionStela3D } from './ContractVersionStela3D';
import { ContractStatusSeal3D } from './ContractStatusSeal3D';
import { useContractStore } from './useContractStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { CONTRACT_LAYOUT } from './contractLayout';

export const DigitalContractRoom3D: React.FC = () => {
  const fetchContractData = useContractStore((state) => state.fetchContractData);
  const isTransitioning = useCameraStore((state) => state.isTransitioning);
  const currentPos = useCameraStore((state) => state.currentPosition);
  const currentTarget = useCameraStore((state) => state.target);

  // Initialize backend data and camera overview on mount
  useEffect(() => {
    fetchContractData();
    const overview = CONTRACT_LAYOUT.cameras.ROOM_OVERVIEW;
    useCameraStore.getState().moveTo(overview.position, overview.target, { immediate: true });
  }, [fetchContractData]);

  return (
    <group name="DigitalContractRoomSector">
      {/* 1. Executive Slate Foundation Disc & Lighting Pylons */}
      <ContractRoomFloor3D />

      {/* 2. Central 3D Holographic Document Lectern */}
      <ContractDocument3D />

      {/* 3. Biometric Touch Plate & Cryptographic Signature Dais */}
      <SignaturePedestal3D />

      {/* 4. Chronological Version History Stela */}
      <ContractVersionStela3D />

      {/* 5. Floating Holographic Legal Status & Quorum Seal */}
      <ContractStatusSeal3D />
    </group>
  );
};
