import React, { useEffect, useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import type { Group } from 'three';
import { CollisionEngine } from '../player/collisionEngine';
import { TeleportPad } from '../player/TeleportPad';
import { useInteractable } from '../raycast/useInteractable';
import { InteractionPipeline } from '../interaction/InteractionPipeline';
import { useInteractionVisualState } from '../interaction/useInteractionState';
import { useSpatialAnimation } from '../animation/useSpatialAnimation';
import { Terminal3D, Modal3D, Button3D, Input3D } from '../ui3d';
import { Text } from '@react-three/drei';
import { useWorldStore } from '@/stores/useWorldStore';
import { useUI3DStore } from '@/stores/useUI3DStore';

export const DemoSecurityGateScene: React.FC = () => {
  const pulseRef = useRef<Group>(null);
  const [beaconActive, setBeaconActive] = useState(false);
  const [pinCode, setPinCode] = useState('');
  const setActiveRoomBounds = useWorldStore((state) => state.setActiveRoomBounds);

  const isModalOpen = useUI3DStore((state) => state.activeModalId === 'security_protocol_modal');
  const openModal = useUI3DStore((state) => state.openModal);
  const closeModal = useUI3DStore((state) => state.closeModal);

  const visualState = useInteractionVisualState('biometric_beacon');
  const animState = beaconActive
    ? 'SUCCESS'
    : visualState === 'HOVER'
    ? 'HOVER'
    : visualState === 'DISABLED'
    ? 'ERROR'
    : 'IDLE';

  const { transform } = useSpatialAnimation({
    id: 'biometric_beacon',
    state: animState,
    baseColor: '#00e5ff',
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
        setBeaconActive((prev) => !prev);
      },
    });

    // 2. Set Room Boundaries
    setActiveRoomBounds({ minX: -15, maxX: 15, minZ: -15, maxZ: 15 });

    // 2. Register Physical Obstacle Colliders
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
      pulseRef.current.rotation.y += delta * 0.5;
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

      {/* Cyber Cyan Laser Perimeter Marker */}
      <mesh position={[0, 2.5, 0]}>
        <planeGeometry args={[4.6, 4.6]} />
        <meshBasicMaterial
          color="#00e5ff"
          wireframe
          transparent
          opacity={0.15}
        />
      </mesh>

      {/* Central Rotating Biometric Verification Beacon with Spatial Animations */}
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
            color={transform.colorHex || '#00e5ff'}
            emissive={transform.colorHex || '#00e5ff'}
            emissiveIntensity={transform.emissiveIntensity}
            metalness={0.9}
            roughness={0.1}
          />
        </mesh>
        <mesh rotation={[Math.PI / 2, 0, 0]}>
          <torusGeometry args={[1.2, 0.02, 16, 64]} />
          <meshBasicMaterial color={transform.colorHex || '#00e5ff'} />
        </mesh>
      </group>

      {/* Floor Access Disc */}
      <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <ringGeometry args={[0.8, 2.8, 32]} />
        <meshStandardMaterial
          color="#00e5ff"
          emissive="#00e5ff"
          emissiveIntensity={0.2}
          roughness={0.4}
        />
      </mesh>

      {/* 3D Spatial Teleport Pad Waypoint */}
      <TeleportPad
        id="gate_teleport_pad"
        name="Gateway Portal"
        position={[0, 0, 4.5]}
        targetPosition={[0, 0, -4.5]}
        color="#00e5ff"
      />

      {/* 3D Interactive Console Terminal (Rendered inside WebGL) */}
      <Terminal3D
        id="security_gate_terminal"
        title="Access Console"
        statusLabel="ONLINE"
        statusVariant="emerald"
        position={[-3.6, 0, 1.2]}
        rotation={[0, Math.PI / 4, 0]}
      >
        <Input3D
          id="gate_pin_input"
          label="Passcode Key"
          value={pinCode}
          onChange={setPinCode}
          placeholder="ENTER PIN..."
          position={[0, 0.22, 0]}
        />
        <Button3D
          id="terminal_verify_btn"
          label="VERIFY ACCESS"
          width={1.6}
          height={0.24}
          variant="cyan"
          position={[0, -0.14, 0]}
          onClick={() => openModal('security_protocol_modal')}
        />
        <Button3D
          id="terminal_protocol_btn"
          label="VIEW PROTOCOLS"
          width={1.6}
          height={0.22}
          variant="amber"
          position={[0, -0.42, 0]}
          onClick={() => openModal('security_protocol_modal')}
        />
      </Terminal3D>

      {/* 3D Spatial Modal Space (Rendered inside WebGL with dimming backdrop plane) */}
      <Modal3D
        id="security_protocol_modal"
        title="Biometric Access Protocol"
        isOpen={isModalOpen}
        onClose={() => closeModal('security_protocol_modal')}
        position={[0, 2.0, 1.2]}
      >
        <Text
          position={[0, 0.25, 0.01]}
          fontSize={0.08}
          color="#00e5ff"
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.05}
        >
          SECURITY LEVEL 4 CLEARANCE REQUIRED
        </Text>
        <Text
          position={[0, 0.06, 0.01]}
          fontSize={0.062}
          color="#8a94a6"
          anchorX="center"
          anchorY="middle"
          maxWidth={2.1}
          textAlign="center"
        >
          Digital twin token verified for co-ownership fleet access. Authorize biometric pass to cycle airlock.
        </Text>

        <Button3D
          id="modal_grant_btn"
          label="GRANT ACCESS"
          variant="emerald"
          width={1.0}
          height={0.26}
          position={[-0.55, -0.32, 0.02]}
          onClick={() => {
            setBeaconActive(true);
            closeModal('security_protocol_modal');
          }}
        />
        <Button3D
          id="modal_dismiss_btn"
          label="CANCEL"
          variant="crimson"
          width={0.8}
          height={0.26}
          position={[0.55, -0.32, 0.02]}
          onClick={() => closeModal('security_protocol_modal')}
        />
      </Modal3D>
    </group>
  );
};
