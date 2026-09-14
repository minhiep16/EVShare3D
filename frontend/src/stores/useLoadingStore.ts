import { create } from 'zustand';

interface LoadingState {
  // Scoped loading map: key -> boolean
  loadingMap: Record<string, boolean>;

  // Progress tracker (0 to 100)
  progress: number;
  loadingMessage: string | null;

  // Actions
  startLoading: (key: string, message?: string) => void;
  stopLoading: (key: string) => void;
  isLoading: (key: string) => boolean;
  isAnyLoading: () => boolean;
  setProgress: (progress: number, message?: string) => void;
  resetLoading: () => void;
}

export const useLoadingStore = create<LoadingState>((set, get) => ({
  loadingMap: {},
  progress: 100,
  loadingMessage: null,

  startLoading: (key, message) =>
    set((state) => ({
      loadingMap: { ...state.loadingMap, [key]: true },
      loadingMessage: message ?? state.loadingMessage,
    })),

  stopLoading: (key) =>
    set((state) => {
      const nextMap = { ...state.loadingMap };
      delete nextMap[key];
      const stillLoading = Object.values(nextMap).some(Boolean);
      return {
        loadingMap: nextMap,
        loadingMessage: stillLoading ? state.loadingMessage : null,
      };
    }),

  isLoading: (key) => Boolean(get().loadingMap[key]),

  isAnyLoading: () => Object.values(get().loadingMap).some(Boolean),

  setProgress: (progress, message) =>
    set((state) => ({
      progress: Math.min(100, Math.max(0, progress)),
      loadingMessage: message ?? state.loadingMessage,
    })),

  resetLoading: () =>
    set({
      loadingMap: {},
      progress: 100,
      loadingMessage: null,
    }),
}));
