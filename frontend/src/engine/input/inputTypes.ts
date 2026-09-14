/**
 * Normalized Input System Types
 * Unified input abstractions across Keyboard, Mouse, Pointer, and Touch
 */

export type InputAction =
  | 'MOVE_FORWARD'
  | 'MOVE_BACKWARD'
  | 'STRAFE_LEFT'
  | 'STRAFE_RIGHT'
  | 'SPRINT'
  | 'JUMP'
  | 'INTERACT'
  | 'CANCEL'
  | 'TOGGLE_HUD';

export interface NormalizedPointer {
  screenX: number;
  screenY: number;
  ndcX: number; // Normalized Device Coordinate [-1, 1]
  ndcY: number; // Normalized Device Coordinate [-1, 1]
  deltaX: number;
  deltaY: number;
  isDown: boolean;
  isDragging: boolean;
  button: number; // 0 = Left, 1 = Middle, 2 = Right
  pointerType: string; // 'mouse' | 'touch' | 'pen'
}

export interface NormalizedMovement {
  forward: number; // -1 (backward) to 1 (forward)
  strafe: number;  // -1 (left) to 1 (right)
  sprint: boolean;
  jump: boolean;
  interact: boolean;
}

export interface NormalizedTouch {
  touchCount: number;
  primaryTouch: [number, number] | null;
  pinchDistance: number | null;
  pinchDelta: number;
  gesture: 'NONE' | 'TAP' | 'PAN' | 'PINCH';
}

export type InputEventCallback<T = unknown> = (data: T) => void;

export interface SpatialHitTarget {
  objectId: string;
  point: [number, number, number];
  distance: number;
}
