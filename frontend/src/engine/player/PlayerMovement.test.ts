import { describe, it, expect, beforeEach } from 'vitest';
import { CollisionEngine } from './collisionEngine';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { RoomBounds } from './movementTypes';

describe('Player Movement & Collision Foundation', () => {
  beforeEach(() => {
    CollisionEngine.clear();
    usePlayerStore.setState({
      position: [0, 0, 0],
      rotation: [0, 0, 0],
      velocity: [0, 0, 0],
      isGrounded: true,
      movementMode: 'IDLE',
      walkSpeed: 4.5,
      runSpeed: 8.5,
      isSprinting: false,
      occupiedVehicleId: null,
      seatIndex: null,
    });
  });

  describe('Boundary Clamping', () => {
    const roomBounds: RoomBounds = {
      minX: -10,
      maxX: 10,
      minZ: -10,
      maxZ: 10,
    };
    const playerRadius = 0.5;

    it('keeps player position within room boundary bounds with radius margin', () => {
      // Attempt to walk past maxX (+15)
      const outsidePos: [number, number, number] = [15, 0, 0];
      const resolved = CollisionEngine.resolvePosition(outsidePos, playerRadius, roomBounds);

      // Clamped to maxX - radius = 10 - 0.5 = 9.5
      expect(resolved[0]).toBeCloseTo(9.5);
      expect(resolved[2]).toBeCloseTo(0);
    });

    it('clamps negative boundaries properly', () => {
      // Attempt to walk past minZ (-12)
      const outsidePos: [number, number, number] = [0, 0, -12];
      const resolved = CollisionEngine.resolvePosition(outsidePos, playerRadius, roomBounds);

      // Clamped to minZ + radius = -10 + 0.5 = -9.5
      expect(resolved[2]).toBeCloseTo(-9.5);
      expect(resolved[0]).toBeCloseTo(0);
    });

    it('preserves valid positions completely untouched', () => {
      const validPos: [number, number, number] = [3, 0, -4];
      const resolved = CollisionEngine.resolvePosition(validPos, playerRadius, roomBounds);
      expect(resolved).toEqual([3, 0, -4]);
    });
  });

  describe('Cylinder Obstacle Resolution & Radial Sliding', () => {
    const playerRadius = 0.5;

    beforeEach(() => {
      // Register a cylindrical pedestal at [0, 0, 0] with radius 2.0
      CollisionEngine.registerCylinderCollider({
        id: 'central_pedestal',
        center: [0, 0, 0],
        radius: 2.0,
        height: 3.0,
      });
    });

    it('pushes player outward when colliding with cylinder perimeter', () => {
      // Player attempting to walk inside cylinder at [1.5, 0, 0]
      const insidePos: [number, number, number] = [1.5, 0, 0];
      const resolved = CollisionEngine.resolvePosition(insidePos, playerRadius, null);

      // Distance from center must be pushed to cylinder.radius + playerRadius = 2.0 + 0.5 = 2.5
      expect(resolved[0]).toBeCloseTo(2.5);
      expect(resolved[2]).toBeCloseTo(0);
    });

    it('allows tangential sliding around cylinder curvature', () => {
      // Player near edge at [1.8, 0, 1.8] (dist = ~2.54, minDist = 2.5)
      const slidingPos: [number, number, number] = [1.5, 0, 1.5]; // dist = ~2.12
      const resolved = CollisionEngine.resolvePosition(slidingPos, playerRadius, null);

      const resolvedDist = Math.sqrt(resolved[0] * resolved[0] + resolved[2] * resolved[2]);
      expect(resolvedDist).toBeCloseTo(2.5);
    });
  });

  describe('Box Obstacle Resolution & Wall Sliding', () => {
    const playerRadius = 0.5;

    beforeEach(() => {
      // Register an axis-aligned barrier box from X: [2, 6], Z: [2, 6]
      CollisionEngine.registerBoxCollider({
        id: 'kiosk_barrier',
        min: [2, 0, 2],
        max: [6, 2, 6],
      });
    });

    it('slides player along closest box face when penetrating obstacle', () => {
      // Player penetrates from left face at [2.2, 0, 4]
      const penetratingPos: [number, number, number] = [2.2, 0, 4];
      const resolved = CollisionEngine.resolvePosition(penetratingPos, playerRadius, null);

      // Must be pushed to box.minX - playerRadius = 2.0 - 0.5 = 1.5
      expect(resolved[0]).toBeCloseTo(1.5);
      expect(resolved[2]).toBeCloseTo(4.0); // Z-axis sliding preserved
    });
  });

  describe('Teleportation Foundation', () => {
    it('teleports player avatar to exact target coordinates and resets velocity', () => {
      const { teleportTo } = usePlayerStore.getState();

      usePlayerStore.setState({ velocity: [5, 0, -3] });

      const destPos: [number, number, number] = [12.5, 0, -8.0];
      const destRot: [number, number, number] = [0, Math.PI / 2, 0];

      teleportTo(destPos, destRot);

      const state = usePlayerStore.getState();
      expect(state.position).toEqual(destPos);
      expect(state.rotation).toEqual(destRot);
      expect(state.velocity).toEqual([0, 0, 0]);
    });
  });
});
