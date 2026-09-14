import { create } from 'zustand';
import { SectorId } from './types';

interface AuthSession {
  token: string | null;
  userId: number | null;
  username: string | null;
  roles: string[];
}

interface AppState {
  // Navigation & Sector
  currentSector: SectorId;
  targetSector: SectorId | null;
  isTeleporting: boolean;

  // Selected Entity References (IDs only - no duplicate entity truth)
  selectedVehicleId: number | null;
  selectedGroupId: number | null;
  selectedProposalId: number | null;
  selectedDisputeId: number | null;

  // Authentication & Session
  auth: AuthSession;
  isAuthenticated: boolean;

  // Network & Connectivity
  isOnline: boolean;

  // Actions
  setCurrentSector: (sector: SectorId) => void;
  initiateTeleport: (target: SectorId) => void;
  completeTeleport: () => void;
  setSelectedVehicleId: (id: number | null) => void;
  setSelectedGroupId: (id: number | null) => void;
  setSelectedProposalId: (id: number | null) => void;
  setSelectedDisputeId: (id: number | null) => void;
  setAuthSession: (session: AuthSession) => void;
  clearAuthSession: () => void;
  setOnline: (isOnline: boolean) => void;
}

export const useAppStore = create<AppState>((set) => ({
  currentSector: 'SECURITY_CHECKPOINT',
  targetSector: null,
  isTeleporting: false,

  selectedVehicleId: null,
  selectedGroupId: null,
  selectedProposalId: null,
  selectedDisputeId: null,

  auth: {
    token: null,
    userId: null,
    username: null,
    roles: [],
  },
  isAuthenticated: false,

  isOnline: typeof navigator !== 'undefined' ? navigator.onLine : true,

  setCurrentSector: (currentSector) =>
    set({ currentSector, targetSector: null, isTeleporting: false }),

  initiateTeleport: (targetSector) =>
    set({ targetSector, isTeleporting: true }),

  completeTeleport: () =>
    set((state) => ({
      currentSector: state.targetSector || state.currentSector,
      targetSector: null,
      isTeleporting: false,
    })),

  setSelectedVehicleId: (selectedVehicleId) => set({ selectedVehicleId }),
  setSelectedGroupId: (selectedGroupId) => set({ selectedGroupId }),
  setSelectedProposalId: (selectedProposalId) => set({ selectedProposalId }),
  setSelectedDisputeId: (selectedDisputeId) => set({ selectedDisputeId }),

  setAuthSession: (auth) =>
    set({
      auth,
      isAuthenticated: Boolean(auth.token && auth.userId),
    }),

  clearAuthSession: () =>
    set({
      auth: { token: null, userId: null, username: null, roles: [] },
      isAuthenticated: false,
    }),

  setOnline: (isOnline) => set({ isOnline }),
}));
