import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import type { Group, Mesh } from 'three';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useCameraStore } from '@/stores/useCameraStore';

export const PlayerAvatar: React.FC = () => {
  const avatarGroupRef = useRef<Group>(null);
  const coreRef = useRef<Mesh>(null);

  const position = usePlayerStore((state) => state.position);
  const rotation = usePlayerStore((state) => state.rotation);
  const movementMode = usePlayerStore((state) => state.movementMode);
  const cameraMode = useCameraStore((state) => state.mode);

  // Hide avatar in first person mode
  const isVisible = cameraMode !== 'FIRST_PERSON';

  useFrame((state) => {
    if (avatarGroupRef.current) {
      avatarGroupRef.current.position.set(position[0], position[1], position[2]);
      avatarGroupRef.current.rotation.y = rotation[1];

      // Subtle walking bobbing animation
      if (movementMode === 'WALKING' || movementMode === 'SPRINTING') {
        const speed = movementMode === 'SPRINTING' ? 14 : 9;
        const bob = Math.sin(state.clock.elapsedTime * speed) * 0.05;
        avatarGroupRef.current.position.y = position[1] + bob;
      }
    }
  });

  if (!isVisible) return null;

  return (
    <group ref={avatarGroupRef} name="PlayerAvatar">
      {/* Sleek Cybernetic Torso Capsule */}
      <mesh position={[0, 0.9, 0]} castShadow>
        <capsuleGeometry args={[0.3, 0.7, 8, 16]} />
        <meshStandardMaterial
          color="#121622"
          metalness={0.8}
          roughness={0.25}
        />
      </mesh>

      {/* Cyber Head & Visor */}
      <mesh position={[0, 1.5, 0]} castShadow>
        <sphereGeometry args={[0.22, 16, 16]} />
        <meshStandardMaterial color="#1a2030" metalness={0.9} roughness={0.2} />
      </mesh>
      {/* Glowing Cyan Visor */}
      <mesh position={[0, 1.52, -0.15]}>
        <boxGeometry args={[0.26, 0.08, 0.1]} />
        <meshStandardMaterial
          color="#00e5ff"
          emissive="#00e5ff"
          emissiveIntensity={2.5}
        />
      </mesh>

      {/* Energy Core / Chest Beacon */}
      <mesh ref={coreRef} position={[0, 1.0, -0.2]}>
        <boxGeometry args={[0.12, 0.12, 0.05]} />
        <meshStandardMaterial
          color="#00e5ff"
          emissive="#00e5ff"
          emissiveIntensity={1.8}
        />
      </mesh>

      {/* Ground Projection Ring */}
      <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.35, 0.45, 32]} />
        <meshBasicMaterial
          color="#00e5ff"
          transparent
          opacity={0.35}
        />
      </mesh>
    </group>
  );
};
