import { describe, it, expect, beforeEach } from 'vitest';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useCameraStore } from '@/stores/useCameraStore';
import { CollisionEngine } from './collisionEngine';

describe('Portal & Teleport Waypoint System (08-AG)', () => {
  beforeEach(() => {
    usePlayerStore.getState().teleportTo([0, 0, 0], [0, 0, 0]);
    useCameraStore.getState().setMode('ORBIT');
    CollisionEngine.clear();
  });

  describe('1. Waypoint Dispatch & Avatar Teleportation', () => {
    it('teleports player avatar to portal destination coordinates', () => {
      const sourcePad = [0, 0, 4.5] as [number, number, number];
      const destinationPad = [0, 0, -4.5] as [number, number, number];
      const targetRotation = [0, Math.PI, 0] as [number, number, number];

      // Player starts at source
      usePlayerStore.getState().teleportTo(sourcePad);
      expect(usePlayerStore.getState().position).toEqual(sourcePad);

      // Trigger teleportation to destination pad
      usePlayerStore.getState().teleportTo(destinationPad, targetRotation);

      expect(usePlayerStore.getState().position).toEqual(destinationPad);
      expect(usePlayerStore.getState().rotation).toEqual(targetRotation);
      expect(usePlayerStore.getState().velocity).toEqual([0, 0, 0]);
    });

    it('smoothly relocates camera to frame player at portal target', () => {
      const targetPosition = [10, 0, 20] as [number, number, number];
      const cameraOffset = [targetPosition[0], targetPosition[1] + 3.5, targetPosition[2] + 7.0] as [
        number,
        number,
        number
      ];
      const lookTarget = [targetPosition[0], targetPosition[1] + 1.2, targetPosition[2]] as [
        number,
        number,
        number
      ];

      useCameraStore.getState().moveTo(cameraOffset, lookTarget, { speed: 5.0 });

      const cameraState = useCameraStore.getState();
      expect(cameraState.desiredPosition).toEqual(cameraOffset);
      expect(cameraState.desiredTarget).toEqual(lookTarget);
      expect(cameraState.isTransitioning).toBe(true);
    });
  });

  describe('2. Portal Destination Safety & Boundary Validation', () => {
    it('pushes player out of obstacle when destination collides with cylinder obstacle', () => {
      CollisionEngine.registerCylinderCollider({
        id: 'central_beacon',
        center: [0, 0, 0],
        radius: 1.2,
        height: 3.0,
      });

      // Destination pad placed too close to obstacle [0.5, 0, 0] (radius 1.2 + player radius 0.4 = 1.6 required)
      const resolved = CollisionEngine.resolvePosition([0.5, 0, 0], 0.4, null);
      const dist = Math.hypot(resolved[0], resolved[2]);
      expect(dist).toBeGreaterThanOrEqual(1.59);

      // Safe portal arrival pad placed far away [0, 0, 6]
      const safePos = CollisionEngine.resolvePosition([0, 0, 6], 0.4, null);
      expect(safePos).toEqual([0, 0, 6]);
    });
  });
});
