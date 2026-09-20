import React, { useState, useCallback, useEffect } from 'react';
import * as THREE from 'three';
import { useAuthStore } from '@/auth/useAuthStore';
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

  // Auth store
  const currentUser = useAuthStore((state) => state.currentUser);

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
  const handleLifecycleChange = useCallback((state: SceneLifecycleState) => {
    setSceneLifecycle(state);
  }, []);

  // 3D Boot: Initialize and bootstrap authentication session
  useEffect(() => {
    useAuthStore.getState().bootstrapAuth();
  }, []);

  const availableSectors: { id: SectorId; label: string }[] = [
    { id: 'SECURITY_CHECKPOINT', label: 'CỔNG AN NINH' },
    { id: 'CENTRAL_GARAGE', label: 'GARAGE TRUNG TÂM' },
    { id: 'CO_OWNERSHIP_HALL', label: 'SẢNH ĐỒNG SỞ HỮU' },
    { id: 'BOOKING_CHAMBER', label: 'PHÒNG ĐẶT LỊCH' },
    { id: 'ENERGY_FINANCE_CENTER', label: 'TRUNG TÂM TÀI CHÍNH' },
    { id: 'SHARED_FUND_VAULT', label: 'KHO QUỸ CHUNG' },
    { id: 'DIGITAL_CONTRACT_ROOM', label: 'PHÒNG HỢP ĐỒNG' },
    { id: 'DECISION_CHAMBER', label: 'PHÒNG BIỂU QUYẾT' },
    { id: 'AI_INTELLIGENCE_CENTER', label: 'TRÍ TUỆ NHÂN TẠO' },
    { id: 'OPERATIONS_CENTER', label: 'TRUNG TÂM VẬN HÀNH' },
    { id: 'SERVICE_WORKSHOP', label: 'XƯỞNG BẢO TRÌ' },
    { id: 'DISPUTE_ROOM', label: 'GIẢI QUYẾT TRANH CHẤP' },
    { id: 'ADMIN_COMMAND_CENTER', label: 'ĐIỀU HÀNH QUẢN TRỊ' },
  ];

  const cameraModes: { id: CameraMode; label: string }[] = [
    { id: 'ORBIT', label: 'XOAY QUANH' },
    { id: 'FIRST_PERSON', label: 'GÓC NHÌN THỨ 1' },
    { id: 'THIRD_PERSON', label: 'GÓC NHÌN THỨ 3' },
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
        name: 'Trạm Cổng Xác Thực Sinh Trắc Học',
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
          onLifecycleChange={handleLifecycleChange}
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
            ĐỒNG SỞ HỮU XE ĐIỆN — EVSHARE 3D
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
            Hệ Thống Đa Vũ Trụ Xe Điện 3D
          </h1>

          <p
            style={{
              color: '#8a94a6',
              fontSize: '0.78rem',
              lineHeight: 1.4,
              marginBottom: '0.85rem',
            }}
          >
            Tương tác không gian thời gian thực: Cần điều khiển cảm ứng ảo, chạm chọn đối tượng 3D, xoay góc nhìn camera và thu phóng hai ngón tay.
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
              CHẾ ĐỘ GÓC NHÌN CAMERA:
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
              CHUYỂN GÓC NHÌN &amp; TOÀN CẢNH:
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
                {isFocused ? 'TRỞ LẠI (ESC)' : 'TIÊU ĐIỂM'}
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
                TOÀN CẢNH
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
                ĐẶT LẠI
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
              KÍCH HOẠT TRẠNG THÁI HOẠT HỌA 3D:
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
                    {anim === 'HOVER'
                      ? 'RÊ CHUỘT'
                      : anim === 'SELECTED'
                      ? 'ĐÃ CHỌN'
                      : anim === 'ACTIVE'
                      ? 'KÍCH HOẠT'
                      : anim === 'LOADING'
                      ? 'ĐANG TẢI'
                      : anim === 'SUCCESS'
                      ? 'THÀNH CÔNG'
                      : anim === 'ERROR'
                      ? 'LỖI'
                      : 'CHỜ'}
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
              <span>HỆ THỐNG ÂM THANH:</span>
              <span style={{ color: isMuted ? '#ff1744' : '#00e676' }}>
                {isMuted ? 'TẮT TIẾNG' : `${Math.round(masterVolume * 100)}%`}
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
                {isMuted ? 'BẬT TIẾNG' : 'TẮT TIẾNG'}
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
                BẬT ÂM NỀN
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
                DỪNG ÂM NỀN
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
              GIAO DIỆN KHÔNG GIAN 3D WEBGL:
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
                TIÊU ĐIỂM TRẠM 3D
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
                HỘP THOẠI 3D
              </button>
            </div>
          </div>

          {/* Identity & RBAC Authentication Controls */}
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
              <span>XÁC THỰC &amp; DANH TÍNH:</span>
              <span id="hud-auth-status" style={{ color: currentUser ? '#00e676' : '#ffab00' }}>
                {currentUser ? `${currentUser.fullName} (${currentUser.roles.map(r => r === 'ROLE_CO_OWNER' ? 'ĐỒNG SỞ HỮU' : r === 'ROLE_STAFF' ? 'NHÂN VIÊN' : r === 'ROLE_ADMIN' ? 'QUẢN TRỊ VIÊN' : r).join(', ')})` : 'CHƯA ĐĂNG NHẬP (KHÁCH)'}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '0.3rem' }}>
              <button
                id="btn-auth-co-owner"
                onClick={() => {
                  useAuthStore.getState().fillDemoCredentials('CO_OWNER');
                  useAuthStore.getState().login();
                }}
                style={{
                  flex: 1,
                  padding: '0.3rem 0.2rem',
                  fontSize: '0.62rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(0, 229, 255, 0.4)',
                  background: 'rgba(0, 229, 255, 0.12)',
                  color: '#00e5ff',
                  cursor: 'pointer',
                }}
              >
                ĐỒNG SỞ HỮU
              </button>
              <button
                id="btn-auth-staff"
                onClick={() => {
                  useAuthStore.getState().fillDemoCredentials('STAFF');
                  useAuthStore.getState().login();
                }}
                style={{
                  flex: 1,
                  padding: '0.3rem 0.2rem',
                  fontSize: '0.62rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(249, 115, 22, 0.4)',
                  background: 'rgba(249, 115, 22, 0.12)',
                  color: '#f97316',
                  cursor: 'pointer',
                }}
              >
                NHÂN VIÊN
              </button>
              <button
                id="btn-auth-admin"
                onClick={() => {
                  useAuthStore.getState().fillDemoCredentials('ADMIN');
                  useAuthStore.getState().login();
                }}
                style={{
                  flex: 1,
                  padding: '0.3rem 0.2rem',
                  fontSize: '0.62rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(255, 255, 255, 0.4)',
                  background: 'rgba(255, 255, 255, 0.12)',
                  color: '#ffffff',
                  cursor: 'pointer',
                }}
              >
                QUẢN TRỊ
              </button>
              <button
                id="btn-auth-logout"
                onClick={() => useAuthStore.getState().logout()}
                style={{
                  flex: 0.8,
                  padding: '0.3rem 0.2rem',
                  fontSize: '0.62rem',
                  fontFamily: "'Orbitron', sans-serif",
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid rgba(255, 23, 68, 0.4)',
                  background: 'rgba(255, 23, 68, 0.12)',
                  color: '#ff1744',
                  cursor: 'pointer',
                }}
              >
                ĐĂNG XUẤT
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
                display: 'flex',
                justifyContent: 'space-between',
              }}
            >
              <span>CHUYỂN KHÔNG GIAN 3D:</span>
              <span id="hud-active-sector" style={{ color: '#00e5ff' }}>
                {availableSectors.find(s => s.id === currentSector)?.label || currentSector}
              </span>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.3rem' }}>
              {availableSectors.map((sector) => (
                <button
                  id={`nav-sector-${sector.id}`}
                  key={sector.id}
                  onClick={() => setCurrentSector(sector.id)}
                  style={{
                    padding: '0.32rem 0.15rem',
                    fontSize: '0.6rem',
                    fontFamily: "'Orbitron', sans-serif",
                    fontWeight: 600,
                    borderRadius: '5px',
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
              <span style={{ color: '#8a94a6' }}>Tọa độ con trỏ: </span>
              <span style={{ color: '#00e5ff' }}>
                [{pointer.ndcX.toFixed(2)}, {pointer.ndcY.toFixed(2)}]
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Trạng thái trỏ: </span>
              <span style={{ color: pointer.isDragging ? '#ffab00' : pointer.isDown ? '#00e676' : '#8a94a6' }}>
                {pointer.isDragging ? 'ĐANG KÉO' : pointer.isDown ? 'NHẤN' : 'CHỜ'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Trục di chuyển: </span>
              <span style={{ color: '#00e676' }}>
                Tiến:{movement.forward} Ngang:{movement.strafe}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Phím bổ trợ: </span>
              <span style={{ color: movement.sprint ? '#ffab00' : '#8a94a6' }}>
                {movement.sprint ? 'CHẠY NHANH' : movement.jump ? 'NHẢY' : 'KHÔNG'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Cảm ứng: </span>
              <span style={{ color: '#00e5ff' }}>
                {touch.gesture} ({touch.touchCount} điểm)
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Chế độ nhập: </span>
              <span style={{ color: isTypingMode ? '#ff1744' : '#00e676' }}>
                {isTypingMode ? 'NHẬP VĂN BẢN' : 'ĐIỀU HƯỚNG'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Vị trí nhân vật: </span>
              <span style={{ color: '#00e5ff' }}>
                [{playerPosition[0].toFixed(1)}, {playerPosition[2].toFixed(1)}]
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Trạng thái đi: </span>
              <span style={{ color: playerMovement === 'IDLE' ? '#8a94a6' : '#00e676' }}>
                {playerMovement === 'IDLE' ? 'ĐỨNG YÊN' : playerMovement === 'WALKING' ? 'ĐANG ĐI' : 'CHẠY'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Mục tiêu rê chuột: </span>
              <span style={{ color: hoveredObjectId ? '#00e676' : '#8a94a6' }}>
                {hoveredObjectId || 'KHÔNG'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Mục tiêu chọn: </span>
              <span style={{ color: selectedObjectId ? '#ffab00' : '#8a94a6' }}>
                {selectedObjectId || 'KHÔNG'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Mục tiêu tiêu điểm: </span>
              <span style={{ color: isFocused ? '#00e5ff' : '#8a94a6' }}>
                {focusedTargetId || 'KHÔNG'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Thể loại tiêu điểm: </span>
              <span style={{ color: activeCategory ? '#ffab00' : '#8a94a6' }}>
                {activeCategory || 'KHÔNG'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Âm thanh đầu ra: </span>
              <span style={{ color: isMuted ? '#ff1744' : '#00e676' }}>
                {isMuted ? 'TẮT TIẾNG' : `${Math.round(masterVolume * 100)}%`}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Góc nhìn camera: </span>
              <span style={{ color: '#00e5ff' }}>
                {cameraMode === 'ORBIT' ? 'XOAY QUANH' : cameraMode === 'FIRST_PERSON' ? 'GÓC NHÌN THỨ 1' : 'GÓC NHÌN THỨ 3'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Hộp thoại mở: </span>
              <span style={{ color: activeModalId ? '#ffab00' : '#8a94a6' }}>
                {activeModalId || 'KHÔNG'}
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Tỷ lệ DPR: </span>
              <span style={{ color: '#00e5ff' }}>{adaptiveDpr.toFixed(2)}</span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Khung hình (FPS): </span>
              <span
                style={{
                  color: measuredFps >= 45 ? '#00e676' : measuredFps >= 28 ? '#ffab00' : '#ff1744',
                }}
              >
                {measuredFps} FPS
              </span>
            </div>
            <div>
              <span style={{ color: '#8a94a6' }}>Trạng thái phòng: </span>
              <span style={{ color: sceneLifecycle === 'ACTIVE' ? '#00e676' : '#ffab00' }}>
                {sceneLifecycle === 'ACTIVE' ? 'HOẠT ĐỘNG' : sceneLifecycle}
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
              MỨC HIỆU NĂNG ĐỒ HỌA:
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
                  {tier === 'LOW' ? 'THẤP' : tier === 'MEDIUM' ? 'TRUNG BÌNH' : 'CAO'}
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
