import React, { useRef, useEffect } from 'react';
import { useThree, useFrame } from '@react-three/fiber';
import { Color, DirectionalLight, HemisphereLight, Fog } from 'three';
import { useWorldEnvironmentStore } from './useWorldEnvironmentStore';
import { useEngineStore } from '../engine/engineStore';

export const EnvironmentManager: React.FC = () => {
  const { scene } = useThree();
  const activePreset = useWorldEnvironmentStore((state) => state.activePreset);
  const targetPreset = useWorldEnvironmentStore((state) => state.targetPreset);
  const transitioning = useWorldEnvironmentStore((state) => state.transitioning);
  const transitionProgress = useWorldEnvironmentStore((state) => state.transitionProgress);
  const engineConfig = useEngineStore((state) => state.getConfig());

  const keyLightRef = useRef<DirectionalLight>(null);
  const hemiLightRef = useRef<HemisphereLight>(null);
  const fillLightRef = useRef<DirectionalLight>(null);

  // Initialize scene background and fog
  useEffect(() => {
    scene.background = new Color(activePreset.fogColor);
    scene.fog = new Fog(activePreset.fogColor, activePreset.fogNear, activePreset.fogFar);

    return () => {
      scene.background = null;
      scene.fog = null;
    };
  }, [scene]);

  // Frame interpolation for smooth environment transitions
  useFrame(() => {
    if (!scene.fog) return;
    const currentFog = scene.fog as Fog;

    if (transitioning && targetPreset) {
      // Lerp colors and fog distances
      const t = transitionProgress;
      const startColor = new Color(activePreset.fogColor);
      const endColor = new Color(targetPreset.fogColor);
      startColor.lerp(endColor, t);

      currentFog.color.copy(startColor);
      if (scene.background instanceof Color) {
        scene.background.copy(startColor);
      }

      currentFog.near = activePreset.fogNear + (targetPreset.fogNear - activePreset.fogNear) * t;
      currentFog.far = activePreset.fogFar + (targetPreset.fogFar - activePreset.fogFar) * t;

      if (keyLightRef.current) {
        const startKey = new Color(activePreset.keyLightColor);
        const endKey = new Color(targetPreset.keyLightColor);
        startKey.lerp(endKey, t);
        keyLightRef.current.color.copy(startKey);
        keyLightRef.current.intensity =
          activePreset.keyLightIntensity +
          (targetPreset.keyLightIntensity - activePreset.keyLightIntensity) * t;
      }

      if (hemiLightRef.current) {
        const startSky = new Color(activePreset.skyColor);
        const endSky = new Color(targetPreset.skyColor);
        startSky.lerp(endSky, t);
        hemiLightRef.current.color.copy(startSky);

        const startGround = new Color(activePreset.groundColor);
        const endGround = new Color(targetPreset.groundColor);
        startGround.lerp(endGround, t);
        hemiLightRef.current.groundColor.copy(startGround);
      }
    } else {
      // Direct preset assignment
      const presetColor = new Color(activePreset.fogColor);
      currentFog.color.copy(presetColor);
      if (scene.background instanceof Color) {
        scene.background.copy(presetColor);
      }
      currentFog.near = activePreset.fogNear;
      currentFog.far = activePreset.fogFar;

      if (keyLightRef.current) {
        keyLightRef.current.color.set(activePreset.keyLightColor);
        keyLightRef.current.intensity = activePreset.keyLightIntensity;
      }

      if (hemiLightRef.current) {
        hemiLightRef.current.color.set(activePreset.skyColor);
        hemiLightRef.current.groundColor.set(activePreset.groundColor);
        hemiLightRef.current.intensity = activePreset.ambientIntensity;
      }

      if (fillLightRef.current) {
        fillLightRef.current.color.set(activePreset.fillLightColor);
        fillLightRef.current.intensity = activePreset.fillLightIntensity;
      }
    }
  });

  const shouldCastShadows = engineConfig.shadows;
  const shadowMapSize = engineConfig.shadowMapSize;

  return (
    <group name="EnvironmentManager">
      {/* Dynamic Hemisphere Ambient Light */}
      <hemisphereLight
        ref={hemiLightRef}
        args={[activePreset.skyColor, activePreset.groundColor, activePreset.ambientIntensity]}
      />

      {/* Main Directional Key Light with Cascaded PCF Shadows */}
      <directionalLight
        ref={keyLightRef}
        position={activePreset.keyLightPosition}
        intensity={activePreset.keyLightIntensity}
        color={activePreset.keyLightColor}
        castShadow={shouldCastShadows}
        shadow-mapSize-width={shadowMapSize}
        shadow-mapSize-height={shadowMapSize}
        shadow-camera-near={1}
        shadow-camera-far={120}
        shadow-camera-left={-40}
        shadow-camera-right={40}
        shadow-camera-top={40}
        shadow-camera-bottom={-40}
        shadow-bias={-0.0004}
      />

      {/* Fill Light for Dark Angle Softening */}
      <directionalLight
        ref={fillLightRef}
        position={[-activePreset.keyLightPosition[0], 12, -activePreset.keyLightPosition[2]]}
        intensity={activePreset.fillLightIntensity}
        color={activePreset.fillLightColor}
      />

      {/* Subtle Rim / Accent Light */}
      <pointLight
        position={[0, 15, 0]}
        intensity={activePreset.rimLightIntensity}
        color={activePreset.accentColor}
        distance={60}
      />
    </group>
  );
};
