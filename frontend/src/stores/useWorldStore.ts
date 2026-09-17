import { create } from 'zustand';

export type LightingProfile =
  | 'CYBER_NEON'
  | 'CLEAN_DAYLIGHT'
  | 'DEEP_NIGHT'
  | 'VAULT_AMBER'
  | 'AMBER_WARM'
  | 'FINANCE_EMERALD'
  | 'EXECUTIVE_SLATE'
  | 'PARLIAMENT_INDIGO'
  | 'NEURAL_PURPLE'
  | 'OPERATIONS_ORANGE'
  | 'WORKSHOP_STEEL'
  | 'DISPUTE_CRIMSON'
  | 'COMMAND_HORIZON';

interface RoomBounds {
  minX: number;
  maxX: number;
  minZ: number;
  maxZ: number;
}

interface WorldState {
  // 3D Scene Environment
  lightingProfile: LightingProfile;
  ambientAudioVolume: number;
  isAudioMuted: boolean;
  sectorLoaded: boolean;
  activeRoomBounds: RoomBounds | null;

  // Debug & Developer Visualizers
  showDebugWireframes: boolean;
  showDebugColliders: boolean;
  showPerfStats: boolean;

  // Actions
  setLightingProfile: (profile: LightingProfile) => void;
  setAmbientAudioVolume: (volume: number) => void;
  toggleAudioMute: () => void;
  setSectorLoaded: (loaded: boolean) => void;
  setActiveRoomBounds: (bounds: RoomBounds | null) => void;
  setShowDebugWireframes: (show: boolean) => void;
  setShowDebugColliders: (show: boolean) => void;
  setShowPerfStats: (show: boolean) => void;
}

export const useWorldStore = create<WorldState>((set) => ({
  lightingProfile: 'CYBER_NEON',
  ambientAudioVolume: 0.5,
  isAudioMuted: false,
  sectorLoaded: false,
  activeRoomBounds: null,

  showDebugWireframes: false,
  showDebugColliders: false,
  showPerfStats: false,

  setLightingProfile: (lightingProfile) => set({ lightingProfile }),
  setAmbientAudioVolume: (ambientAudioVolume) => set({ ambientAudioVolume }),
  toggleAudioMute: () => set((state) => ({ isAudioMuted: !state.isAudioMuted })),
  setSectorLoaded: (sectorLoaded) => set({ sectorLoaded }),
  setActiveRoomBounds: (activeRoomBounds) => set({ activeRoomBounds }),
  setShowDebugWireframes: (showDebugWireframes) => set({ showDebugWireframes }),
  setShowDebugColliders: (showDebugColliders) => set({ showDebugColliders }),
  setShowPerfStats: (showPerfStats) => set({ showPerfStats }),
}));
