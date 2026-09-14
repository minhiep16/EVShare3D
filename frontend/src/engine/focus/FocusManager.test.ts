import { describe, it, expect, beforeEach, vi } from 'vitest';
import { FocusRegistry, DEFAULT_CATEGORY_PRESETS } from './FocusRegistry';
import { useFocusStore } from './useFocusStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { FocusTargetConfig } from './focusTypes';

describe('FocusManager & Spatial Framing Subsystem', () => {
  beforeEach(() => {
    FocusRegistry.clear();
    useFocusStore.setState({
      focusedTargetId: null,
      activeCategory: null,
      isFocused: false,
      activeConfig: null,
      savedCameraStack: [],
    });
    useCameraStore.setState({
      mode: 'ORBIT',
      currentPosition: [0, 5, 10],
      desiredPosition: [0, 5, 10],
      currentTarget: [0, 0, 0],
      desiredTarget: [0, 0, 0],
      currentFov: 45,
      desiredFov: 45,
      isTransitioning: false,
    });
  });

  describe('FocusRegistry Presets', () => {
    it('provides standardized ergonomic presets for vehicles, terminals, portals, and objects', () => {
      const vehiclePreset = FocusRegistry.getPresetForCategory('VEHICLE');
      expect(vehiclePreset.distance).toBe(5.8);
      expect(vehiclePreset.fov).toBe(42);

      const terminalPreset = FocusRegistry.getPresetForCategory('TERMINAL');
      expect(terminalPreset.distance).toBe(1.8);
      expect(terminalPreset.fov).toBe(34); // Crisp text legibility
      expect(terminalPreset.azimuth).toBe(0); // Perpendicular viewing

      const portalPreset = FocusRegistry.getPresetForCategory('PORTAL');
      expect(portalPreset.distance).toBe(4.5);
      expect(portalPreset.elevation).toBe(1.2);
    });
  });

  describe('focusObject & Smooth Camera Framing', () => {
    it('focuses a terminal, computes eye coordinates, and saves previous camera state', () => {
      const enterSpy = vi.fn();

      const terminalTarget: FocusTargetConfig = {
        id: 'kiosk_terminal_01',
        name: 'Co-Ownership Decision Kiosk',
        category: 'TERMINAL',
        targetPosition: [5, 1.2, -4],
        onFocusEnter: enterSpy,
      };

      FocusRegistry.registerTarget(terminalTarget);

      useFocusStore.getState().focusObject('kiosk_terminal_01');

      const focusState = useFocusStore.getState();
      expect(focusState.isFocused).toBe(true);
      expect(focusState.focusedTargetId).toBe('kiosk_terminal_01');
      expect(focusState.activeCategory).toBe('TERMINAL');
      expect(focusState.savedCameraStack.length).toBe(1);

      // Verify saved camera was the initial ORBIT view
      expect(focusState.savedCameraStack[0].mode).toBe('ORBIT');
      expect(focusState.savedCameraStack[0].transform.position).toEqual([0, 5, 10]);

      // Verify camera was set to INSPECT and desired target updated to terminal position
      const camState = useCameraStore.getState();
      expect(camState.mode).toBe('INSPECT');
      expect(camState.desiredTarget).toEqual([5, 1.2, -4]);

      // Check eye position: azimuth=0 -> eyeX=5, eyeY=1.2+0.2=1.4, eyeZ=-4 + 1.8 = -2.2
      expect(camState.desiredPosition[0]).toBeCloseTo(5);
      expect(camState.desiredPosition[1]).toBeCloseTo(1.4);
      expect(camState.desiredPosition[2]).toBeCloseTo(-2.2);
      expect(camState.desiredFov).toBe(34);
      expect(camState.isTransitioning).toBe(true);
      expect(enterSpy).toHaveBeenCalledTimes(1);
    });

    it('adapts camera distance for large bounding radius objects (e.g. EV trucks)', () => {
      const vehicleTarget: FocusTargetConfig = {
        id: 'cybertruck_vehicle',
        name: 'Tesla Cybertruck Dual Motor',
        category: 'VEHICLE',
        targetPosition: [0, 0, 0],
        boundingRadius: 4.0, // 4-meter radius vehicle
      };

      FocusRegistry.registerTarget(vehicleTarget);

      useFocusStore.getState().focusObject('cybertruck_vehicle');

      const camState = useCameraStore.getState();
      // Distance scaled: boundingRadius * 2.2 = 8.8m (larger than base 5.8m)
      const dx = camState.desiredPosition[0] - 0;
      const dz = camState.desiredPosition[2] - 0;
      const horizontalDist = Math.sqrt(dx * dx + dz * dz);

      expect(horizontalDist).toBeCloseTo(8.8);
      expect(camState.desiredFov).toBe(42);
    });
  });

  describe('returnToPreviousCamera & Focus History', () => {
    it('restores previous camera vantage point and mode smoothly', () => {
      const exitSpy = vi.fn();

      // Initial custom camera: FIRST_PERSON
      useCameraStore.setState({
        mode: 'FIRST_PERSON',
        currentPosition: [2, 1.7, 3],
        currentTarget: [2, 1.7, -2],
        currentFov: 55,
      });

      const portalTarget: FocusTargetConfig = {
        id: 'main_portal',
        name: 'Gateway Portal',
        category: 'PORTAL',
        targetPosition: [0, 0, -8],
        onFocusExit: exitSpy,
      };

      FocusRegistry.registerTarget(portalTarget);

      // Focus
      useFocusStore.getState().focusObject('main_portal');
      expect(useCameraStore.getState().mode).toBe('INSPECT');
      expect(useFocusStore.getState().isFocused).toBe(true);

      // Return
      useFocusStore.getState().returnToPreviousCamera();

      expect(useFocusStore.getState().isFocused).toBe(false);
      expect(useFocusStore.getState().focusedTargetId).toBeNull();

      // Camera restored to FIRST_PERSON and original coordinates
      const camState = useCameraStore.getState();
      expect(camState.mode).toBe('FIRST_PERSON');
      expect(camState.desiredPosition).toEqual([2, 1.7, 3]);
      expect(camState.desiredTarget).toEqual([2, 1.7, -2]);
      expect(camState.desiredFov).toBe(55);
      expect(camState.isTransitioning).toBe(true);
      expect(exitSpy).toHaveBeenCalledTimes(1);
    });

    it('supports nested focus stacking (Room -> Vehicle -> Dashboard Terminal -> Vehicle -> Room)', () => {
      const vehicle: FocusTargetConfig = {
        id: 'ev_car',
        name: 'Model 3',
        category: 'VEHICLE',
        targetPosition: [0, 0, 0],
      };

      const dashboardTerminal: FocusTargetConfig = {
        id: 'ev_dashboard',
        name: 'Touchscreen Console',
        category: 'TERMINAL',
        targetPosition: [0, 0.9, 0.4],
      };

      FocusRegistry.registerTarget(vehicle);
      FocusRegistry.registerTarget(dashboardTerminal);

      // 1. Focus vehicle from room view
      useFocusStore.getState().focusObject('ev_car');
      expect(useFocusStore.getState().focusedTargetId).toBe('ev_car');
      expect(useFocusStore.getState().savedCameraStack.length).toBe(1);

      // 2. Focus dashboard terminal from vehicle view
      useFocusStore.getState().focusObject('ev_dashboard');
      expect(useFocusStore.getState().focusedTargetId).toBe('ev_dashboard');
      expect(useFocusStore.getState().savedCameraStack.length).toBe(2);

      // 3. Return once -> returns to vehicle
      useFocusStore.getState().returnToPreviousCamera();
      expect(useFocusStore.getState().focusedTargetId).toBeNull(); // Reset from dashboard
      expect(useFocusStore.getState().savedCameraStack.length).toBe(1);

      // 4. Return second time -> returns to original room view
      useFocusStore.getState().returnToPreviousCamera();
      expect(useFocusStore.getState().savedCameraStack.length).toBe(0);
      expect(useCameraStore.getState().desiredPosition).toEqual([0, 5, 10]);
    });
  });
});
