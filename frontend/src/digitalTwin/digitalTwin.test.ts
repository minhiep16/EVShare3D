import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useDigitalTwinStore } from './useDigitalTwinStore';
import { vehiclesApi } from '../api/vehiclesApi';

vi.mock('../api/vehiclesApi', () => ({
  vehiclesApi: {
    getVehicleById: vi.fn(),
    getVehicleTelemetry: vi.fn(),
    updateStatus: vi.fn(),
  },
}));

describe('Vehicle Digital Twin Synchronization Layer (09-O)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useDigitalTwinStore.getState().resetToDefaults();
  });

  describe('1. Authoritative Digital Twin Pipeline & Baseline Schema', () => {
    it('initializes fleet with authoritative baseline schema (zero fake truth)', () => {
      const { digitalTwins } = useDigitalTwinStore.getState();
      const ids = Object.keys(digitalTwins).map(Number);

      expect(ids).toHaveLength(5);
      expect(ids).toContain(1);
      expect(ids).toContain(2);
      expect(ids).toContain(3);
      expect(ids).toContain(4);
      expect(ids).toContain(5);

      const tesla = digitalTwins[1];
      expect(tesla.vin).toBe('1HGCR2F83HA001201');
      expect(tesla.licensePlate).toBe('51K-882.14');
      expect(tesla.modelName).toBe('Model S Plaid');
      expect(tesla.syncSource).toBe('BACKEND_AUTHORITATIVE');
    });

    it('verifies all 7 conceptual facets are present on each digital twin', () => {
      const { digitalTwins } = useDigitalTwinStore.getState();
      const twin = digitalTwins[1];

      expect(twin.battery).toBeDefined();
      expect(twin.status).toBeDefined();
      expect(twin.ownership).toBeDefined();
      expect(twin.booking).toBeDefined();
      expect(twin.usage).toBeDefined();
      expect(twin.maintenance).toBeDefined();
      expect(twin.finance).toBeDefined();
    });
  });

  describe('2. 7-Facet State Reflection', () => {
    it('reflects Facet 1: Battery (SoC, charging rate, range, thermal)', () => {
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.battery.level).toBe(88);
      expect(twin.battery.estimatedRangeKm).toBe(512);
      expect(twin.battery.healthPercentage).toBe(99);
      expect(twin.battery.temperatureCelsius).toBe(28.5);

      useDigitalTwinStore.getState().updateBattery(1, {
        level: 95,
        isCharging: true,
        chargingPowerKw: 150,
      });

      const updated = useDigitalTwinStore.getState().digitalTwins[1];
      expect(updated.battery.level).toBe(95);
      expect(updated.battery.isCharging).toBe(true);
      expect(updated.battery.chargingPowerKw).toBe(150);
    });

    it('reflects Facet 2: Status (Lifecycle status, lock state, stall code)', async () => {
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.status.status).toBe('AVAILABLE');
      expect(twin.status.isLocked).toBe(true);
      expect(twin.status.stallLocationCode).toBe('BAY-01');

      await useDigitalTwinStore
        .getState()
        .updateStatus(1, 'MAINTENANCE', 'Dispatched to workshop for brake overhaul');

      const updated = useDigitalTwinStore.getState().digitalTwins[1];
      expect(updated.status.status).toBe('MAINTENANCE');
      expect(updated.status.lastStatusChangeReason).toContain('overhaul');
    });

    it('reflects Facet 3: Ownership (Syndicate equity, voting power, cap table hash)', () => {
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.ownership.groupName).toBe('CyberSprint Syndicate');
      expect(twin.ownership.userSharePercentage).toBe(25);
      expect(twin.ownership.userVotingPower).toBe(25);
      expect(twin.ownership.capTableHash).toContain('0x8f2a9e');

      useDigitalTwinStore.getState().updateOwnership(1, {
        userSharePercentage: 30,
        userVotingPower: 30,
      });

      const updated = useDigitalTwinStore.getState().digitalTwins[1];
      expect(updated.ownership.userSharePercentage).toBe(30);
    });

    it('reflects Facet 4: Booking (Reservation slot, co-owner, conflict status)', () => {
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.booking.activeBookingId).toBe(201);
      expect(twin.booking.reservedByUserName).toBe('Alice Owner');
      expect(twin.booking.conflictStatus).toBe('NONE');

      useDigitalTwinStore.getState().updateBooking(1, {
        activeBookingId: 202,
        reservedByUserName: 'Bob Driver',
        conflictStatus: 'RESOLVED',
      });

      const updated = useDigitalTwinStore.getState().digitalTwins[1];
      expect(updated.booking.activeBookingId).toBe(202);
      expect(updated.booking.reservedByUserName).toBe('Bob Driver');
      expect(updated.booking.conflictStatus).toBe('RESOLVED');
    });

    it('reflects Facet 5: Usage (Driver, speed, odometer telemetry, damage report)', () => {
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.usage.odometerKm).toBe(14250);
      expect(twin.usage.currentSpeedKmh).toBe(0);

      useDigitalTwinStore.getState().updateUsage(1, {
        activeSessionId: 106,
        driverUserName: 'Charlie Member',
        currentSpeedKmh: 68,
        odometerKm: 14285,
        checkOutDamageReported: true,
      });

      const updated = useDigitalTwinStore.getState().digitalTwins[1];
      expect(updated.usage.activeSessionId).toBe(106);
      expect(updated.usage.currentSpeedKmh).toBe(68);
      expect(updated.usage.odometerKm).toBe(14285);
      expect(updated.usage.checkOutDamageReported).toBe(true);
    });

    it('reflects Facet 6: Maintenance (Subsystems, health score, DTC fault codes)', () => {
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.maintenance.serviceStatus).toBe('NOMINAL');
      expect(twin.maintenance.overallHealthScore).toBe(98);
      expect(twin.maintenance.subsystems.BRAKES.name).toBe('Carbon Ceramic Brakes');

      useDigitalTwinStore.getState().updateMaintenance(1, {
        serviceStatus: 'NEEDS_SERVICE',
        overallHealthScore: 78,
        activeDtcCodes: ['DTC-P1A24', 'DTC-U0100'],
      });

      const updated = useDigitalTwinStore.getState().digitalTwins[1];
      expect(updated.maintenance.serviceStatus).toBe('NEEDS_SERVICE');
      expect(updated.maintenance.activeDtcCodes).toHaveLength(2);
      expect(updated.maintenance.activeDtcCodes).toContain('DTC-P1A24');
    });

    it('reflects Facet 7: Finance (SharedFund balance, liabilities, cost per km)', () => {
      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.finance.vaultBalanceVnd).toBe(12450000);
      expect(twin.finance.costPerKm).toBe(2500);
      expect(twin.finance.depositStatus).toBe('HELD');

      useDigitalTwinStore.getState().updateFinance(1, {
        vaultBalanceVnd: 14450000,
        accruedExpenseLiabilityVnd: 0,
        depositStatus: 'CLEARED',
      });

      const updated = useDigitalTwinStore.getState().digitalTwins[1];
      expect(updated.finance.vaultBalanceVnd).toBe(14450000);
      expect(updated.finance.accruedExpenseLiabilityVnd).toBe(0);
      expect(updated.finance.depositStatus).toBe('CLEARED');
    });
  });

  describe('3. Backend Response -> Application State -> Twin State Pipeline', () => {
    it('synchronizes digital twin state directly from backend response payload', async () => {
      vi.mocked(vehiclesApi.getVehicleById).mockResolvedValue({
        id: 1,
        vin: '1HGCR2F83HA001201',
        licensePlate: '51K-882.14',
        modelName: 'Model S Plaid',
        manufacturer: 'Tesla',
        status: 'CHARGING',
        batteryLevel: 94,
        odometerKm: 14260,
      });

      vi.mocked(vehiclesApi.getVehicleTelemetry).mockResolvedValue({
        vehicleId: 1,
        vin: '1HGCR2F83HA001201',
        status: 'CHARGING',
        batteryLevel: 94,
        odometerKm: 14260,
        stallLocationCode: 'BAY-CHG-01',
        estimatedRangeKm: 535,
        lastTelemetrySync: '2026-09-16T10:15:00Z',
      });

      const success = await useDigitalTwinStore.getState().syncFromBackend(1);
      expect(success).toBe(true);

      const twin = useDigitalTwinStore.getState().digitalTwins[1];
      expect(twin.status.status).toBe('CHARGING');
      expect(twin.status.stallLocationCode).toBe('BAY-CHG-01');
      expect(twin.battery.level).toBe(94);
      expect(twin.battery.estimatedRangeKm).toBe(535);
      expect(twin.usage.odometerKm).toBe(14260);
      expect(twin.syncSource).toBe('BACKEND_AUTHORITATIVE');
    });

    it('synchronizes entire fleet in bulk', async () => {
      vi.mocked(vehiclesApi.getVehicleById).mockResolvedValue({
        id: 1,
        vin: 'VIN-TEST',
        licensePlate: 'PLATE-TEST',
        modelName: 'Test EV',
        manufacturer: 'Test',
        status: 'AVAILABLE',
        batteryLevel: 90,
        odometerKm: 1000,
      });

      vi.mocked(vehiclesApi.getVehicleTelemetry).mockResolvedValue({
        vehicleId: 1,
        vin: 'VIN-TEST',
        status: 'AVAILABLE',
        batteryLevel: 90,
        odometerKm: 1000,
        stallLocationCode: 'BAY-01',
        estimatedRangeKm: 500,
        lastTelemetrySync: '2026-09-16T10:20:00Z',
      });

      const success = await useDigitalTwinStore.getState().syncAllFromBackend();
      expect(success).toBe(true);
      expect(useDigitalTwinStore.getState().syncNotice).toContain('PIPELINE COMPLETE');
    });

    it('switches active facet tab cleanly', () => {
      useDigitalTwinStore.getState().setActiveFacetTab('MAINTENANCE');
      expect(useDigitalTwinStore.getState().activeFacetTab).toBe('MAINTENANCE');

      useDigitalTwinStore.getState().setActiveFacetTab('FINANCE');
      expect(useDigitalTwinStore.getState().activeFacetTab).toBe('FINANCE');
    });
  });
});
