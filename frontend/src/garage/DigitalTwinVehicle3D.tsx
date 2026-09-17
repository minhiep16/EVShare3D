import React, { useEffect, useMemo } from 'react';
import type { GarageVehicle } from './garageTypes';
import type { VehicleDigitalTwin } from '@/digitalTwin/digitalTwinTypes';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';
import { DigitalTwinVehicle3D as DigitalTwinCore3D } from '@/digitalTwin/DigitalTwinVehicle3D';
import { InteractionPipeline } from '@/engine/interaction/InteractionPipeline';
import { InteractionContext } from '@/engine/interaction/interactionTypes';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { useAppStore } from '@/stores/useAppStore';
import { useGarageStore } from './useGarageStore';

interface DigitalTwinVehicle3DProps {
  vehicle: GarageVehicle;
}

/**
 * Garage DigitalTwin Vehicle Adapter.
 * Bridges the Garage 3D vehicle object to:
 * 1. The 10-Stage Interaction Pipeline (approach -> hover -> select -> focus -> 3D info ...)
 * 2. The Authoritative Digital Twin 7-Facet state machine
 */
export const DigitalTwinVehicle3D: React.FC<DigitalTwinVehicle3DProps> = ({
  vehicle,
}) => {
  const storeTwin = useDigitalTwinStore((s) => s.digitalTwins[vehicle.id]);

  // 1. Register with the centralized InteractionPipeline
  useEffect(() => {
    const unregister = InteractionPipeline.register({
      id: `vehicle-${vehicle.id}`,
      name: `${vehicle.manufacturer} ${vehicle.modelName}`,
      targetPosition: vehicle.position,
      requirements: {
        maxInteractionDistance: 8.0,
      },
      onActivate: async () => {
        useGarageStore.getState().selectVehicle(vehicle.id);
        useDigitalTwinStore.getState().selectVehicle(vehicle.id);
      },
    });

    return unregister;
  }, [vehicle.id, vehicle.manufacturer, vehicle.modelName, vehicle.position]);

  // 2. Synthesize baseline twin if dynamic from backend
  const twin = useMemo<VehicleDigitalTwin>(() => {
    if (storeTwin) return storeTwin;

    return {
      vehicleId: vehicle.id,
      vin: vehicle.vin,
      licensePlate: vehicle.licensePlate,
      modelName: vehicle.modelName,
      manufacturer: vehicle.manufacturer,
      bodyColor: vehicle.bodyColor,
      battery: {
        level: vehicle.batteryLevel,
        isCharging: vehicle.isCharging,
        chargingPowerKw: vehicle.chargingPowerKw,
        estimatedRangeKm: Math.round(vehicle.batteryLevel * 4.6),
        healthPercentage: 98,
        temperatureCelsius: 28.0,
      },
      status: {
        status: vehicle.status,
        isLocked: vehicle.isLocked,
        stallLocationCode: vehicle.stallLocationCode,
        position: vehicle.position,
        rotation: vehicle.rotation,
        lastStatusChangeReason: 'Synchronized with Central Garage',
      },
      ownership: {
        groupId: vehicle.ownership?.groupId ?? 101,
        groupName: vehicle.ownership?.groupName ?? 'General Fleet',
        memberCount: vehicle.ownership?.memberCount ?? 1,
        userSharePercentage: vehicle.ownership?.userSharePercentage ?? 100,
        userVotingPower: vehicle.ownership?.userVotingPower ?? 100,
        capTableHash: '0x0000...0000',
        totalEquityValueVnd: 2000000000,
      },
      booking: {
        activeBookingId: null,
        reservedByUserId: null,
        reservedByUserName: null,
        startTime: null,
        endTime: null,
        conflictStatus: 'NONE',
        purpose: 'Available',
      },
      usage: {
        activeSessionId: null,
        driverUserId: null,
        driverUserName: null,
        sessionStartTime: null,
        currentSpeedKmh: 0,
        odometerKm: vehicle.odometerKm,
        checkOutDamageReported: false,
      },
      maintenance: {
        serviceStatus: vehicle.status === 'MAINTENANCE' ? 'IN_SERVICE' : 'NOMINAL',
        activeWorkOrderId: null,
        overallHealthScore: 98,
        subsystems: {
          BRAKES: { name: 'Carbon Ceramic Brakes', health: 98, status: 'NOMINAL' },
          ADAS: { name: 'Level 2+ Driving Assistant', health: 100, status: 'NOMINAL' },
          BATTERY: { name: 'High-Voltage Battery Pack', health: 99, status: 'NOMINAL' },
          SUSPENSION: { name: 'Adaptive Dampers', health: 98, status: 'NOMINAL' },
          INVERTER: { name: 'Dual SiC Inverter', health: 99, status: 'NOMINAL' },
        },
        activeDtcCodes: [],
        lastCertifiedAt: new Date().toISOString(),
      },
      finance: {
        vaultBalanceVnd: 10000000,
        accruedExpenseLiabilityVnd: 0,
        userDepositVnd: 5000000,
        costPerKm: 2500,
        lastFundDeductionRef: 'INITIAL',
        depositStatus: 'HELD',
      },
      lastSyncTimestamp: new Date().toISOString(),
      syncSource: 'BACKEND_AUTHORITATIVE',
    };
  }, [storeTwin, vehicle]);

  // 3. Stage 1 (Approach) -> Stage 3 (Select) validation & execution
  const handleSelect = (id: number) => {
    const playerPos = usePlayerStore.getState().position;
    const [tx, ty, tz] = vehicle.position;
    const dx = playerPos[0] - tx;
    const dy = playerPos[1] - ty;
    const dz = playerPos[2] - tz;
    const distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

    const auth = useAppStore.getState().auth;
    const context: InteractionContext = {
      targetId: `vehicle-${id}`,
      playerPosition: playerPos,
      targetPosition: vehicle.position,
      distanceToPlayer: distance,
      userRoles: auth?.roles || [],
      isAuthenticated: useAppStore.getState().isAuthenticated,
      userId: auth?.userId,
    };

    InteractionPipeline.execute(`vehicle-${id}`, context);
  };

  // 4. Stage 2 (Hover) dispatch
  const handleHover = (id: number | null) => {
    useInteractionStore.getState().setHoveredObjectId(id ? `vehicle-${id}` : null);
    useGarageStore.getState().setHoveredVehicle(id);
  };

  return (
    <DigitalTwinCore3D
      vehicle={twin}
      onSelect={handleSelect}
      onHover={handleHover}
    />
  );
};
