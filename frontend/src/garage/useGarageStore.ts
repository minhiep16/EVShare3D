import { create } from 'zustand';
import type { GarageVehicle, ParkingBay, ChargingStation } from './garageTypes';
import { GARAGE_PARKING_BAYS, GARAGE_CHARGING_STATIONS } from './garageLayout';
import { vehiclesApi, VehicleResponseDTO, BackendVehicleStatus } from '@/api/vehiclesApi';
import { ownershipGroupsApi, OwnershipGroupResponseDTO } from '@/api/ownershipGroupsApi';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import { useCameraStore } from '@/stores/useCameraStore';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';

// Initial realistic bootstrap fleet conforming 100% to backend schema
const BOOTSTRAP_VEHICLES: GarageVehicle[] = [
  {
    id: 1,
    vin: '1HGCR2F83HA001201',
    licensePlate: '51K-882.14',
    modelName: 'Model S Plaid',
    manufacturer: 'Tesla',
    status: 'AVAILABLE',
    batteryLevel: 88,
    odometerKm: 14250,
    stallLocationCode: 'BAY-01',
    bodyColor: '#00e5ff', // Cyber cyan
    position: [-9.5, 0.45, -6],
    rotation: [0, Math.PI / 4, 0],
    ownership: {
      groupId: 101,
      groupName: 'CyberSprint Syndicate',
      memberCount: 4,
      userSharePercentage: 25,
      userVotingPower: 25,
      isRepresentative: true,
    },
    isLocked: true,
    isCharging: false,
    chargingPowerKw: 0,
  },
  {
    id: 2,
    vin: 'WP0AB2Y11MSA04912',
    licensePlate: '29A-991.88',
    modelName: 'Taycan Turbo S',
    manufacturer: 'Porsche',
    status: 'AVAILABLE',
    batteryLevel: 74,
    odometerKm: 8900,
    stallLocationCode: 'BAY-02',
    bodyColor: '#e0e7ff', // Silver frost
    position: [-11.5, 0.45, 3],
    rotation: [0, Math.PI / 2, 0],
    ownership: {
      groupId: 102,
      groupName: 'Veloce Co-Ownership',
      memberCount: 3,
      userSharePercentage: 33.3,
      userVotingPower: 33.3,
    },
    isLocked: true,
    isCharging: false,
    chargingPowerKw: 0,
  },
  {
    id: 3,
    vin: 'VF8A1102923004819',
    licensePlate: '51H-671.02',
    modelName: 'VF 8 Plus',
    manufacturer: 'VinFast',
    status: 'RESERVED',
    batteryLevel: 92,
    odometerKm: 5400,
    stallLocationCode: 'BAY-03',
    bodyColor: '#f59e0b', // Solar gold
    position: [-9.5, 0.45, 12],
    rotation: [0, (3 * Math.PI) / 4, 0],
    ownership: {
      groupId: 103,
      groupName: 'GreenTransit Collective',
      memberCount: 5,
      userSharePercentage: 20,
      userVotingPower: 20,
    },
    isLocked: true,
    isCharging: false,
    chargingPowerKw: 0,
  },
  {
    id: 4,
    vin: 'WAUZZZF27NA019283',
    licensePlate: '30E-482.91',
    modelName: 'e-tron GT',
    manufacturer: 'Audi',
    status: 'MAINTENANCE',
    batteryLevel: 45,
    odometerKm: 21300,
    stallLocationCode: 'BAY-04',
    bodyColor: '#ef4444', // Crimson red
    position: [9.5, 0.45, -6],
    rotation: [0, -Math.PI / 4, 0],
    ownership: {
      groupId: 104,
      groupName: 'UrbanApex Pool',
      memberCount: 2,
      userSharePercentage: 50,
      userVotingPower: 50,
    },
    isLocked: true,
    isCharging: false,
    chargingPowerKw: 0,
  },
  {
    id: 5,
    vin: '5YJ3E1EB8NF192847',
    licensePlate: '50N-119.33',
    modelName: 'Ioniq 5 AWD',
    manufacturer: 'Hyundai',
    status: 'CHARGING',
    batteryLevel: 62,
    odometerKm: 11200,
    stallLocationCode: 'BAY-05',
    bodyColor: '#10b981', // Emerald green
    position: [11.5, 0.45, 3],
    rotation: [0, -Math.PI / 2, 0],
    ownership: {
      groupId: 105,
      groupName: 'EcoHorizon Fleet',
      memberCount: 4,
      userSharePercentage: 25,
      userVotingPower: 25,
    },
    isLocked: false,
    isCharging: true,
    chargingPowerKw: 150,
  },
];

const COLOR_PALETTE = ['#00e5ff', '#e0e7ff', '#f59e0b', '#ef4444', '#10b981', '#8b5cf6'];

