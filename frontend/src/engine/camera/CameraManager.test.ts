import { describe, it, expect, beforeEach } from 'vitest';
import { useCameraStore } from '@/stores/useCameraStore';

describe('CameraManager & useCameraStore Subsystem', () => {
  beforeEach(() => {
    // Reset camera store to clean default state before each test
    useCameraStore.setState({
      mode: 'ORBIT',
      previousMode: 'ORBIT',
      currentPosition: [0, 3.5, 7.5],
      desiredPosition: [0, 3.5, 7.5],
      currentTarget: [0, 1.5, 0],
      desiredTarget: [0, 1.5, 0],
      currentFov: 45,
      desiredFov: 45,
      isTransitioning: false,
      immediateSnap: false,
      inspectionTargetId: null,
      cinematicKeyframes: [],
      currentCinematicIndex: 0,
      cinematicElapsed: 0,
    });
  });

  it('initializes with default ORBIT mode and standard perspective coordinates', () => {
    const state = useCameraStore.getState();
    expect(state.mode).toBe('ORBIT');
    expect(state.currentPosition).toEqual([0, 3.5, 7.5]);
    expect(state.currentTarget).toEqual([0, 1.5, 0]);
    expect(state.currentFov).toBe(45);
    expect(state.isTransitioning).toBe(false);
    expect(state.immediateSnap).toBe(false);
  });

  it('switches between first-person, third-person, and orbit camera modes smoothly', () => {
    const { setMode } = useCameraStore.getState();

    setMode('FIRST_PERSON');
    expect(useCameraStore.getState().mode).toBe('FIRST_PERSON');
    expect(useCameraStore.getState().previousMode).toBe('ORBIT');
    expect(useCameraStore.getState().isTransitioning).toBe(true);

    setMode('THIRD_PERSON');
    expect(useCameraStore.getState().mode).toBe('THIRD_PERSON');
    expect(useCameraStore.getState().previousMode).toBe('FIRST_PERSON');

    setMode('ORBIT');
    expect(useCameraStore.getState().mode).toBe('ORBIT');
  });

  it('enforces smooth interpolation without hard camera teleport by default', () => {
    const { moveTo } = useCameraStore.getState();
    const newPos: [number, number, number] = [12, 6, 15];
    const newTgt: [number, number, number] = [0, 2, 0];

    // Default smooth movement
    moveTo(newPos, newTgt);

    const state = useCameraStore.getState();
    // Invariant: current position must NOT have snapped immediately
    expect(state.currentPosition).toEqual([0, 3.5, 7.5]);
    expect(state.desiredPosition).toEqual(newPos);
    expect(state.desiredTarget).toEqual(newTgt);
    expect(state.isTransitioning).toBe(true);
    expect(state.immediateSnap).toBe(false);
  });

  it('permits immediate hard snap ONLY when explicitly requested with immediate: true', () => {
    const { moveTo } = useCameraStore.getState();
    const snapPos: [number, number, number] = [20, 10, 25];
    const snapTgt: [number, number, number] = [5, 1, 5];

    moveTo(snapPos, snapTgt, { immediate: true, fov: 50 });

    const state = useCameraStore.getState();
    expect(state.currentPosition).toEqual(snapPos);
    expect(state.desiredPosition).toEqual(snapPos);
    expect(state.currentTarget).toEqual(snapTgt);
    expect(state.desiredTarget).toEqual(snapTgt);
    expect(state.currentFov).toBe(50);
    expect(state.immediateSnap).toBe(true);
    expect(state.isTransitioning).toBe(false);
  });

  it('focuses on a 3D target with smooth framing and offset calculation', () => {
    const { focusTarget } = useCameraStore.getState();
    const beaconCoords: [number, number, number] = [0, 1.8, 0];

    focusTarget('beacon_pedestal_01', beaconCoords, {
      distance: 4.0,
      elevation: 1.0,
      azimuthAngle: 0,
      fov: 38,
    });

    const state = useCameraStore.getState();
    expect(state.mode).toBe('INSPECT');
    expect(state.inspectionTargetId).toBe('beacon_pedestal_01');
    expect(state.desiredTarget).toEqual(beaconCoords);
    // Calculated eye pos: [0 + 4*sin(0), 1.8 + 1.0, 0 + 4*cos(0)] = [0, 2.8, 4]
    expect(state.desiredPosition[0]).toBeCloseTo(0);
    expect(state.desiredPosition[1]).toBeCloseTo(2.8);
    expect(state.desiredPosition[2]).toBeCloseTo(4.0);
    expect(state.desiredFov).toBe(38);
    expect(state.isTransitioning).toBe(true);
  });

  it('executes cinematic keyframe sequences and terminates back to ORBIT mode', () => {
    const { startCinematic, stopCinematic } = useCameraStore.getState();
    const keyframes = [
      {
        position: [10, 8, 10] as [number, number, number],
        target: [0, 1, 0] as [number, number, number],
        durationSeconds: 2.0,
      },
      {
        position: [-10, 8, 10] as [number, number, number],
        target: [0, 1, 0] as [number, number, number],
        durationSeconds: 2.0,
      },
    ];

    startCinematic(keyframes);

    let state = useCameraStore.getState();
    expect(state.mode).toBe('CINEMATIC');
    expect(state.cinematicKeyframes.length).toBe(2);
    expect(state.desiredPosition).toEqual([10, 8, 10]);
    expect(state.isTransitioning).toBe(true);

    stopCinematic();
    state = useCameraStore.getState();
    expect(state.mode).toBe('ORBIT');
    expect(state.cinematicKeyframes.length).toBe(0);
  });

  it('smoothly resets camera to default vantage point', () => {
    const { moveTo, resetCamera } = useCameraStore.getState();

    // First move camera far away
    moveTo([50, 25, 50], [10, 0, 10], { immediate: true });
    expect(useCameraStore.getState().currentPosition).toEqual([50, 25, 50]);

    // Reset camera smoothly
    resetCamera();

    const state = useCameraStore.getState();
    expect(state.mode).toBe('ORBIT');
    expect(state.desiredPosition).toEqual([0, 3.5, 7.5]);
    expect(state.desiredTarget).toEqual([0, 1.5, 0]);
    expect(state.isTransitioning).toBe(true);
    expect(state.immediateSnap).toBe(false);
  });
});
