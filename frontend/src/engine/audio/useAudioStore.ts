import { create } from 'zustand';
import { AudioSettings, SoundCategory } from './audioTypes';

interface AudioStoreState extends AudioSettings {
  setMasterVolume: (volume: number) => void;
  setCategoryVolume: (category: SoundCategory, volume: number) => void;
  toggleMute: () => void;
  setMuted: (isMuted: boolean) => void;
}

export const useAudioStore = create<AudioStoreState>((set) => ({
  masterVolume: 0.8,
  uiVolume: 0.7,
  ambientVolume: 0.4,
  transitionVolume: 0.8,
  notificationVolume: 0.8,
  isMuted: false,

  setMasterVolume: (volume) =>
    set({ masterVolume: Math.max(0, Math.min(1, volume)) }),

  setCategoryVolume: (category, volume) => {
    const clamped = Math.max(0, Math.min(1, volume));
    set((state) => {
      switch (category) {
        case 'UI':
          return { ...state, uiVolume: clamped };
        case 'AMBIENT':
          return { ...state, ambientVolume: clamped };
        case 'TRANSITION':
          return { ...state, transitionVolume: clamped };
        case 'NOTIFICATION':
          return { ...state, notificationVolume: clamped };
        default:
          return state;
      }
    });
  },

  toggleMute: () => set((state) => ({ isMuted: !state.isMuted })),
  setMuted: (isMuted) => set({ isMuted }),
}));
