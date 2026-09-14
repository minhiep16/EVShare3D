import { InputAction, NormalizedMovement } from './inputTypes';

/**
 * Normalizes client screen coordinates to Three.js Normalized Device Coordinates (NDC).
 * X: [-1 (left) to 1 (right)]
 * Y: [1 (top) to -1 (bottom)]
 */
export const normalizePointerCoords = (
  clientX: number,
  clientY: number,
  rect: { left: number; top: number; width: number; height: number }
): { ndcX: number; ndcY: number } => {
  if (rect.width <= 0 || rect.height <= 0) {
    return { ndcX: 0, ndcY: 0 };
  }

  const ndcX = ((clientX - rect.left) / rect.width) * 2 - 1;
  const ndcY = -(((clientY - rect.top) / rect.height) * 2 - 1);

  // Clamp to [-1, 1] bounds
  return {
    ndcX: Math.max(-1, Math.min(1, ndcX)),
    ndcY: Math.max(-1, Math.min(1, ndcY)),
  };
};

/**
 * Calculates euclidean distance between two touch points for pinch gestures.
 */
export const calculatePinchDistance = (touchA: Touch, touchB: Touch): number => {
  const dx = touchA.clientX - touchB.clientX;
  const dy = touchA.clientY - touchB.clientY;
  return Math.sqrt(dx * dx + dy * dy);
};

/**
 * Maps raw keyboard codes to canonical InputActions
 */
export const mapKeyCodeToAction = (code: string): InputAction | null => {
  switch (code) {
    case 'KeyW':
    case 'ArrowUp':
      return 'MOVE_FORWARD';
    case 'KeyS':
    case 'ArrowDown':
      return 'MOVE_BACKWARD';
    case 'KeyA':
    case 'ArrowLeft':
      return 'STRAFE_LEFT';
    case 'KeyD':
    case 'ArrowRight':
      return 'STRAFE_RIGHT';
    case 'ShiftLeft':
    case 'ShiftRight':
      return 'SPRINT';
    case 'Space':
      return 'JUMP';
    case 'KeyE':
    case 'Enter':
      return 'INTERACT';
    case 'Escape':
      return 'CANCEL';
    case 'KeyH':
      return 'TOGGLE_HUD';
    default:
      return null;
  }
};

/**
 * Computes forward and strafe axes from an active set of keyboard codes
 */
export const computeMovementVector = (activeKeys: Set<string>): NormalizedMovement => {
  let forward = 0;
  let strafe = 0;

  if (activeKeys.has('KeyW') || activeKeys.has('ArrowUp')) forward += 1;
  if (activeKeys.has('KeyS') || activeKeys.has('ArrowDown')) forward -= 1;
  if (activeKeys.has('KeyA') || activeKeys.has('ArrowLeft')) strafe -= 1;
  if (activeKeys.has('KeyD') || activeKeys.has('ArrowRight')) strafe += 1;

  const sprint = activeKeys.has('ShiftLeft') || activeKeys.has('ShiftRight');
  const jump = activeKeys.has('Space');
  const interact = activeKeys.has('KeyE') || activeKeys.has('Enter');

  return { forward, strafe, sprint, jump, interact };
};
