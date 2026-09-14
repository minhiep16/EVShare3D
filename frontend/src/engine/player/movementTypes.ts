/**
 * Player Movement & Spatial Collision Types
 */

export interface BoundingBox3D {
  min: [number, number, number];
  max: [number, number, number];
}

export interface CylinderCollider {
  id: string;
  center: [number, number, number];
  radius: number;
  height: number;
}

export interface BoxCollider {
  id: string;
  min: [number, number, number];
  max: [number, number, number];
}

export interface RoomBounds {
  minX: number;
  maxX: number;
  minZ: number;
  maxZ: number;
}

export interface TeleportTarget {
  id: string;
  name: string;
  position: [number, number, number];
  rotation?: [number, number, number];
}

export interface PlayerPhysicsConfig {
  walkSpeed: number;
  runSpeed: number;
  acceleration: number;
  damping: number;
  turnSpeed: number;
  avatarRadius: number;
  avatarHeight: number;
}
