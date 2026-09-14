import React from 'react';
import { Text } from '@react-three/drei';
import { Panel3DProps, VARIANT_PALETTES } from './ui3dTypes';

export const Panel3D: React.FC<Panel3DProps> = ({
  title,
  width = 2.4,
  height = 1.6,
  depth = 0.04,
  variant = 'cyan',
  position = [0, 0, 0],
  rotation = [0, 0, 0],
  children,
}) => {
  const palette = VARIANT_PALETTES[variant];

  return (
    <group position={position} rotation={rotation}>
      {/* Frosted Dark Glass Slab */}
      <mesh castShadow receiveShadow>
        <boxGeometry args={[width, height, depth]} />
        <meshStandardMaterial
          color={palette.background}
          metalness={0.85}
          roughness={0.2}
          transparent
          opacity={0.88}
        />
      </mesh>

      {/* Cybernetic Glowing Border Frame */}
      <mesh position={[0, 0, depth / 2 + 0.001]}>
        <planeGeometry args={[width + 0.02, height + 0.02]} />
        <meshBasicMaterial
          color={palette.primary}
          wireframe
          transparent
          opacity={0.45}
        />
      </mesh>

      {/* Top Header Banner Bar */}
      {title && (
        <group position={[0, height / 2 - 0.12, depth / 2 + 0.005]}>
          <mesh>
            <planeGeometry args={[width - 0.08, 0.18]} />
            <meshBasicMaterial
              color={palette.primary}
              transparent
              opacity={0.12}
            />
          </mesh>
          <Text
            position={[-width / 2 + 0.12, 0, 0.002]}
            fontSize={0.09}
            color={palette.primary}
            anchorX="left"
            anchorY="middle"
            letterSpacing={0.12}
          >
            {title.toUpperCase()}
          </Text>
        </group>
      )}

      {/* Corner Tech Markers */}
      <mesh position={[-width / 2 + 0.04, height / 2 - 0.04, depth / 2 + 0.003]}>
        <planeGeometry args={[0.04, 0.04]} />
        <meshBasicMaterial color={palette.primary} />
      </mesh>
      <mesh position={[width / 2 - 0.04, height / 2 - 0.04, depth / 2 + 0.003]}>
        <planeGeometry args={[0.04, 0.04]} />
        <meshBasicMaterial color={palette.primary} />
      </mesh>

      {/* 3D Child Nodes on Panel Surface */}
      <group position={[0, title ? -0.1 : 0, depth / 2 + 0.01]}>
        {children}
      </group>
    </group>
  );
};
