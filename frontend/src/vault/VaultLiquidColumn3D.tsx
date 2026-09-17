import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useVaultStore } from './useVaultStore';
import { VAULT_LAYOUT } from './vaultLayout';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export const VaultLiquidColumn3D: React.FC = () => {
  const sharedFund = useVaultStore((state) => state.sharedFund);
  const focusCamera = useVaultStore((state) => state.focusCamera);
  const [hovered, setHovered] = useState(false);

  const columnMeshRef = useRef<THREE.Mesh>(null);
  const liquidMeshRef = useRef<THREE.Mesh>(null);
  const warningBeaconRef = useRef<THREE.Mesh>(null);
  const hudGroupRef = useRef<THREE.Group>(null);

  const currentBalance = sharedFund?.currentBalance ?? 45000000;
  const minimumReserve = sharedFund?.minimumReserve ?? 15000000;
  const isBelowReserve = currentBalance < minimumReserve;

  const maxCapacityVnd = 100000000; // 100M VND visual scale
  const totalColumnHeight = 4.2;
  const radius = 1.15;

  const fillRatio = Math.max(0.08, Math.min(1.0, currentBalance / maxCapacityVnd));
  const liquidHeight = totalColumnHeight * fillRatio;
  const reserveRatio = Math.max(0.05, Math.min(1.0, minimumReserve / maxCapacityVnd));
  const reserveLineHeight = totalColumnHeight * reserveRatio;

  const reserveCoveragePercent = ((currentBalance / minimumReserve) * 100).toFixed(0);

  // Micro-animations for fluid movement, warning pulse, and floating HUD
  useFrame(({ clock }) => {
    const t = clock.getElapsedTime();

    if (liquidMeshRef.current) {
      liquidMeshRef.current.rotation.y = t * 0.15;
    }

    if (warningBeaconRef.current) {
      if (isBelowReserve) {
        warningBeaconRef.current.rotation.y = t * 5.0;
        const pulse = 0.4 + Math.sin(t * 10) * 0.6;
        (warningBeaconRef.current.material as THREE.MeshBasicMaterial).opacity = pulse;
      } else {
        warningBeaconRef.current.rotation.y = t * 0.5;
        (warningBeaconRef.current.material as THREE.MeshBasicMaterial).opacity = 0.35;
      }
    }

    if (hudGroupRef.current) {
      hudGroupRef.current.position.y = totalColumnHeight + 1.25 + Math.sin(t * 1.8) * 0.08;
    }
  });

  const handlePointerOver = (e: { stopPropagation: () => void }) => {
    e.stopPropagation();
    setHovered(true);
    AudioEngine.play('UI_HOVER');
  };

  const handlePointerOut = () => {
    setHovered(false);
  };

  const handleClick = (e: { stopPropagation: () => void }) => {
    e.stopPropagation();
    focusCamera('LIQUID_COLUMN_FOCUS');
  };

  const fluidColor = isBelowReserve ? VAULT_LAYOUT.theme.dangerRed : VAULT_LAYOUT.theme.goldPrimary;
  const fluidGlow = isBelowReserve ? '#ff1744' : '#ffb300';

  return (
    <group
      position={VAULT_LAYOUT.liquidColumnPosition}
      onPointerOver={handlePointerOver}
      onPointerOut={handlePointerOut}
      onClick={handleClick}
    >
      {/* 1. Heavy Titanium Cylindrical Base */}
      <mesh position={[0, 0.25, 0]} castShadow receiveShadow>
        <cylinderGeometry args={[radius + 0.35, radius + 0.5, 0.5, 32]} />
        <meshStandardMaterial
          color={VAULT_LAYOUT.theme.titaniumDark}
          metalness={0.95}
          roughness={0.15}
        />
      </mesh>

      {/* Outer illuminated base ring */}
      <mesh position={[0, 0.51, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[radius + 0.05, radius + 0.35, 32]} />
        <meshBasicMaterial
          color={hovered ? VAULT_LAYOUT.theme.amberAccent : fluidColor}
          transparent
          opacity={0.8}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* 2. Double-Walled Outer Glass Containment Chamber */}
      <mesh
        ref={columnMeshRef}
        position={[0, totalColumnHeight / 2 + 0.5, 0]}
      >
        <cylinderGeometry args={[radius + 0.04, radius + 0.04, totalColumnHeight, 32]} />
        <meshPhysicalMaterial
          color="#0d1b2a"
          transmission={0.88}
          opacity={0.7}
          transparent
          roughness={0.08}
          ior={1.45}
          thickness={0.6}
          metalness={0.1}
        />
      </mesh>

      {/* 3. Dynamic Liquid Fluid Volume */}
      <mesh
        ref={liquidMeshRef}
        position={[0, liquidHeight / 2 + 0.5, 0]}
      >
        <cylinderGeometry args={[radius * 0.95, radius * 0.95, liquidHeight, 32]} />
        <meshStandardMaterial
          color={fluidColor}
          emissive={fluidGlow}
          emissiveIntensity={hovered ? 0.75 : 0.45}
          transparent
          opacity={0.78}
          roughness={0.2}
          metalness={0.3}
        />
      </mesh>

      {/* Fluid meniscus surface lid */}
      <mesh position={[0, liquidHeight + 0.5, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <circleGeometry args={[radius * 0.95, 32]} />
        <meshBasicMaterial
          color={hovered ? '#ffffff' : fluidColor}
          transparent
          opacity={0.9}
        />
      </mesh>

      {/* 4. BR-FIN-03 Safety Reserve Laser Ring */}
      <group position={[0, reserveLineHeight + 0.5, 0]}>
        {/* Laser glow ring */}
        <mesh rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[radius + 0.02, radius + 0.12, 32]} />
          <meshBasicMaterial
            color={VAULT_LAYOUT.theme.dangerRed}
            transparent
            opacity={0.9}
            side={THREE.DoubleSide}
          />
        </mesh>

        {/* Laser emitter brackets */}
        {[-radius - 0.1, radius + 0.1].map((x, i) => (
          <mesh key={i} position={[x, 0, 0]}>
            <boxGeometry args={[0.08, 0.06, 0.15]} />
            <meshBasicMaterial color="#ff1744" />
          </mesh>
        ))}

        {/* Laser Line Inscribed Label */}
        <Text
          position={[0, 0.12, radius + 0.18]}
          fontSize={0.15}
          color="#ff5252"
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          BR-FIN-03 MIN RESERVE: {minimumReserve.toLocaleString('vi-VN')} VND
        </Text>
      </group>

      {/* 5. 4 Vertical Titanium Containment Struts */}
      {[0, Math.PI / 2, Math.PI, (Math.PI * 3) / 2].map((angle, i) => {
        const x = Math.cos(angle) * (radius + 0.14);
        const z = Math.sin(angle) * (radius + 0.14);
        return (
          <mesh
            key={i}
            position={[x, totalColumnHeight / 2 + 0.5, z]}
            castShadow
          >
            <cylinderGeometry args={[0.06, 0.06, totalColumnHeight, 16]} />
            <meshStandardMaterial
              color="#1e293b"
              metalness={0.9}
              roughness={0.2}
            />
          </mesh>
        );
      })}

      {/* 6. Heavy Top Sealing Dome & Warning Beacon */}
      <group position={[0, totalColumnHeight + 0.5, 0]}>
        <mesh position={[0, 0.18, 0]} castShadow>
          <cylinderGeometry args={[radius + 0.4, radius + 0.2, 0.36, 32]} />
          <meshStandardMaterial
            color={VAULT_LAYOUT.theme.titaniumDark}
            metalness={0.95}
            roughness={0.15}
          />
        </mesh>

        {/* Rotational Beacon Lens */}
        <mesh ref={warningBeaconRef} position={[0, 0.45, 0]}>
          <cylinderGeometry args={[0.3, 0.35, 0.2, 16]} />
          <meshBasicMaterial
            color={fluidColor}
            transparent
            opacity={0.8}
          />
        </mesh>
      </group>

      {/* 7. Floating 3D Holographic Balance & Status HUD */}
      <group ref={hudGroupRef}>
        {/* Hologram Backdrop Plaque */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[3.6, 1.4]} />
          <meshBasicMaterial
            color="#060c18"
            transparent
            opacity={0.85}
            side={THREE.DoubleSide}
          />
        </mesh>

        {/* Glowing border */}
        <lineSegments position={[0, 0, 0.01]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(3.6, 1.4)]} />
          <lineBasicMaterial color={fluidColor} />
        </lineSegments>

        {/* Syndicate Vault Label */}
        <Text
          position={[0, 0.44, 0.02]}
          fontSize={0.18}
          color={VAULT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          SYNDICATE LIQUID TREASURY
        </Text>

        {/* Big Balance Text */}
        <Text
          position={[0, 0.12, 0.02]}
          fontSize={0.34}
          color={fluidColor}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {currentBalance.toLocaleString('vi-VN')} VND
        </Text>

        {/* Solvency & Reserve Status Pill */}
        <Text
          position={[0, -0.22, 0.02]}
          fontSize={0.16}
          color={isBelowReserve ? '#ff5252' : VAULT_LAYOUT.theme.solvencyGreen}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          {isBelowReserve
            ? '⚠️ CRITICAL DEFICIT • REPLENISH VAULT'
            : `HEALTHY • ${reserveCoveragePercent}% OF RESERVE THRESHOLD`}
        </Text>

        <Text
          position={[0, -0.48, 0.02]}
          fontSize={0.12}
          color={VAULT_LAYOUT.theme.textMuted}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          [ CLICK TO FOCUS INSPECTION CAMERA ]
        </Text>
      </group>
    </group>
  );
};
