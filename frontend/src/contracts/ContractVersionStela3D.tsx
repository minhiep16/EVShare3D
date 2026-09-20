import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useContractStore } from './useContractStore';
import { CONTRACT_LAYOUT } from './contractLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import { formatDateVN, formatStatusVN } from '@/i18n';

export const ContractVersionStela3D: React.FC = () => {
  const contractVersions = useContractStore((state) => state.contractVersions);
  const activeContract = useContractStore((state) => state.activeContract);
  const selectVersion = useContractStore((state) => state.selectVersion);
  const createContractDraft = useContractStore((state) => state.createContractDraft);
  const focusCamera = useContractStore((state) => state.focusCamera);

  const [hoveredVersionId, setHoveredVersionId] = useState<number | null>(null);
  const [hoveredDraftBtn, setHoveredDraftBtn] = useState<boolean>(false);

  const versions = contractVersions.length > 0 ? contractVersions : [
    {
      id: 1,
      groupId: 1,
      groupName: 'VinFast VF8 Founders Syndicate',
      contractTitle: 'Co-Ownership Master Agreement v2.0',
      contractTermsText: '',
      version: 2,
      status: 'ACTIVE' as const,
      effectiveDate: '2026-09-01',
      createdAt: '2026-08-28T10:00:00Z',
    },
  ];

  return (
    <group
      position={CONTRACT_LAYOUT.versionStelaPosition}
      name="ContractVersionStelaMaster"
      onClick={(e) => {
        e.stopPropagation();
        focusCamera('VERSION_FOCUS');
      }}
    >
      {/* 1. Pedestal Base */}
      <mesh position={[0, 0.2, 0]} castShadow receiveShadow>
        <boxGeometry args={[2.4, 0.4, 1.2]} />
        <meshStandardMaterial
          color={CONTRACT_LAYOUT.theme.slateDark}
          metalness={0.9}
          roughness={0.2}
        />
      </mesh>

      {/* 2. Upright Monolith Column */}
      <mesh position={[0, 1.8, 0]} castShadow receiveShadow>
        <boxGeometry args={[2.1, 2.8, 0.16]} />
        <meshStandardMaterial
          color="#060d1a"
          metalness={0.85}
          roughness={0.15}
        />
      </mesh>

      {/* Frame Trim */}
      <lineSegments position={[0, 1.8, 0.09]}>
        <edgesGeometry args={[new THREE.BoxGeometry(2.1, 2.8, 0.16)]} />
        <lineBasicMaterial color={CONTRACT_LAYOUT.theme.sapphirePrimary} />
      </lineSegments>

      {/* 3. Stela Content */}
      <group position={[0, 1.8, 0.1]}>
        {/* Title */}
        <Text
          position={[0, 1.18, 0]}
          fontSize={0.12}
          color={CONTRACT_LAYOUT.theme.sapphirePrimary}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          CÂY PHIÊN BẢN HỢP ĐỒNG
        </Text>

        <Text
          position={[0, 1.02, 0]}
          fontSize={0.08}
          color={CONTRACT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          LỊCH SỬ PHIÊN BẢN & TU CHÍNH ÁN
        </Text>

        {/* Version Nodes List */}
        <group position={[0, 0.65, 0]}>
          {versions.slice(0, 3).map((v, idx) => {
            const y = -idx * 0.52;
            const isSelected = activeContract?.id === v.id;
            const isHovered = hoveredVersionId === v.id;

            const isCurrentActive = v.status === 'ACTIVE';
            const statusColor = isCurrentActive
              ? CONTRACT_LAYOUT.theme.sealGreen
              : v.status === 'PENDING_SIGNATURE'
                ? CONTRACT_LAYOUT.theme.warningAmber
                : v.status === 'DRAFT'
                  ? CONTRACT_LAYOUT.theme.ceruleanNeon
                  : '#94a3b8';

            return (
              <group
                key={v.id}
                position={[0, y, 0]}
                onPointerOver={() => {
                  setHoveredVersionId(v.id);
                  AudioEngine.play('UI_HOVER');
                }}
                onPointerOut={() => setHoveredVersionId(null)}
                onClick={(e) => {
                  e.stopPropagation();
                  selectVersion(v.id);
                }}
              >
                {/* Node Box */}
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[1.9, 0.44]} />
                  <meshBasicMaterial
                    color={
                      isSelected
                        ? '#0e2444'
                        : isHovered
                          ? '#101e35'
                          : '#091526'
                    }
                  />
                </mesh>

                {/* Node Selection Outline */}
                <lineSegments position={[0, 0, 0.01]}>
                  <edgesGeometry args={[new THREE.PlaneGeometry(1.9, 0.44)]} />
                  <lineBasicMaterial
                    color={isSelected ? CONTRACT_LAYOUT.theme.ceruleanNeon : isHovered ? '#3b82f6' : '#1e293b'}
                  />
                </lineSegments>

                {/* Version Title */}
                <Text
                  position={[-0.85, 0.1, 0.02]}
                  fontSize={0.095}
                  color={isSelected ? '#ffffff' : '#e2e8f0'}
                  anchorX="left"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  PHIÊN BẢN {v.version}.0 {isSelected ? '• [ĐANG CHỌN]' : ''}
                </Text>

                {/* Status Pill */}
                <Text
                  position={[0.85, 0.1, 0.02]}
                  fontSize={0.075}
                  color={statusColor}
                  anchorX="right"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  [{formatStatusVN(v.status)}]
                </Text>

                {/* Effective Date & Details */}
                <Text
                  position={[-0.85, -0.1, 0.02]}
                  fontSize={0.075}
                  color={CONTRACT_LAYOUT.theme.textMuted}
                  anchorX="left"
                  anchorY="middle"
                  maxWidth={1.7}
                  font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                >
                  Hiệu lực: {formatDateVN(v.effectiveDate)} • {v.contractTitle.slice(0, 30)}
                </Text>
              </group>
            );
          })}
        </group>

        {/* Action Button: Draft New Contract Amendment */}
        <group
          position={[0, -1.02, 0.02]}
          onPointerOver={() => {
            setHoveredDraftBtn(true);
            AudioEngine.play('UI_HOVER');
          }}
          onPointerOut={() => setHoveredDraftBtn(false)}
          onClick={(e) => {
            e.stopPropagation();
            createContractDraft();
          }}
        >
          <mesh>
            <planeGeometry args={[1.9, 0.26]} />
            <meshBasicMaterial
              color={hoveredDraftBtn ? CONTRACT_LAYOUT.theme.ceruleanNeon : '#0c274d'}
            />
          </mesh>
          <Text
            fontSize={0.08}
            color={hoveredDraftBtn ? '#020b17' : '#ffffff'}
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            + TẠO BẢN DỰ THẢO MỚI (SPRING BOOT)
          </Text>
        </group>

        {/* Footer Instructions */}
        <Text
          position={[0, -1.22, 0]}
          fontSize={0.07}
          color={CONTRACT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          [ CHỌN PHIÊN BẢN ĐỂ TẢI LÊN BÀN ĐỌC 3D ]
        </Text>
      </group>
    </group>
  );
};
