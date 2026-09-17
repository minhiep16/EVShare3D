import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAdminStore } from './useAdminStore';
import { ADMIN_CORES_CONFIG, COMMAND_THEME } from './adminLayout';

export const SystemCore3D: React.FC = () => {
  const {
    systemHealth,
    userRole,
    toggleGlobalPlatformLockdown,
    flushSystemCaches,
    isExecuting,
  } = useAdminStore();

  const sphereRef = useRef<THREE.Mesh>(null);
  const ring1Ref = useRef<THREE.Mesh>(null);
  const ring2Ref = useRef<THREE.Mesh>(null);
  const [hoveredBtn, setHoveredBtn] = useState<string | null>(null);

  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();
    if (sphereRef.current) {
      sphereRef.current.position.y = 2.4 + Math.sin(t * 1.8) * 0.06;
    }
    if (ring1Ref.current) ring1Ref.current.rotation.x = t * 0.8;
    if (ring2Ref.current) ring2Ref.current.rotation.y = t * 0.6;
  });

  const config = ADMIN_CORES_CONFIG.SYSTEM_CORE;
  const isAuthorized = userRole === 'ROLE_ADMIN';
  const isLockdown = systemHealth.globalLockdownActive;

  return (
    <group name="ZenithSystemCoreStation" position={config.relativePosition}>
      {/* Title */}
      <Text
        position={[0, 4.2, 0]}
        fontSize={0.26}
        color={isLockdown ? COMMAND_THEME.alertRed : config.primaryColor}
        anchorX="center"
        anchorY="middle"
        font="https://fonts.gstatic.com/s/outfit/v11/QEUw-pXakupjh6eODBs.woff"
      >
        OMNI-COMMAND ZENITH SYSTEM CORE
      </Text>
      <Text
        position={[0, 3.88, 0]}
        fontSize={0.12}
        color={COMMAND_THEME.textMuted}
        anchorX="center"
        anchorY="middle"
      >
        Central Metaverse Nexus • WebGL Shader Throughput • Emergency Platform Lockdown
      </Text>

      {/* Floating Zenith Master Crystalline Sphere */}
      <mesh ref={sphereRef} position={[0, 2.4, 0]}>
        <sphereGeometry args={[0.75, 32, 32]} />
        <meshStandardMaterial
          color={isLockdown ? COMMAND_THEME.alertRed : '#38bdf8'}
          emissive={isLockdown ? COMMAND_THEME.alertRed : '#0284c7'}
          emissiveIntensity={isAuthorized ? 1.2 : 0.2}
          roughness={0.15}
          metalness={0.9}
          wireframe={!isAuthorized}
        />
      </mesh>

      {/* Rotating Orbital Rings */}
      <group position={[0, 2.4, 0]}>
        <mesh ref={ring1Ref}>
          <torusGeometry args={[1.05, 0.03, 16, 48]} />
          <meshBasicMaterial
            color={isLockdown ? COMMAND_THEME.alertRed : COMMAND_THEME.primary}
            transparent
            opacity={0.8}
          />
        </mesh>
        <mesh ref={ring2Ref}>
          <torusGeometry args={[1.25, 0.02, 16, 48]} />
          <meshBasicMaterial
            color={COMMAND_THEME.accentGold}
            transparent
            opacity={0.6}
          />
        </mesh>
      </group>

      {/* Holographic Zenith Control Board */}
      <group position={[0, 0.85, 1.8]} rotation={[-0.2, 0, 0]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[3.4, 1.8]} />
          <meshStandardMaterial
            color="#050a16"
            roughness={0.2}
            metalness={0.9}
            transparent
            opacity={0.96}
          />
        </mesh>
        <mesh position={[0, 0, -0.015]}>
          <planeGeometry args={[3.44, 1.84]} />
          <meshBasicMaterial
            color={
              isLockdown
                ? COMMAND_THEME.alertRed
                : isAuthorized
                ? COMMAND_THEME.primary
                : COMMAND_THEME.alertRed
            }
            wireframe
          />
        </mesh>

        {/* Telemetry Information */}
        <group position={[-1.5, 0.65, 0.02]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.08}
            color={COMMAND_THEME.primary}
            anchorX="left"
            anchorY="middle"
          >
            METAVERSE ZENITH NODE: {systemHealth.nodeHealth}
          </Text>
          <Text
            position={[0, -0.16, 0]}
            fontSize={0.065}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            WebGL Throughput: {systemHealth.metaverseFps} FPS •{' '}
            {systemHealth.shaderThroughput}
          </Text>
          <Text
            position={[0, -0.32, 0]}
            fontSize={0.065}
            color="#cbd5e1"
            anchorX="left"
            anchorY="middle"
          >
            Active Sessions: {systemHealth.activeSessions} • Heartbeat: Online
          </Text>
          <Text
            position={[0, -0.48, 0]}
            fontSize={0.07}
            color={isLockdown ? COMMAND_THEME.alertRed : '#34d399'}
            anchorX="left"
            anchorY="middle"
          >
            Emergency Lockdown:{' '}
            {isLockdown ? '🚨 ACTIVE (PLATFORM FROZEN)' : '✓ DEACTIVATED'}
          </Text>
        </group>

        {/* 3D Action Controls */}
        <group position={[0, -0.35, 0.02]}>
          {/* Action 1: Global Platform Lockdown */}
          <group
            position={[0, 0.16, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) toggleGlobalPlatformLockdown();
            }}
            onPointerOver={() => {
              setHoveredBtn('lockdown');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[3.0, 0.26]} />
              <meshStandardMaterial
                color={
                  isLockdown
                    ? '#166534'
                    : hoveredBtn === 'lockdown'
                    ? '#991b1b'
                    : '#7f1d1d'
                }
                emissive={
                  isLockdown ? '#22c55e' : COMMAND_THEME.alertRed
                }
                emissiveIntensity={hoveredBtn === 'lockdown' ? 0.8 : 0.4}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.075}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              {isLockdown
                ? '[ 🔓 DEACTIVATE GLOBAL PLATFORM LOCKDOWN ]'
                : '[ 🚨 TRIGGER GLOBAL PLATFORM EMERGENCY LOCKDOWN ]'}
            </Text>
          </group>

          {/* Action 2: Flush System Caches */}
          <group
            position={[0, -0.16, 0]}
            onClick={(e) => {
              e.stopPropagation();
              if (isAuthorized && !isExecuting) flushSystemCaches();
            }}
            onPointerOver={() => {
              setHoveredBtn('flush');
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoveredBtn(null);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh position={[0, 0, 0]}>
              <planeGeometry args={[3.0, 0.24]} />
              <meshStandardMaterial
                color={hoveredBtn === 'flush' ? '#0369a1' : '#0c4a6e'}
                emissive={COMMAND_THEME.primary}
                emissiveIntensity={hoveredBtn === 'flush' ? 0.7 : 0.3}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.07}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              [ 🔄 FLUSH SYSTEM CACHES & SHADER PIPELINES ]
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
