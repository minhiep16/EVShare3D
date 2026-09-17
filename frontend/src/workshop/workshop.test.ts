import { describe, it, expect, beforeEach } from 'vitest';
import {
  WORKSHOP_SECTOR_CENTER,
  WORKSHOP_STATIONS,
  WORKSHOP_CAMERA_PRESETS,
  WORKSHOP_THEME,
} from './workshopLayout';
import { useWorkshopStore } from './useWorkshopStore';

describe('Service Workshop Sector (09-L)', () => {
  beforeEach(() => {
    useWorkshopStore.getState().resetToDefaults();
  });

  describe('1. Spatial Layout, Stations & Presets', () => {
    it('defines accurate sector center and theme palette', () => {
      expect(WORKSHOP_SECTOR_CENTER).toEqual([-40, 0, 40]);
      expect(WORKSHOP_THEME.primary).toBe('#38bdf8');
      expect(WORKSHOP_THEME.secondary).toBe('#f59e0b');
      expect(WORKSHOP_THEME.darkBase).toBe('#09090b');
    });

    it('defines 4 workshop station coordinates', () => {
      const stations = ['HYDRAULIC_LIFT', 'DIAGNOSTIC_CART', 'PARTS_RACK', 'WORK_ORDER_STELA'] as const;
      stations.forEach((st) => {
        expect(WORKSHOP_STATIONS[st]).toBeDefined();
        expect(WORKSHOP_STATIONS[st].relativePosition).toHaveLength(3);
        expect(WORKSHOP_STATIONS[st].worldPosition).toHaveLength(3);
      });

      expect(WORKSHOP_STATIONS.HYDRAULIC_LIFT.worldPosition).toEqual([-40, 0, 40]);
      expect(WORKSHOP_STATIONS.DIAGNOSTIC_CART.worldPosition).toEqual([-43.6, 0, 42.4]);
      expect(WORKSHOP_STATIONS.PARTS_RACK.worldPosition).toEqual([-36.2, 0, 42.4]);
      expect(WORKSHOP_STATIONS.WORK_ORDER_STELA.worldPosition).toEqual([-40, 0, 35.2]);
    });

    it('defines 6 camera presets with valid position and target tuples', () => {
      const presets = Object.keys(WORKSHOP_CAMERA_PRESETS);
      expect(presets).toHaveLength(6);
      expect(presets).toContain('WORKSHOP_OVERVIEW');
      expect(presets).toContain('HYDRAULIC_LIFT_FOCUS');
      expect(presets).toContain('DIAGNOSTIC_CART_FOCUS');
      expect(presets).toContain('PARTS_RACK_FOCUS');
      expect(presets).toContain('WORK_ORDER_STELA_FOCUS');
      expect(presets).toContain('UNDERCARRIAGE_INSPECTION');

      Object.values(WORKSHOP_CAMERA_PRESETS).forEach((cp) => {
        expect(cp.position).toHaveLength(3);
        expect(cp.target).toHaveLength(3);
      });
    });
  });

  describe('2. Vehicle Subsystems & Work Order State', () => {
    it('initializes with VinFast VF6 Eco on hydraulic lift with 5 monitored subsystems', () => {
      const { workOrder, subsystems } = useWorkshopStore.getState();
      expect(workOrder.vehicleId).toBe(3);
      expect(workOrder.vehicleModel).toBe('VinFast VF6 Eco');
      expect(workOrder.licensePlate).toBe('30A-666.66');
      expect(workOrder.status).toBe('IN_SERVICE');

      const subs = Object.values(subsystems);
      expect(subs).toHaveLength(5);
      expect(subsystems.BRAKE_SYSTEM.faultCode).toBe('DTC-P1A24');
      expect(subsystems.BRAKE_SYSTEM.healthPercentage).toBe(38);
      expect(subsystems.LIDAR_ADAS.faultCode).toBe('DTC-U0100');
      expect(subsystems.LIDAR_ADAS.healthPercentage).toBe(42);
      expect(subsystems.HIGH_VOLTAGE_BATTERY.faultCode).toBeNull();
      expect(subsystems.HIGH_VOLTAGE_BATTERY.healthPercentage).toBe(94);
    });

    it('handles station and subsystem selection', () => {
      const store = useWorkshopStore.getState();
      store.setActiveStation('DIAGNOSTIC_CART');
      expect(useWorkshopStore.getState().activeTab).toBe('DIAGNOSTICS');

      store.selectSubsystem('LIDAR_ADAS');
      expect(useWorkshopStore.getState().selectedSubsystemId).toBe('LIDAR_ADAS');
    });
  });

  describe('3. Hydraulic Vehicle Lift Controls', () => {
    it('elevates lift smoothly to 1.8m undercarriage service height', () => {
      const store = useWorkshopStore.getState();
      expect(store.isElevated).toBe(false);
      expect(store.liftHeight).toBe(0.2);

      store.elevateLift();
      const elevated = useWorkshopStore.getState();
      expect(elevated.isElevated).toBe(true);
      expect(elevated.liftHeight).toBe(1.8);
      expect(elevated.feedbackMessage).toContain('elevated');
    });

    it('lowers lift back to ground staging height (0.2m)', () => {
      const store = useWorkshopStore.getState();
      store.elevateLift();
      store.lowerLift();

      const lowered = useWorkshopStore.getState();
      expect(lowered.isElevated).toBe(false);
      expect(lowered.liftHeight).toBe(0.2);
      expect(lowered.feedbackMessage).toContain('lowered');
    });
  });

  describe('4. Diagnostic Scanning & Subsystem Repairs', () => {
    it('executes OBD-II scan and reports detected fault codes', async () => {
      const store = useWorkshopStore.getState();
      await store.runObdDiagnostic();

      const state = useWorkshopStore.getState();
      expect(state.diagnosticCompleted).toBe(true);
      expect(state.isScanningObd).toBe(false);
      expect(state.feedbackMessage).toContain('DTC-P1A24');
    });

    it('repairs individual subsystem, restores 100% health, and clears DTC fault code', async () => {
      const store = useWorkshopStore.getState();
      expect(store.subsystems.BRAKE_SYSTEM.faultCode).toBe('DTC-P1A24');

      await store.repairSubsystem('BRAKE_SYSTEM');

      const updated = useWorkshopStore.getState().subsystems.BRAKE_SYSTEM;
      expect(updated.faultCode).toBeNull();
      expect(updated.healthPercentage).toBe(100);
      expect(updated.status).toBe('REPAIRED');
    });

    it('repairs all vehicle faults and updates work order to READY_FOR_RELEASE', async () => {
      const store = useWorkshopStore.getState();
      await store.repairAllFaults();

      const state = useWorkshopStore.getState();
      expect(state.subsystems.BRAKE_SYSTEM.faultCode).toBeNull();
      expect(state.subsystems.LIDAR_ADAS.faultCode).toBeNull();
      expect(state.workOrder.status).toBe('READY_FOR_RELEASE');
    });
  });

  describe('5. Strict "No Fake Completed Maintenance" Interlock', () => {
    it('rejects vehicle certification and blocks release when active DTC faults remain', async () => {
      const store = useWorkshopStore.getState();
      // Vehicle still has DTC-P1A24 and DTC-U0100
      const released = await store.certifyAndReleaseVehicle();

      expect(released).toBe(false);
      const state = useWorkshopStore.getState();
      expect(state.workOrder.status).toBe('IN_SERVICE');
      expect(state.safetyViolationNotice).toContain('SAFETY INTERLOCK ACTIVATED');
      expect(state.safetyViolationNotice).toContain('DTC-P1A24');
    });

    it('rejects vehicle certification when repairs are completed but ledger expense is uncommitted', async () => {
      const store = useWorkshopStore.getState();
      // Clear faults first
      await store.repairAllFaults();
      expect(useWorkshopStore.getState().subsystems.BRAKE_SYSTEM.faultCode).toBeNull();

      // Attempt release without committing expense
      const released = await store.certifyAndReleaseVehicle();

      expect(released).toBe(false);
      const state = useWorkshopStore.getState();
      expect(state.workOrder.status).toBe('READY_FOR_RELEASE');
      expect(state.safetyViolationNotice).toContain('AUDIT INTERLOCK ACTIVATED');
    });

    it('commits maintenance expense to syndicate ledger and assigns backend expense ID', async () => {
      const store = useWorkshopStore.getState();
      await store.commitMaintenanceExpense();

      const state = useWorkshopStore.getState();
      expect(state.workOrder.backendExpenseId).toBeTruthy();
      expect(state.workOrder.invoiceReference).toContain('INV-WS');
      expect(state.feedbackMessage?.toLowerCase()).toContain('expense committed');
    });

    it('permits certification and releases vehicle to AVAILABLE only after repairs and ledger commitment', async () => {
      const store = useWorkshopStore.getState();

      // 1. Repair all faults
      await store.repairAllFaults();
      // 2. Commit maintenance expense to backend ledger
      await store.commitMaintenanceExpense();

      // 3. Certify and release
      const released = await store.certifyAndReleaseVehicle();

      expect(released).toBe(true);
      const state = useWorkshopStore.getState();
      expect(state.workOrder.status).toBe('RELEASED');
      expect(state.workOrder.completedAt).toBeTruthy();
      expect(state.safetyViolationNotice).toBeNull();
      expect(state.feedbackMessage).toContain('CERTIFICATION COMPLETE');
    });
  });
});
