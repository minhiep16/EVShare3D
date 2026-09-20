import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useDisputeStore } from './useDisputeStore';
import { DISPUTE_THEME, DISPUTE_STATIONS } from './disputeLayout';
import type { ArbitrationVerdictChoice } from './disputeTypes';

import { formatCurrencyVND } from '@/i18n';

export const AdminArbitrationDais3D: React.FC = () => {
  const {
    activeDispute,
    userRole,
    adminVerdictChoice,
    adminDeductibleInput,
    setAdminVerdictChoice,
    setAdminDeductibleInput,
    executeAdminArbitration,
    isSubmitting,
  } = useDisputeStore();

  const scalesRef = useRef<THREE.Group>(null);
  const [hoveredAction, setHoveredAction] = useState<string | null>(null);

  const isAdmin = userRole === 'ROLE_ADMIN';
  const daisPos = DISPUTE_STATIONS.ADMIN_DAIS.relativePosition;

  // Gently tilt and rotate the floating scales of justice
  useFrame(({ clock }) => {
    if (scalesRef.current) {
      const t = clock.getElapsedTime();
      scalesRef.current.rotation.y = t * 0.5;
      scalesRef.current.position.y = 2.65 + Math.sin(t * 1.8) * 0.04;
    }
  });

  const VERDICT_OPTIONS: Array<{
    id: ArbitrationVerdictChoice;
    title: string;
    description: string;
  }> = [
    {
      id: 'RESOLVED_COMPLAINANT_FAVORED',
      title: 'CHẤP THUẬN NGUYÊN ĐƠN',
      description: 'Lỗi do bị đơn; khấu trừ tiền bồi thường.',
    },
    {
      id: 'RESOLVED_RESPONDENT_FAVORED',
      title: 'CHẤP THUẬN BỊ ĐƠN',
      description: 'Hao mòn tự nhiên / không trách nhiệm bị đơn.',
    },
    {
      id: 'DISMISSED',
      title: 'BÁC ĐƠN KHIẾU NẠI',
      description: 'Không đủ chứng cứ; bác bỏ toàn bộ yêu cầu.',
    },
  ];

  return (
    <group name="AdminArbitrationDaisStation" position={daisPos}>
      {/* Dais Title */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.21}
        color={DISPUTE_THEME.secondary}
        anchorX="center"
        anchorY="middle"
      >
        BỤC PHÁN QUYẾT TRỌNG TÀI CHUNG THẨM
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.11}
        color={DISPUTE_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Phán quyết thẩm quyền BR-DIS-05 / BR-DIS-06 • Quyết toán quỹ nguyên tử
      </Text>

      {/* Tiered Hexagonal Obsidian Base Platform */}
      <mesh position={[0, 0.2, 0]} receiveShadow castShadow>
        <cylinderGeometry args={[2.8, 3.1, 0.4, 6]} />
        <meshStandardMaterial
          color="#0d0202"
          roughness={0.6}
          metalness={0.8}
        />
      </mesh>

      <mesh position={[0, 0.45, 0]} receiveShadow castShadow>
        <cylinderGeometry args={[2.4, 2.7, 0.3, 6]} />
        <meshStandardMaterial
          color="#160404"
          roughness={0.5}
          metalness={0.9}
        />
      </mesh>

      {/* Golden Altar Ring */}
      <mesh position={[0, 0.61, 0]}>
        <ringGeometry args={[1.5, 2.38, 6]} />
        <meshStandardMaterial
          color={DISPUTE_THEME.secondary}
          emissive={DISPUTE_THEME.secondary}
          emissiveIntensity={0.6}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* Floating Holographic Scales of Justice */}
      <group ref={scalesRef} position={[0, 2.65, 0]}>
        {/* Central Balance Beam */}
        <mesh position={[0, 0, 0]}>
          <cylinderGeometry args={[0.02, 0.02, 1.3, 8]} />
          <meshStandardMaterial
            color={DISPUTE_THEME.secondary}
            emissive={DISPUTE_THEME.secondary}
            emissiveIntensity={0.8}
          />
        </mesh>
        <mesh position={[0, 0.65, 0]}>
          <boxGeometry args={[1.4, 0.03, 0.03]} />
          <meshStandardMaterial
            color={DISPUTE_THEME.secondary}
            emissive={DISPUTE_THEME.secondary}
            emissiveIntensity={0.9}
          />
        </mesh>
        {/* Left Scale Pan */}
        <mesh position={[-0.6, 0.35, 0]}>
          <cylinderGeometry args={[0.2, 0.2, 0.03, 16]} />
          <meshStandardMaterial
            color="#fbbf24"
            wireframe
            emissive="#fbbf24"
            emissiveIntensity={0.5}
          />
        </mesh>
        {/* Right Scale Pan */}
        <mesh position={[0.6, 0.35, 0]}>
          <cylinderGeometry args={[0.2, 0.2, 0.03, 16]} />
          <meshStandardMaterial
            color="#fbbf24"
            wireframe
            emissive="#fbbf24"
            emissiveIntensity={0.5}
          />
        </mesh>
      </group>

      {/* Sovereign Arbitration Terminal Desk */}
      <group position={[0, 1.45, 0]} rotation={[-0.2, 0, 0]}>
        {/* Terminal Screen Backing */}
        <mesh position={[0, 0, -0.02]} receiveShadow>
          <planeGeometry args={[3.2, 1.9]} />
          <meshStandardMaterial
            color="#120303"
            roughness={0.3}
            metalness={0.9}
            transparent
            opacity={0.96}
          />
        </mesh>

        {/* Outer Frame */}
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[3.24, 1.94]} />
          <meshBasicMaterial
            color={isAdmin ? DISPUTE_THEME.secondary : DISPUTE_THEME.alertRed}
            wireframe
          />
        </mesh>

        {/* Security & RBAC Status Banner */}
        <group position={[0, 0.8, 0.02]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[2.9, 0.16]} />
            <meshBasicMaterial color={isAdmin ? '#78350f' : '#7f1d1d'} />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.062}
            color={isAdmin ? '#fde68a' : '#fca5a5'}
            anchorX="center"
            anchorY="middle"
          >
            {isAdmin
              ? '👑 ĐẶC QUYỀN TRỌNG TÀI TỐI CAO ĐANG HOẠT ĐỘNG (ROLE_ADMIN)'
              : `🔒 HẠN CHẾ RBAC: Yêu cầu ROLE_ADMIN để phán quyết (Hiện tại: ${userRole})`}
          </Text>
        </group>

        {/* VERDICT SELECTOR (3 Options) */}
        <group position={[0, 0.45, 0.02]}>
          <Text
            position={[-1.4, 0.15, 0]}
            fontSize={0.07}
            color={DISPUTE_THEME.secondary}
            anchorX="left"
            anchorY="middle"
          >
            LỰA CHỌN PHÁN QUYẾT RÀNG BUỘC CHUNG THẨM:
          </Text>

          {VERDICT_OPTIONS.map((opt, idx) => {
            const isSelected = adminVerdictChoice === opt.id;
            const xOffset = (idx - 1) * 0.96;
            return (
              <group
                key={opt.id}
                position={[xOffset, -0.15, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  if (isAdmin) setAdminVerdictChoice(opt.id);
                }}
                onPointerOver={() => {
                  if (isAdmin) {
                    setHoveredAction(`verdict-${opt.id}`);
                    document.body.style.cursor = 'pointer';
                  }
                }}
                onPointerOut={() => {
                  setHoveredAction(null);
                  document.body.style.cursor = 'auto';
                }}
              >
                <mesh position={[0, 0, 0]}>
                  <planeGeometry args={[0.9, 0.35]} />
                  <meshStandardMaterial
                    color={isSelected ? '#3b1212' : '#1f0808'}
                    roughness={0.4}
                    metalness={0.8}
                  />
                </mesh>
                <mesh position={[0, 0, 0.005]}>
                  <planeGeometry args={[0.92, 0.37]} />
                  <meshBasicMaterial
                    color={
                      isSelected
                        ? DISPUTE_THEME.secondary
                        : hoveredAction === `verdict-${opt.id}`
                        ? DISPUTE_THEME.cyberCyan
                        : '#4b1515'
                    }
                    wireframe
                  />
                </mesh>
                <Text
                  position={[0, 0.06, 0.02]}
                  fontSize={0.052}
                  color={isSelected ? DISPUTE_THEME.secondary : '#ffffff'}
                  anchorX="center"
                  anchorY="middle"
                >
                  {opt.title}
                </Text>
                <Text
                  position={[0, -0.06, 0.02]}
                  fontSize={0.04}
                  color={DISPUTE_THEME.textMuted}
                  anchorX="center"
                  anchorY="middle"
                  maxWidth={0.85}
                  lineHeight={1.1}
                >
                  {opt.description}
                </Text>
              </group>
            );
          })}
        </group>

        {/* FUND ADJUSTMENT CONTROLS */}
        <group position={[-1.4, -0.12, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.07}
            color={DISPUTE_THEME.secondary}
            anchorX="left"
            anchorY="middle"
          >
            ĐIỀU CHỈNH KHẤU TRỪ QUỸ CHUNG (SHAREDFUND):
          </Text>

          <Text
            position={[0, -0.15, 0]}
            fontSize={0.06}
            color="#f8fafc"
            anchorX="left"
            anchorY="middle"
          >
            {`Mức khấu trừ xác định: ${formatCurrencyVND(adminDeductibleInput)}`}
          </Text>

          {/* Quick preset buttons */}
          {isAdmin && (
            <group position={[0, -0.32, 0]}>
              {[250000, 500000, 1000000].map((amt, idx) => (
                <group
                  key={amt}
                  position={[idx * 0.95 + 0.45, 0, 0]}
                  onClick={(e) => {
                    e.stopPropagation();
                    setAdminDeductibleInput(amt);
                  }}
                  onPointerOver={() => setHoveredAction(`preset-${amt}`)}
                  onPointerOut={() => setHoveredAction(null)}
                >
                  <mesh position={[0, 0, 0]}>
                    <planeGeometry args={[0.85, 0.16]} />
                    <meshBasicMaterial
                      color={
                        adminDeductibleInput === amt
                          ? '#78350f'
                          : hoveredAction === `preset-${amt}`
                          ? '#451a03'
                          : '#200b0b'
                      }
                    />
                  </mesh>
                  <Text
                    position={[0, 0, 0.01]}
                    fontSize={0.048}
                    color="#ffffff"
                    anchorX="center"
                    anchorY="middle"
                  >
                    {formatCurrencyVND(amt)}
                  </Text>
                </group>
              ))}
            </group>
          )}
        </group>

        {/* EXECUTE BINDING ARBITRATION BUTTON */}
        <group position={[0, -0.72, 0.02]}>
          <group
            onClick={(e) => {
              e.stopPropagation();
              if (isAdmin && !isSubmitting) {
                executeAdminArbitration();
              }
            }}
            onPointerOver={() => {
              if (isAdmin) {
                setHoveredAction('exec-arbitration');
                document.body.style.cursor = 'pointer';
              }
            }}
            onPointerOut={() => {
              setHoveredAction(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[2.8, 0.26]} />
              <meshStandardMaterial
                color={
                  !isAdmin
                    ? '#374151'
                    : hoveredAction === 'exec-arbitration'
                    ? DISPUTE_THEME.secondary
                    : '#b45309'
                }
                emissive={isAdmin ? DISPUTE_THEME.secondary : '#000000'}
                emissiveIntensity={
                  hoveredAction === 'exec-arbitration' ? 0.9 : 0.4
                }
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.068}
              color={isAdmin ? '#000000' : '#9ca3af'}
              anchorX="center"
              anchorY="middle"
            >
              [ ⚖ THI HÀNH PHÁN QUYẾT CHUNG THẨM & ĐIỀU CHỈNH QUỸ ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
