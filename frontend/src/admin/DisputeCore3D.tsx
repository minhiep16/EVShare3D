import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAdminStore } from './useAdminStore';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';

export const DisputeCore3D: React.FC = () => {
  const {
    disputes,
    userRole,
    enforceSummaryArbitration,
    issueCompensatoryCredit,
    isExecuting,
  } = useAdminStore();

  const crystalRef = useRef<THREE.Mesh>(null);
  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  useFrame(({ clock }) => {
    if (crystalRef.current) {
      const t = clock.getElapsedTime();
      crystalRef.current.rotation.y = t * 0.9;
      crystalRef.current.position.y = 2.2 + Math.sin(t * 1.6) * 0.08;
    }
  });

  const config = ADMIN_CORES_CONFIG.DISPUTE_CORE;
  const isAuthorized = userRole === 'ROLE_ADMIN';
  const activeDispute = disputes[0];

  return (
    <group name="DisputeCoreStation" position={config.relativePosition}>
      {/* Title */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.22}
        color={config.primaryColor}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        ARBITRATION DOCKET & SUMMARY CORE
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.11}
        color={COMMAND_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Supreme Summary Judgment • Compensatory Credits • Docket Expeditions
      </Text>

      {/* Floating Crimson Arbitration Crystal */}
      <mesh ref={crystalRef} position={[0, 2.2, 0]}>
        <octahedronGeometry args={[0.48, 0]} />
        <meshStandardMaterial
          color={config.primaryColor}
          emissive={config.primaryColor}
          emissiveIntensity={isAuthorized ? 1.0 : 0.2}
          roughness={0.2}
          metalness={0.9}
        />
      </mesh>

      {/* Terminal Board */}
      <group position={[0, 1.25, 0.4]} rotation={[-0.15, 0, 0]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[2.8, 1.6]} />
          <meshStandardMaterial
            color="#1f0606"
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

        {/* Dispute Docket Data */}
        <group position={[-1.25, 0.55, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.075}
            color={config.primaryColor}
            anchorX="left"
            anchorY="middle"
          >
            DOCKET CASE #{activeDispute.id}: {activeDispute.status}
          </Text>
          <Text
            position={[0, -0.14, 0]}
            fontSize={0.06}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            Parties: {activeDispute.complainant} vs {activeDispute.respondent}
          </Text>
          <Text
            position={[0, -0.28, 0]}
            fontSize={0.062}
            color={
              activeDispute.status === 'RESOLVED' ? '#34d399' : '#fbbf24'
            }
            anchorX="left"
            anchorY="middle"
          >
            Claim: {activeDispute.claimAmount.toLocaleString()} VND • Status:{' '}
            {activeDispute.status}
          </Text>
        </group>

        {/* 3D Action Controls */}
        <group position={[0, -0.25, 0.02]}>
          {/* Action 1: Enforce Summary Arbitration */}
          <group
            position={[0, 0.18, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                enforceSummaryArbitration(
                  activeDispute.id,
                  'FAVOR_COMPLAINANT',
                  activeDispute.claimAmount
                );
            }}
            onPointerOver={() => {
              setHoveredBtn('enforce');
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
                color={hoveredBtn === 'enforce' ? '#991b1b' : '#4c0d0d'}
                emissive={config.primaryColor}
                emissiveIntensity={hoveredBtn === 'enforce' ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.062}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ ⚖ ENFORCE SUMMARY ARBITRATION VERDICT ]
            </Text>
          </group>

          {/* Action 2: Compensatory Credit */}
          <group
            position={[0, -0.1, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                issueCompensatoryCredit(activeDispute.id, 250000);
            }}
            onPointerOver={() => {
              setHoveredBtn('credit');
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
                color={hoveredBtn === 'credit' ? '#78350f' : '#271007'}
                emissive={COMMAND_THEME.accentGold}
                emissiveIntensity={hoveredBtn === 'credit' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.062}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 💸 GRANT 250,000 VND COMPENSATORY CREDIT ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
