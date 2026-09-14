import { BoxCollider, CylinderCollider, RoomBounds } from './movementTypes';

class CollisionEngineClass {
  private boxColliders = new Map<string, BoxCollider>();
  private cylinderColliders = new Map<string, CylinderCollider>();

  public registerBoxCollider(collider: BoxCollider): () => void {
    this.boxColliders.set(collider.id, collider);
    return () => {
      this.boxColliders.delete(collider.id);
    };
  }

  public registerCylinderCollider(collider: CylinderCollider): () => void {
    this.cylinderColliders.set(collider.id, collider);
    return () => {
      this.cylinderColliders.delete(collider.id);
    };
  }

  public unregisterCollider(id: string): void {
    this.boxColliders.delete(id);
    this.cylinderColliders.delete(id);
  }

  public getBoxColliders(): BoxCollider[] {
    return Array.from(this.boxColliders.values());
  }

  public getCylinderColliders(): CylinderCollider[] {
    return Array.from(this.cylinderColliders.values());
  }

  public clear(): void {
    this.boxColliders.clear();
    this.cylinderColliders.clear();
  }

  /**
   * Resolves player collisions against room boundaries and registered static colliders.
   * Performs tangential sliding along obstacle surfaces.
   */
  public resolvePosition(
    position: [number, number, number],
    radius: number,
    bounds: RoomBounds | null
  ): [number, number, number] {
    let [x, y, z] = position;

    // 1. Boundary Clamping (Walls & Perimeters)
    if (bounds) {
      const minX = bounds.minX + radius;
      const maxX = bounds.maxX - radius;
      const minZ = bounds.minZ + radius;
      const maxZ = bounds.maxZ - radius;

      x = Math.max(minX, Math.min(maxX, x));
      z = Math.max(minZ, Math.min(maxZ, z));
    }

    // 2. Cylinder Obstacle Resolution (Pillars, Pedestals, Charging Pods)
    for (const cyl of this.cylinderColliders.values()) {
      const dx = x - cyl.center[0];
      const dz = z - cyl.center[2];
      const distSq = dx * dx + dz * dz;
      const minDist = cyl.radius + radius;

      if (distSq < minDist * minDist && distSq > 0.000001) {
        const dist = Math.sqrt(distSq);
        const overlap = minDist - dist;
        // Push outward along normal
        const nx = dx / dist;
        const nz = dz / dist;
        x += nx * overlap;
        z += nz * overlap;
      }
    }

    // 3. Axis-Aligned Box Obstacle Resolution (Kiosks, Gateway Posts, Barriers)
    for (const box of this.boxColliders.values()) {
      // Expanded box by player radius
      const bMinX = box.min[0] - radius;
      const bMaxX = box.max[0] + radius;
      const bMinZ = box.min[2] - radius;
      const bMaxZ = box.max[2] + radius;

      // Check overlap in XZ
      if (x > bMinX && x < bMaxX && z > bMinZ && z < bMaxZ) {
        // Find shortest push-out distance
        const leftDist = x - bMinX;
        const rightDist = bMaxX - x;
        const topDist = z - bMinZ;
        const bottomDist = bMaxZ - z;

        const minPush = Math.min(leftDist, rightDist, topDist, bottomDist);

        if (minPush === leftDist) x = bMinX;
        else if (minPush === rightDist) x = bMaxX;
        else if (minPush === topDist) z = bMinZ;
        else z = bMaxZ;
      }
    }

    return [x, y, z];
  }
}

export const CollisionEngine = new CollisionEngineClass();
