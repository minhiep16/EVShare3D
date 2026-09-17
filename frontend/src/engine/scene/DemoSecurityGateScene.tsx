import React, { useEffect, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import type { Group } from 'three';
import { CollisionEngine } from '../player/collisionEngine';
import { TeleportPad } from '../player/TeleportPad';
import { useInteractable } from '../raycast/useInteractable';
import { InteractionPipeline } from '../interaction/InteractionPipeline';
import { useInteractionVisualState } from '../interaction/useInteractionState';
import { useSpatialAnimation } from '../animation/useSpatialAnimation';
import { useWorldStore } from '@/stores/useWorldStore';
import { useAuthStore } from '@/auth/useAuthStore';
import { SecurityGateAuthConsole3D } from '@/auth/SecurityGateAuthConsole3D';
import { useCameraStore } from '@/stores/useCameraStore';

export const DemoSecurityGateScene: React.FC = () => {
  const pulseRef = useRef<Group>(null);
  const setActiveRoomBounds = useWorldStore((state) => state.setActiveRoomBounds);

  const isGateUnlocked = useAuthStore((state) => state.isGateUnlocked);
  const visualState = useInteractionVisualState('biometric_beacon');

  const animState = isGateUnlocked
    ? 'SUCCESS'
    : visualState === 'HOVER'
    ? 'HOVER'
    : visualState === 'DISABLED'
    ? 'ERROR'
    : 'IDLE';

  const themeColor = isGateUnlocked ? '#00e676' : '#00e5ff';

  const { transform } = useSpatialAnimation({
    id: 'biometric_beacon',
    state: animState,
    baseColor: themeColor,
    hoverElevation: 0.1,
    hoverScale: 1.08,
  });

  // Register interactive biometric beacon mesh with raycaster
  useInteractable({
    id: 'biometric_beacon',
    ref: pulseRef,
    priority: 50,
    cursor: 'POINTER',
  });

  useEffect(() => {
    // 1. Register with Interaction Pipeline (6-Stage Pipeline)
    const unregisterPipeline = InteractionPipeline.register({
      id: 'biometric_beacon',
      name: 'Security Biometric Beacon',
      actionType: 'AUTHENTICATE_PERSONA',
      targetPosition: [0, 1.5, 0],
      requirements: {
        maxInteractionDistance: 6.0,
      },
      onActivate: () => {
        if (useAuthStore.getState().isGateUnlocked) {
          useAuthStore.getState().enterWorld();
        } else {
          // Focus camera toward Auth Console
          useCameraStore.getState().moveTo([-2.2, 2.2, 3.2], [-3.6, 1.0, 1.2], { speed: 4.5 });
        }
      },
    });

    // 2. Set Room Boundaries
    setActiveRoomBounds({ minX: -15, maxX: 15, minZ: -15, maxZ: 15 });

    // 3. Register Physical Obstacle Colliders
    const unregisterLeftPost = CollisionEngine.registerBoxCollider({
      id: 'arch_post_left',
      min: [-2.7, 0, -0.2],
      max: [-2.3, 5, 0.2],
    });

    const unregisterRightPost = CollisionEngine.registerBoxCollider({
      id: 'arch_post_right',
      min: [2.3, 0, -0.2],
      max: [2.7, 5, 0.2],
    });

    const unregisterBeacon = CollisionEngine.registerCylinderCollider({
      id: 'central_beacon',
      center: [0, 0, 0],
      radius: 1.2,
      height: 3.0,
    });

    return () => {
      unregisterPipeline();
      unregisterLeftPost();
      unregisterRightPost();
      unregisterBeacon();
      setActiveRoomBounds(null);
    };
  }, [setActiveRoomBounds]);

  useFrame((_, delta) => {
    if (pulseRef.current) {
      pulseRef.current.rotation.y += delta * (isGateUnlocked ? 0.9 : 0.4);
    }
  });

  return (
    <group name="DemoSecurityGateScene" position={[0, 0, 0]}>
      {/* Cyber Industrial Archway Frame */}
      <mesh position={[-2.5, 2.5, 0]} castShadow receiveShadow>
        <boxGeometry args={[0.4, 5, 0.4]} />
        <meshStandardMaterial color="#1a1d24" metalness={0.85} roughness={0.25} />
      </mesh>
      <mesh position={[2.5, 2.5, 0]} castShadow receiveShadow>
        <boxGeometry args={[0.4, 5, 0.4]} />
        <meshStandardMaterial color="#1a1d24" metalness={0.85} roughness={0.25} />
      </mesh>
      <mesh position={[0, 5, 0]} castShadow receiveShadow>
        <boxGeometry args={[5.4, 0.4, 0.4]} />
        <meshStandardMaterial color="#1a1d24" metalness={0.85} roughness={0.25} />
      </mesh>

      {/* Cyber Laser Perimeter Barrier (Active red/cyan warning when locked, clear green when unlocked) */}
      <mesh position={[0, 2.5, 0]}>
        <planeGeometry args={[4.6, 4.6]} />
        <meshBasicMaterial
          color={isGateUnlocked ? '#00e676' : '#ff1744'}
          wireframe
          transparent
          opacity={isGateUnlocked ? 0.04 : 0.22}
        />
      </mesh>

      {/* Central Rotating Biometric Verification Beacon */}
      <group
        ref={pulseRef}
        position={[
          transform.positionOffset[0],
          1.5 + transform.positionOffset[1],
          transform.positionOffset[2],
        ]}
        scale={transform.scale}
      >
        <mesh castShadow>
          <octahedronGeometry args={[0.6]} />
          <meshStandardMaterial
            color={isGateUnlocked ? '#00e676' : transform.colorHex || '#00e5ff'}
            emissive={isGateUnlocked ? '#00e676' : transform.colorHex || '#00e5ff'}
            emissiveIntensity={isGateUnlocked ? 0.8 : transform.emissiveIntensity}
            metalness={0.9}
            roughness={0.1}
          />
        </mesh>
        <mesh rotation={[Math.PI / 2, 0, 0]}>
          <torusGeometry args={[1.2, 0.02, 16, 64]} />
          <meshBasicMaterial color={isGateUnlocked ? '#00e676' : transform.colorHex || '#00e5ff'} />
        </mesh>
      </group>

      {/* Floor Access Disc */}
      <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <ringGeometry args={[0.8, 2.8, 32]} />
        <meshStandardMaterial
          color={isGateUnlocked ? '#00e676' : '#00e5ff'}
          emissive={isGateUnlocked ? '#00e676' : '#00e5ff'}
          emissiveIntensity={isGateUnlocked ? 0.5 : 0.2}
          roughness={0.4}
        />
      </mesh>

      {/* 3D Spatial Teleport Pad Waypoint */}
      <TeleportPad
        id="gate_teleport_pad"
        name="Gateway Portal"
        position={[0, 0, 4.5]}
        targetPosition={[0, 0, -4.5]}
        color={isGateUnlocked ? '#00e676' : '#00e5ff'}
      />

      {/* 3D Interactive Biometric Authentication Console (Pure WebGL) */}
      <SecurityGateAuthConsole3D
        position={[-3.6, 0, 1.2]}
        rotation={[0, Math.PI / 4, 0]}
      />
    </group>
  );
};
