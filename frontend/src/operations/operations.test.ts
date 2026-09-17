import { describe, it, expect, beforeEach } from 'vitest';
import {
  OPERATIONS_SECTOR_CENTER,
  OPERATIONS_STATIONS,
  OPERATIONS_CAMERA_PRESETS,
  SERVICE_BAYS,
  OPERATIONS_THEME,
} from './operationsLayout';
import { useOperationsStore } from './useOperationsStore';

describe('Operations Center Sector (09-K)', () => {
  beforeEach(() => {
    useOperationsStore.getState().resetToDefaults();
  });

  describe('1. Spatial Layout, Stations & Presets', () => {
    it('defines accurate sector center and theme palette', () => {
      expect(OPERATIONS_SECTOR_CENTER).toEqual([-40, 0, 0]);
      expect(OPERATIONS_THEME.primary).toBe('#f97316');
      expect(OPERATIONS_THEME.hazardYellow).toBe('#eab308');
      expect(OPERATIONS_THEME.darkBase).toBe('#080b12');
    });

    it('defines 5 operational station coordinates', () => {
      const stations = ['QR_DESK', 'DISPATCH_CONSOLE', 'FLEET_STELA', 'NOTIFICATION_BOARD', 'INSPECTION_BAY'] as const;
      stations.forEach((st) => {
        expect(OPERATIONS_STATIONS[st]).toBeDefined();
        expect(OPERATIONS_STATIONS[st].relativePosition).toHaveLength(3);
        expect(OPERATIONS_STATIONS[st].worldPosition).toHaveLength(3);
      });

      expect(OPERATIONS_STATIONS.QR_DESK.worldPosition).toEqual([-43.4, 0, 1.2]);
      expect(OPERATIONS_STATIONS.DISPATCH_CONSOLE.worldPosition).toEqual([-40, 0, 2.5]);
      expect(OPERATIONS_STATIONS.FLEET_STELA.worldPosition).toEqual([-40, 0, -4.8]);
      expect(OPERATIONS_STATIONS.NOTIFICATION_BOARD.worldPosition).toEqual([-36.4, 0, 1.2]);
      expect(OPERATIONS_STATIONS.INSPECTION_BAY.worldPosition).toEqual([-40, 0, -1.2]);
    });

    it('defines 6 camera presets with valid position and target tuples', () => {
      const presets = Object.keys(OPERATIONS_CAMERA_PRESETS);
      expect(presets).toHaveLength(6);
      expect(presets).toContain('HANGAR_OVERVIEW');
      expect(presets).toContain('QR_STATION_FOCUS');
      expect(presets).toContain('DISPATCH_CONSOLE_FOCUS');
      expect(presets).toContain('FLEET_STATUS_FOCUS');
      expect(presets).toContain('NOTIFICATION_BOARD_FOCUS');
      expect(presets).toContain('INSPECTION_BAY_FOCUS');

      Object.values(OPERATIONS_CAMERA_PRESETS).forEach((cp) => {
        expect(cp.position).toHaveLength(3);
        expect(cp.target).toHaveLength(3);
      });
    });

    it('defines 3 service bays with correct offsets', () => {
      expect(SERVICE_BAYS).toHaveLength(3);
      expect(SERVICE_BAYS[0].id).toBe('BAY_01');
      expect(SERVICE_BAYS[1].id).toBe('BAY_02');
      expect(SERVICE_BAYS[2].id).toBe('BAY_03');
    });
  });

  describe('2. Operations Store & Fleet State', () => {
    it('initializes with 3 digital twin fleet vehicles', () => {
      const fleet = useOperationsStore.getState().fleet;
      expect(fleet).toHaveLength(3);
      expect(fleet[0].modelName).toBe('VinFast VF8 Plus');
      expect(fleet[0].status).toBe('AVAILABLE');
      expect(fleet[1].modelName).toBe('VinFast VF9 Executive');
      expect(fleet[1].status).toBe('IN_USE');
      expect(fleet[2].modelName).toBe('VinFast VF6 Eco');
      expect(fleet[2].status).toBe('MAINTENANCE');
    });

    it('selects vehicle and updates input buffers', () => {
      const store = useOperationsStore.getState();
      store.selectVehicle(2);

      const state = useOperationsStore.getState();
      expect(state.selectedVehicleId).toBe(2);
      expect(state.inputOdometer).toBe(18230);
      expect(state.inputBattery).toBe(64);
      expect(state.operationMessage).toContain('VinFast VF9 Executive');
    });

    it('allows clamping odometer and battery inputs', () => {
      const store = useOperationsStore.getState();
      store.setInputOdometer(-500);
      expect(useOperationsStore.getState().inputOdometer).toBe(0);

      store.setInputBattery(150);
      expect(useOperationsStore.getState().inputBattery).toBe(100);

      store.setInputBattery(-20);
      expect(useOperationsStore.getState().inputBattery).toBe(0);
    });

    it('handles station and mode toggles', () => {
      const store = useOperationsStore.getState();
      store.setActiveStation('QR_DESK');
      expect(useOperationsStore.getState().activeTab).toBe('QR_STATION');

      store.setActiveStation('DISPATCH_CONSOLE');
      expect(useOperationsStore.getState().activeTab).toBe('CHECK_IN_OUT');

      store.setCheckInOutMode('CHECK_OUT');
      expect(useOperationsStore.getState().checkInOutMode).toBe('CHECK_OUT');
    });
  });

  describe('3. Operational Incident Alerts', () => {
    it('acknowledges an individual alert', () => {
      const store = useOperationsStore.getState();
      expect(store.alerts[0].isAcknowledged).toBe(false);

      store.acknowledgeAlert(store.alerts[0].id);
      expect(useOperationsStore.getState().alerts[0].isAcknowledged).toBe(true);
      expect(useOperationsStore.getState().alerts[1].isAcknowledged).toBe(false);
    });

    it('dismisses all operational alerts simultaneously', () => {
      const store = useOperationsStore.getState();
      store.dismissAllAlerts();

      const allAck = useOperationsStore.getState().alerts.every((a) => a.isAcknowledged);
      expect(allAck).toBe(true);
    });
  });

  describe('4. Optical QR Station Workflow', () => {
    it('generates optical QR code token', async () => {
      const store = useOperationsStore.getState();
      await store.generateQrCode(105);

      const state = useOperationsStore.getState();
      expect(state.qrToken).toBeTruthy();
      expect(state.isSubmitting).toBe(false);
      expect(state.operationMessage?.toLowerCase()).toContain('token');
    });

    it('validates optical QR code token and updates state', async () => {
      const store = useOperationsStore.getState();
      await store.validateQrCode('TEST-QR-TOKEN-123');

      const state = useOperationsStore.getState();
      expect(state.qrValidationResult).toBeTruthy();
      expect(state.qrValidationResult?.valid).toBe(true);
      expect(state.isScanningQr).toBe(false);
    });
  });

  describe('5. Check-In & Check-Out Dispatch Workflows', () => {
    it('executes vehicle check-in and updates vehicle to IN_USE', async () => {
      const store = useOperationsStore.getState();
      store.selectVehicle(1); // VF8 is AVAILABLE
      store.setInputOdometer(12500);
      store.setInputBattery(95);

      await store.executeCheckIn();

      const state = useOperationsStore.getState();
      const updatedVehicle = state.fleet.find((v) => v.vehicleId === 1);
      expect(updatedVehicle?.status).toBe('IN_USE');
      expect(updatedVehicle?.odometerKm).toBe(12500);
      expect(updatedVehicle?.batterySoc).toBe(95);
      expect(state.operationMessage).toContain('CHECK-IN');
    });

    it('executes vehicle check-out and reconciles status to AVAILABLE if clean', async () => {
      const store = useOperationsStore.getState();
      store.selectVehicle(2); // VF9 is IN_USE
      store.setCheckInOutMode('CHECK_OUT');
      store.setInputOdometer(18450);
      store.setInputBattery(50);
      store.setHasDamageReported(false);

      await store.executeCheckOut();

      const state = useOperationsStore.getState();
      const updatedVehicle = state.fleet.find((v) => v.vehicleId === 2);
      expect(updatedVehicle?.status).toBe('AVAILABLE');
      expect(updatedVehicle?.odometerKm).toBe(18450);
      expect(updatedVehicle?.batterySoc).toBe(50);
      expect(state.operationMessage).toContain('CHECK-OUT');
    });

    it('executes vehicle check-out with damage and flags status to MAINTENANCE', async () => {
      const store = useOperationsStore.getState();
      store.selectVehicle(2);
      store.setCheckInOutMode('CHECK_OUT');
      store.setHasDamageReported(true);

      await store.executeCheckOut();

      const state = useOperationsStore.getState();
      const updatedVehicle = state.fleet.find((v) => v.vehicleId === 2);
      expect(updatedVehicle?.status).toBe('MAINTENANCE');
    });

    it('toggles vehicle operational maintenance status', async () => {
      const store = useOperationsStore.getState();
      // Vehicle 1 is AVAILABLE
      await store.updateVehicleOperationalStatus(1, 'MAINTENANCE');
      expect(useOperationsStore.getState().fleet.find((v) => v.vehicleId === 1)?.status).toBe('MAINTENANCE');

      await store.updateVehicleOperationalStatus(1, 'AVAILABLE');
      expect(useOperationsStore.getState().fleet.find((v) => v.vehicleId === 1)?.status).toBe('AVAILABLE');
    });
  });
});
