import React, { useRef, useState, useEffect } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import type { Group, Mesh } from 'three';
import type { WorldPortalNode } from './portalNetwork';
import { useNavigationStore } from './useNavigationStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { WorldInteractionRegistry } from './WorldInteractionRegistry';
import { InteractableRegistry } from '@/engine/raycast/interactableRegistry';
import { AudioEngine } from '@/engine/audio/AudioEngine';

interface WorldPortal3DProps {
  portal: WorldPortalNode;
  onActivate?: () => void;
}

export const WorldPortal3D: React.FC<WorldPortal3DProps> = ({ portal, onActivate }) => {
  const rootRef = useRef<Group>(null);
  const vortexRef = useRef<Group>(null);
  const ring1Ref = useRef<Mesh>(null);
  const ring2Ref = useRef<Mesh>(null);
  const barrierRef = useRef<Mesh>(null);

  const [hovered, setHovered] = useState(false);
  const [proximityTriggered, setProximityTriggered] = useState(false);

  const canAccessSector = useNavigationStore((state) => state.canAccessSector);
  const teleportToSector = useNavigationStore((state) => state.teleportToSector);
  const transitionPhase = useNavigationStore((state) => state.transitionPhase);
  const userRole = useNavigationStore((state) => state.userRole);

  const isAuthorized = canAccessSector(portal.toSector);
  const effectiveColor = isAuthorized ? portal.color : '#ff1744';

  // Register with spatial interaction registry
  useEffect(() => {
    if (!rootRef.current) return;

    const unregWorld = WorldInteractionRegistry.registerEntity({
      id: portal.id,
      sectorId: portal.fromSector,
      name: portal.name,
      category: 'PORTAL',
      position: portal.position,
      interactionDistance: 2.8,
      requiredRole: portal.requiredRole,
    });

    const unregRaycast = InteractableRegistry.register({
      id: portal.id,
      object: rootRef.current,
      name: portal.name,
      priority: 15,
      cursor: isAuthorized ? 'POINTER' : 'NOT_ALLOWED',
      onHoverEnter: () => {
        setHovered(true);
        AudioEngine.playSpatial('UI_HOVER', portal.position);
      },
      onHoverExit: () => setHovered(false),
      onClick: () => handleTrigger(),
    });

    return () => {
      unregWorld();
      unregRaycast();
    };
  }, [portal.id, isAuthorized, userRole]);

  const handleTrigger = async () => {
    if (transitionPhase !== 'IDLE') return;
    onActivate?.();
    await teleportToSector(portal.toSector, portal.id);
  };

  // Per-frame animated vortex and physical proximity walking detection
  useFrame((_, delta) => {
    if (transitionPhase !== 'IDLE') return;

    // 1. Vortex rotation animation
    const spinMultiplier = hovered ? 2.5 : 1.0;
    if (ring1Ref.current) {
      ring1Ref.current.rotation.z += delta * 1.5 * spinMultiplier;
    }
    if (ring2Ref.current) {
      ring2Ref.current.rotation.z -= delta * 2.0 * spinMultiplier;
    }
    if (vortexRef.current) {
      // Subtle hovering breathing pulse
      const time = performance.now() * 0.003;
      vortexRef.current.scale.setScalar(1.0 + Math.sin(time) * 0.04);
    }

    // 2. Walking proximity trigger: detect if player walks directly into the portal
    const playerPos = usePlayerStore.getState().position;
    const dx = playerPos[0] - portal.position[0];
    const dy = playerPos[1] - portal.position[1];
    const dz = playerPos[2] - portal.position[2];
    const distSq = dx * dx + dy * dy + dz * dz;

    if (distSq < 2.25) { // within 1.5m radius
      if (!proximityTriggered) {
        setProximityTriggered(true);
        handleTrigger();
      }
    } else if (distSq > 9.0) { // reset trigger once player walks away (>3m)
      if (proximityTriggered) {
        setProximityTriggered(false);
      }
    }
  });

  return (
    <group
      ref={rootRef}
      position={portal.position}
      rotation={portal.rotation}
      name={`Portal_${portal.id}`}
    >
      {/* 1. Portal Pedestal Base */}
      <mesh position={[0, 0.1, 0]} receiveShadow>
        <cylinderGeometry args={[1.7, 1.9, 0.2, 16]} />
        <meshStandardMaterial
          color="#0b0f19"
          metalness={0.8}
          roughness={0.2}
          emissive={effectiveColor}
          emissiveIntensity={hovered ? 0.4 : 0.15}
        />
      </mesh>

      {/* Glowing Inner Floor Runes */}
      <mesh position={[0, 0.21, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.5, 1.4, 32]} />
        <meshBasicMaterial
          color={effectiveColor}
          transparent
          opacity={hovered ? 0.8 : 0.4}
        />
      </mesh>

      {/* 2. Cybernetic Arch Pillars (Left & Right) */}
      <mesh position={[-1.2, 1.7, 0]} castShadow>
        <boxGeometry args={[0.25, 3.2, 0.35]} />
        <meshStandardMaterial
          color="#1e293b"
          metalness={0.7}
          roughness={0.3}
          emissive={effectiveColor}
          emissiveIntensity={hovered ? 0.5 : 0.2}
        />
      </mesh>
      <mesh position={[1.2, 1.7, 0]} castShadow>
        <boxGeometry args={[0.25, 3.2, 0.35]} />
        <meshStandardMaterial
          color="#1e293b"
          metalness={0.7}
          roughness={0.3}
          emissive={effectiveColor}
          emissiveIntensity={hovered ? 0.5 : 0.2}
        />
      </mesh>

      {/* Overhead Crossbeam Header */}
      <mesh position={[0, 3.4, 0]} castShadow>
        <boxGeometry args={[2.65, 0.35, 0.4]} />
        <meshStandardMaterial
          color="#1e293b"
          metalness={0.8}
          roughness={0.25}
          emissive={effectiveColor}
          emissiveIntensity={hovered ? 0.6 : 0.25}
        />
      </mesh>

      {/* 3. Central Holographic Vortex Membrane (Authorized) */}
      {isAuthorized ? (
        <group ref={vortexRef} position={[0, 1.8, 0]}>
          {/* Outer Concentric Energy Ring */}
          <mesh ref={ring1Ref}>
            <torusGeometry args={[0.95, 0.03, 16, 48]} />
            <meshBasicMaterial color={effectiveColor} wireframe />
          </mesh>

          {/* Inner Counter-Spinning Ring */}
          <mesh ref={ring2Ref}>
            <torusGeometry args={[0.7, 0.025, 16, 36]} />
            <meshBasicMaterial color="#ffffff" wireframe />
          </mesh>

          {/* Translucent Energy Disc Membrane */}
          <mesh>
            <circleGeometry args={[0.9, 32]} />
            <meshBasicMaterial
              color={effectiveColor}
              transparent
              opacity={hovered ? 0.55 : 0.35}
              side={THREE.DoubleSide}
            />
          </mesh>

          {/* Core Energy Flare PointLight */}
          <pointLight
            color={effectiveColor}
            intensity={hovered ? 2.5 : 1.2}
            distance={5.0}
            decay={2}
          />
        </group>
      ) : (
        /* 4. Locked Security Grid Barrier (Unauthorized) */
        <group position={[0, 1.8, 0]}>
          <mesh ref={barrierRef}>
            <planeGeometry args={[2.1, 3.0, 8, 12]} />
            <meshBasicMaterial
              color="#ff1744"
              wireframe
              transparent
              opacity={0.85}
              side={THREE.DoubleSide}
            />
          </mesh>
          <pointLight color="#ff1744" intensity={2.0} distance={4.0} />
        </group>
      )}

      {/* 5. Overhead 3D Holographic Signage & Status Display */}
      <group position={[0, 4.1, 0]}>
        {/* Main Sector Destination Title */}
        <Text
          position={[0, 0.25, 0]}
          fontSize={0.32}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
        >
          {portal.label}
        </Text>

        {/* Subtitle / Sector Classification */}
        <Text
          position={[0, -0.1, 0]}
          fontSize={0.16}
          color={effectiveColor}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
        >
          {portal.subtitle}
        </Text>

        {/* Action Prompt or Access Restriction Badge */}
        <Text
          position={[0, -0.32, 0]}
          fontSize={0.14}
          color={isAuthorized ? (hovered ? '#ffffff' : effectiveColor) : '#ff1744'}
          anchorX="center"
          anchorY="middle"
          font="https://fonts.gstatic.com/s/jetbrainsmono/v18/tDbY2o-flEEny0FZhsfKu5WU4zr3E_ad56U.woff"
        >
          {isAuthorized
            ? portal.isReturnPortal
              ? '<<< WALK OR CLICK TO RETURN <<<'
              : '>>> WALK OR CLICK TO WARP >>>'
            : userRole === 'GUEST'
              ? '[UNAUTHORIZED] LOGIN REQUIRED'
              : `[RESTRICTED] REQUIRES ${portal.requiredRole || 'HIGHER PRIVILEGE'}`}
        </Text>
      </group>
    </group>
  );
};
