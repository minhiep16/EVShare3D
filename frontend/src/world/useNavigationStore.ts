import { create } from 'zustand';
import type { SectorId, UserRole } from './worldTypes';
import { SECTOR_METADATA_REGISTRY } from './worldCoordinates';
import { getSectorSpawnPoint, getDefaultSpawnPoint } from './spawnPoints';
import { useWorldEnvironmentStore } from './useWorldEnvironmentStore';
import { useWorldLoaderStore } from './WorldLoader';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { useLoadingStore } from '@/stores/useLoadingStore';
import { useAppStore } from '@/stores/useAppStore';
import { AudioEngine } from '@/engine/audio/AudioEngine';

export type TransitionPhase = 'IDLE' | 'WARPING' | 'STREAMING' | 'ARRIVING';

export interface DeniedAccessNotice {
  portalId: string;
  requiredRole: UserRole;
  targetSectorId: SectorId;
  message: string;
  timestamp: number;
}

interface NavigationState {
  currentSector: SectorId;
  destinationSector: SectorId | null;
  navigationHistory: SectorId[];
  transitionPhase: TransitionPhase;
  deniedAccessNotice: DeniedAccessNotice | null;

  // Active User Simulated Role (for testing and RBAC enforcement)
  userRole: UserRole;

  // Actions
  setUserRole: (role: UserRole) => void;
  canAccessSector: (sectorId: SectorId) => boolean;
  handleSectorBoundaryIntrusion: (intrudedSector: SectorId) => boolean;
  teleportToSector: (
    targetSectorId: SectorId,
    portalId?: string,
    options?: { isReturn?: boolean }
  ) => Promise<boolean>;
  teleportReturn: () => Promise<boolean>;
  clearDeniedNotice: () => void;
}

