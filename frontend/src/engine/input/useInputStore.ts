import { create } from 'zustand';
import { NormalizedMovement, NormalizedPointer, NormalizedTouch } from './inputTypes';

interface InputState {
  pointer: NormalizedPointer;
  movement: NormalizedMovement;
  touch: NormalizedTouch;
  isTypingMode: boolean;
  isPointerLocked: boolean;

  // Actions
  setPointer: (pointer: Partial<NormalizedPointer>) => void;
  setMovement: (movement: Partial<NormalizedMovement>) => void;
  setTouch: (touch: Partial<NormalizedTouch>) => void;
  setTypingMode: (isTyping: boolean) => void;
  setPointerLocked: (locked: boolean) => void;
  resetInputs: () => void;
}

const DEFAULT_POINTER: NormalizedPointer = {
  screenX: 0,
  screenY: 0,
  ndcX: 0,
  ndcY: 0,
  deltaX: 0,
  deltaY: 0,
  isDown: false,
  isDragging: false,
  button: 0,
  pointerType: 'mouse',
};

const DEFAULT_MOVEMENT: NormalizedMovement = {
  forward: 0,
  strafe: 0,
  sprint: false,
  jump: false,
  interact: false,
};

const DEFAULT_TOUCH: NormalizedTouch = {
  touchCount: 0,
  primaryTouch: null,
  pinchDistance: null,
  pinchDelta: 0,
  gesture: 'NONE',
};

export const useInputStore = create<InputState>((set) => ({
  pointer: { ...DEFAULT_POINTER },
  movement: { ...DEFAULT_MOVEMENT },
  touch: { ...DEFAULT_TOUCH },
  isTypingMode: false,
  isPointerLocked: false,

  setPointer: (partial) =>
    set((state) => ({
      pointer: { ...state.pointer, ...partial },
    })),

  setMovement: (partial) =>
    set((state) => ({
      movement: { ...state.movement, ...partial },
    })),

  setTouch: (partial) =>
    set((state) => ({
      touch: { ...state.touch, ...partial },
    })),

  setTypingMode: (isTypingMode) => set({ isTypingMode }),
  setPointerLocked: (isPointerLocked) => set({ isPointerLocked }),

  resetInputs: () =>
    set({
      pointer: { ...DEFAULT_POINTER },
      movement: { ...DEFAULT_MOVEMENT },
      touch: { ...DEFAULT_TOUCH },
    }),
}));
