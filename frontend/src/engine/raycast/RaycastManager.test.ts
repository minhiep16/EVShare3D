import { describe, it, expect, beforeEach } from 'vitest';
import * as THREE from 'three';
import { InteractableRegistry } from './interactableRegistry';
import {
  computeRaycastHit,
  createRaycastTracker,
  shouldPerformRaycast,
} from './raycastEngine';

describe('RaycastManager & Spatial Interaction Subsystem', () => {
  beforeEach(() => {
    InteractableRegistry.clear();
  });

  describe('InteractableRegistry', () => {
    it('registers and retrieves interactables with hierarchy traversal', () => {
      const parent = new THREE.Group();
      const childMesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1));
      parent.add(childMesh);

      let clicked = false;
      const unregister = InteractableRegistry.register({
        id: 'terminal_kiosk',
        object: parent,
        priority: 50,
        cursor: 'POINTER',
        callbacks: {
          onClick: () => {
            clicked = true;
          },
        },
      });

      // Checking child mesh should resolve to parent interactable
      const found = InteractableRegistry.getByObject(childMesh);
      expect(found).toBeDefined();
      expect(found?.id).toBe('terminal_kiosk');
      expect(found?.priority).toBe(50);
      expect(found?.cursor).toBe('POINTER');

      found?.callbacks?.onClick?.({
        id: 'terminal_kiosk',
        point: [0, 0, 0],
        normal: null,
        distance: 2,
        object: childMesh,
        priority: 50,
      });
      expect(clicked).toBe(true);

      unregister();
      expect(InteractableRegistry.getByObject(childMesh)).toBeUndefined();
    });
  });

  describe('Interaction Priority Resolution', () => {
    it('prioritizes high-priority objects over low-priority objects regardless of distance', () => {
      // Create a background floor (low priority: 0) closer/further
      const floorMesh = new THREE.Mesh(new THREE.PlaneGeometry(10, 10));
      floorMesh.position.set(0, 0, 0);

      // Create a floating UI panel / terminal (high priority: 100)
      const uiMesh = new THREE.Mesh(new THREE.BoxGeometry(2, 2, 0.1));
      uiMesh.position.set(0, 0, 1);

      InteractableRegistry.register({
        id: 'floor_plane',
        object: floorMesh,
        priority: 0,
      });

      InteractableRegistry.register({
        id: 'floating_modal_btn',
        object: uiMesh,
        priority: 100,
      });

      const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
      camera.position.set(0, 0, 5);
      camera.lookAt(0, 0, 0);
      camera.updateMatrixWorld(true);

      const raycaster = new THREE.Raycaster();
      // Center raycast [0, 0] hits both UI button and floor
      const hit = computeRaycastHit(0, 0, camera, raycaster);

      expect(hit).toBeDefined();
      expect(hit?.id).toBe('floating_modal_btn');
      expect(hit?.priority).toBe(100);
    });

    it('resolves by closest distance when two objects have equal priority', () => {
      const nearMesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1));
      nearMesh.position.set(0, 0, 3);

      const farMesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1));
      farMesh.position.set(0, 0, 0);

      InteractableRegistry.register({
        id: 'near_object',
        object: nearMesh,
        priority: 10,
      });

      InteractableRegistry.register({
        id: 'far_object',
        object: farMesh,
        priority: 10,
      });

      const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
      camera.position.set(0, 0, 5);
      camera.lookAt(0, 0, 0);
      camera.updateMatrixWorld(true);

      const raycaster = new THREE.Raycaster();
      const hit = computeRaycastHit(0, 0, camera, raycaster);

      expect(hit).toBeDefined();
      expect(hit?.id).toBe('near_object');
    });
  });

  describe('Avoid Expensive Unnecessary Raycasts (Dirty Checking)', () => {
    it('skips raycasting when pointer and camera are stationary', () => {
      const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
      camera.position.set(0, 0, 5);
      camera.updateMatrixWorld(true);

      const tracker = createRaycastTracker();
      tracker.lastNdcX = 0.25;
      tracker.lastNdcY = -0.15;
      tracker.lastCamMatrix.copy(camera.matrixWorld);

      // Same pointer coordinates and same camera matrix
      const shouldRaycast = shouldPerformRaycast(0.25, -0.15, camera, tracker);
      expect(shouldRaycast).toBe(false);
    });

    it('triggers raycast when pointer moves beyond threshold', () => {
      const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
      camera.position.set(0, 0, 5);
      camera.updateMatrixWorld(true);

      const tracker = createRaycastTracker();
      tracker.lastNdcX = 0.25;
      tracker.lastNdcY = -0.15;
      tracker.lastCamMatrix.copy(camera.matrixWorld);

      // Pointer moved
      const shouldRaycast = shouldPerformRaycast(0.28, -0.15, camera, tracker);
      expect(shouldRaycast).toBe(true);
    });

    it('triggers raycast when camera orientation or position shifts', () => {
      const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
      camera.position.set(0, 0, 5);
      camera.updateMatrixWorld(true);

      const tracker = createRaycastTracker();
      tracker.lastNdcX = 0.25;
      tracker.lastNdcY = -0.15;
      tracker.lastCamMatrix.copy(camera.matrixWorld);

      // Camera rotates/moves
      camera.position.set(1, 0, 5);
      camera.updateMatrixWorld(true);

      const shouldRaycast = shouldPerformRaycast(0.25, -0.15, camera, tracker);
      expect(shouldRaycast).toBe(true);
    });
  });
});