interface GarageState {
  vehicles: GarageVehicle[];
  parkingBays: ParkingBay[];
  chargingStations: ChargingStation[];
  selectedVehicleId: number | null;
  inspectedVehicle: GarageVehicle | null;
  hoveredVehicleId: number | null;
  isBackendConnected: boolean;
  isLoading: boolean;
  lastSyncTime: number | null;
  error: string | null;

  // Status & Telemetry API state
  isActionExecuting: boolean;
  actionNotice: string | null;
  actionError: string | null;

  // Actions
  fetchGarageData: () => Promise<void>;
  fetchVehicleDetail: (id: number) => Promise<VehicleResponseDTO | null>;
  fetchVehicleTelemetry: (id: number) => Promise<VehicleTelemetryResponseDTO | null>;
  transitionVehicleStatus: (
    id: number,
    targetStatus: BackendVehicleStatus,
    reason?: string
  ) => Promise<boolean>;
  getPermittedActions: (status: BackendVehicleStatus) => BackendVehicleStatus[];
  selectVehicle: (id: number | null) => void;
  setHoveredVehicle: (id: number | null) => void;
  toggleVehicleLock: (id: number) => Promise<void>;
  toggleCharging: (id: number) => Promise<void>;
  updateVehicleStatusLocally: (id: number, status: BackendVehicleStatus) => void;
  updateVehicleStatus: (id: number, status: BackendVehicleStatus) => void;
}

