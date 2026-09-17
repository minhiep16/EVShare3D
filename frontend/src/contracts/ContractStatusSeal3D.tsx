import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useContractStore } from './useContractStore';
import { CONTRACT_LAYOUT } from './contractLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const ContractStatusSeal3D: React.FC = () => {
  const activeContract = useContractStore((state) => state.activeContract);
  const signaturesOverview = useContractStore((state) => state.signaturesOverview);
  const transitionStatus = useContractStore((state) => state.transitionStatus);
  const focusCamera = useContractStore((state) => state.focusCamera);

  const [hoveredButton, setHoveredButton] = useState<string | null>(null);
  const haloRingRef = useRef<THREE.Mesh>(null);
  const emblemGroupRef = useRef<THREE.Group>(null);

  const status = activeContract?.status || 'ACTIVE';
  const totalRequired = signaturesOverview?.totalRequiredSignatures || 4;
  const totalSubmitted = signaturesOverview?.totalSubmittedSignatures || 4;
  const allSigned = signaturesOverview?.allSigned ?? (totalSubmitted >= totalRequired);

  const quorumPercent = Math.min(100, Math.round((totalSubmitted / totalRequired) * 100));

  const isRatified = status === 'ACTIVE' || status === 'SIGNED';
  const sealColor = isRatified
    ? CONTRACT_LAYOUT.theme.sealGreen
    : status === 'PENDING_SIGNATURE'
      ? CONTRACT_LAYOUT.theme.warningAmber
      : CONTRACT_LAYOUT.theme.ceruleanNeon;

  // Floating undulation and rotating ring
  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (haloRingRef.current) {
      haloRingRef.current.rotation.z = t * 0.4;
    }
    if (emblemGroupRef.current) {
      emblemGroupRef.current.position.y = CONTRACT_LAYOUT.statusSealPosition[1] + Math.sin(t * 1.6) * 0.08;
    }
  });

  return (
    <group
      ref={emblemGroupRef}
      position={CONTRACT_LAYOUT.statusSealPosition}
      name="ContractStatusSealMaster"
      onClick={(e) => {
        e.stopPropagation();
        focusCamera('STATUS_FOCUS');
      }}
    >
      {/* 1. Outer Rotating Holographic Halo Ring */}
      <mesh ref={haloRingRef} position={[0, 0, 0]}>
        <ringGeometry args={[1.35, 1.45, 48]} />
        <meshBasicMaterial
          color={sealColor}
          transparent
          opacity={0.65}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 2. Inner Radial Backdrop Disc */}
      <mesh position={[0, 0, -0.02]}>
        <circleGeometry args={[1.3, 48]} />
        <meshBasicMaterial
          color="#060e1c"
          transparent
          opacity={0.88}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 3. Status Emblem Typography */}
      <group position={[0, 0, 0.01]}>
        <Text
          position={[0, 0.72, 0]}
          fontSize={0.11}
          color={CONTRACT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          LEGAL RATIFICATION STATUS
        </Text>

        {/* Large Status Badge */}
        <Text
          position={[0, 0.44, 0]}
          fontSize={0.24}
          color={sealColor}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {status}
        </Text>

        {/* Quorum Progress Bar Indicator */}
        <group position={[0, 0.12, 0]}>
          {/* Track */}
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[1.8, 0.12]} />
            <meshBasicMaterial color="#0e1e36" />
          </mesh>

          {/* Fill */}
          <mesh position={[-0.9 + (1.8 * (quorumPercent / 100)) / 2, 0, 0.01]}>
            <planeGeometry args={[1.8 * (quorumPercent / 100), 0.12]} />
            <meshBasicMaterial color={sealColor} />
          </mesh>
        </group>

        {/* Quorum Metric Text */}
        <Text
          position={[0, -0.1, 0]}
          fontSize={0.11}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          QUORUM: {totalSubmitted}/{totalRequired} SIGNED ({quorumPercent}%)
        </Text>

        {/* Dynamic Signatures & Pending Signers List */}
        <group position={[0, -0.38, 0]}>
          {signaturesOverview && signaturesOverview.signatures.length > 0 ? (
            signaturesOverview.signatures.slice(0, 2).map((sig, idx) => (
              <Text
                key={`sig_${sig.id || idx}`}
                position={[-0.85, 0.08 - idx * 0.14, 0]}
                fontSize={0.08}
                color={CONTRACT_LAYOUT.theme.sealGreen}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                {`✓ ${sig.userFullName} • Ratified (${sig.signatureHash.slice(0, 8)}...)`}
              </Text>
            ))
          ) : (
            <Text
              position={[-0.85, 0.08, 0]}
              fontSize={0.08}
              color={CONTRACT_LAYOUT.theme.textMuted}
              anchorX="left"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
            >
              No co-owner signatures recorded yet.
            </Text>
          )}

          {signaturesOverview && signaturesOverview.pendingSigners.length > 0 ? (
            signaturesOverview.pendingSigners.slice(0, 2).map((p, idx) => (
              <Text
                key={`pen_${p.userId || idx}`}
                position={[-0.85, -0.2 - idx * 0.14, 0]}
                fontSize={0.08}
                color={CONTRACT_LAYOUT.theme.warningAmber}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                {`⏳ ${p.userFullName} (${p.sharePercentage || 20}%) • Pending Signature`}
              </Text>
            ))
          ) : (
            signaturesOverview?.allSigned && (
              <Text
                position={[-0.85, -0.2, 0]}
                fontSize={0.08}
                color={CONTRACT_LAYOUT.theme.sealGreen}
                anchorX="left"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                ✓ All active co-owners have signed.
              </Text>
            )
          )}
        </group>

        {/* Action Button: Transition to Pending Signature if in Draft */}
        {status === 'DRAFT' && (
          <group
            position={[0, -0.85, 0.02]}
            onPointerOver={() => {
              setHoveredButton('SUBMIT_BTN');
              AudioEngine.play('UI_HOVER');
            }}
            onPointerOut={() => setHoveredButton(null)}
            onClick={(e) => {
              e.stopPropagation();
              transitionStatus('PENDING_SIGNATURE', 'Draft submitted for syndicate signatures');
            }}
          >
            <mesh>
              <planeGeometry args={[1.8, 0.26]} />
              <meshBasicMaterial
                color={hoveredButton === 'SUBMIT_BTN' ? '#0284c7' : '#0369a1'}
              />
            </mesh>
            <Text
              fontSize={0.085}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              ⚡ SUBMIT DRAFT FOR SIGNING
            </Text>
          </group>
        )}

        {/* Action Button: Transition to Active if fully signed but not active */}
        {allSigned && status !== 'ACTIVE' && (
          <group
            position={[0, -0.85, 0.02]}
            onPointerOver={() => {
              setHoveredButton('ACTIVATE_BTN');
              AudioEngine.play('UI_HOVER');
            }}
            onPointerOut={() => setHoveredButton(null)}
            onClick={(e) => {
              e.stopPropagation();
              transitionStatus('ACTIVE', 'All 4 co-owners executed cryptographic signatures');
            }}
          >
            <mesh>
              <planeGeometry args={[1.8, 0.28]} />
              <meshBasicMaterial
                color={hoveredButton === 'ACTIVATE_BTN' ? CONTRACT_LAYOUT.theme.sealGreen : '#143828'}
              />
            </mesh>
            <Text
              fontSize={0.095}
              color={hoveredButton === 'ACTIVATE_BTN' ? '#000000' : '#ffffff'}
              anchorX="center"
              anchorY="middle"
              font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
            >
              🔒 RATIFY & ACTIVATE CONTRACT
            </Text>
          </group>
        )}
      </group>
    </group>
  );
};