export const useNavigationStore = create<NavigationState>((set, get) => ({
  currentSector: 'SECURITY_CHECKPOINT',
  destinationSector: null,
  navigationHistory: [],
  transitionPhase: 'IDLE',
  deniedAccessNotice: null,
  userRole: 'ROLE_CO_OWNER', // Default user role in world session

  setUserRole: (userRole: UserRole) => set({ userRole }),

  canAccessSector: (sectorId: SectorId) => {
    const { userRole } = get();
    const isAuthenticated = useAppStore.getState().isAuthenticated || userRole !== 'GUEST';

    // 1. Unauthenticated guests are strictly restricted to SECURITY_CHECKPOINT
    if ((!isAuthenticated || userRole === 'GUEST') && sectorId !== 'SECURITY_CHECKPOINT') {
      return false;
    }

    // 2. ADMIN has unrestricted oversight deck and access across all sectors
    if (userRole === 'ROLE_ADMIN') {
      return true;
    }

    // 3. Security Checkpoint and Central Garage are accessible by all authenticated users
    if (sectorId === 'SECURITY_CHECKPOINT' || sectorId === 'CENTRAL_GARAGE') {
      return true;
    }

    // 4. Dispute Room is accessible by CO_OWNER (filing/evidence), STAFF (arbitration), and ADMIN (resolution)
    if (sectorId === 'DISPUTE_ROOM') {
      return userRole === 'ROLE_CO_OWNER' || userRole === 'ROLE_STAFF';
    }

    // 5. Operations Center and Service Workshop are restricted to STAFF (and ADMIN)
    if (sectorId === 'OPERATIONS_CENTER' || sectorId === 'SERVICE_WORKSHOP') {
      return userRole === 'ROLE_STAFF';
    }

    // 6. Owner environments are strictly restricted to CO_OWNER (and ADMIN)
    const ownerEnvironments: SectorId[] = [
      'CO_OWNERSHIP_HALL',
      'BOOKING_CHAMBER',
      'ENERGY_FINANCE_CENTER',
      'SHARED_FUND_VAULT',
      'DIGITAL_CONTRACT_ROOM',
      'DECISION_CHAMBER',
      'AI_INTELLIGENCE_CENTER',
    ];
    if (ownerEnvironments.includes(sectorId)) {
      return userRole === 'ROLE_CO_OWNER';
    }

    // 7. Admin Command Center is strictly restricted to ADMIN
    if (sectorId === 'ADMIN_COMMAND_CENTER') {
      return false;
    }

    return false;
  },

  handleSectorBoundaryIntrusion: (intrudedSector: SectorId): boolean => {
    const state = get();
    if (state.canAccessSector(intrudedSector)) {
      return true;
    }

    const meta = SECTOR_METADATA_REGISTRY[intrudedSector];
    const requiredRole: UserRole = meta?.requiredRole || (
      intrudedSector === 'OPERATIONS_CENTER' || intrudedSector === 'SERVICE_WORKSHOP'
        ? 'ROLE_STAFF'
        : intrudedSector === 'ADMIN_COMMAND_CENTER'
          ? 'ROLE_ADMIN'
          : 'ROLE_CO_OWNER'
    );
    const isGuest = state.userRole === 'GUEST' || !useAppStore.getState().isAuthenticated;
    const message = isGuest && intrudedSector !== 'SECURITY_CHECKPOINT'
      ? 'PERIMETER VIOLATION: Complete 3D Biometric Authentication to enter Metaverse'
      : `PERIMETER RESTRICTION: Sector requires ${requiredRole}`;

    // Determine safe fallback spawn
    const safeSector: SectorId = state.canAccessSector(state.currentSector)
      ? state.currentSector
      : isGuest
        ? 'SECURITY_CHECKPOINT'
        : 'CENTRAL_GARAGE';

    const safeSpawn = getSectorSpawnPoint(safeSector) || getDefaultSpawnPoint();

    // Physically bounce unauthorized avatar back to safe spawn
    usePlayerStore.getState().teleportTo(safeSpawn.position, safeSpawn.rotation);
    AudioEngine.playSpatial('NOTIF_ERROR', safeSpawn.position);

    set({
      deniedAccessNotice: {
        portalId: `PERIMETER_${intrudedSector}`,
        requiredRole,
        targetSectorId: intrudedSector,
        message,
        timestamp: Date.now(),
      },
    });

    return false;
  },

  teleportToSector: async (
    targetSectorId: SectorId,
    portalId?: string,
    options?: { isReturn?: boolean }
  ): Promise<boolean> => {
    const state = get();
    if (state.transitionPhase !== 'IDLE') return false;

    const meta = SECTOR_METADATA_REGISTRY[targetSectorId];
    if (!meta) {
      console.warn(`[Navigation] Target sector "${targetSectorId}" does not exist.`);
      return false;
    }

    // RBAC Permission Check
    if (!state.canAccessSector(targetSectorId)) {
      const requiredRole: UserRole = meta.requiredRole || (
        targetSectorId === 'OPERATIONS_CENTER' || targetSectorId === 'SERVICE_WORKSHOP'
          ? 'ROLE_STAFF'
          : targetSectorId === 'ADMIN_COMMAND_CENTER'
            ? 'ROLE_ADMIN'
            : 'ROLE_CO_OWNER'
      );
      const isUnauthenticated = !useAppStore.getState().isAuthenticated && state.userRole === 'GUEST';
      const message = isUnauthenticated && targetSectorId !== 'SECURITY_CHECKPOINT'
        ? 'UNAUTHORIZED: Complete 3D Biometric Authentication to enter Metaverse'
        : `ACCESS RESTRICTED: Requires ${requiredRole}`;

      AudioEngine.playSpatial('NOTIF_ERROR', [0, 1, 0]);
      set({
        deniedAccessNotice: {
          portalId: portalId || `PORTAL_TO_${targetSectorId}`,
          requiredRole,
          targetSectorId,
          message,
          timestamp: Date.now(),
        },
      });
      return false;
    }

    set({
      destinationSector: targetSectorId,
      transitionPhase: 'WARPING',
      deniedAccessNotice: null,
    });

    // 1. Warping Phase: SFX & Screen Transition Veil
    AudioEngine.playSpatial('WARP_TRANSITION', [0, 2, 0]);
    useLoadingStore.getState().startLoading('scene_transition', `Warping to ${meta.name}...`);

    await new Promise((r) => setTimeout(r, 400));

    // 2. Streaming Phase: Ensure sector asset loader runs
    set({ transitionPhase: 'STREAMING' });
    useWorldLoaderStore.getState().startLoadingSector(targetSectorId);
    useWorldEnvironmentStore.getState().startTransition(targetSectorId);

    // Resolve spawn point
    const spawn = getSectorSpawnPoint(targetSectorId) || getDefaultSpawnPoint();
    usePlayerStore.getState().teleportTo(spawn.position, spawn.rotation);

    // Coordinate Camera framing at destination
    const [sx, sy, sz] = spawn.position;
    useCameraStore.getState().moveTo(
      [sx, sy + 3.2, sz + 6.0],
      [sx, sy + 1.2, sz],
      { speed: 5.0 }
    );

    await new Promise((r) => setTimeout(r, 400));
    useWorldLoaderStore.getState().finishLoadingSector(targetSectorId);

    // 3. Arriving Phase
    set({ transitionPhase: 'ARRIVING' });
    useWorldEnvironmentStore.getState().completeTransition();
    useLoadingStore.getState().stopLoading('scene_transition');

    // Update history stack (only push when not returning)
    const newHistory = options?.isReturn
      ? get().navigationHistory
      : [...get().navigationHistory, state.currentSector];

    set({
      currentSector: targetSectorId,
      destinationSector: null,
      navigationHistory: newHistory,
      transitionPhase: 'IDLE',
    });

    return true;
  },

  teleportReturn: async (): Promise<boolean> => {
    const { navigationHistory, teleportToSector } = get();
    if (navigationHistory.length === 0) {
      return teleportToSector('CENTRAL_GARAGE', undefined, { isReturn: true });
    }

    const previousSector = navigationHistory[navigationHistory.length - 1];
    set((s) => ({ navigationHistory: s.navigationHistory.slice(0, -1) }));
    return teleportToSector(previousSector, undefined, { isReturn: true });
  },

  clearDeniedNotice: () => set({ deniedAccessNotice: null }),
}));