export const useGarageStore = create<GarageState>((set, get) => ({
  vehicles: BOOTSTRAP_VEHICLES,
  parkingBays: GARAGE_PARKING_BAYS.map((bay) => {
    const matchingVeh = BOOTSTRAP_VEHICLES.find((v) => v.stallLocationCode === bay.id);
    return {
      ...bay,
      isOccupied: Boolean(matchingVeh),
      occupiedVehicleId: matchingVeh?.id,
    };
  }),
  chargingStations: GARAGE_CHARGING_STATIONS,
  selectedVehicleId: null,
  inspectedVehicle: null,
  hoveredVehicleId: null,
  isBackendConnected: false,
  isLoading: false,
  lastSyncTime: null,
  error: null,
  isActionExecuting: false,
  actionNotice: null,
  actionError: null,

  fetchGarageData: async () => {
    set({ isLoading: true, error: null });

    try {
      const [backendVehicles, ownershipGroups] = await Promise.all([
        vehiclesApi.getVehicles(),
        ownershipGroupsApi.getGroups().catch(() => [] as OwnershipGroupResponseDTO[]),
      ]);

      if (backendVehicles && backendVehicles.length > 0) {
        // Map backend vehicles to garage slots
        const mappedVehicles: GarageVehicle[] = backendVehicles.map((dto, index) => {
          // Find assigned bay from stallLocationCode or cycle through bays
          const bayIndex = index % GARAGE_PARKING_BAYS.length;
          const assignedBay =
            GARAGE_PARKING_BAYS.find((b) => b.id === dto.stallLocationCode) ||
            GARAGE_PARKING_BAYS[bayIndex];

          const group = ownershipGroups.find((g) => g.vehicleId === dto.id);

          return {
            id: dto.id,
            vin: dto.vin,
            licensePlate: dto.licensePlate,
            modelName: dto.modelName,
            manufacturer: dto.manufacturer,
            model3dAssetPath: dto.model3dAssetPath,
            status: dto.status,
            batteryLevel: dto.batteryLevel,
            odometerKm: Number(dto.odometerKm),
            stallLocationCode: assignedBay.id,
            bodyColor: COLOR_PALETTE[index % COLOR_PALETTE.length],
            position: [assignedBay.position[0], 0.45, assignedBay.position[2]],
            rotation: assignedBay.rotation,
            ownership: group
              ? {
                  groupId: group.id,
                  groupName: group.groupName,
                  memberCount: group.memberCount,
                  userSharePercentage: group.memberShares?.[0]?.sharePercentage ?? 25,
                  userVotingPower: group.memberShares?.[0]?.votingPowerPercentage ?? 25,
                  isRepresentative: group.memberShares?.[0]?.isRepresentative ?? false,
                }
              : undefined,
            isLocked: dto.status !== 'IN_USE',
            isCharging: dto.status === 'CHARGING',
            chargingPowerKw: dto.status === 'CHARGING' ? 150 : 0,
          };
        });

        // Recompute bay occupancies
        const updatedBays = GARAGE_PARKING_BAYS.map((bay) => {
          const veh = mappedVehicles.find((v) => v.stallLocationCode === bay.id);
          return {
            ...bay,
            isOccupied: Boolean(veh),
            occupiedVehicleId: veh?.id,
          };
        });

        set({
          vehicles: mappedVehicles,
          parkingBays: updatedBays,
          isBackendConnected: true,
          isLoading: false,
          lastSyncTime: Date.now(),
        });
        return;
      }
    } catch {
      // Fallback: Backend not reachable or network error, retain bootstrap fleet
      set({
        isBackendConnected: false,
        isLoading: false,
        lastSyncTime: Date.now(),
      });
    }
  },

  fetchVehicleDetail: async (id: number) => {
    try {
      const detail = await vehiclesApi.getVehicleById(id);
      if (detail) {
        set((state) => {
          const updated = state.vehicles.map((v) =>
            v.id === id
              ? {
                  ...v,
                  status: detail.status,
                  batteryLevel: detail.batteryLevel ?? v.batteryLevel,
                  odometerKm: Number(detail.odometerKm ?? v.odometerKm),
                  stallLocationCode: detail.stallLocationCode ?? v.stallLocationCode,
                }
              : v
          );
          const inspected =
            state.inspectedVehicle?.id === id
              ? {
                  ...state.inspectedVehicle,
                  status: detail.status,
                  batteryLevel: detail.batteryLevel ?? state.inspectedVehicle.batteryLevel,
                  odometerKm: Number(detail.odometerKm ?? state.inspectedVehicle.odometerKm),
                  stallLocationCode: detail.stallLocationCode ?? state.inspectedVehicle.stallLocationCode,
                }
              : state.inspectedVehicle;
          return { vehicles: updated, inspectedVehicle: inspected };
        });
      }
      return detail;
    } catch {
      return null;
    }
  },

  fetchVehicleTelemetry: async (id: number) => {
    try {
      const telemetry = await vehiclesApi.getVehicleTelemetry(id);
      if (telemetry) {
        set((state) => {
          const updated = state.vehicles.map((v) =>
            v.id === id
              ? {
                  ...v,
                  batteryLevel: telemetry.batteryLevel ?? v.batteryLevel,
                  odometerKm: Number(telemetry.odometerKm ?? v.odometerKm),
                  status: telemetry.status ?? v.status,
                  stallLocationCode: telemetry.stallLocationCode ?? v.stallLocationCode,
                }
              : v
          );
          const inspected =
            state.inspectedVehicle?.id === id
              ? {
                  ...state.inspectedVehicle,
                  batteryLevel: telemetry.batteryLevel ?? state.inspectedVehicle.batteryLevel,
                  odometerKm: Number(telemetry.odometerKm ?? state.inspectedVehicle.odometerKm),
                  status: telemetry.status ?? state.inspectedVehicle.status,
                  stallLocationCode: telemetry.stallLocationCode ?? state.inspectedVehicle.stallLocationCode,
                }
              : state.inspectedVehicle;
          return { vehicles: updated, inspectedVehicle: inspected };
        });

        // Also update digitalTwinStore synchronously if present
        useDigitalTwinStore.getState().updateBattery(id, {
          level: telemetry.batteryLevel,
          estimatedRangeKm: telemetry.estimatedRangeKm,
        });
        useDigitalTwinStore.getState().updateUsage(id, {
          odometerKm: Number(telemetry.odometerKm),
        });
      }
      return telemetry;
    } catch {
      return null;
    }
  },

  transitionVehicleStatus: async (
    id: number,
    targetStatus: BackendVehicleStatus,
    reason?: string
  ) => {
    set({
      isActionExecuting: true,
      actionNotice: `TRANSMITTING: Updating Vehicle #${id} to ${targetStatus}...`,
      actionError: null,
    });

    try {
      const updatedDto = await vehiclesApi.updateStatus(
        id,
        targetStatus,
        reason || 'Updated from 3D Central Garage interactive terminal'
      );

      const resolvedStatus = updatedDto?.status || targetStatus;

      set((state) => {
        const updatedVehicles = state.vehicles.map((v) =>
          v.id === id
            ? {
                ...v,
                status: resolvedStatus,
                isCharging: resolvedStatus === 'CHARGING',
                chargingPowerKw: resolvedStatus === 'CHARGING' ? 150 : 0,
              }
            : v
        );

        const currentInspected = state.inspectedVehicle;
        const targetVehicle = state.vehicles.find((v) => v.id === id);

        const updatedStations = state.chargingStations.map((st) =>
          targetVehicle && st.stallId === targetVehicle.stallLocationCode
            ? {
                ...st,
                status: (resolvedStatus === 'CHARGING' ? 'CHARGING' : 'IDLE') as 'CHARGING' | 'IDLE',
                currentPowerKw: resolvedStatus === 'CHARGING' ? 150 : 0,
                connectedVehicleId: resolvedStatus === 'CHARGING' ? id : undefined,
              }
            : st
        );

        return {
          vehicles: updatedVehicles,
          chargingStations: updatedStations,
          inspectedVehicle:
            currentInspected?.id === id
              ? {
                  ...currentInspected,
                  status: resolvedStatus,
                  isCharging: resolvedStatus === 'CHARGING',
                  chargingPowerKw: resolvedStatus === 'CHARGING' ? 150 : 0,
                }
              : currentInspected,
          isActionExecuting: false,
          actionNotice: `DATABASE COMMITTED: Vehicle #${id} status -> ${resolvedStatus}.`,
          actionError: null,
        };
      });

      AudioEngine.play('NOTIF_SUCCESS');

      // Propagate to useDigitalTwinStore
      await useDigitalTwinStore.getState().updateStatus(id, resolvedStatus, reason);

      return true;
    } catch (err: unknown) {
      const errorMsg = (err as Error)?.message || 'State machine transition rejected by backend.';
      set({
        isActionExecuting: false,
        actionNotice: null,
        actionError: errorMsg,
      });
      AudioEngine.play('UI_BACK');
      return false;
    }
  },

  getPermittedActions: (status: BackendVehicleStatus): BackendVehicleStatus[] => {
    switch (status) {
      case 'AVAILABLE':
        return ['CHARGING', 'MAINTENANCE', 'BOOKED', 'UNAVAILABLE'];
      case 'CHARGING':
        return ['AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE'];
      case 'MAINTENANCE':
        return ['AVAILABLE', 'CHARGING', 'UNAVAILABLE'];
      case 'BOOKED':
      case 'RESERVED':
        return ['IN_USE', 'AVAILABLE', 'MAINTENANCE'];
      case 'IN_USE':
        return ['AVAILABLE', 'CHARGING', 'MAINTENANCE', 'DAMAGED'];
      case 'DAMAGED':
        return ['MAINTENANCE', 'UNAVAILABLE'];
      case 'UNAVAILABLE':
        return ['AVAILABLE', 'MAINTENANCE'];
      default:
        return ['AVAILABLE'];
    }
  },

  selectVehicle: (id: number | null) => {
    if (id === null) {
      set({
        selectedVehicleId: null,
        inspectedVehicle: null,
        actionNotice: null,
        actionError: null,
      });
      return;
    }

    const { vehicles } = get();
    const vehicle = vehicles.find((v) => v.id === id);
    if (!vehicle) return;

    AudioEngine.play('UI_CLICK');

    set({
      selectedVehicleId: id,
      inspectedVehicle: vehicle,
      actionNotice: null,
      actionError: null,
    });

    // Step 4 Focus: Coordinate Camera framing towards selected vehicle
    const [vx, vy, vz] = vehicle.position;
    useCameraStore.getState().moveTo(
      [vx - 3.5, vy + 2.4, vz + 4.2],
      [vx, vy + 0.6, vz],
      { speed: 4.5 }
    );

    // Step 5 3D Info: Trigger background telemetry & detail sync
    get().fetchVehicleTelemetry(id);
    get().fetchVehicleDetail(id);

    // Sync Digital Twin selection
    useDigitalTwinStore.getState().selectVehicle(id);
  },

  setHoveredVehicle: (id: number | null) => {
    if (id !== get().hoveredVehicleId) {
      if (id !== null) {
        AudioEngine.play('UI_HOVER');
      }
      set({ hoveredVehicleId: id });
    }
  },

  toggleVehicleLock: async (id: number) => {
    const { vehicles, inspectedVehicle } = get();
    const target = vehicles.find((v) => v.id === id);
    if (!target) return;

    const newLockState = !target.isLocked;
    AudioEngine.play(newLockState ? 'UI_CLICK' : 'NOTIF_SUCCESS');

    const updated = vehicles.map((v) => (v.id === id ? { ...v, isLocked: newLockState } : v));

    set({
      vehicles: updated,
      inspectedVehicle: inspectedVehicle?.id === id ? { ...inspectedVehicle, isLocked: newLockState } : inspectedVehicle,
    });
  },

  toggleCharging: async (id: number) => {
    const { vehicles } = get();
    const target = vehicles.find((v) => v.id === id);
    if (!target) return;

    const nextStatus: BackendVehicleStatus = target.isCharging ? 'AVAILABLE' : 'CHARGING';
    await get().transitionVehicleStatus(
      id,
      nextStatus,
      target.isCharging
        ? 'Unplugged from Central Garage charger'
        : 'Docked and plugged into Central Garage Supercharger'
    );
  },

  updateVehicleStatusLocally: (id: number, status: BackendVehicleStatus) => {
    set((state) => {
      const updated = state.vehicles.map((v) => (v.id === id ? { ...v, status } : v));
      return {
        vehicles: updated,
        inspectedVehicle:
          state.inspectedVehicle?.id === id ? { ...state.inspectedVehicle, status } : state.inspectedVehicle,
      };
    });
  },

  updateVehicleStatus: (id: number, status: BackendVehicleStatus) => {
    get().updateVehicleStatusLocally(id, status);
  },
}));
