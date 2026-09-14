import React from 'react';
import { Text } from '@react-three/drei';
import { Terminal3DProps, VARIANT_PALETTES } from './ui3dTypes';
import { useFocusTarget } from '../focus/useFocusTarget';

export const Terminal3D: React.FC<Terminal3DProps> = ({
  id,
  title,
  statusLabel = 'ONLINE',
  statusVariant = 'emerald',
  width = 2.2,
  height = 1.4,
  position = [0, 0, 0],
  rotation = [0, 0, 0],
  children,
}) => {
  const statusPalette = VARIANT_PALETTES[statusVariant];
  const screenAngle = -Math.PI / 10; // -18 degree tilt for ergonomic eye-level viewing

  // Register with FocusManager so camera can automatically glide straight to this terminal
  useFocusTarget({
    id,
    name: title,
    category: 'TERMINAL',
    targetPosition: [position[0], position[1] + 1.2, position[2]],
    preset: {
      distance: 1.8,
      elevation: 0.2,
      fov: 34,
    },
  });

  return (
    <group position={position} rotation={rotation}>
      {/* 1. Pedestal Ground Stand */}
      <mesh position={[0, 0.6, 0]} castShadow receiveShadow>
        <cylinderGeometry args={[0.12, 0.22, 1.2, 16]} />
        <meshStandardMaterial color="#131720" metalness={0.85} roughness={0.2} />
      </mesh>

      {/* 2. Ground Anchor Base Flange */}
      <mesh position={[0, 0.02, 0]} receiveShadow>
        <cylinderGeometry args={[0.38, 0.45, 0.04, 16]} />
        <meshStandardMaterial color="#0b0e14" metalness={0.9} roughness={0.15} />
      </mesh>
      <mesh position={[0, 0.045, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.34, 0.38, 32]} />
        <meshBasicMaterial color="#00e5ff" transparent opacity={0.6} />
      </mesh>

      {/* 3. Tilted Console Display Head */}
      <group position={[0, 1.2, 0]} rotation={[screenAngle, 0, 0]}>
        {/* Rear Enclosure Housing */}
        <mesh castShadow receiveShadow>
          <boxGeometry args={[width + 0.1, height + 0.1, 0.08]} />
          <meshStandardMaterial color="#10151f" metalness={0.9} roughness={0.2} />
        </mesh>

        {/* Glass Screen Surface */}
        <mesh position={[0, 0, 0.042]}>
          <planeGeometry args={[width, height]} />
          <meshStandardMaterial
            color="#06090e"
            metalness={0.9}
            roughness={0.1}
            transparent
            opacity={0.92}
          />
        </mesh>

        {/* Neon Border Edge */}
        <mesh position={[0, 0, 0.044]}>
          <planeGeometry args={[width, height]} />
          <meshBasicMaterial color="#00e5ff" wireframe transparent opacity={0.4} />
        </mesh>

        {/* Terminal Header Bar */}
        <group position={[0, height / 2 - 0.12, 0.048]}>
          <mesh>
            <planeGeometry args={[width - 0.06, 0.16]} />
            <meshBasicMaterial color="#00e5ff" transparent opacity={0.12} />
          </mesh>
          <Text
            position={[-width / 2 + 0.1, 0, 0.002]}
            fontSize={0.08}
            color="#00e5ff"
            anchorX="left"
            anchorY="middle"
            letterSpacing={0.1}
          >
            {title.toUpperCase()}
          </Text>

          {/* Status Indicator LED */}
          <group position={[width / 2 - 0.35, 0, 0.002]}>
            <mesh position={[-0.1, 0, 0]}>
              <sphereGeometry args={[0.024, 16, 16]} />
              <meshBasicMaterial color={statusPalette.primary} />
            </mesh>
            <Text
              position={[0, 0, 0]}
              fontSize={0.065}
              color={statusPalette.primary}
              anchorX="left"
              anchorY="middle"
              font="JetBrains Mono"
            >
              {statusLabel}
            </Text>
          </group>
        </group>

        {/* Screen Interactive Surface Children */}
        <group position={[0, -0.08, 0.052]}>{children}</group>
      </group>
    </group>
  );
};
