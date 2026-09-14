import React from 'react';

interface SceneEnvironmentProps {
  backgroundColor?: string;
  fogColor?: string;
  fogNear?: number;
  fogFar?: number;
  enableGroundGrid?: boolean;
}

export const SceneEnvironment: React.FC<SceneEnvironmentProps> = ({
  backgroundColor = '#06070a',
  fogColor = '#06070a',
  fogNear = 20,
  fogFar = 70,
  enableGroundGrid = true,
}) => {
  return (
    <group name="SceneEnvironment">
      {/* Background Clear Color */}
      <color attach="background" args={[backgroundColor]} />

      {/* Atmospheric Depth Fog */}
      <fog attach="fog" args={[fogColor, fogNear, fogFar]} />

      {/* Foundational Ground Shadow Receiver Plane */}
      {enableGroundGrid && (
        <mesh
          rotation={[-Math.PI / 2, 0, 0]}
          position={[0, -0.01, 0]}
          receiveShadow
        >
          <planeGeometry args={[120, 120]} />
          <meshStandardMaterial
            color="#080a10"
            roughness={0.85}
            metalness={0.2}
          />
        </mesh>
      )}
    </group>
  );
};
