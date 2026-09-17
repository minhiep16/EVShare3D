import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useGarageStore } from './useGarageStore';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { InteractionPipeline } from '@/engine/interaction/InteractionPipeline';
import { InteractionContext } from '@/engine/interaction/interactionTypes';
import {
  vehiclesApi,
  VehicleResponseDTO,
  VehicleTelemetryResponseDTO,
} from '@/api/vehiclesApi';

vi.mock('@/engine/audio/AudioEngine', () => ({
  AudioEngine: {
    play: vi.fn(),
  },
}));

vi.mock('@/stores/useCameraStore', () => ({
  useCameraStore: {
    getState: vi.fn(() => ({
      moveTo: vi.fn(),
    })),
  },
}));

describe('09-R: Vehicle API Integration — Complete 10-Stage Pipeline', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    InteractionPipeline.clear();

    useGarageStore.setState({
      selectedVehicleId: null,
      inspectedVehicle: null,
      hoveredVehicleId: null,
      isBackendConnected: false,
      isLoading: false,
      error: null,
      isActionExecuting: false,
      actionNotice: null,
      actionError: null,
    });

    usePlayerStore.setState({
      position: [0, 0, 0],
    });

    useInteractionStore.setState({
      hoveredObjectId: null,
      selectedObjectId: null,
    });
  });

  describe('Stage 1 (Approach) & Stage 2 (Hover) & Stage 3 (Select)', () => {
    it('enforces proximity distance check during player approach', async () => {
      const vehicle = useGarageStore.getState().vehicles[0]; // ID 1 at [-9.5, 0.45, -6]
      const targetPos = vehicle.position;

      // Register vehicle definition into InteractionPipeline
      InteractionPipeline.register({
        id: `vehicle-${vehicle.id}`,
        name: `${vehicle.manufacturer} ${vehicle.modelName}`,
        targetPosition: targetPos,
        requirements: {
          maxInteractionDistance: 8.0,
        },
        onActivate: async () => {
          useGarageStore.getState().selectVehicle(vehicle.id);
        },
      });

      // 1. Far away (player at [0, 0, 0], distance ~11.23m > 8.0m)
      const farContext: InteractionContext = {
        targetId: `vehicle-${vehicle.id}`,
        playerPosition: [0, 0, 0],
        targetPosition: targetPos,
        distanceToPlayer: 11.23,
        userRoles: ['USER'],
        isAuthenticated: true,
      };

      const farResult = await InteractionPipeline.execute(
        `vehicle-${vehicle.id}`,
        farContext
      );
      expect(farResult.success).toBe(false);
      expect(farResult.reason).toContain('Too far away');
      expect(useGarageStore.getState().selectedVehicleId).toBeNull();

      // 2. Approached within range (player moves to [-8.0, 0, -5.0], distance ~1.8m < 8.0m)
      const nearContext: InteractionContext = {
        targetId: `vehicle-${vehicle.id}`,
        playerPosition: [-8.0, 0, -5.0],
        targetPosition: targetPos,
        distanceToPlayer: 1.8,
        userRoles: ['USER'],
        isAuthenticated: true,
      };

      const nearResult = await InteractionPipeline.execute(
        `vehicle-${vehicle.id}`,
        nearContext
      );
      expect(nearResult.success).toBe(true);
      expect(useGarageStore.getState().selectedVehicleId).toBe(1);
    });

    it('updates hover state and visual interaction state', () => {
      useGarageStore.getState().setHoveredVehicle(1);
      expect(useGarageStore.getState().hoveredVehicleId).toBe(1);

      useGarageStore.getState().setHoveredVehicle(null);
      expect(useGarageStore.getState().hoveredVehicleId).toBeNull();
    });
  });

  describe('Stage 4 (Focus) & Stage 5 (3D Info)', () => {
    it('focuses camera framing and fetches detail & live telemetry from Vehicle APIs', async () => {
      const moveToMock = vi.fn();
      vi.mocked(useCameraStore.getState).mockReturnValue({
        moveTo: moveToMock,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } as any);

      const mockTelemetry: VehicleTelemetryResponseDTO = {
        vehicleId: 1,
        vin: '1HGCR2F83HA001201',
        status: 'AVAILABLE',
        batteryLevel: 91,
        odometerKm: 14320,
        stallLocationCode: 'BAY-01',
        estimatedRangeKm: 520,
        lastTelemetrySync: '2026-09-16T12:00:00Z',
      };

      const mockDetail: VehicleResponseDTO = {
        id: 1,
        vin: '1HGCR2F83HA001201',
        licensePlate: '51K-882.14',
        modelName: 'Model S Plaid',
        manufacturer: 'Tesla',
        status: 'AVAILABLE',
        batteryLevel: 91,
        odometerKm: 14320,
        stallLocationCode: 'BAY-01',
      };

      vi.spyOn(vehiclesApi, 'getVehicleTelemetry').mockResolvedValue(mockTelemetry);
      vi.spyOn(vehiclesApi, 'getVehicleById').mockResolvedValue(mockDetail);

      // Select vehicle -> triggers camera focus and background sync
      useGarageStore.getState().selectVehicle(1);

      expect(useGarageStore.getState().selectedVehicleId).toBe(1);
      expect(useGarageStore.getState().inspectedVehicle?.id).toBe(1);
      expect(moveToMock).toHaveBeenCalledTimes(1);

      // Wait for background telemetry and detail async resolution
      await useGarageStore.getState().fetchVehicleTelemetry(1);
      await useGarageStore.getState().fetchVehicleDetail(1);

      const updated = useGarageStore.getState().inspectedVehicle;
      expect(updated?.batteryLevel).toBe(91);
      expect(updated?.odometerKm).toBe(14320);

      // Verify DigitalTwin store synchronization
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.battery.level).toBe(91);
      expect(twin.battery.estimatedRangeKm).toBe(520);
      expect(twin.usage.odometerKm).toBe(14320);
    });

    it('computes permitted actions matching the authoritative VehicleStateMachine', () => {
      const { getPermittedActions } = useGarageStore.getState();

      expect(getPermittedActions('AVAILABLE')).toEqual([
        'CHARGING',
        'MAINTENANCE',
        'BOOKED',
        'UNAVAILABLE',
      ]);
      expect(getPermittedActions('CHARGING')).toEqual([
        'AVAILABLE',
        'MAINTENANCE',
        'UNAVAILABLE',
      ]);
      expect(getPermittedActions('MAINTENANCE')).toEqual([
        'AVAILABLE',
        'CHARGING',
        'UNAVAILABLE',
      ]);
      expect(getPermittedActions('BOOKED')).toEqual([
        'IN_USE',
        'AVAILABLE',
        'MAINTENANCE',
      ]);
      expect(getPermittedActions('IN_USE')).toEqual([
        'AVAILABLE',
        'CHARGING',
        'MAINTENANCE',
        'DAMAGED',
      ]);
    });
  });

  describe('Stage 6 (Action) → Stage 7 (API) → Stage 8 (Backend) → Stage 9 (Database) → Stage 10 (State & Visual Update)', () => {
    it('executes vehicle status transition to CHARGING, persists to backend, and updates 3D charging bay', async () => {
      useGarageStore.getState().selectVehicle(1); // Bay 01
      expect(useGarageStore.getState().inspectedVehicle?.status).toBe('AVAILABLE');

      const mockResponse: VehicleResponseDTO = {
        id: 1,
        vin: '1HGCR2F83HA001201',
        licensePlate: '51K-882.14',
        modelName: 'Model S Plaid',
        manufacturer: 'Tesla',
        status: 'CHARGING',
        batteryLevel: 88,
        odometerKm: 14250,
        stallLocationCode: 'BAY-01',
      };

      const updateSpy = vi
        .spyOn(vehiclesApi, 'updateStatus')
        .mockResolvedValue(mockResponse);

      const success = await useGarageStore
        .getState()
        .transitionVehicleStatus(1, 'CHARGING', 'Plugged in from 3D console');

      expect(success).toBe(true);
      expect(updateSpy).toHaveBeenCalledWith(
        1,
        'CHARGING',
        'Plugged in from 3D console'
      );

      // Verify store state & visual updates
      const state = useGarageStore.getState();
      expect(state.inspectedVehicle?.status).toBe('CHARGING');
      expect(state.inspectedVehicle?.isCharging).toBe(true);
      expect(state.inspectedVehicle?.chargingPowerKw).toBe(150);
      expect(state.actionNotice).toContain('DATABASE COMMITTED');
      expect(state.actionError).toBeNull();
      expect(state.isActionExecuting).toBe(false);

      // Verify digital twin facet updated
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.status.status).toBe('CHARGING');
    });

    it('transitions to MAINTENANCE, dispatches to backend, and updates visual state', async () => {
      useGarageStore.getState().selectVehicle(2); // Taycan

      const mockResponse: VehicleResponseDTO = {
        id: 2,
        vin: 'WP0AB2Y11MSA04912',
        licensePlate: '29A-991.88',
        modelName: 'Taycan Turbo S',
        manufacturer: 'Porsche',
        status: 'MAINTENANCE',
        batteryLevel: 74,
        odometerKm: 8900,
        stallLocationCode: 'BAY-02',
      };

      vi.spyOn(vehiclesApi, 'updateStatus').mockResolvedValue(mockResponse);

      const success = await useGarageStore
        .getState()
        .transitionVehicleStatus(2, 'MAINTENANCE', 'Brake checkup');

      expect(success).toBe(true);

      const state = useGarageStore.getState();
      expect(state.inspectedVehicle?.status).toBe('MAINTENANCE');
      expect(state.actionNotice).toContain('MAINTENANCE');
    });

    it('handles backend API rejection or invalid state transition gracefully', async () => {
      useGarageStore.getState().selectVehicle(1);

      vi.spyOn(vehiclesApi, 'updateStatus').mockRejectedValue(
        new Error('Invalid state transition: Cannot transition from AVAILABLE to DAMAGED')
      );

      const success = await useGarageStore
        .getState()
        .transitionVehicleStatus(1, 'DAMAGED' as any);

      expect(success).toBe(false);

      const state = useGarageStore.getState();
      expect(state.isActionExecuting).toBe(false);
      expect(state.actionError).toContain('Invalid state transition');
      expect(state.actionNotice).toBeNull();
      // Original status preserved
      expect(state.inspectedVehicle?.status).toBe('AVAILABLE');
    });

    it('toggles vehicle lock status locally and in inspected vehicle', async () => {
      useGarageStore.getState().selectVehicle(1);
      expect(useGarageStore.getState().inspectedVehicle?.isLocked).toBe(true);

      await useGarageStore.getState().toggleVehicleLock(1);
      expect(useGarageStore.getState().inspectedVehicle?.isLocked).toBe(false);

      await useGarageStore.getState().toggleVehicleLock(1);
      expect(useGarageStore.getState().inspectedVehicle?.isLocked).toBe(true);
    });
  });
});
