import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAdminStore } from './useAdminStore';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';

export const UserCore3D: React.FC = () => {
  const {
    users,
    userRole,
    verifyUserKyc,
    toggleUserAccountStatus,
    elevateUserRole,
    isExecuting,
  } = useAdminStore();

  const coreMeshRef = useRef<THREE.Mesh>(null);
  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  useFrame(({ clock }) => {
    if (coreMeshRef.current) {
      const t = clock.getElapsedTime();
      coreMeshRef.current.rotation.y = t * 0.8;
      coreMeshRef.current.position.y = 2.2 + Math.sin(t * 1.5) * 0.08;
    }
  });

  const config = ADMIN_CORES_CONFIG.USER_CORE;
  const isAuthorized = userRole === 'ROLE_ADMIN';
  const targetUser = users.find((u) => u.id === 3) || users[1]; // Bob Driver

  const roleDisplay =
    targetUser.role === 'ROLE_ADMIN'
      ? 'Quản trị viên'
      : targetUser.role === 'ROLE_STAFF'
      ? 'Nhân viên sàn'
      : 'Đồng sở hữu';
  const statusDisplay =
    targetUser.accountStatus === 'ACTIVE' ? 'Hoạt động' : 'Tạm khóa';

  return (
    <group name="UserCoreStation" position={config.relativePosition}>
      {/* Core Title */}
      <Text
        position={[0, 3.4, 0]}
        fontSize={0.22}
        color={config.primaryColor}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        LÕI ĐỊNH DANH & SINH TRẮC HỌC
      </Text>
      <Text
        position={[0, 3.12, 0]}
        fontSize={0.105}
        color={COMMAND_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Mã băm chứng chỉ KYC • Thăng cấp vai trò RBAC • Bảo mật tài khoản
      </Text>

      {/* Floating Polyhedral Nucleus */}
      <mesh ref={coreMeshRef} position={[0, 2.2, 0]}>
        <icosahedronGeometry args={[0.45, 1]} />
        <meshStandardMaterial
          color={config.primaryColor}
          emissive={config.primaryColor}
          emissiveIntensity={isAuthorized ? 0.9 : 0.2}
          roughness={0.2}
          metalness={0.8}
          wireframe={!isAuthorized}
        />
      </mesh>

      {/* Outer Orbiting Gyro Ring */}
      <mesh position={[0, 2.2, 0]} rotation={[Math.PI / 4, 0, 0]}>
        <torusGeometry args={[0.7, 0.02, 16, 32]} />
        <meshBasicMaterial
          color={config.primaryColor}
          transparent
          opacity={0.6}
        />
      </mesh>

      {/* 3D User Dossier & Command Board */}
      <group position={[0, 1.25, 0.4]} rotation={[-0.15, 0, 0]}>
        {/* Terminal Frame */}
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[2.8, 1.6]} />
          <meshStandardMaterial
            color="#050c1a"
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

        {/* User Telemetry List */}
        <group position={[-1.25, 0.55, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.072}
            color={config.primaryColor}
            anchorX="left"
            anchorY="middle"
          >
            DANH TÍNH HOẠT ĐỘNG: {targetUser.fullName} (MÃ #{targetUser.id})
          </Text>
          <Text
            position={[0, -0.14, 0]}
            fontSize={0.062}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            Vai trò: {roleDisplay} • Trạng thái: {statusDisplay}
          </Text>
          <Text
            position={[0, -0.28, 0]}
            fontSize={0.062}
            color={
              targetUser.kycStatus === 'VERIFIED' ? '#34d399' : '#fbbf24'
            }
            anchorX="left"
            anchorY="middle"
          >
            Sinh trắc KYC:{' '}
            {targetUser.kycStatus === 'VERIFIED'
              ? '✓ ĐÃ XÁC THỰC (SHA-256)'
              : '⏳ CHỜ XÁC THỰC'}
          </Text>
        </group>

        {/* 3D Action Controls */}
        <group position={[0, -0.25, 0.02]}>
          {/* Action 1: Verify KYC */}
          <group
            position={[0, 0.22, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) verifyUserKyc(targetUser.id);
            }}
            onPointerOver={() => {
              setHoveredBtn('kyc');
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
                color={hoveredBtn === 'kyc' ? '#0369a1' : '#0c2340'}
                emissive={config.primaryColor}
                emissiveIntensity={hoveredBtn === 'kyc' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.058}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 🛡 XÁC THỰC & KÝ CHỨNG THƯ KYC ]
            </Text>
          </group>

          {/* Action 2: Toggle Account Status */}
          <group
            position={[-0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                toggleUserAccountStatus(targetUser.id);
            }}
            onPointerOver={() => {
              setHoveredBtn('status');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'status' ? '#4c0519' : '#1e0812'}
                emissive={COMMAND_THEME.alertRed}
                emissiveIntensity={hoveredBtn === 'status' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.055}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {targetUser.accountStatus === 'ACTIVE'
                ? '[ ⛔ TẠM KHÓA ]'
                : '[ ⚡ KÍCH HOẠT ]'}
            </Text>
          </group>

          {/* Action 3: Elevate Role */}
          <group
            position={[0.65, -0.06, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting)
                elevateUserRole(targetUser.id, 'ROLE_STAFF');
            }}
            onPointerOver={() => {
              setHoveredBtn('elevate');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[1.15, 0.22]} />
              <meshStandardMaterial
                color={hoveredBtn === 'elevate' ? '#78350f' : '#221008'}
                emissive={COMMAND_THEME.accentGold}
                emissiveIntensity={hoveredBtn === 'elevate' ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.052}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 🔑 THĂNG CẤP NHÂN VIÊN ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
