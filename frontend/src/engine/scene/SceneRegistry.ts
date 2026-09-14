import { SceneDefinition } from './sceneTypes';

class SceneRegistryClass {
  private scenes: Map<string, SceneDefinition> = new Map();

  public registerScene(definition: SceneDefinition): void {
    if (this.scenes.has(definition.id)) {
      console.warn(`[SceneRegistry] Scene with ID "${definition.id}" is being overwritten.`);
    }
    this.scenes.set(definition.id, definition);
  }

  public unregisterScene(id: string): boolean {
    return this.scenes.delete(id);
  }

  public getScene(id: string): SceneDefinition | undefined {
    return this.scenes.get(id);
  }

  public getAllScenes(): SceneDefinition[] {
    return Array.from(this.scenes.values());
  }

  public hasScene(id: string): boolean {
    return this.scenes.has(id);
  }

  public clear(): void {
    this.scenes.clear();
  }
}

export const SceneRegistry = new SceneRegistryClass();
