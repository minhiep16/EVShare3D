import React, { useRef } from 'react';
import type { DirectionalLight } from 'three';
import { LightingConfig } from './types';
import { useEngineStore } from './engineStore';

interface LightingRigProps {
  config?: Partial<LightingConfig>;
}

const DEFAULT_LIGHTING_CONFIG: LightingConfig = {
  ambientColor: '#0b0e14',
  ambientIntensity: 0.8,
  keyLightColor: '#ffffff',
  keyLightIntensity: 1.6,
  keyLightPosition: [8, 14, 8],
  fillLightColor: '#00e5ff', // Cyber cyan accent fill
  fillLightIntensity: 0.6,
  fillLightPosition: [-8, 6, -6],
  rimLightColor: '#ffab00', // Cyber amber rim
  rimLightIntensity: 0.4,
  rimLightPosition: [0, 8, -10],
  castShadows: true,
};

export const LightingRig: React.FC<LightingRigProps> = ({ config }) => {
  const mergedConfig: LightingConfig = { ...DEFAULT_LIGHTING_CONFIG, ...config };
  const keyLightRef = useRef<DirectionalLight>(null);

  const engineConfig = useEngineStore((state) => state.getConfig());
  const shouldCastShadows = mergedConfig.castShadows && engineConfig.shadows;
  const shadowMapSize = engineConfig.shadowMapSize;

  return (
    <group name="LightingRig">
      {/* Foundational sky/ground ambient contrast */}
      <hemisphereLight
        args={['#1e293b', '#06070a', 0.5]}
      />

      {/* Baseline ambient fill */}
      <ambientLight
        color={mergedConfig.ambientColor}
        intensity={mergedConfig.ambientIntensity}
      />

      {/* Primary Key Directional Light (Casts Shadow) */}
      <directionalLight
        ref={keyLightRef}
        color={mergedConfig.keyLightColor}
        intensity={mergedConfig.keyLightIntensity}
        position={mergedConfig.keyLightPosition}
        castShadow={shouldCastShadows}
        shadow-mapSize-width={shadowMapSize}
        shadow-mapSize-height={shadowMapSize}
        shadow-bias={-0.0001}
        shadow-camera-near={0.5}
        shadow-camera-far={50}
        shadow-camera-left={-15}
        shadow-camera-right={15}
        shadow-camera-top={15}
        shadow-camera-bottom={-15}
      />

      {/* Cyber Cyan Fill Light */}
      <directionalLight
        color={mergedConfig.fillLightColor}
        intensity={mergedConfig.fillLightIntensity}
        position={mergedConfig.fillLightPosition}
      />

      {/* Warm Cyber Rim Light */}
      {mergedConfig.rimLightColor && (
        <directionalLight
          color={mergedConfig.rimLightColor}
          intensity={mergedConfig.rimLightIntensity ?? 0.3}
          position={mergedConfig.rimLightPosition ?? [0, 8, -10]}
        />
      )}
    </group>
  );
};
