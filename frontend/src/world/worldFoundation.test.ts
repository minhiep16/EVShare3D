import { describe, it, expect, beforeEach } from 'vitest';
import {
  SECTOR_CENTERS,
  SECTOR_METADATA_REGISTRY,
  WORLD_PORTALS,
  getSectorAtPosition,
  getNearestSector,
  isPositionInSector,
} from './worldCoordinates';
import {
  SPAWN_POINTS,
  getSpawnPoint,
  getDefaultSpawnPoint,
  getSectorSpawnPoint,
  getAllSpawnPoints,
} from './spawnPoints';
import { useWorldEnvironmentStore, ENVIRONMENT_PRESETS } from './useWorldEnvironmentStore';
import { useWorldLoaderStore } from './WorldLoader';
import { WorldInteractionRegistry } from './WorldInteractionRegistry';
import type { SectorId, WorldInteractiveEntity } from './worldTypes';

describe('EVShare 3D World Foundation Subsystem (09-A)', () => {
  beforeEach(() => {
    WorldInteractionRegistry.clear();
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
  });

  describe('1. World Coordinate System & Sector Boundaries', () => {
    it('defines 13 contiguous sectors with valid bounding volumes', () => {
      const sectorIds = Object.keys(SECTOR_METADATA_REGISTRY) as SectorId[];
      expect(sectorIds.length).toBe(13);

      for (const sectorId of sectorIds) {
        const meta = SECTOR_METADATA_REGISTRY[sectorId];
        expect(meta).toBeDefined();
        expect(meta.centerCoordinates).toBeDefined();
        expect(meta.bounds.minX).toBeLessThan(meta.bounds.maxX);
        expect(meta.bounds.minZ).toBeLessThan(meta.bounds.maxZ);
        expect(meta.bounds.radius).toBeGreaterThan(0);
      }
    });

    it('resolves correct sector from 3D coordinates using getSectorAtPosition', () => {
      // Central Garage center
      expect(getSectorAtPosition([0, 2, 0])).toBe('CENTRAL_GARAGE');

      // Security Checkpoint center
      expect(getSectorAtPosition([0, 1, 80])).toBe('SECURITY_CHECKPOINT');

      // AI Intelligence Center
      expect(getSectorAtPosition([40, 1, 0])).toBe('AI_INTELLIGENCE_CENTER');

      // Shared Fund Vault
      expect(getSectorAtPosition([-40, 2, -80])).toBe('SHARED_FUND_VAULT');

      // Far out in the void
      expect(getSectorAtPosition([500, 0, 500])).toBeNull();
    });

    it('finds nearest sector accurately', () => {
      // Slightly offset from AI Intelligence Center
      const nearest = getNearestSector([38, 0, 2]);
      expect(nearest).toBe('AI_INTELLIGENCE_CENTER');

      // Close to Service Workshop
      expect(getNearestSector([-41, 0, 42])).toBe('SERVICE_WORKSHOP');
    });

    it('accurately verifies point inside specific sector using isPositionInSector', () => {
      expect(isPositionInSector([0, 1, 0], 'CENTRAL_GARAGE')).toBe(true);
      expect(isPositionInSector([0, 1, 0], 'SECURITY_CHECKPOINT')).toBe(false);
      expect(isPositionInSector([0, 1, 80], 'SECURITY_CHECKPOINT')).toBe(true);
    });
  });

  describe('2. Spawn Points System', () => {
    it('provides a valid default entry spawn point', () => {
      const defaultSpawn = getDefaultSpawnPoint();
      expect(defaultSpawn.id).toBe('SPAWN_SECURITY_ENTRY');
      expect(defaultSpawn.sectorId).toBe('SECURITY_CHECKPOINT');
      expect(defaultSpawn.position).toEqual([0, 0, 88]);
    });

    it('maps spawn points across all sectors', () => {
      const allSpawns = getAllSpawnPoints();
      expect(allSpawns.length).toBeGreaterThanOrEqual(13);

      const garageSpawn = getSectorSpawnPoint('CENTRAL_GARAGE');
      expect(garageSpawn.sectorId).toBe('CENTRAL_GARAGE');
      expect(garageSpawn.position).toEqual([0, 0, 15]);

      const adminSpawn = getSectorSpawnPoint('ADMIN_COMMAND_CENTER');
      expect(adminSpawn.position[1]).toBe(25); // Elevated orbital deck
      expect(adminSpawn.requiredRole).toBe('ROLE_ADMIN');
    });

    it('falls back gracefully on unknown spawn IDs', () => {
      const fallback = getSpawnPoint('UNKNOWN_SPAWN_XYZ');
      expect(fallback.id).toBe('SPAWN_SECURITY_ENTRY');
    });
  });

  describe('3. World Environment Presets & Transition Engine', () => {
    it('switches environment preset when active sector changes', () => {
      const store = useWorldEnvironmentStore.getState();
      expect(store.activePreset.id).toBe('SECURITY_GATE');

      store.setActiveSector('CENTRAL_GARAGE');
      expect(useWorldEnvironmentStore.getState().activeSectorId).toBe('CENTRAL_GARAGE');
      expect(useWorldEnvironmentStore.getState().activePreset.id).toBe('GARAGE_DAYLIGHT');
    });

    it('manages sector transition lifecycle with progress interpolation', () => {
      const store = useWorldEnvironmentStore.getState();

      store.startTransition('AI_INTELLIGENCE_CENTER');
      let state = useWorldEnvironmentStore.getState();
      expect(state.transitioning).toBe(true);
      expect(state.transitionProgress).toBe(0.0);
      expect(state.targetPreset?.id).toBe('NEURAL_PURPLE');

      store.setTransitionProgress(0.5);
      state = useWorldEnvironmentStore.getState();
      expect(state.transitionProgress).toBe(0.5);

      store.setTransitionProgress(1.2); // Should clamp to 1.0
      state = useWorldEnvironmentStore.getState();
      expect(state.transitionProgress).toBe(1.0);

      store.completeTransition();
      state = useWorldEnvironmentStore.getState();
      expect(state.transitioning).toBe(false);
      expect(state.activeSectorId).toBe('AI_INTELLIGENCE_CENTER');
      expect(state.activePreset.id).toBe('NEURAL_PURPLE');
    });
  });

  describe('4. World Loader Streaming State', () => {
    it('tracks streaming progress and adds sector to loaded set on completion', () => {
      const loader = useWorldLoaderStore.getState();
      expect(loader.isSectorLoaded('SECURITY_CHECKPOINT')).toBe(true);
      expect(loader.isSectorLoaded('BOOKING_CHAMBER')).toBe(false);

      loader.startLoadingSector('BOOKING_CHAMBER');
      let state = useWorldLoaderStore.getState();
      expect(state.isStreaming).toBe(true);
      expect(state.activeLoadingSector).toBe('BOOKING_CHAMBER');
      expect(state.loadingProgress).toBe(0);

      loader.updateProgress(65);
      state = useWorldLoaderStore.getState();
      expect(state.loadingProgress).toBe(65);

      loader.finishLoadingSector('BOOKING_CHAMBER');
      state = useWorldLoaderStore.getState();
      expect(state.isStreaming).toBe(false);
      expect(state.activeLoadingSector).toBeNull();
      expect(state.isSectorLoaded('BOOKING_CHAMBER')).toBe(true);
    });
  });

  describe('5. World Interaction Registry & RBAC Permissions', () => {
    it('registers interactive entities and queries by sector and distance', () => {
      const testEntity: WorldInteractiveEntity = {
        id: 'TEST_GARAGE_KIOSK',
        sectorId: 'CENTRAL_GARAGE',
        name: 'Garage Information Kiosk',
        category: 'TERMINAL',
        position: [2, 0, 3],
        interactionDistance: 2.5,
      };

      const unregister = WorldInteractionRegistry.registerEntity(testEntity);
      expect(WorldInteractionRegistry.getEntity('TEST_GARAGE_KIOSK')).toEqual(testEntity);

      const garageEntities = WorldInteractionRegistry.getEntitiesBySector('CENTRAL_GARAGE');
      expect(garageEntities).toHaveLength(1);
      expect(garageEntities[0].id).toBe('TEST_GARAGE_KIOSK');

      // Distance search: within 5 units of origin [0, 0, 0]
      const nearby = WorldInteractionRegistry.getEntitiesNearPosition([0, 0, 0], 5);
      expect(nearby).toHaveLength(1);

      // Distance search: too far
      const farAway = WorldInteractionRegistry.getEntitiesNearPosition([50, 0, 50], 5);
      expect(farAway).toHaveLength(0);

      unregister();
      expect(WorldInteractionRegistry.getEntity('TEST_GARAGE_KIOSK')).toBeUndefined();
    });

    it('enforces RBAC role authority on interactive world entities', () => {
      const publicEntity: WorldInteractiveEntity = {
        id: 'PUBLIC_DOOR',
        sectorId: 'SECURITY_CHECKPOINT',
        name: 'Public Entry Turnstile',
        category: 'PORTAL',
        position: [0, 0, 80],
        interactionDistance: 2.0,
      };

      const coOwnerEntity: WorldInteractiveEntity = {
        id: 'SYNDICATE_PEDESTAL',
        sectorId: 'CO_OWNERSHIP_HALL',
        name: 'Syndicate Member Pedestal',
        category: 'PEDESTAL',
        position: [40, 0, 15],
        interactionDistance: 2.0,
        requiredRole: 'ROLE_CO_OWNER',
      };

      const staffEntity: WorldInteractiveEntity = {
        id: 'OBD_DIAGNOSTIC_BENCH',
        sectorId: 'OPERATIONS_CENTER',
        name: 'OBD-II Diagnostic Rig',
        category: 'TERMINAL',
        position: [-40, 0, 0],
        interactionDistance: 2.0,
        requiredRole: 'ROLE_STAFF',
      };

      const adminEntity: WorldInteractiveEntity = {
        id: 'SYSTEM_CORE_MONOLITH',
        sectorId: 'ADMIN_COMMAND_CENTER',
        name: 'System Core Tower',
        category: 'TERMINAL',
        position: [0, 25, 0],
        interactionDistance: 3.0,
        requiredRole: 'ROLE_ADMIN',
      };

      // Public
      expect(WorldInteractionRegistry.canUserAccessEntity(publicEntity, 'GUEST')).toBe(true);

      // Co-owner
      expect(WorldInteractionRegistry.canUserAccessEntity(coOwnerEntity, 'ROLE_CO_OWNER')).toBe(true);
      expect(WorldInteractionRegistry.canUserAccessEntity(coOwnerEntity, 'GUEST')).toBe(false);

      // Staff
      expect(WorldInteractionRegistry.canUserAccessEntity(staffEntity, 'ROLE_STAFF')).toBe(true);
      expect(WorldInteractionRegistry.canUserAccessEntity(staffEntity, 'ROLE_CO_OWNER')).toBe(false);

      // Admin has universal super-access
      expect(WorldInteractionRegistry.canUserAccessEntity(adminEntity, 'ROLE_ADMIN')).toBe(true);
      expect(WorldInteractionRegistry.canUserAccessEntity(staffEntity, 'ROLE_ADMIN')).toBe(true);
      expect(WorldInteractionRegistry.canUserAccessEntity(coOwnerEntity, 'ROLE_ADMIN')).toBe(true);
      expect(WorldInteractionRegistry.canUserAccessEntity(adminEntity, 'ROLE_STAFF')).toBe(false);
    });
  });

  describe('6. World Portals Network', () => {
    it('defines valid inter-sector traversal portals with matching destination sectors', () => {
      expect(WORLD_PORTALS.length).toBeGreaterThan(4);

      for (const portal of WORLD_PORTALS) {
        expect(portal.fromSector).toBeDefined();
        expect(portal.toSector).toBeDefined();
        expect(SECTOR_METADATA_REGISTRY[portal.fromSector]).toBeDefined();
        expect(SECTOR_METADATA_REGISTRY[portal.toSector]).toBeDefined();
        expect(portal.position).toBeDefined();
        expect(portal.destinationPosition).toBeDefined();
      }
    });
  });
});
