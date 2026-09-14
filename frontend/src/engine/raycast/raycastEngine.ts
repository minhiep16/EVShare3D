import * as THREE from 'three';
import { RaycastHit } from './raycastTypes';
import { InteractableRegistry } from './interactableRegistry';

export interface RaycastStateTracker {
  lastNdcX: number;
  lastNdcY: number;
  lastCamMatrix: THREE.Matrix4;
  cachedHit: RaycastHit | null;
  raycastCount: number;
  skippedCount: number;
}

export const createRaycastTracker = (): RaycastStateTracker => ({
  lastNdcX: -999,
  lastNdcY: -999,
  lastCamMatrix: new THREE.Matrix4(),
  cachedHit: null,
  raycastCount: 0,
  skippedCount: 0,
});

/**
 * Evaluates whether pointer or camera has moved enough to warrant a raycast calculation.
 * Prevents expensive unnecessary raycasts every frame when pointer & camera are stationary.
 */
export const shouldPerformRaycast = (
  ndcX: number,
  ndcY: number,
  camera: THREE.Camera,
  tracker: RaycastStateTracker,
  pointerMovedThreshold = 0.001
): boolean => {
  const pointerDeltaX = Math.abs(ndcX - tracker.lastNdcX);
  const pointerDeltaY = Math.abs(ndcY - tracker.lastNdcY);
  const pointerMoved = pointerDeltaX > pointerMovedThreshold || pointerDeltaY > pointerMovedThreshold;

  // Check camera matrix changes
  const cameraMoved = !camera.matrixWorld.equals(tracker.lastCamMatrix);

  return pointerMoved || cameraMoved;
};

/**
 * Performs raycast against registered interactable objects and resolves with interaction priority.
 */
export const computeRaycastHit = (
  ndcX: number,
  ndcY: number,
  camera: THREE.Camera,
  raycaster: THREE.Raycaster
): RaycastHit | null => {
  const interactables = InteractableRegistry.getInteractableObjects();
  if (interactables.length === 0) return null;

  raycaster.setFromCamera(new THREE.Vector2(ndcX, ndcY), camera);
  const rawIntersections = raycaster.intersectObjects(interactables, true);

  if (rawIntersections.length === 0) return null;

  // Map intersections to registered interactable configurations
  const candidates: RaycastHit[] = [];

  for (const hit of rawIntersections) {
    const config = InteractableRegistry.getByObject(hit.object);
    if (config && config.enabled) {
      candidates.push({
        id: config.id,
        point: [hit.point.x, hit.point.y, hit.point.z],
        normal: hit.face ? [hit.face.normal.x, hit.face.normal.y, hit.face.normal.z] : null,
        distance: hit.distance,
        object: hit.object,
        priority: config.priority ?? 0,
        uv: hit.uv ? [hit.uv.x, hit.uv.y] : undefined,
      });
    }
  }

  if (candidates.length === 0) return null;

  // Sort candidates by Interaction Priority (Descending), then Distance (Ascending)
  candidates.sort((a, b) => {
    if (b.priority !== a.priority) {
      return b.priority - a.priority; // Higher priority wins
    }
    return a.distance - b.distance;   // Closer distance wins if equal priority
  });

  return candidates[0];
};
