import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import type { Group } from 'three';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useCameraStore } from '@/stores/useCameraStore';

interface TeleportPadProps {
  id: string;
  name: string;
  position: [number, number, number];
  targetPosition: [number, number, number];
  targetRotation?: [number, number, number];
  color?: string;
}

export const TeleportPad: React.FC<TeleportPadProps> = ({
  id,
  name,
  position,
  targetPosition,
  targetRotation = [0, 0, 0],
  color = '#00e5ff',
}) => {
  const ringRef = useRef<Group>(null);
  const [hovered, setHovered] = useState(false);

  const teleportPlayer = usePlayerStore((state) => state.teleportTo);
  const moveToCamera = useCameraStore((state) => state.moveTo);

  useFrame((_, delta) => {
    if (ringRef.current) {
      ringRef.current.rotation.z += delta * (hovered ? 1.5 : 0.6);
    }
  });

  const handleActivate = () => {
    // Teleport player avatar to destination
    teleportPlayer(targetPosition, targetRotation);

    // Smoothly adjust camera to frame player at new destination
    moveToCamera(
      [targetPosition[0], targetPosition[1] + 3.5, targetPosition[2] + 7.0],
      [targetPosition[0], targetPosition[1] + 1.2, targetPosition[2]],
      { speed: 4.5 }
    );
  };

  return (
    <group name={`TeleportPad_${id}`} position={position}>
      {/* Interactive Floor Disc (Raycast collider) */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.02, 0]}
        receiveShadow
        onPointerOver={(e) => {
          e.stopPropagation();
          setHovered(true);
        }}
        onPointerOut={() => setHovered(false)}
        onClick={(e) => {
          e.stopPropagation();
          handleActivate();
        }}
      >
        <circleGeometry args={[1.5, 32]} />
        <meshStandardMaterial
          color={hovered ? '#ffffff' : color}
          emissive={color}
          emissiveIntensity={hovered ? 1.8 : 0.8}
          roughness={0.2}
          metalness={0.8}
        />
      </mesh>

      {/* Rotating Holographic Teleport Chevron Ring */}
      <group ref={ringRef} position={[0, 0.05, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <mesh>
          <ringGeometry args={[1.2, 1.45, 32]} />
          <meshBasicMaterial
            color={color}
            wireframe
            transparent
            opacity={hovered ? 0.9 : 0.4}
          />
        </mesh>
      </group>

      {/* Vertical Holographic Beam Column on Hover */}
      {hovered && (
        <mesh position={[0, 1.5, 0]}>
          <cylinderGeometry args={[1.4, 1.4, 3, 32, 1, true]} />
          <meshBasicMaterial
            color={color}
            transparent
            opacity={0.15}
            depthWrite={false}
          />
        </mesh>
      )}
    </group>
  );
};
