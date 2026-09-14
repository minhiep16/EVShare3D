import { create } from 'zustand';
import { CameraMode } from './types';
import { CinematicKeyframe, FocusOptions, MoveOptions } from '@/engine/camera/cameraTypes';

interface CameraState {
  // Mode & Transform
  mode: CameraMode;
  previousMode: CameraMode;
  currentPosition: [number, number, number];
  desiredPosition: [number, number, number];
  currentTarget: [number, number, number];
  desiredTarget: [number, number, number];
  currentFov: number;
  desiredFov: number;

  // Orbit / Constraint Limits
  minDistance: number;
  maxDistance: number;
  enableDamping: boolean;

  // Transition & Interpolation
  isTransitioning: boolean;
  interpolationSpeed: number;
  immediateSnap: boolean;
  inspectionTargetId: string | null;

  // Cinematic Sequences
  cinematicKeyframes: CinematicKeyframe[];
  currentCinematicIndex: number;
  cinematicElapsed: number;

  // Backward compatibility getters
  position: [number, number, number];
  target: [number, number, number];
  fov: number;

  // Actions
  setMode: (mode: CameraMode) => void;
  moveTo: (
    position: [number, number, number],
    target: [number, number, number],
    options?: MoveOptions
  ) => void;
  setPosition: (position: [number, number, number], immediate?: boolean) => void;
  setTarget: (target: [number, number, number], immediate?: boolean) => void;
  setFov: (fov: number, immediate?: boolean) => void;
  focusTarget: (
    targetId: string,
    targetPosition: [number, number, number],
    options?: FocusOptions
  ) => void;
  startCinematic: (keyframes: CinematicKeyframe[]) => void;
  stopCinematic: () => void;
  resetCamera: (defaults?: {
    position?: [number, number, number];
    target?: [number, number, number];
    fov?: number;
  }) => void;
  updateRenderedTransform: (
    currentPosition: [number, number, number],
    currentTarget: [number, number, number],
    currentFov: number,
    isComplete: boolean
  ) => void;
}

const DEFAULT_ORIGIN: [number, number, number] = [0, 3.5, 7.5];
const DEFAULT_LOOKAT: [number, number, number] = [0, 1.5, 0];
const DEFAULT_FOV = 45;

