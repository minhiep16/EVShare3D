import React, { useEffect, useRef } from 'react';
import { Canvas } from '@react-three/fiber';
import * as THREE from 'three';
import { CameraManager } from './camera/CameraManager';
import { RaycastManager } from './raycast/RaycastManager';
import { InteractionManager } from './interaction/InteractionManager';
import { FocusManager } from './focus/FocusManager';
import { AnimationManager } from './animation/AnimationManager';
import { AudioManager } from './audio/AudioManager';
import { AdaptivePerformanceController } from './performance/AdaptivePerformanceController';
import { ResponsiveViewportController } from './touch/ResponsiveViewportController';
import { LightingRig } from './LightingRig';
import { SceneEnvironment } from './SceneEnvironment';
import { ErrorBoundary3D } from './ErrorBoundary3D';
import { CameraConfig, LightingConfig } from './types';
import { useEngineStore } from './engineStore';
import { useWebGLRecoveryStore, RecoveryScreen } from './recovery';

export interface Canvas3DFoundationProps {
  children?: React.ReactNode;
  cameraConfig?: Partial<CameraConfig>;
  lightingConfig?: Partial<LightingConfig>;
  enableDefaultCamera?: boolean;
  enableDefaultLighting?: boolean;
  enableDefaultEnvironment?: boolean;
  enableGroundGrid?: boolean;
  className?: string;
  style?: React.CSSProperties;
}

export const Canvas3DFoundation: React.FC<Canvas3DFoundationProps> = ({
  children,
  cameraConfig,
  lightingConfig,
  enableDefaultCamera = true,
  enableDefaultLighting = true,
  enableDefaultEnvironment = true,
  enableGroundGrid = true,
  className,
  style,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const engineConfig = useEngineStore((state) => state.getConfig());
  const setReady = useEngineStore((state) => state.setReady);
  const setWebglSupported = useEngineStore((state) => state.setWebglSupported);
  const setDimensions = useEngineStore((state) => state.setDimensions);
  const recoveryStatus = useWebGLRecoveryStore((state) => state.status);

  // Resize handling
  useEffect(() => {
    const handleResize = (): void => {
      if (containerRef.current) {
        const { clientWidth, clientHeight } = containerRef.current;
        setDimensions(clientWidth, clientHeight);
      }
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [setDimensions]);

  return (
    <ErrorBoundary3D>
      <div
        ref={containerRef}
        id="canvas-container"
        className={className}
        style={{
          position: 'relative',
          width: '100%',
          height: '100%',
          overflow: 'hidden',
          backgroundColor: '#06070a',
          ...style,
        }}
      >
        <Canvas
          shadows={engineConfig.shadows}
          dpr={engineConfig.dpr}
          gl={{
            powerPreference: engineConfig.powerPreference,
            antialias: engineConfig.antialias,
            toneMapping: engineConfig.toneMapping,
            toneMappingExposure: engineConfig.toneMappingExposure,
            outputColorSpace: THREE.SRGBColorSpace,
          }}
          onCreated={({ gl }) => {
            setReady(true);
            setWebglSupported(true);

            // WebGL context loss listener
            gl.domElement.addEventListener(
              'webglcontextlost',
              (event) => {
                event.preventDefault();
                console.warn('[Canvas3DFoundation] WebGL Context Lost.');
                setReady(false);
                useWebGLRecoveryStore.getState().triggerContextLost();
              },
              false
            );

            gl.domElement.addEventListener(
              'webglcontextrestored',
              () => {
                console.info('[Canvas3DFoundation] WebGL Context Restored.');
                setReady(true);
                useWebGLRecoveryStore.getState().triggerContextRestored();
              },
              false
            );
          }}
        >
          {/* Foundational Unified Camera Manager */}
          {enableDefaultCamera && <CameraManager />}

          {/* Central Spatial Raycast, Interaction, Focus, Animation & Audio Managers */}
          <RaycastManager />
          <InteractionManager />
          <FocusManager />
          <AnimationManager />
          <AudioManager />
          <AdaptivePerformanceController />
          <ResponsiveViewportController />

          {/* Foundational Multi-point PBR Lighting */}
          {enableDefaultLighting && <LightingRig config={lightingConfig} />}

          {/* Depth Fog, Clear Color, Ground Shadow Plane */}
          {enableDefaultEnvironment && (
            <SceneEnvironment enableGroundGrid={enableGroundGrid} />
          )}

          {/* Render 3D Child Nodes */}
          {children}
        </Canvas>

        {/* WebGL Context Loss Recovery Screen (No Blank Page / No 2D Dashboard Fallback) */}
        {recoveryStatus === 'CONTEXT_LOST' && <RecoveryScreen />}
      </div>
    </ErrorBoundary3D>
  );
};
