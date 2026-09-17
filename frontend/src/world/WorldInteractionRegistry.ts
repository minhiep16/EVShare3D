import type { Vector3Tuple } from 'three';
import type { SectorId, WorldInteractiveEntity, UserRole } from './worldTypes';
import { InteractableRegistry } from '../engine/raycast/interactableRegistry';
import { FocusRegistry } from '../engine/focus/FocusRegistry';

class WorldInteractionRegistryClass {
  private entities = new Map<string, WorldInteractiveEntity>();

  public registerEntity(entity: WorldInteractiveEntity): () => void {
    this.entities.set(entity.id, entity);

    // Register focus framing preset if object category matches
    FocusRegistry.registerTarget({
      id: entity.id,
      name: entity.name,
      category: entity.category === 'VEHICLE' ? 'VEHICLE' : entity.category === 'PORTAL' ? 'PORTAL' : 'TERMINAL',
      position: entity.position,
      preset: {
        distance: entity.focusFramingDistance ?? (entity.category === 'VEHICLE' ? 5.8 : 2.0),
        elevation: entity.category === 'VEHICLE' ? 1.5 : 0.2,
        azimuth: entity.category === 'VEHICLE' ? Math.PI / 4 : 0,
        fov: entity.category === 'VEHICLE' ? 42 : 35,
        speed: 4.5,
      },
    });

    return () => {
      this.unregisterEntity(entity.id);
    };
  }

  public unregisterEntity(id: string): void {
    this.entities.delete(id);
    InteractableRegistry.unregister(id);
    FocusRegistry.unregisterTarget(id);
  }

  public getEntity(id: string): WorldInteractiveEntity | undefined {
    return this.entities.get(id);
  }

  public getAllEntities(): WorldInteractiveEntity[] {
    return Array.from(this.entities.values());
  }

  public getEntitiesBySector(sectorId: SectorId): WorldInteractiveEntity[] {
    const list: WorldInteractiveEntity[] = [];
    for (const entity of this.entities.values()) {
      if (entity.sectorId === sectorId) {
        list.push(entity);
      }
    }
    return list;
  }

  public getEntitiesNearPosition(pos: Vector3Tuple, radius: number): WorldInteractiveEntity[] {
    const radiusSq = radius * radius;
    const list: WorldInteractiveEntity[] = [];

    for (const entity of this.entities.values()) {
      const dx = entity.position[0] - pos[0];
      const dy = entity.position[1] - pos[1];
      const dz = entity.position[2] - pos[2];
      const distSq = dx * dx + dy * dy + dz * dz;

      if (distSq <= radiusSq) {
        list.push(entity);
      }
    }

    return list;
  }

  /**
   * Evaluates if a given user role possesses authority to interact with the entity.
   */
  public canUserAccessEntity(entity: WorldInteractiveEntity, userRole: UserRole): boolean {
    if (!entity.requiredRole) return true;
    if (userRole === 'ROLE_ADMIN') return true;
    if (entity.requiredRole === 'ROLE_CO_OWNER') {
      return userRole === 'ROLE_CO_OWNER';
    }
    if (entity.requiredRole === 'ROLE_STAFF') {
      return userRole === 'ROLE_STAFF';
    }
    return false;
  }

  public clear(): void {
    for (const id of this.entities.keys()) {
      InteractableRegistry.unregister(id);
      FocusRegistry.unregisterTarget(id);
    }
    this.entities.clear();
  }
}

export const WorldInteractionRegistry = new WorldInteractionRegistryClass();
