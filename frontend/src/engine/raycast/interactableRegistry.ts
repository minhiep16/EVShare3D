import * as THREE from 'three';
import { InteractableConfig } from './raycastTypes';

class InteractableRegistryClass {
  private interactables = new Map<string, InteractableConfig>();
  private objectToIdMap = new WeakMap<THREE.Object3D, string>();

  public register(config: InteractableConfig): () => void {
    const priority = config.priority ?? 0;
    const cursor = config.cursor ?? 'POINTER';
    const enabled = config.enabled ?? true;

    const entry: InteractableConfig = {
      ...config,
      priority,
      cursor,
      enabled,
    };

    this.interactables.set(config.id, entry);
    this.mapObjectHierarchy(config.object, config.id);

    return () => {
      this.unregister(config.id);
    };
  }

  public unregister(id: string): void {
    this.interactables.delete(id);
  }

  public get(id: string): InteractableConfig | undefined {
    return this.interactables.get(id);
  }

  public getByObject(object: THREE.Object3D): InteractableConfig | undefined {
    let curr: THREE.Object3D | null = object;
    while (curr) {
      const id = this.objectToIdMap.get(curr);
      if (id && this.interactables.has(id)) {
        return this.interactables.get(id);
      }
      curr = curr.parent;
    }
    return undefined;
  }

  public getInteractableObjects(): THREE.Object3D[] {
    const targets: THREE.Object3D[] = [];
    for (const item of this.interactables.values()) {
      if (item.enabled) {
        targets.push(item.object);
      }
    }
    return targets;
  }

  public clear(): void {
    this.interactables.clear();
  }

  private mapObjectHierarchy(root: THREE.Object3D, id: string): void {
    this.objectToIdMap.set(root, id);
    root.traverse((child) => {
      this.objectToIdMap.set(child, id);
    });
  }
}

export const InteractableRegistry = new InteractableRegistryClass();
