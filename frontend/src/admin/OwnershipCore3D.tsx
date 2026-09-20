import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAdminStore } from './useAdminStore';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';

export const OwnershipCore3D: React.FC = () => {
  const {
    syndicates,
    userRole,
    toggleSyndicateTransferFreeze,
    auditCapTable,
    isExecuting,
  } = useAdminStore();

  const torus1Ref = useRef<THREE.Mesh>(null);
  const torus2Ref = useRef<THREE.Mesh>(null);
  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (torus1Ref.current) torus1Ref.current.rotation.x = t * 0.7;
    if (torus2Ref.current) torus2Ref.current.rotation.y = t * 0.5;
  });

  const config = ADMIN_CORES_CONFIG.OWNERSHIP_CORE;
  const isAuthorized = userRole === 'ROLE_ADMIN';
  const targetSyndicate = syndicates[0]; // Syndicate #1

  return (
    <group name="OwnershipCoreStation" position={config.relativePosition}>
      {/* Title */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.22}
        color={config.primaryColor}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        LÕI QUẢN TRỊ & CỔ PHẦN TỔ HỢP
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.105}
        color={COMMAND_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Mật mã bảng cổ đông • Khóa chuyển nhượng • Quản trị quỹ cổ phần
      </Text>

      {/* Floating Dual-Torus Equity Balance Rings */}
      <group position={[0, 2.2, 0]}>
        <mesh ref={torus1Ref}>
          <torusGeometry args={[0.5, 0.04, 16, 32]} />
          <meshStandardMaterial
            color={config.primaryColor}
            emissive={config.primaryColor}
            emissiveIntensity={isAuthorized ? 0.8 : 0.2}
          />
        </mesh>
        <mesh ref={torus2Ref}>
          <torusGeometry args={[0.62, 0.03, 16, 32]} />
          <meshStandardMaterial
            color={config.accentColor}
            emissive={config.accentColor}
            emissiveIntensity={isAuthorized ? 0.6 : 0.1}
          />
        </mesh>
      </group>

      {/* Terminal Board */}
      <group position={[0, 1.25, 0.4]} rotation={[-0.15, 0, 0]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[2.8, 1.6]} />
          <meshStandardMaterial
            color="#14061a"
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

        {/* Syndicate Data */}
        <group position={[-1.25, 0.55, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.072}
            color={config.primaryColor}
            anchorX="left"
            anchorY="middle"
          >
            TỔ HỢP #{targetSyndicate.id}: {targetSyndicate.name.slice(0, 28)}
          </Text>
          <Text
            position={[0, -0.14, 0]}
            fontSize={0.062}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            Cổ phần đã cấp: {targetSyndicate.allocatedShares}/
            {targetSyndicate.totalShares} (100%)
          </Text>
          <Text
            position={[0, -0.28, 0]}
            fontSize={0.062}
            color={targetSyndicate.transferFrozen ? COMMAND_THEME.alertRed : '#34d399'}
            anchorX="left"
            anchorY="middle"
          >
            Chuyển nhượng: {targetSyndicate.transferFrozen ? '🔒 QUẢN TRỊ ĐÓNG BĂNG' : '✓ KHÔNG GIỚI HẠN'} • Tuân thủ: {targetSyndicate.complianceScore}%
          </Text>
        </group>

        {/* 3D Action Controls */}
        <group position={[0, -0.25, 0.02]}>
          {/* Action 1: Freeze Transfers */}
          <group
            position={[0, 0.18, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                toggleSyndicateTransferFreeze(targetSyndicate.id);
            }}
            onPointerOver={() => {
              setHoveredBtn('freeze');
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
                color={hoveredBtn === 'freeze' ? '#581c87' : '#2e1065'}
                emissive={config.primaryColor}
                emissiveIntensity={hoveredBtn === 'freeze' ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.058}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {targetSyndicate.transferFrozen
                ? '[ 🔓 MỞ KHÓA CHUYỂN NHƯỢNG ]'
                : '[ 🔒 ĐÓNG BĂNG CHUYỂN NHƯỢNG ]'}
            </Text>
          </group>

          {/* Action 2: Audit Cap Table */}
          <group
            position={[0, -0.1, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                auditCapTable(targetSyndicate.id);
            }}
            onPointerOver={() => {
              setHoveredBtn('audit');
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
                color={hoveredBtn === 'audit' ? '#701a75' : '#3b0764'}
                emissive="#d946ef"
                emissiveIntensity={hoveredBtn === 'audit' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.054}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 📜 KIỂM TOÁN MẬT MÃ DANH SÁCH CỔ ĐÔNG ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
