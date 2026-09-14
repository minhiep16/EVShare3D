import { create } from 'zustand';
import { useInputStore } from '../input/useInputStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { TouchTapEvent, VirtualJoystickState } from './touchTypes';

interface TouchState {
  isTouchDevice: boolean;
  isTouchControlsVisible: boolean;
  joystickState: VirtualJoystickState;
  lastTap: TouchTapEvent | null;

  // Actions
  setTouchDevice: (isTouch: boolean) => void;
  setTouchControlsVisible: (visible: boolean) => void;
  updateJoystick: (origin: [number, number], current: [number, number], vector: [number, number]) => void;
  resetJoystick: () => void;
  registerTap: (tap: TouchTapEvent) => void;
}

export const useTouchStore = create<TouchState>((set, get) => ({
  isTouchDevice:
    typeof window !== 'undefined' &&
    ('ontouchstart' in window || navigator.maxTouchPoints > 0),
  isTouchControlsVisible: true,
  joystickState: {
    active: false,
    origin: [0, 0],
    current: [0, 0],
    vector: [0, 0],
  },
  lastTap: null,

  setTouchDevice: (isTouchDevice: boolean) => set({ isTouchDevice }),
  setTouchControlsVisible: (isTouchControlsVisible: boolean) => set({ isTouchControlsVisible }),

  updateJoystick: (origin, current, vector) => {
    set({
      joystickState: {
        active: true,
        origin,
        current,
        vector,
      },
    });

    // Forward vector maps directly to player movement
    const strafe = vector[0];
    const forward = vector[1];

    useInputStore.getState().setMovement({
      forward,
      strafe,
      sprint: Math.hypot(strafe, forward) > 0.85,
      jump: false,
      interact: false,
    });

    if (forward !== 0 || strafe !== 0) {
      usePlayerStore.getState().setMovementMode(
        Math.hypot(strafe, forward) > 0.85 ? 'SPRINTING' : 'WALKING'
      );
    } else {
      usePlayerStore.getState().setMovementMode('IDLE');
    }
  },

  resetJoystick: () => {
    set({
      joystickState: {
        active: false,
        origin: [0, 0],
        current: [0, 0],
        vector: [0, 0],
      },
    });

    useInputStore.getState().setMovement({
      forward: 0,
      strafe: 0,
      sprint: false,
      jump: false,
      interact: false,
    });

    usePlayerStore.getState().setMovementMode('IDLE');
  },

  registerTap: (lastTap: TouchTapEvent) => set({ lastTap }),
}));
