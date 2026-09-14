/**
 * Shared Type Definitions for EVShare 3D Zustand Stores
 */

// World Sectors in the Metaverse Complex (docs/WORLD_ARCHITECTURE.md)
export type SectorId =
  | 'SECURITY_CHECKPOINT'
  | 'CENTRAL_GARAGE'
  | 'CO_OWNERSHIP_HALL'
  | 'BOOKING_CHAMBER'
  | 'FINANCE_CENTER'
  | 'SHARED_FUND_VAULT'
  | 'CONTRACT_ROOM'
  | 'DECISION_CHAMBER'
  | 'AI_INTELLIGENCE'
  | 'OPERATIONS_CENTER'
  | 'SERVICE_WORKSHOP'
  | 'DISPUTE_ROOM';

// Spatial Camera Modes
export type CameraMode =
  | 'ORBIT'
  | 'FIRST_PERSON'
  | 'THIRD_PERSON'
  | 'INSPECT'
  | 'CINEMATIC';

// Player Avatar Movement Modes
export type MovementMode =
  | 'IDLE'
  | 'WALKING'
  | 'SPRINTING'
  | 'DRIVING'
  | 'SEATED';

// 3D Pointer / Cursor Modes
export type CursorMode =
  | 'DEFAULT'
  | 'POINTER'
  | 'GRAB'
  | 'GRABBING'
  | 'TEXT'
  | 'CROSSHAIR'
  | 'DISABLED';

// 3D Spatial Notification Model
export interface SpatialNotification {
  id: string;
  title: string;
  message: string;
  type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';
  timestamp: number;
  durationMs?: number;
  worldPosition?: [number, number, number];
}

// System Error Model
export interface AppError {
  id: string;
  code?: string;
  message: string;
  details?: string;
  timestamp: number;
  fatal?: boolean;
  retryAction?: () => void;
}
