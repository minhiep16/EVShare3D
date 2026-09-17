import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useContractStore } from './useContractStore';
import { CONTRACT_LAYOUT } from './contractLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const SignaturePedestal3D: React.FC = () => {
  const acceptedTerms = useContractStore((state) => state.acceptedTerms);
  const setAcceptedTerms = useContractStore((state) => state.setAcceptedTerms);
  const signingState = useContractStore((state) => state.signingState);
  const executeSignContract = useContractStore((state) => state.executeSignContract);
  const operationResult = useContractStore((state) => state.operationResult);
  const lastSignedHash = useContractStore((state) => state.lastSignedHash);
  const activeContract = useContractStore((state) => state.activeContract);
  const signaturesOverview = useContractStore((state) => state.signaturesOverview);
  const focusCamera = useContractStore((state) => state.focusCamera);

  const [hoveredButton, setHoveredButton] = useState<string | null>(null);
  const laserScannerRef = useRef<THREE.Mesh>(null);

  const isAlreadySigned = Boolean(
    signaturesOverview?.signatures?.some((s) => s.userId === 1) || signingState === 'SIGNED'
  );
  const isDraft = activeContract?.status === 'DRAFT';
  const isActive = activeContract?.status === 'ACTIVE';

  // Micro-animation for the laser biometric sweep line
  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (laserScannerRef.current) {
      if (signingState === 'SCANNING_BIOMETRIC') {
        laserScannerRef.current.position.y = Math.sin(t * 8.0) * 0.22;
        (laserScannerRef.current.material as THREE.MeshBasicMaterial).opacity = 0.9;
      } else {
        laserScannerRef.current.position.y = Math.sin(t * 1.5) * 0.18;
        (laserScannerRef.current.material as THREE.MeshBasicMaterial).opacity = 0.4;
      }
    }
  });

  const isSuccess = isAlreadySigned || signingState === 'SIGNED' || operationResult.status === 'SUCCESS';
  const isProcessing = signingState === 'SCANNING_BIOMETRIC' || operationResult.status === 'PROCESSING';

  return (
    <group
      position={CONTRACT_LAYOUT.signaturePedestalPosition}
      name="SignaturePedestalMaster"
      onClick={(e) => {
        e.stopPropagation();
        focusCamera('SIGNATURE_FOCUS');
      }}
    >
      {/* 1. Heavy Pedestal Base */}
      <mesh position={[0, 0.5, 0]} castShadow receiveShadow>
        <cylinderGeometry args={[0.5, 0.7, 1.0, 16]} />
        <meshStandardMaterial
          color={CONTRACT_LAYOUT.theme.slateDark}
          metalness={0.9}
          roughness={0.2}
        />
      </mesh>

      {/* Glowing Base Collar */}
      <mesh position={[0, 1.01, 0]}>
        <cylinderGeometry args={[0.52, 0.52, 0.04, 16]} />
        <meshBasicMaterial color={isSuccess ? CONTRACT_LAYOUT.theme.sealGreen : CONTRACT_LAYOUT.theme.sapphirePrimary} />
      </mesh>

      {/* 2. Angled Console Top (-25 degrees tilt) */}
      <group position={[0, 1.35, 0]} rotation={[-0.45, 0, 0]}>
        {/* Console Plate */}
        <mesh position={[0, 0, -0.03]} castShadow>
          <boxGeometry args={[2.4, 1.9, 0.06]} />
          <meshStandardMaterial
            color="#08101d"
            metalness={0.9}
            roughness={0.2}
          />
        </mesh>

        {/* Console Glass Deck */}
        <mesh position={[0, 0, 0.01]}>
          <planeGeometry args={[2.3, 1.8]} />
          <meshStandardMaterial
            color="#091526"
            roughness={0.15}
            metalness={0.5}
          />
        </mesh>

        {/* Bezel */}
        <lineSegments position={[0, 0, 0.02]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(2.3, 1.8)]} />
          <lineBasicMaterial color={isSuccess ? CONTRACT_LAYOUT.theme.sealGreen : CONTRACT_LAYOUT.theme.sapphirePrimary} />
        </lineSegments>

        {/* Station Title */}
        <Text
          position={[0, 0.75, 0.03]}
          fontSize={0.12}
          color={CONTRACT_LAYOUT.theme.sapphirePrimary}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          BIOMETRIC SIGNATURE DAIS
        </Text>

        <Text
          position={[0, 0.62, 0.03]}
          fontSize={0.085}
          color={CONTRACT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          CRYPTOGRAPHIC SHA-256 DIGITAL RATIFICATION
        </Text>

        {/* 3. Biometric Scanner Glass Pad */}
        <group position={[0, 0.28, 0.03]}>
          {/* Scanner Pad Plate */}
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[0.9, 0.55]} />
            <meshStandardMaterial
              color="#040c17"
              metalness={0.9}
              roughness={0.1}
            />
          </mesh>

          {/* Border */}
          <lineSegments position={[0, 0, 0.01]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(0.9, 0.55)]} />
            <lineBasicMaterial
              color={
                isSuccess
                  ? CONTRACT_LAYOUT.theme.sealGreen
                  : isProcessing
                    ? CONTRACT_LAYOUT.theme.warningAmber
                    : CONTRACT_LAYOUT.theme.ceruleanNeon
              }
            />
          </lineSegments>

          {/* Laser Sweep Line */}
          <mesh ref={laserScannerRef} position={[0, 0, 0.02]}>
            <planeGeometry args={[0.86, 0.03]} />
            <meshBasicMaterial
              color={isSuccess ? '#00e676' : '#38bdf8'}
              transparent
              opacity={0.6}
            />
          </mesh>

          {/* Biometric Icon / Instructions */}
          <Text
            position={[0, 0, 0.03]}
            fontSize={0.08}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            {isSuccess ? '✓ BIOMETRIC CAPTURED' : isProcessing ? 'SCANNING...' : 'TOUCH PLATE READY'}
          </Text>
        </group>

        {/* 4. Terms Acknowledgment Checkbox Toggle */}
        <group
          position={[0, -0.15, 0.03]}
          onPointerOver={() => setHoveredButton('CHECKBOX')}
          onPointerOut={() => setHoveredButton(null)}
          onClick={(e) => {
            e.stopPropagation();
            setAcceptedTerms(!acceptedTerms);
          }}
        >
          {/* Checkbox Square */}
          <mesh position={[-0.95, 0, 0]}>
            <boxGeometry args={[0.18, 0.18, 0.02]} />
            <meshBasicMaterial
              color={acceptedTerms ? CONTRACT_LAYOUT.theme.sealGreen : '#1e293b'}
            />
          </mesh>

          {acceptedTerms && (
            <Text
              position={[-0.95, 0, 0.02]}
              fontSize={0.12}
              color="#000000"
              anchorX="center"
              anchorY="middle"
            >
              ✓
            </Text>
          )}

          <Text
            position={[-0.78, 0, 0]}
            fontSize={0.085}
            color={acceptedTerms ? '#ffffff' : CONTRACT_LAYOUT.theme.textMuted}
            anchorX="left"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
          >
            I ACKNOWLEDGE & ACCEPT ALL CO-OWNERSHIP COVENANTS
          </Text>
        </group>

        {/* 5. 3D "SIGN DIGITALLY" Action Button */}
        <group
          position={[0, -0.42, 0.03]}
          onPointerOver={() => {
            setHoveredButton('SIGN_BTN');
            AudioEngine.play('UI_HOVER');
          }}
          onPointerOut={() => setHoveredButton(null)}
          onClick={(e) => {
            e.stopPropagation();
            if (!isDraft && !isAlreadySigned && !isActive) {
              executeSignContract();
            }
          }}
        >
          <mesh>
            <planeGeometry args={[1.9, 0.32]} />
            <meshBasicMaterial
              color={
                isAlreadySigned || isActive
                  ? CONTRACT_LAYOUT.theme.sealGreen
                  : isDraft
                    ? '#0284c7'
                    : isProcessing
                      ? '#1e293b'
                      : hoveredButton === 'SIGN_BTN'
                        ? CONTRACT_LAYOUT.theme.sapphirePrimary
                        : '#132a4a'
              }
            />
          </mesh>
          <Text
            fontSize={0.1}
            color={isAlreadySigned || isActive ? '#000000' : '#ffffff'}
            anchorX="center"
            anchorY="middle"
            font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
          >
            {isAlreadySigned
              ? '✓ YOUR SIGNATURE RECORDED'
              : isActive
                ? '✓ CONTRACT FULLY ACTIVE'
                : isDraft
                  ? 'DRAFT MODE • SUBMIT TO SIGN'
                  : isProcessing
                    ? 'TRANSMITTING SIGNATURE...'
                    : 'EXECUTE DIGITAL SIGNATURE'}
          </Text>
        </group>

        {/* 6. Signature Digest / Status Feedback */}
        <group position={[0, -0.72, 0.03]}>
          {isSuccess && lastSignedHash && (
            <>
              <Text
                position={[0, 0.08, 0]}
                fontSize={0.075}
                color={CONTRACT_LAYOUT.theme.sealGreen}
                anchorX="center"
                anchorY="middle"
                font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
              >
                SHA-256 PROVENANCE DIGEST:
              </Text>
              <Text
                position={[0, -0.06, 0]}
                fontSize={0.07}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
                maxWidth={2.1}
                font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
              >
                {lastSignedHash.slice(0, 32)}...
              </Text>
            </>
          )}

          {operationResult.status === 'ERROR' && (
            <Text
              position={[0, 0, 0]}
              fontSize={0.075}
              color={CONTRACT_LAYOUT.theme.dangerRed}
              anchorX="center"
              anchorY="middle"
              maxWidth={2.1}
              font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
            >
              ⚠️ {operationResult.message}
            </Text>
          )}
        </group>
      </group>
    </group>
  );
};
