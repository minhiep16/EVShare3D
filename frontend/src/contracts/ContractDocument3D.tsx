import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useContractStore, getActiveContractSections } from './useContractStore';
import { CONTRACT_LAYOUT } from './contractLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const ContractDocument3D: React.FC = () => {
  const activeContract = useContractStore((state) => state.activeContract);
  const currentSectionIndex = useContractStore((state) => state.currentSectionIndex);
  const setSectionIndex = useContractStore((state) => state.setSectionIndex);
  const nextSection = useContractStore((state) => state.nextSection);
  const prevSection = useContractStore((state) => state.prevSection);
  const transitionStatus = useContractStore((state) => state.transitionStatus);
  const focusCamera = useContractStore((state) => state.focusCamera);

  const [hoveredButton, setHoveredButton] = useState<string | null>(null);

  const sections = getActiveContractSections(activeContract);
  const section = sections[currentSectionIndex] || sections[0];
  const totalSections = sections.length;

  const versionText = activeContract ? `v${activeContract.version}.0` : 'v2.0';
  const contractTitle = activeContract?.contractTitle || 'Co-Ownership Master Agreement';
  const status = activeContract?.status || 'ACTIVE';

  const isRatified = status === 'ACTIVE' || status === 'SIGNED';
  const statusColor = isRatified
    ? CONTRACT_LAYOUT.theme.sealGreen
    : status === 'PENDING_SIGNATURE'
      ? CONTRACT_LAYOUT.theme.warningAmber
      : status === 'TERMINATED'
        ? '#ef4444'
        : CONTRACT_LAYOUT.theme.ceruleanNeon;

  return (
    <group position={CONTRACT_LAYOUT.documentLecternPosition} name="DocumentLecternMaster">
      {/* 1. Ergonomic Titanium Stand */}
      <mesh position={[0, 0.55, 0]} castShadow receiveShadow>
        <cylinderGeometry args={[0.35, 0.55, 1.1, 16]} />
        <meshStandardMaterial
          color={CONTRACT_LAYOUT.theme.slateDark}
          metalness={0.9}
          roughness={0.2}
        />
      </mesh>

      {/* Illuminated Base Collar */}
      <mesh position={[0, 1.08, 0]}>
        <cylinderGeometry args={[0.38, 0.38, 0.05, 16]} />
        <meshBasicMaterial color={CONTRACT_LAYOUT.theme.sapphirePrimary} />
      </mesh>

      {/* 2. Angled Holographic Document Tablet (-30 degrees tilt) */}
      <group
        position={[0, 1.5, 0]}
        rotation={[-0.52, 0, 0]}
        onClick={(e) => {
          e.stopPropagation();
          focusCamera('DOCUMENT_FOCUS');
        }}
      >
        {/* Physical Tablet Backplate */}
        <mesh position={[0, 0, -0.04]} castShadow>
          <boxGeometry args={[4.2, 2.7, 0.08]} />
          <meshStandardMaterial
            color="#070d1a"
            metalness={0.92}
            roughness={0.18}
          />
        </mesh>

        {/* Sapphire Glass Document Surface */}
        <mesh position={[0, 0, 0.01]}>
          <planeGeometry args={[4.05, 2.55]} />
          <meshStandardMaterial
            color="#081426"
            roughness={0.15}
            metalness={0.6}
          />
        </mesh>

        {/* Outer Laser Bezel */}
        <lineSegments position={[0, 0, 0.02]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(4.05, 2.55)]} />
          <lineBasicMaterial color={statusColor} />
        </lineSegments>

        {/* Cryptographic Watermark in Background */}
        <Text
          position={[0, 0, 0.02]}
          fontSize={0.26}
          color={
            status === 'DRAFT'
              ? 'rgba(56, 189, 248, 0.15)'
              : status === 'PENDING_SIGNATURE'
                ? 'rgba(245, 158, 11, 0.18)'
                : status === 'TERMINATED'
                  ? 'rgba(239, 68, 68, 0.18)'
                  : CONTRACT_LAYOUT.theme.watermarkCyan
          }
          anchorX="center"
          anchorY="middle"
          rotation={[0, 0, 0.25]}
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {status === 'DRAFT'
            ? 'DRAFT AMENDMENT • UNRATIFIED'
            : status === 'PENDING_SIGNATURE'
              ? 'PENDING SIGNATURE • MULTI-SIG'
              : status === 'TERMINATED'
                ? 'TERMINATED • SUPERSEDED'
                : 'SHA-256 VERIFIED • ACTIVE TRUST'}
        </Text>

        {/* 3. Document Header Bar */}
        <group position={[0, 1.08, 0.03]}>
          {/* Main Title */}
          <Text
            position={[-1.9, 0, 0]}
            fontSize={0.14}
            color={CONTRACT_LAYOUT.theme.platinumLegal}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {contractTitle.toUpperCase()}
          </Text>

          {/* Version & Status Badge */}
          <group position={[1.45, 0, 0]}>
            <mesh>
              <planeGeometry args={[1.05, 0.24]} />
              <meshBasicMaterial color="#0c1a30" />
            </mesh>
            <Text
              fontSize={0.095}
              color={statusColor}
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              [{versionText} • {status}]
            </Text>
          </group>
        </group>

        {/* Dynamic Lifecycle Quick Action Banner */}
        {status === 'DRAFT' && (
          <group
            position={[0, 0.94, 0.035]}
            onPointerOver={() => {
              setHoveredButton('SUBMIT_DRAFT_BTN');
              AudioEngine.play('UI_HOVER');
            }}
            onPointerOut={() => setHoveredButton(null)}
            onClick={(e) => {
              e.stopPropagation();
              transitionStatus('PENDING_SIGNATURE', 'Draft submitted for co-owner signature');
            }}
          >
            <mesh>
              <planeGeometry args={[3.9, 0.16]} />
              <meshBasicMaterial color={hoveredButton === 'SUBMIT_DRAFT_BTN' ? '#0284c7' : '#0369a1'} />
            </mesh>
            <Text
              fontSize={0.08}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              ⚡ SUBMIT DRAFT FOR CO-OWNER SIGNATURES ▶
            </Text>
          </group>
        )}

        {status === 'SIGNED' && (
          <group
            position={[0, 0.94, 0.035]}
            onPointerOver={() => {
              setHoveredButton('ACTIVATE_CONTRACT_BTN');
              AudioEngine.play('UI_HOVER');
            }}
            onPointerOut={() => setHoveredButton(null)}
            onClick={(e) => {
              e.stopPropagation();
              transitionStatus('ACTIVE', 'Multi-signature quorum finalized');
            }}
          >
            <mesh>
              <planeGeometry args={[3.9, 0.16]} />
              <meshBasicMaterial color={hoveredButton === 'ACTIVATE_CONTRACT_BTN' ? '#16a34a' : '#15803d'} />
            </mesh>
            <Text
              fontSize={0.08}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              🔒 RATIFY & ACTIVATE CONTRACT ON LEDGER ▶
            </Text>
          </group>
        )}

        {/* 4. Section Quick Jump Strip */}
        <group position={[0, status === 'DRAFT' || status === 'SIGNED' ? 0.78 : 0.84, 0.03]}>
          {sections.map((sec, idx) => {
            const chipWidth = 0.74;
            const xPos = -1.56 + idx * (chipWidth + 0.04);
            const isCurrent = currentSectionIndex === idx;
            const isHovered = hoveredButton === `SEC_TAB_${idx}`;

            return (
              <group
                key={sec.id}
                position={[xPos, 0, 0]}
                onPointerOver={() => {
                  setHoveredButton(`SEC_TAB_${idx}`);
                  AudioEngine.play('UI_HOVER');
                }}
                onPointerOut={() => setHoveredButton(null)}
                onClick={(e) => {
                  e.stopPropagation();
                  setSectionIndex(idx);
                }}
              >
                <mesh>
                  <planeGeometry args={[chipWidth, 0.2]} />
                  <meshBasicMaterial
                    color={
                      isCurrent
                        ? CONTRACT_LAYOUT.theme.sapphirePrimary
                        : isHovered
                          ? '#1e3355'
                          : '#0d1d36'
                    }
                  />
                </mesh>
                <Text
                  fontSize={0.1}
                  color={isCurrent ? '#ffffff' : '#94a3b8'}
                  anchorX="center"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  {sec.sectionNumber}
                </Text>
              </group>
            );
          })}
        </group>

        {/* 5. Main Section Body Content */}
        <group position={[0, -0.05, 0.03]}>
          {/* Section Heading */}
          <Text
            position={[-1.9, 0.62, 0]}
            fontSize={0.16}
            color={CONTRACT_LAYOUT.theme.ceruleanNeon}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {section.sectionNumber}: {section.title}
          </Text>

          {/* Subtitle */}
          <Text
            position={[-1.9, 0.44, 0]}
            fontSize={0.11}
            color={CONTRACT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            {section.subtitle}
          </Text>

          {/* Content Narrative Box */}
          <Text
            position={[-1.9, 0.18, 0]}
            fontSize={0.12}
            color={CONTRACT_LAYOUT.theme.platinumLegal}
            anchorX="left"
            anchorY="top"
            maxWidth={3.8}
            lineHeight={1.4}
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            {section.content}
          </Text>

          {/* Key Clauses Bullet Points */}
          <group position={[-1.9, -0.32, 0]}>
            {section.keyClauses.map((clause, cIdx) => (
              <Text
                key={cIdx}
                position={[0, -cIdx * 0.18, 0]}
                fontSize={0.105}
                color={cIdx === 0 ? CONTRACT_LAYOUT.theme.ceruleanNeon : CONTRACT_LAYOUT.theme.platinumLegal}
                anchorX="left"
                anchorY="top"
                maxWidth={3.8}
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                ▸ {clause}
              </Text>
            ))}
          </group>
        </group>

        {/* 6. Document Footer Pagination Strip */}
        <group position={[0, -1.02, 0.03]}>
          {/* Previous Page Button */}
          <group
            position={[-1.3, 0, 0]}
            onPointerOver={() => {
              setHoveredButton('PREV_SEC');
              AudioEngine.play('UI_HOVER');
            }}
            onPointerOut={() => setHoveredButton(null)}
            onClick={(e) => {
              e.stopPropagation();
              prevSection();
            }}
          >
            <mesh>
              <planeGeometry args={[1.1, 0.28]} />
              <meshBasicMaterial
                color={
                  currentSectionIndex === 0
                    ? '#0b1626'
                    : hoveredButton === 'PREV_SEC'
                      ? CONTRACT_LAYOUT.theme.sapphirePrimary
                      : '#132845'
                }
              />
            </mesh>
            <Text
              fontSize={0.11}
              color={currentSectionIndex === 0 ? '#475569' : '#ffffff'}
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              ◀ PREV SECTION
            </Text>
          </group>

          {/* Current Page Counter */}
          <Text
            position={[0, 0, 0]}
            fontSize={0.13}
            color={CONTRACT_LAYOUT.theme.ceruleanNeon}
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            SECTION {currentSectionIndex + 1} OF {totalSections}
          </Text>

          {/* Next Page Button */}
          <group
            position={[1.3, 0, 0]}
            onPointerOver={() => {
              setHoveredButton('NEXT_SEC');
              AudioEngine.play('UI_HOVER');
            }}
            onPointerOut={() => setHoveredButton(null)}
            onClick={(e) => {
              e.stopPropagation();
              nextSection();
            }}
          >
            <mesh>
              <planeGeometry args={[1.1, 0.28]} />
              <meshBasicMaterial
                color={
                  currentSectionIndex === totalSections - 1
                    ? '#0b1626'
                    : hoveredButton === 'NEXT_SEC'
                      ? CONTRACT_LAYOUT.theme.sapphirePrimary
                      : '#132845'
                }
              />
            </mesh>
            <Text
              fontSize={0.11}
              color={currentSectionIndex === totalSections - 1 ? '#475569' : '#ffffff'}
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              NEXT SECTION ▶
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
