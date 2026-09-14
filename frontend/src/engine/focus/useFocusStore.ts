import { create } from 'zustand';
import { FocusCategory, FocusPreset, FocusTargetConfig, SavedCameraState } from './focusTypes';
import { FocusRegistry } from './FocusRegistry';
import { useCameraStore } from '@/stores/useCameraStore';

interface FocusState {
  focusedTargetId: string | null;
  activeCategory: FocusCategory | null;
  isFocused: boolean;
  activeConfig: FocusTargetConfig | null;
  savedCameraStack: SavedCameraState[];

  // Actions
  focusObject: (
    targetOrId: FocusTargetConfig | string,
    customOverrides?: Partial<FocusPreset>
  ) => void;
  returnToPreviousCamera: () => void;
  exitFocus: () => void;
  isObjectFocused: (id: string) => boolean;
}

export const useFocusStore = create<FocusState>((set, get) => ({
  focusedTargetId: null,
  activeCategory: null,
  isFocused: false,
  activeConfig: null,
  savedCameraStack: [],

  focusObject: (targetOrId, customOverrides) => {
    let targetConfig: FocusTargetConfig | undefined;

    if (typeof targetOrId === 'string') {
      targetConfig = FocusRegistry.getTarget(targetOrId);
      if (!targetConfig) {
        console.warn(`[FocusManager] Target with ID "${targetOrId}" not found in FocusRegistry.`);
        return;
      }
    } else {
      targetConfig = targetOrId;
    }

    const currentCamera = useCameraStore.getState();

    // 1. Save Current Camera State (if not already focusing)
    const savedState: SavedCameraState = {
      transform: {
        position: [...currentCamera.currentPosition],
        target: [...currentCamera.currentTarget],
        fov: currentCamera.currentFov,
      },
      mode: currentCamera.mode,
    };

    // 2. Resolve Framing Geometry
    const defaultPreset = FocusRegistry.getPresetForCategory(targetConfig.category);
    const resolvedPreset: FocusPreset = {
      ...defaultPreset,
      ...targetConfig.preset,
      ...customOverrides,
    };

    const targetPos = targetConfig.targetPosition;
    const azimuth = resolvedPreset.azimuth ?? 0;
    const elevation = resolvedPreset.elevation;
    const baseDistance = resolvedPreset.distance;

    // Scale distance if bounding radius is supplied
    const distance = targetConfig.boundingRadius
      ? Math.max(baseDistance, targetConfig.boundingRadius * 2.2)
      : baseDistance;

    const eyeX = targetPos[0] + distance * Math.sin(azimuth);
    const eyeY = targetPos[1] + elevation;
    const eyeZ = targetPos[2] + distance * Math.cos(azimuth);

    // 3. Update Camera to INSPECT and Smoothly Glide
    currentCamera.setMode('INSPECT');
    currentCamera.moveTo([eyeX, eyeY, eyeZ], targetPos, {
      fov: resolvedPreset.fov ?? 40,
      speed: resolvedPreset.speed ?? 4.5,
      immediate: false, // Invariant: No hard teleport!
    });

    targetConfig.onFocusEnter?.();

    set((state) => ({
      focusedTargetId: targetConfig!.id,
      activeCategory: targetConfig!.category,
      isFocused: true,
      activeConfig: targetConfig!,
      savedCameraStack: [...state.savedCameraStack, savedState],
    }));
  },

  returnToPreviousCamera: () => {
    const { savedCameraStack, activeConfig } = get();
    if (savedCameraStack.length === 0) {
      // Default fallback
      useCameraStore.getState().resetCamera();
      set({ focusedTargetId: null, activeCategory: null, isFocused: false, activeConfig: null });
      return;
    }

    // Pop the previous camera state
    const previous = savedCameraStack[savedCameraStack.length - 1];
    const newStack = savedCameraStack.slice(0, savedCameraStack.length - 1);

    const camera = useCameraStore.getState();

    // Smoothly restore previous camera transform and mode
    camera.setMode(previous.mode);
    camera.moveTo(previous.transform.position, previous.transform.target, {
      fov: previous.transform.fov,
      speed: 4.5,
      immediate: false,
    });

    activeConfig?.onFocusExit?.();

    set({
      focusedTargetId: null,
      activeCategory: null,
      isFocused: false,
      activeConfig: null,
      savedCameraStack: newStack,
    });
  },

  exitFocus: () => {
    get().returnToPreviousCamera();
  },

  isObjectFocused: (id) => get().focusedTargetId === id,
}));
