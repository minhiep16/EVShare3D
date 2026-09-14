import React, { useState } from 'react';
import * as THREE from 'three';
import {
  Canvas3DFoundation,
  SceneManager,
  SceneTransitionVeil,
  SceneLifecycleState,
  InputManager,
  useEngineStore,
  useInputStore,
  useFocusStore,
  AnimationRegistry,
  AnimationState,
  useAudioStore,
  AudioEngine,
  useSound,
  usePerformanceStore,
  VirtualTouchJoystick,
  TouchGestureController,
} from '@/engine';
import { EngineQuality } from '@/engine/types';
import { PerformanceTier } from '@/engine/performance';
import {
  useAppStore,
  useWorldStore,
  usePlayerStore,
  useCameraStore,
  useInteractionStore,
  useUI3DStore,
  useLoadingStore,
  useErrorStore,
} from '@/stores';
import { CameraMode, SectorId } from '@/stores/types';

export const App: React.FC = () => {
  // Engine store (graphics & canvas)
  const quality = useEngineStore((state) => state.quality);
  const setQuality = useEngineStore((state) => state.setQuality);
  const dimensions = useEngineStore((state) => state.canvasDimensions);
  const isReady = useEngineStore((state) => state.isReady);

  // Performance store
  const currentTier = usePerformanceStore((state) => state.currentTier);
  const setPerformanceTier = usePerformanceStore((state) => state.setTier);
  const adaptiveDpr = usePerformanceStore((state) => state.adaptiveDpr);
  const measuredFps = usePerformanceStore((state) => state.measuredFps);

  // App & World stores
  const currentSector = useAppStore((state) => state.currentSector);
  const setCurrentSector = useAppStore((state) => state.setCurrentSector);
  const lightingProfile = useWorldStore((state) => state.lightingProfile);
  const playerMovement = usePlayerStore((state) => state.movementMode);
  const playerPosition = usePlayerStore((state) => state.position);
  const cursorMode = useInteractionStore((state) => state.cursorMode);
  const hoveredObjectId = useInteractionStore((state) => state.hoveredObjectId);
  const selectedObjectId = useInteractionStore((state) => state.selectedObjectId);
  const isHudVisible = useUI3DStore((state) => state.isHudVisible);
  const activeModalId = useUI3DStore((state) => state.activeModalId);
  const isAnyLoading = useLoadingStore((state) => state.isAnyLoading());
  const errorCount = useErrorStore((state) => state.errors.length);

  // Unified Input Store telemetry
  const pointer = useInputStore((state) => state.pointer);
  const movement = useInputStore((state) => state.movement);
  const touch = useInputStore((state) => state.touch);
  const isTypingMode = useInputStore((state) => state.isTypingMode);

  // Camera Store Hooks & Actions
  const cameraMode = useCameraStore((state) => state.mode);
  const setCameraMode = useCameraStore((state) => state.setMode);
  const focusTarget = useCameraStore((state) => state.focusTarget);
  const startCinematic = useCameraStore((state) => state.startCinematic);
  const resetCamera = useCameraStore((state) => state.resetCamera);
  const isCameraTransitioning = useCameraStore((state) => state.isTransitioning);
  const currentPos = useCameraStore((state) => state.currentPosition);

  // Focus Manager Hooks & States
  const isFocused = useFocusStore((state) => state.isFocused);
  const focusedTargetId = useFocusStore((state) => state.focusedTargetId);
  const activeCategory = useFocusStore((state) => state.activeCategory);
  const focusObject = useFocusStore((state) => state.focusObject);
  const returnToPreviousCamera = useFocusStore((state) => state.returnToPreviousCamera);

  // Audio Engine Hooks & Sound triggers
  const isMuted = useAudioStore((state) => state.isMuted);
  const masterVolume = useAudioStore((state) => state.masterVolume);
  const toggleMute = useAudioStore((state) => state.toggleMute);
  const setMasterVolume = useAudioStore((state) => state.setMasterVolume);
  const sound = useSound();

  // Scene lifecycle tracking
  const [sceneLifecycle, setSceneLifecycle] = useState<SceneLifecycleState>('ACTIVE');

  const availableSectors: { id: SectorId; label: string }[] = [
    { id: 'SECURITY_CHECKPOINT', label: 'SECURITY GATE' },
    { id: 'CENTRAL_GARAGE', label: 'EV SHOWROOM' },
  ];

  const cameraModes: { id: CameraMode; label: string }[] = [
    { id: 'ORBIT', label: 'ORBIT' },
    { id: 'FIRST_PERSON', label: '1ST PERSON' },
    { id: 'THIRD_PERSON', label: '3RD PERSON' },
  ];

  const qualities: EngineQuality[] = ['LOW', 'MEDIUM', 'HIGH', 'ULTRA'];

  // Test Cinematic Sweep
  const handleCinematicSweep = () => {
    startCinematic([
      { position: [8, 5.5, 8], target: [0, 1.5, 0], durationSeconds: 2.0 },
      { position: [-7, 3.5, 7], target: [0, 1.5, 0], durationSeconds: 2.0 },
      { position: [0, 8.0, 4], target: [0, 1.5, 0], durationSeconds: 2.0 },
      { position: [0, 3.5, 7.5], target: [0, 1.5, 0], durationSeconds: 2.0 },
    ]);
  };

  // Test Focus Target via FocusManager (supports future vehicles, terminals, portals)
  const handleFocusObject = () => {
    if (isFocused) {
      returnToPreviousCamera();
    } else {
      focusObject({
        id: 'biometric_beacon',
        name: 'Biometric Gateway Terminal',
        category: 'TERMINAL',
        targetPosition: [0, 1.5, 0],
      });
    }
  };

  return (
    <div style={{ position: 'relative', width: '100vw', height: '100vh', overflow: 'hidden' }}>
      {/* Central Unified Input Manager (No Per-Object Listener Duplication) */}
      <InputManager />
      {/* Mobile/Tablet Touch Gestures & Selection (08-AF) */}
      <TouchGestureController />

      {/* Foundational 3D Canvas Rig with Unified CameraManager */}
      <Canvas3DFoundation
        enableDefaultLighting={false}
        enableDefaultEnvironment={false}
      >
        {/* Reusable SceneManager Orchestrating Environments & Lifecycles */}
        <SceneManager
          activeSceneId={currentSector}
          onLifecycleChange={(state) => setSceneLifecycle(state)}
          transitionDurationMs={600}
        />
      </Canvas3DFoundation>

      {/* On-Screen Cyber Virtual Joystick for Touch/Mobile (08-AF) */}
      <VirtualTouchJoystick />

      {/* Cyber Transition Loading Veil during Environment Switching */}
      <SceneTransitionVeil />

      {/* Cybernetic HUD Badge & Telemetry */}
      {isHudVisible && (
        <div
          style={{
            position: 'absolute',
            top: '1.5rem',
            left: '1.5rem',
            zIndex: 10,
            background: 'rgba(10, 12, 20, 0.85)',
            backdropFilter: 'blur(16px)',
            border: '1px solid rgba(0, 229, 255, 0.35)',
            borderRadius: '14px',
            padding: '1.25rem 1.5rem',
            boxShadow: '0 12px 40px rgba(0, 0, 0, 0.7), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
            maxWidth: '470px',
            pointerEvents: 'auto',
          }}
        >
          <div
            style={{
              display: 'inline-block',
              padding: '3px 10px',
              borderRadius: '999px',
              background: 'rgba(0, 229, 255, 0.12)',
              border: '1px solid rgba(0, 229, 255, 0.45)',
              color: '#00e5ff',
              fontSize: '0.75rem',
              fontFamily: "'Orbitron', sans-serif",
              letterSpacing: '1.2px',
              marginBottom: '0.6rem',
            }}
          >
            CHECKPOINT 08-AF MOBILE/TABLET 3D INPUT
          </div>

          <h1
            style={{
              fontFamily: "'Orbitron', sans-serif",
              fontSize: '1.25rem',
              fontWeight: 700,
              letterSpacing: '1px',
              color: '#ffffff',
              marginBottom: '0.35rem',
              textShadow: '0 0 16px rgba(0, 229, 255, 0.4)',
            }}
          >
            Mobile &amp; Tablet 3D Touch Engine
          </h1>

          <p
            style={{
              color: '#8a94a6',
              fontSize: '0.78rem',
              lineHeight: 1.4,
              marginBottom: '0.85rem',
            }}
          >
            Touch virtual joystick, tap selection raycasting, touch camera orbit &amp; look, two-finger pinch zoom, and responsive viewport FOV adaptation.
          </p>

          {/* Camera Modes Selector */}
          <div style={{ marginBottom: '0.75rem' }}>
            <div
              style={{
                fontSize: '0.68rem',
                color: '#8a94a6',
                fontFamily: "'JetBrains Mono', monospace",
                marginBottom: '0.3rem',
                letterSpacing: '0.5px',
              }}
            >
              CAMERA PERSPECTIVE MODE:
            </div>
            <div style={{ display: 'flex', gap: '0.35rem' }}>
              {cameraModes.map((m) => (
                <button
                  key={m.id}
                  onClick={() => setCameraMode(m.id)}
                  style={{
                    flex: 1,
                    padding: '0.35rem 0.4rem',
                    fontSize: '0.68rem',
                    fontFamily: "'Orbitron', sans-serif",
                    fontWeight: 600,
                    borderRadius: '6px',
                    border:
                      cameraMode === m.id
                        ? '1px solid #00e5ff'
                        : '1px solid rgba(255, 255, 255, 0.1)',
                    background:
                      cameraMode === m.id
                        ? 'rgba(0, 229, 255, 0.22)'
                        : 'rgba(255, 255, 255, 0.04)',
                    color: cameraMode === m.id ? '#00e5ff' : '#8a94a6',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          {/* Camera Transition Actions */}
          <div style={{ marginBottom: '0.75rem' }}>
            <div
              style={{
                fontSize: '0.68rem',
                color: '#8a94a6',
                fontFamily: "'JetBrains Mono', monospace",
                marginBottom: '0.3rem',
                letterSpacing: '0.5px',
              }}
            >
              CAMERA TRANSITIONS &amp; SEQUENCES:
            </div>
            <div style={{ display: 'flex', gap: '0.35rem' }}>
              <button
                onClick={handleFocusObject}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.68rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '6px',
                  border: isFocused
                    ? '1px solid #00e5ff'
                    : '1px solid rgba(255, 171, 0, 0.4)',
                  background: isFocused
                    ? 'rgba(0, 229, 255, 0.22)'
                    : 'rgba(255, 171, 0, 0.12)',
                  color: isFocused ? '#00e5ff' : '#ffab00',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
              >
                {isFocused ? 'RETURN CAM (ESC)' : 'FOCUS BEACON'}
              </button>
              <button
                onClick={handleCinematicSweep}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.68rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '6px',
                  border: '1px solid rgba(0, 230, 118, 0.4)',
                  background: 'rgba(0, 230, 118, 0.12)',
                  color: '#00e676',
                  cursor: 'pointer',
                }}
              >
                CINEMATIC
              </button>
              <button
                onClick={() => resetCamera()}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.68rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '6px',
                  border: '1px solid rgba(255, 255, 255, 0.2)',
                  background: 'rgba(255, 255, 255, 0.05)',
                  color: '#f0f4fc',
                  cursor: 'pointer',
                }}
              >
                RESET
              </button>
            </div>
          </div>

          {/* Spatial Animation System Controls */}
          <div style={{ marginBottom: '0.75rem' }}>
            <div
              style={{
                fontSize: '0.68rem',
                color: '#8a94a6',
                fontFamily: "'JetBrains Mono', monospace",
                marginBottom: '0.3rem',
                letterSpacing: '0.5px',
              }}
            >
              TRIGGER 3D ANIMATION STATES:
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.3rem' }}>
              {(['HOVER', 'SELECTED', 'ACTIVE', 'LOADING', 'SUCCESS', 'ERROR', 'IDLE'] as AnimationState[]).map(
                (anim) => (
                  <button
                    key={anim}
                    onClick={() => AnimationRegistry.setTrackState('biometric_beacon', anim)}
                    style={{
                      padding: '0.3rem 0.15rem',
                      fontSize: '0.62rem',
                      fontFamily: "'JetBrains Mono', monospace",
                      fontWeight: 600,
                      borderRadius: '4px',
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      background:
                        anim === 'SUCCESS'
                          ? 'rgba(0, 230, 118, 0.18)'
                          : anim === 'ERROR'
                          ? 'rgba(255, 23, 68, 0.18)'
                          : anim === 'ACTIVE'
                          ? 'rgba(255, 234, 0, 0.18)'
                          : 'rgba(255, 255, 255, 0.05)',
                      color:
                        anim === 'SUCCESS'
                          ? '#00e676'
                          : anim === 'ERROR'
                          ? '#ff1744'
                          : anim === 'ACTIVE'
                          ? '#ffea00'
                          : '#ffffff',
                      cursor: 'pointer',
                    }}
                  >
                    {anim}
                  </button>
                )
              )}
            </div>
          </div>

          {/* Audio Engine Controls & Procedural Synth */}
          <div style={{ marginBottom: '0.75rem' }}>
            <div
              style={{
                fontSize: '0.68rem',
                color: '#8a94a6',
                fontFamily: "'JetBrains Mono', monospace",
                marginBottom: '0.3rem',
                letterSpacing: '0.5px',
                display: 'flex',
                justifyContent: 'space-between',
              }}
            >
              <span>AUDIO ENGINE &amp; PROCEDURAL SYNTH:</span>
              <span style={{ color: isMuted ? '#ff1744' : '#00e676' }}>
                {isMuted ? 'MUTED' : `${Math.round(masterVolume * 100)}%`}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '0.35rem', marginBottom: '0.35rem' }}>
              <button
                onClick={() => toggleMute()}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.66rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: isMuted ? '1px solid #ff1744' : '1px solid #00e676',
                  background: isMuted ? 'rgba(255, 23, 68, 0.15)' : 'rgba(0, 230, 118, 0.15)',
                  color: isMuted ? '#ff1744' : '#00e676',
                  cursor: 'pointer',
                }}
              >
                {isMuted ? 'UNMUTE' : 'MUTE'}
              </button>
              <button
                onClick={() => sound.startAmbient()}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.66rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(0, 229, 255, 0.4)',
                  background: 'rgba(0, 229, 255, 0.12)',
                  color: '#00e5ff',
                  cursor: 'pointer',
                }}
              >
                START DRONE
              </button>
              <button
                onClick={() => sound.stopAmbient()}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.66rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(255, 255, 255, 0.2)',
                  background: 'rgba(255, 255, 255, 0.05)',
                  color: '#8a94a6',
                  cursor: 'pointer',
                }}
              >
                STOP DRONE
              </button>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.3rem' }}>
              <button
                onClick={() => sound.playHover()}
                style={{
                  padding: '0.28rem 0.15rem',
                  fontSize: '0.6rem',
                  fontFamily: "'JetBrains Mono', monospace",
                  borderRadius: '4px',
                  border: '1px solid rgba(0, 229, 255, 0.3)',
                  background: 'rgba(0, 229, 255, 0.08)',
                  color: '#00e5ff',
                  cursor: 'pointer',
                }}
              >
                UI BLIP
              </button>
              <button
                onClick={() => sound.playClick()}
                style={{
                  padding: '0.28rem 0.15rem',
                  fontSize: '0.6rem',
                  fontFamily: "'JetBrains Mono', monospace",
                  borderRadius: '4px',
                  border: '1px solid rgba(255, 255, 255, 0.2)',
                  background: 'rgba(255, 255, 255, 0.05)',
                  color: '#f0f4fc',
                  cursor: 'pointer',
                }}
              >
                CLICK POP
              </button>
              <button
                onClick={() => sound.playWarp()}
                style={{
                  padding: '0.28rem 0.15rem',
                  fontSize: '0.6rem',
                  fontFamily: "'JetBrains Mono', monospace",
                  borderRadius: '4px',
                  border: '1px solid rgba(255, 171, 0, 0.3)',
                  background: 'rgba(255, 171, 0, 0.08)',
                  color: '#ffab00',
                  cursor: 'pointer',
                }}
              >
                WARP SWOOP
              </button>
              <button
                onClick={() => sound.playSuccess()}
                style={{
                  padding: '0.28rem 0.15rem',
                  fontSize: '0.6rem',
                  fontFamily: "'JetBrains Mono', monospace",
                  borderRadius: '4px',
                  border: '1px solid rgba(0, 230, 118, 0.3)',
                  background: 'rgba(0, 230, 118, 0.08)',
                  color: '#00e676',
                  cursor: 'pointer',
                }}
              >
                CHIME
              </button>
            </div>
          </div>

          {/* 3D WebGL UI Controls */}
          <div style={{ marginBottom: '0.75rem' }}>
            <div
              style={{
                fontSize: '0.68rem',
                color: '#8a94a6',
                fontFamily: "'JetBrains Mono', monospace",
                marginBottom: '0.3rem',
                letterSpacing: '0.5px',
              }}
            >
              3D WEBGL SPATIAL INTERFACES:
            </div>
            <div style={{ display: 'flex', gap: '0.35rem' }}>
              <button
                onClick={() => useFocusStore.getState().focusObject('security_gate_terminal')}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.66rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(0, 229, 255, 0.4)',
                  background: 'rgba(0, 229, 255, 0.12)',
                  color: '#00e5ff',
                  cursor: 'pointer',
                }}
              >
                FOCUS TERMINAL
              </button>
              <button
                onClick={() => useUI3DStore.getState().openModal('security_protocol_modal')}
                style={{
                  flex: 1,
                  padding: '0.35rem 0.2rem',
                  fontSize: '0.66rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(255, 171, 0, 0.4)',
                  background: 'rgba(255, 171, 0, 0.12)',
                  color: '#ffab00',
                  cursor: 'pointer',
                }}
              >
                OPEN 3D MODAL
              </button>
            </div>
          </div>

          {/* Environment Switching Controls */}
          <div style={{ marginBottom: '0.85rem' }}>
            <div
              style={{
                fontSize: '0.68rem',
                color: '#8a94a6',
                fontFamily: "'JetBrains Mono', monospace",
                marginBottom: '0.3rem',
                letterSpacing: '0.5px',
              }}
            >
              SWITCH ACTIVE ENVIRONMENT:
            </div>
            <div style={{ display: 'flex', gap: '0.35rem' }}>
              {availableSectors.map((sector) => (
                <button
                  key={sector.id}
                  onClick={() => setCurrentSector(sector.id)}
                  style={{
                    flex: 1,
                    padding: '0.35rem 0.4rem',
                    fontSize: '0.68rem',
                    fontFamily: "'Orbitron', sans-serif",
                    fontWeight: 600,
                    borderRadius: '6px',
                    border:
                      currentSector === sector.id
                        ? '1px solid #00e5ff'
                        : '1px solid rgba(255, 255, 255, 0.1)',
                    background:
                      currentSector === sector.id
                        ? 'rgba(0, 229, 255, 0.22)'
                        : 'rgba(255, 255, 255, 0.04)',
                    color: currentSector === sector.id ? '#00e5ff' : '#8a94a6',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  {sector.label}
                </button>
              ))}
            </div>
          </div>

          {/* Normalized Input Telemetry Matrix */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, 1fr)',
              gap: '0.45rem',
              fontSize: '0.72rem',
              fontFamily: "'JetBrains Mono', monospace",
              background: 'rgba(6, 7, 10, 0.8)',
              padding: '0.75rem',
              borderRadius: '8px',
              border: '1px solid rgba(255, 255, 255, 0.06)',
              marginBottom: '0.85rem',
            }}
          >
            <div>
              <span style={{ color: '#8a94a6' }}>Pointer NDC: </span>
              <span style={{ color: '#00e5ff' }}>
                [{pointer.ndcX.toFixed(2)}, {pointer.ndcY.toFixed(2)}]
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Pointer State: </span>
              <span style={{ color: pointer.isDragging ? '#ffab00' : pointer.isDown ? '#00e676' : '#8a94a6' }}>
                {pointer.isDragging ? 'DRAGGING' : pointer.isDown ? 'DOWN' : 'IDLE'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>WASD Axes: </span>
              <span style={{ color: '#00e676' }}>
                F:{movement.forward} S:{movement.strafe}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Modifiers: </span>
              <span style={{ color: movement.sprint ? '#ffab00' : '#8a94a6' }}>
                {movement.sprint ? 'SPRINT' : movement.jump ? 'JUMP' : 'NONE'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Touch Gesture: </span>
              <span style={{ color: '#00e5ff' }}>
                {touch.gesture} ({touch.touchCount})
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Typing Mode: </span>
              <span style={{ color: isTypingMode ? '#ff1744' : '#00e676' }}>
                {isTypingMode ? 'TEXT INPUT' : 'NAVIGATION'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Avatar Pos: </span>
              <span style={{ color: '#00e5ff' }}>
                [{playerPosition[0].toFixed(1)}, {playerPosition[2].toFixed(1)}]
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Player Mode: </span>
              <span style={{ color: playerMovement === 'IDLE' ? '#8a94a6' : '#00e676' }}>
                {playerMovement}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Hover Target: </span>
              <span style={{ color: hoveredObjectId ? '#00e676' : '#8a94a6' }}>
                {hoveredObjectId || 'NONE'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Select Target: </span>
              <span style={{ color: selectedObjectId ? '#ffab00' : '#8a94a6' }}>
                {selectedObjectId || 'NONE'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Focus Target: </span>
              <span style={{ color: isFocused ? '#00e5ff' : '#8a94a6' }}>
                {focusedTargetId || 'NONE'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Focus Preset: </span>
              <span style={{ color: activeCategory ? '#ffab00' : '#8a94a6' }}>
                {activeCategory || 'NONE'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Audio Output: </span>
              <span style={{ color: isMuted ? '#ff1744' : '#00e676' }}>
                {isMuted ? 'MUTED' : `${Math.round(masterVolume * 100)}%`}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Camera Mode: </span>
              <span style={{ color: '#00e5ff' }}>{cameraMode}</span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Active Modal: </span>
              <span style={{ color: activeModalId ? '#ffab00' : '#8a94a6' }}>
                {activeModalId || 'NONE'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Adaptive DPR: </span>
              <span style={{ color: '#00e5ff' }}>{adaptiveDpr.toFixed(2)}</span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Measured FPS: </span>
              <span
                style={{
                  color: measuredFps >= 45 ? '#00e676' : measuredFps >= 28 ? '#ffab00' : '#ff1744',
                }}
              >
                {measuredFps} FPS
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Scene State: </span>
              <span style={{ color: sceneLifecycle === 'ACTIVE' ? '#00e676' : '#ffab00' }}>
                {sceneLifecycle}
              </span>
            </div>
          </div>

          {/* Dynamic Performance Tier Selector (08-AD) */}
          <div>
            <div
              style={{
                fontSize: '0.68rem',
                color: '#8a94a6',
                fontFamily: "'JetBrains Mono', monospace",
                marginBottom: '0.3rem',
                letterSpacing: '0.5px',
              }}
            >
              PERFORMANCE TIER (08-AD):
            </div>
            <div style={{ display: 'flex', gap: '0.35rem' }}>
              {(['LOW', 'MEDIUM', 'HIGH'] as PerformanceTier[]).map((tier) => (
                <button
                  key={tier}
                  onClick={() => {
                    setQuality(tier as EngineQuality);
                    setPerformanceTier(tier);
                  }}
                  style={{
                    flex: 1,
                    padding: '0.32rem 0',
                    fontSize: '0.68rem',
                    fontFamily: "'Orbitron', sans-serif",
                    fontWeight: 600,
                    borderRadius: '6px',
                    border:
                      currentTier === tier
                        ? '1px solid #00e5ff'
                        : '1px solid rgba(255, 255, 255, 0.1)',
                    background:
                      currentTier === tier
                        ? 'rgba(0, 229, 255, 0.2)'
                        : 'rgba(255, 255, 255, 0.03)',
                    color: currentTier === tier ? '#00e5ff' : '#8a94a6',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  {tier}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default App;