export const useCameraStore = create<CameraState>((set, get) => ({
  mode: 'ORBIT',
  previousMode: 'ORBIT',
  currentPosition: [...DEFAULT_ORIGIN],
  desiredPosition: [...DEFAULT_ORIGIN],
  currentTarget: [...DEFAULT_LOOKAT],
  desiredTarget: [...DEFAULT_LOOKAT],
  currentFov: DEFAULT_FOV,
  desiredFov: DEFAULT_FOV,

  // Orbit parameters
  minDistance: 1.5,
  maxDistance: 60,
  enableDamping: true,

  // Transition states
  isTransitioning: false,
  interpolationSpeed: 4.5,
  immediateSnap: false,
  inspectionTargetId: null,

  // Cinematic keyframe state
  cinematicKeyframes: [],
  currentCinematicIndex: 0,
  cinematicElapsed: 0,

  // Compatibility aliases
  position: [...DEFAULT_ORIGIN],
  target: [...DEFAULT_LOOKAT],
  fov: DEFAULT_FOV,

  setMode: (mode) =>
    set((state) => ({
      previousMode: state.mode,
      mode,
      isTransitioning: true,
      immediateSnap: false,
    })),

  moveTo: (position, target, options) => {
    const immediate = options?.immediate ?? false;
    const desiredFov = options?.fov ?? get().desiredFov;
    const speed = options?.speed ?? get().interpolationSpeed;

    if (immediate) {
      set({
        currentPosition: [...position],
        desiredPosition: [...position],
        currentTarget: [...target],
        desiredTarget: [...target],
        currentFov: desiredFov,
        desiredFov,
        position: [...position],
        target: [...target],
        fov: desiredFov,
        immediateSnap: true,
        isTransitioning: false,
        interpolationSpeed: speed,
      });
    } else {
      set({
        desiredPosition: [...position],
        desiredTarget: [...target],
        desiredFov,
        immediateSnap: false,
        isTransitioning: true,
        interpolationSpeed: speed,
      });
    }
  },

  setPosition: (position, immediate = false) => {
    if (immediate) {
      set({
        currentPosition: [...position],
        desiredPosition: [...position],
        position: [...position],
        immediateSnap: true,
      });
    } else {
      set({
        desiredPosition: [...position],
        immediateSnap: false,
        isTransitioning: true,
      });
    }
  },

  setTarget: (target, immediate = false) => {
    if (immediate) {
      set({
        currentTarget: [...target],
        desiredTarget: [...target],
        target: [...target],
        immediateSnap: true,
      });
    } else {
      set({
        desiredTarget: [...target],
        immediateSnap: false,
        isTransitioning: true,
      });
    }
  },

  setFov: (fov, immediate = false) => {
    if (immediate) {
      set({ currentFov: fov, desiredFov: fov, fov, immediateSnap: true });
    } else {
      set({ desiredFov: fov, immediateSnap: false, isTransitioning: true });
    }
  },

  focusTarget: (targetId, targetPosition, options) => {
    const distance = options?.distance ?? 3.5;
    const elevation = options?.elevation ?? 1.2;
    const azimuthAngle = options?.azimuthAngle ?? 0;
    const fov = options?.fov ?? 40;
    const immediate = options?.immediate ?? false;

    // Calculate focal vantage point around target
    const eyeX = targetPosition[0] + distance * Math.sin(azimuthAngle);
    const eyeY = targetPosition[1] + elevation;
    const eyeZ = targetPosition[2] + distance * Math.cos(azimuthAngle);

    set((state) => ({
      previousMode: state.mode,
      mode: 'INSPECT',
      inspectionTargetId: targetId,
    }));

    get().moveTo([eyeX, eyeY, eyeZ], targetPosition, {
      fov,
      immediate,
      speed: options?.speed ?? 4.0,
    });
  },

  startCinematic: (keyframes) => {
    if (!keyframes || keyframes.length === 0) return;
    const first = keyframes[0];
    set({
      mode: 'CINEMATIC',
      cinematicKeyframes: keyframes,
      currentCinematicIndex: 0,
      cinematicElapsed: 0,
      desiredPosition: [...first.position],
      desiredTarget: [...first.target],
      desiredFov: first.fov ?? DEFAULT_FOV,
      isTransitioning: true,
      immediateSnap: false,
      interpolationSpeed: 2.5,
    });
  },

  stopCinematic: () => {
    set((state) => ({
      mode: state.previousMode === 'CINEMATIC' ? 'ORBIT' : state.previousMode,
      cinematicKeyframes: [],
      currentCinematicIndex: 0,
      cinematicElapsed: 0,
      isTransitioning: true,
    }));
  },

  resetCamera: (defaults) => {
    const pos = defaults?.position ?? DEFAULT_ORIGIN;
    const tgt = defaults?.target ?? DEFAULT_LOOKAT;
    const fov = defaults?.fov ?? DEFAULT_FOV;

    set({
      mode: 'ORBIT',
      inspectionTargetId: null,
      cinematicKeyframes: [],
    });

    get().moveTo(pos, tgt, { fov, immediate: false, speed: 4.0 });
  },

  updateRenderedTransform: (currentPosition, currentTarget, currentFov, isComplete) => {
    set({
      currentPosition: [...currentPosition],
      currentTarget: [...currentTarget],
      currentFov,
      position: [...currentPosition],
      target: [...currentTarget],
      fov: currentFov,
      isTransitioning: !isComplete,
      immediateSnap: false,
    });
  },
}));
