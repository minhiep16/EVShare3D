import React, { useEffect, useRef, useState } from 'react';
import type { Group } from 'three';
import { SceneRegistry } from './SceneRegistry';
import { SceneDefinition, SceneLifecycleState } from './sceneTypes';
import { disposeObject3D } from './resourceDisposal';
import { LightingRig } from '../LightingRig';
import { SceneEnvironment } from '../SceneEnvironment';
import { PlayerController } from '../player/PlayerController';
import { useCameraStore } from '@/stores/useCameraStore';
import { useWorldStore } from '@/stores/useWorldStore';
import { useLoadingStore } from '@/stores/useLoadingStore';
import { registerDefaultScenes } from './defaultScenes';

// Ensure default scenes are registered
registerDefaultScenes();

interface SceneManagerProps {
  activeSceneId: string;
  onLifecycleChange?: (state: SceneLifecycleState, sceneId: string) => void;
  transitionDurationMs?: number;
}

export const SceneManager: React.FC<SceneManagerProps> = ({
  activeSceneId,
  onLifecycleChange,
  transitionDurationMs = 500,
}) => {
  const [currentScene, setCurrentScene] = useState<SceneDefinition | null>(() => {
    return SceneRegistry.getScene(activeSceneId) || null;
  });
  const [lifecycle, setLifecycle] = useState<SceneLifecycleState>('INITIALIZING');

  const sceneContainerRef = useRef<Group>(null);
  const activeSceneIdRef = useRef<string>(activeSceneId);

  // Store hooks
  const setCameraPosition = useCameraStore((state) => state.setPosition);
  const setCameraTarget = useCameraStore((state) => state.setTarget);
  const setCameraFov = useCameraStore((state) => state.setFov);
  const setLightingProfile = useWorldStore((state) => state.setLightingProfile);
  const setSectorLoaded = useWorldStore((state) => state.setSectorLoaded);
  const startLoading = useLoadingStore((state) => state.startLoading);
  const setProgress = useLoadingStore((state) => state.setProgress);
  const stopLoading = useLoadingStore((state) => state.stopLoading);

  // Scene transition orchestration
  useEffect(() => {
    let isCancelled = false;
    activeSceneIdRef.current = activeSceneId;

    const nextScene = SceneRegistry.getScene(activeSceneId);
    if (!nextScene) {
      console.warn(`[SceneManager] Scene "${activeSceneId}" not found in SceneRegistry.`);
      return;
    }

    const performTransition = async (): Promise<void> => {
      // 1. EXITING outgoing scene
      setLifecycle('EXITING');
      onLifecycleChange?.('EXITING', currentScene?.id || '');
      startLoading('scene_transition', `Loading environment: ${nextScene.name}...`);
      setProgress(15);

      if (currentScene?.onExit) {
        try {
          await currentScene.onExit();
        } catch (err) {
          console.error(`[SceneManager] Error in onExit for scene "${currentScene.id}":`, err);
        }
      }

      if (isCancelled) return;
      setProgress(40);

      // 2. DISPOSAL of old Three.js GPU resources
      if (sceneContainerRef.current) {
        disposeObject3D(sceneContainerRef.current);
      }

      // Small delay for smooth fade transition
      if (transitionDurationMs > 0) {
        await new Promise((res) => setTimeout(res, transitionDurationMs * 0.4));
      }

      if (isCancelled) return;
      setProgress(65);

      // 3. INITIALIZING incoming scene environment & camera
      setLifecycle('INITIALIZING');
      onLifecycleChange?.('INITIALIZING', nextScene.id);

      // Update camera framing to scene defaults
      setCameraPosition(nextScene.camera.position);
      setCameraTarget(nextScene.camera.target);
      setCameraFov(nextScene.camera.fov);

      // Update world lighting profile
      setLightingProfile(nextScene.environment.lightingProfile);

      setCurrentScene(nextScene);

      // 4. ENTERING incoming scene
      if (nextScene.onEnter) {
        try {
          await nextScene.onEnter();
        } catch (err) {
          console.error(`[SceneManager] Error in onEnter for scene "${nextScene.id}":`, err);
        }
      }

      if (isCancelled) return;
      setProgress(90);

      // 5. ACTIVE
      setLifecycle('ACTIVE');
      setSectorLoaded(true);
      onLifecycleChange?.('ACTIVE', nextScene.id);
      setProgress(100);
      stopLoading('scene_transition');
    };

    performTransition();

    return () => {
      isCancelled = true;
    };
  }, [
    activeSceneId,
    setCameraPosition,
    setCameraTarget,
    setCameraFov,
    setLightingProfile,
    setSectorLoaded,
    startLoading,
    setProgress,
    stopLoading,
    transitionDurationMs,
    onLifecycleChange,
  ]);

  // Clean up on component unmount
  useEffect(() => {
    const container = sceneContainerRef.current;
    return () => {
      if (container) {
        disposeObject3D(container);
      }
    };
  }, []);

  if (!currentScene) {
    return null;
  }

  const env = currentScene.environment;
  const SceneComponent = currentScene.Component;

  return (
    <group name={`SceneManager_${currentScene.id}`}>
      {/* Dynamic Lighting Rig configured for active scene */}
      <LightingRig
        config={{
          ambientColor: env.ambientColor || '#0b0e14',
          ambientIntensity: env.ambientIntensity,
          keyLightColor: env.keyLightColor || '#ffffff',
          keyLightIntensity: env.keyLightIntensity,
          keyLightPosition: env.keyLightPosition,
          fillLightColor: env.fillColor,
          fillLightIntensity: env.fillIntensity,
          fillLightPosition: env.fillPosition || [-8, 6, -6],
          rimLightColor: env.rimColor,
          rimLightIntensity: env.rimIntensity,
          rimLightPosition: env.rimPosition,
        }}
      />

      {/* Dynamic Scene Environment (Clear color, Fog, Ground) */}
      <SceneEnvironment
        backgroundColor={env.backgroundColor}
        fogColor={env.fogColor}
        fogNear={env.fogNear}
        fogFar={env.fogFar}
        enableGroundGrid={env.enableGroundGrid ?? true}
      />

      {/* Active Scene 3D Content Hierarchy */}
      <group ref={sceneContainerRef} name={`SceneContent_${currentScene.id}`}>
        <SceneComponent />
      </group>

      {/* 3D Spatial Player Movement Controller & Avatar */}
      <PlayerController />
    </group>
  );
};
