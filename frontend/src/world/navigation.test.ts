import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useNavigationStore } from './useNavigationStore';
import { PORTAL_NETWORK, getPortalsForSector, getPortalById } from './portalNetwork';
import { useWorldEnvironmentStore, ENVIRONMENT_PRESETS } from './useWorldEnvironmentStore';
import { useWorldLoaderStore } from './WorldLoader';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { useLoadingStore } from '@/stores/useLoadingStore';
import type { SectorId, UserRole } from './worldTypes';

describe('EVShare 3D Navigation & Portals Subsystem (09-B)', () => {
  beforeEach(() => {
    useNavigationStore.setState({
      currentSector: 'SECURITY_CHECKPOINT',
      destinationSector: null,
      navigationHistory: [],
      transitionPhase: 'IDLE',
      deniedAccessNotice: null,
      userRole: 'ROLE_CO_OWNER',
    });
    useWorldEnvironmentStore.setState({
      activeSectorId: 'SECURITY_CHECKPOINT',
      activePreset: ENVIRONMENT_PRESETS.SECURITY_GATE,
      transitioning: false,
      transitionProgress: 1.0,
      targetPreset: null,
    });
    useWorldLoaderStore.setState({
      loadedSectors: new Set<SectorId>(['SECURITY_CHECKPOINT', 'CENTRAL_GARAGE']),
      activeLoadingSector: null,
      loadingProgress: 100,
      isStreaming: false,
    });
    usePlayerStore.setState({
      position: [0, 0, 88],
      rotation: [0, 0, 0],
    });
    useLoadingStore.setState({
      activeLoaders: new Set(),
      loadingMessage: '',
      progress: 100,
    });
  });

  describe('1. Portal Network Topology & Return Navigation', () => {
    it('defines outbound and return portals linking all sectors', () => {
      expect(PORTAL_NETWORK.length).toBeGreaterThanOrEqual(18);

      // Verify Garage has outbound portals to major sectors
      const garagePortals = getPortalsForSector('CENTRAL_GARAGE');
      expect(garagePortals.length).toBeGreaterThanOrEqual(6);

      const toBooking = garagePortals.find((p) => p.toSector === 'BOOKING_CHAMBER');
      expect(toBooking).toBeDefined();
      expect(toBooking?.requiredRole).toBe('ROLE_CO_OWNER');

      const toOperations = garagePortals.find((p) => p.toSector === 'OPERATIONS_CENTER');
      expect(toOperations).toBeDefined();
      expect(toOperations?.requiredRole).toBe('ROLE_STAFF');

      const toAdmin = garagePortals.find((p) => p.toSector === 'ADMIN_COMMAND_CENTER');
      expect(toAdmin).toBeDefined();
      expect(toAdmin?.requiredRole).toBe('ROLE_ADMIN');
    });

    it('ensures all destination sectors have a return portal back to the Central Garage', () => {
      const allSectorIds: SectorId[] = [
        'SECURITY_CHECKPOINT',
        'CO_OWNERSHIP_HALL',
        'BOOKING_CHAMBER',
        'ENERGY_FINANCE_CENTER',
        'SHARED_FUND_VAULT',
        'DIGITAL_CONTRACT_ROOM',
        'DECISION_CHAMBER',
        'AI_INTELLIGENCE_CENTER',
        'OPERATIONS_CENTER',
        'SERVICE_WORKSHOP',
        'DISPUTE_ROOM',
        'ADMIN_COMMAND_CENTER',
      ];

      for (const sectorId of allSectorIds) {
        const sectorPortals = getPortalsForSector(sectorId);
        const returnPortal = sectorPortals.find(
          (p) => p.toSector === 'CENTRAL_GARAGE' || p.toSector === 'SECURITY_CHECKPOINT'
        );
        expect(returnPortal).toBeDefined();
        expect(returnPortal?.isReturnPortal).toBe(true);
      }
    });
  });

  describe('2. Permission-Aware Destinations (RBAC Enforcement)', () => {
    it('allows co-owner to access co-ownership sectors and blocks staff/admin sectors', () => {
      const nav = useNavigationStore.getState();
      useNavigationStore.setState({ userRole: 'ROLE_CO_OWNER' });

      // Authorized
      expect(nav.canAccessSector('CENTRAL_GARAGE')).toBe(true);
      expect(nav.canAccessSector('BOOKING_CHAMBER')).toBe(true);
      expect(nav.canAccessSector('CO_OWNERSHIP_HALL')).toBe(true);

      // Blocked
      expect(nav.canAccessSector('OPERATIONS_CENTER')).toBe(false);
      expect(nav.canAccessSector('SERVICE_WORKSHOP')).toBe(false);
      expect(nav.canAccessSector('ADMIN_COMMAND_CENTER')).toBe(false);
    });

    it('allows staff to access operations and workshop sectors', () => {
      const nav = useNavigationStore.getState();
      useNavigationStore.setState({ userRole: 'ROLE_STAFF' });

      expect(nav.canAccessSector('OPERATIONS_CENTER')).toBe(true);
      expect(nav.canAccessSector('SERVICE_WORKSHOP')).toBe(true);
      expect(nav.canAccessSector('ADMIN_COMMAND_CENTER')).toBe(false);
    });

    it('grants admin universal access across all sectors', () => {
      const nav = useNavigationStore.getState();
      useNavigationStore.setState({ userRole: 'ROLE_ADMIN' });

      expect(nav.canAccessSector('ADMIN_COMMAND_CENTER')).toBe(true);
      expect(nav.canAccessSector('OPERATIONS_CENTER')).toBe(true);
      expect(nav.canAccessSector('BOOKING_CHAMBER')).toBe(true);
      expect(nav.canAccessSector('SHARED_FUND_VAULT')).toBe(true);
    });

    it('sets deniedAccessNotice and rejects teleportation when permission fails', async () => {
      useNavigationStore.setState({
        currentSector: 'CENTRAL_GARAGE',
        userRole: 'ROLE_CO_OWNER',
      });

      const success = await useNavigationStore
        .getState()
        .teleportToSector('ADMIN_COMMAND_CENTER', 'PORTAL_GARAGE_TO_ADMIN');

      expect(success).toBe(false);

      const notice = useNavigationStore.getState().deniedAccessNotice;
      expect(notice).not.toBeNull();
      expect(notice?.portalId).toBe('PORTAL_GARAGE_TO_ADMIN');
      expect(notice?.requiredRole).toBe('ROLE_ADMIN');
      expect(notice?.targetSectorId).toBe('ADMIN_COMMAND_CENTER');
      expect(notice?.message).toContain('Requires ROLE_ADMIN');
    });
  });

  describe('3. Teleportation Lifecycle, Camera Transition & Loading Coordination', () => {
    it('executes full teleportation flow: loading veil, player position, camera framing, and history stack', async () => {
      const cameraMoveToSpy = vi.spyOn(useCameraStore.getState(), 'moveTo');

      useNavigationStore.setState({
        currentSector: 'SECURITY_CHECKPOINT',
        userRole: 'ROLE_CO_OWNER',
      });

      const teleportPromise = useNavigationStore.getState().teleportToSector('CENTRAL_GARAGE');

      // Check transition phase during execution
      expect(['WARPING', 'STREAMING', 'ARRIVING']).toContain(
        useNavigationStore.getState().transitionPhase
      );

      const result = await teleportPromise;
      expect(result).toBe(true);

      // Verify arrival state
      const navState = useNavigationStore.getState();
      expect(navState.transitionPhase).toBe('IDLE');
      expect(navState.currentSector).toBe('CENTRAL_GARAGE');
      expect(navState.navigationHistory).toEqual(['SECURITY_CHECKPOINT']);

      // Verify player position updated to Garage spawn point [0, 0, 15]
      const playerPos = usePlayerStore.getState().position;
      expect(playerPos).toEqual([0, 0, 15]);

      // Verify camera framing coordinated with player position
      expect(cameraMoveToSpy).toHaveBeenCalled();
      const lastCallArgs = cameraMoveToSpy.mock.calls[cameraMoveToSpy.mock.calls.length - 1];
      expect(lastCallArgs[0]).toEqual([0, 3.2, 21]); // camera eye position
      expect(lastCallArgs[1]).toEqual([0, 1.2, 15]); // camera target position

      // Verify environment preset switched to Garage
      expect(useWorldEnvironmentStore.getState().activeSectorId).toBe('CENTRAL_GARAGE');

      // Verify sector added to loaded sectors in WorldLoader
      expect(useWorldLoaderStore.getState().isSectorLoaded('CENTRAL_GARAGE')).toBe(true);

      cameraMoveToSpy.mockRestore();
    });
  });

  describe('4. Return Navigation Flow', () => {
    it('pops history stack and teleports back to previous sector', async () => {
      useNavigationStore.setState({
        currentSector: 'CENTRAL_GARAGE',
        navigationHistory: ['SECURITY_CHECKPOINT'],
        userRole: 'ROLE_CO_OWNER',
      });

      const success = await useNavigationStore.getState().teleportReturn();
      expect(success).toBe(true);

      const navState = useNavigationStore.getState();
      expect(navState.currentSector).toBe('SECURITY_CHECKPOINT');
      expect(navState.navigationHistory).toEqual([]);

      // Calling return with empty history defaults back to Central Garage
      const returnToGarage = await useNavigationStore.getState().teleportReturn();
      expect(returnToGarage).toBe(true);
      expect(useNavigationStore.getState().currentSector).toBe('CENTRAL_GARAGE');
    });
  });
});
