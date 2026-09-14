import { create } from 'zustand';
import { CursorMode } from './types';

interface InteractionState {
  // Raycast Targets & Selection
  hoveredObjectId: string | null;
  selectedObjectId: string | null;
  focusedInputId: string | null;

  // Pointer & Cursor
  cursorMode: CursorMode;
  isPointerLocked: boolean;
  pointerCoords: [number, number]; // Normalized Device Coordinates [-1, 1]

  // Active Spatial Manipulation
  isInteracting: boolean;
  activeDragId: string | null;

  // Actions
  setHoveredObjectId: (id: string | null) => void;
  setSelectedObjectId: (id: string | null) => void;
  setFocusedInputId: (id: string | null) => void;
  setCursorMode: (mode: CursorMode) => void;
  setPointerLocked: (locked: boolean) => void;
  setPointerCoords: (coords: [number, number]) => void;
  setActiveDragId: (id: string | null) => void;
  clearInteraction: () => void;
}

export const useInteractionStore = create<InteractionState>((set) => ({
  hoveredObjectId: null,
  selectedObjectId: null,
  focusedInputId: null,

  cursorMode: 'DEFAULT',
  isPointerLocked: false,
  pointerCoords: [0, 0],

  isInteracting: false,
  activeDragId: null,

  setHoveredObjectId: (hoveredObjectId) =>
    set({
      hoveredObjectId,
      cursorMode: hoveredObjectId ? 'POINTER' : 'DEFAULT',
    }),

  setSelectedObjectId: (selectedObjectId) =>
    set({
      selectedObjectId,
      isInteracting: Boolean(selectedObjectId),
    }),

  setFocusedInputId: (focusedInputId) =>
    set({
      focusedInputId,
      cursorMode: focusedInputId ? 'TEXT' : 'DEFAULT',
    }),

  setCursorMode: (cursorMode) => set({ cursorMode }),
  setPointerLocked: (isPointerLocked) => set({ isPointerLocked }),
  setPointerCoords: (pointerCoords) => set({ pointerCoords }),
  setActiveDragId: (activeDragId) =>
    set({
      activeDragId,
      cursorMode: activeDragId ? 'GRABBING' : 'DEFAULT',
    }),

  clearInteraction: () =>
    set({
      hoveredObjectId: null,
      selectedObjectId: null,
      focusedInputId: null,
      cursorMode: 'DEFAULT',
      isInteracting: false,
      activeDragId: null,
    }),
}));
