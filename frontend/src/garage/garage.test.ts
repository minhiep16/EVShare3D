import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useGarageStore } from './useGarageStore';
import { GARAGE_PARKING_BAYS, GARAGE_CHARGING_STATIONS } from './garageLayout';
import { vehiclesApi, VehicleResponseDTO } from '@/api/vehiclesApi';
import { ownershipGroupsApi, OwnershipGroupResponseDTO } from '@/api/ownershipGroupsApi';
import { useCameraStore } from '@/stores/useCameraStore';

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

describe('Central Garage System', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useGarageStore.setState({
      selectedVehicleId: null,
      inspectedVehicle: null,
      hoveredVehicleId: null,
      isBackendConnected: false,
      isLoading: false,
      error: null,
    });
  });

  describe('Garage Layout Specifications', () => {
    it('defines parking bays within the 24m garage radius', () => {
      expect(GARAGE_PARKING_BAYS.length).toBeGreaterThanOrEqual(6);
      GARAGE_PARKING_BAYS.forEach((bay) => {
        const [x, , z] = bay.position;
        const dist = Math.sqrt(x * x + z * z);
        expect(dist).toBeLessThan(23.5); // Inside 24m showroom perimeter
      });
    });

    it('contains designated charging bays and matching superchargers', () => {
      const chargingBays = GARAGE_PARKING_BAYS.filter((b) => b.type === 'CHARGING');
      expect(chargingBays.length).toBe(2);

      GARAGE_CHARGING_STATIONS.forEach((station) => {
        const matchingBay = GARAGE_PARKING_BAYS.find((b) => b.id === station.stallId);
        expect(matchingBay).toBeDefined();
        expect(matchingBay?.type).toBe('CHARGING');
      });
    });
  });

  describe('Vehicle Selection & Camera Framing', () => {
    it('selects a vehicle and updates inspected vehicle', () => {
      const moveToMock = vi.fn();
      vi.mocked(useCameraStore.getState).mockReturnValue({
        moveTo: moveToMock,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } as any);

      useGarageStore.getState().selectVehicle(1);

      const state = useGarageStore.getState();
      expect(state.selectedVehicleId).toBe(1);
      expect(state.inspectedVehicle?.id).toBe(1);
      expect(state.inspectedVehicle?.modelName).toBe('Model S Plaid');
      expect(moveToMock).toHaveBeenCalled();
    });

    it('deselects a vehicle when passing null', () => {
      useGarageStore.getState().selectVehicle(1);
      expect(useGarageStore.getState().selectedVehicleId).toBe(1);

      useGarageStore.getState().selectVehicle(null);
      expect(useGarageStore.getState().selectedVehicleId).toBeNull();
      expect(useGarageStore.getState().inspectedVehicle).toBeNull();
    });
  });

  describe('Vehicle Controls (Lock & Charging)', () => {
    it('toggles vehicle lock status', async () => {
      useGarageStore.getState().selectVehicle(1);
      const initialLock = useGarageStore.getState().inspectedVehicle?.isLocked;

      await useGarageStore.getState().toggleVehicleLock(1);
      expect(useGarageStore.getState().inspectedVehicle?.isLocked).toBe(!initialLock);

      await useGarageStore.getState().toggleVehicleLock(1);
      expect(useGarageStore.getState().inspectedVehicle?.isLocked).toBe(initialLock);
    });

    it('toggles vehicle charging and updates charging station status', async () => {
      vi.spyOn(vehiclesApi, 'updateStatus').mockResolvedValue({} as VehicleResponseDTO);

      useGarageStore.getState().selectVehicle(5); // Vehicle in BAY-05 (CHG-01)
      expect(useGarageStore.getState().inspectedVehicle?.isCharging).toBe(true);

      // Toggle off
      await useGarageStore.getState().toggleCharging(5);
      expect(useGarageStore.getState().inspectedVehicle?.isCharging).toBe(false);
      expect(useGarageStore.getState().inspectedVehicle?.status).toBe('AVAILABLE');

      const chg01 = useGarageStore.getState().chargingStations.find((s) => s.id === 'CHG-01');
      expect(chg01?.status).toBe('IDLE');
      expect(chg01?.currentPowerKw).toBe(0);

      // Toggle back on
      await useGarageStore.getState().toggleCharging(5);
      expect(useGarageStore.getState().inspectedVehicle?.isCharging).toBe(true);
      expect(useGarageStore.getState().inspectedVehicle?.status).toBe('CHARGING');

      const chg01Active = useGarageStore.getState().chargingStations.find((s) => s.id === 'CHG-01');
      expect(chg01Active?.status).toBe('CHARGING');
      expect(chg01Active?.currentPowerKw).toBe(150);
    });
  });

  describe('Backend Integration & Graceful Fallback', () => {
    it('hydrates with live backend vehicles and ownership groups when available', async () => {
      const mockBackendVehicles: VehicleResponseDTO[] = [
        {
          id: 99,
          vin: 'TESTVIN998877665544',
          licensePlate: '51A-123.45',
          modelName: 'Taycan 4S',
          manufacturer: 'Porsche',
          status: 'AVAILABLE',
          batteryLevel: 95,
          odometerKm: 3200,
          stallLocationCode: 'BAY-01',
        },
      ];

      const mockGroups: OwnershipGroupResponseDTO[] = [
        {
          id: 201,
          groupName: 'Stuttgart Co-Op',
          vehicleId: 99,
          formationDate: '2026-01-01',
          isActive: true,
          memberCount: 3,
          memberShares: [
            {
              id: 1,
              userId: 10,
              userName: 'alex',
              sharePercentage: 33.3,
              votingPowerPercentage: 33.3,
              isRepresentative: true,
            },
          ],
        },
      ];

      vi.spyOn(vehiclesApi, 'getVehicles').mockResolvedValue(mockBackendVehicles);
      vi.spyOn(ownershipGroupsApi, 'getGroups').mockResolvedValue(mockGroups);

      await useGarageStore.getState().fetchGarageData();

      const state = useGarageStore.getState();
      expect(state.isBackendConnected).toBe(true);
      expect(state.vehicles.length).toBe(1);
      expect(state.vehicles[0].id).toBe(99);
      expect(state.vehicles[0].modelName).toBe('Taycan 4S');
      expect(state.vehicles[0].ownership?.groupName).toBe('Stuttgart Co-Op');
      expect(state.vehicles[0].ownership?.userSharePercentage).toBe(33.3);
      expect(state.vehicles[0].ownership?.isRepresentative).toBe(true);

      const bay01 = state.parkingBays.find((b) => b.id === 'BAY-01');
      expect(bay01?.isOccupied).toBe(true);
      expect(bay01?.occupiedVehicleId).toBe(99);
    });

    it('falls back gracefully when backend is offline', async () => {
      vi.spyOn(vehiclesApi, 'getVehicles').mockRejectedValue(new Error('Network error'));

      await useGarageStore.getState().fetchGarageData();

      const state = useGarageStore.getState();
      expect(state.isBackendConnected).toBe(false);
      expect(state.vehicles.length).toBeGreaterThan(0);
      expect(state.isLoading).toBe(false);
    });
  });
});
