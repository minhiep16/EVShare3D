import { FocusCategory, FocusPreset, FocusTargetConfig } from './focusTypes';

export const DEFAULT_CATEGORY_PRESETS: Record<FocusCategory, FocusPreset> = {
  VEHICLE: {
    distance: 5.8,
    elevation: 1.5,
    azimuth: Math.PI / 4, // 45-degree angle for dramatic automotive framing
    fov: 42,
    speed: 4.0,
    minOrbitDistance: 3.0,
    maxOrbitDistance: 15.0,
  },
  TERMINAL: {
    distance: 1.8,
    elevation: 0.2,
    azimuth: 0, // Direct forward-facing view for crisp UI legibility
    fov: 34,
    speed: 5.0,
    minOrbitDistance: 1.2,
    maxOrbitDistance: 5.0,
  },
  PORTAL: {
    distance: 4.5,
    elevation: 1.2,
    azimuth: 0,
    fov: 45,
    speed: 4.2,
    minOrbitDistance: 2.0,
    maxOrbitDistance: 12.0,
  },
  OBJECT: {
    distance: 3.2,
    elevation: 0.8,
    azimuth: Math.PI / 6,
    fov: 40,
    speed: 4.5,
    minOrbitDistance: 1.5,
    maxOrbitDistance: 8.0,
  },
};

class FocusRegistryClass {
  private targets = new Map<string, FocusTargetConfig>();
  private customPresets = new Map<FocusCategory, FocusPreset>();

  public registerTarget(config: FocusTargetConfig): () => void {
    this.targets.set(config.id, config);
    return () => {
      this.targets.delete(config.id);
    };
  }

  public unregisterTarget(id: string): boolean {
    return this.targets.delete(id);
  }

  public getTarget(id: string): FocusTargetConfig | undefined {
    return this.targets.get(id);
  }

  public hasTarget(id: string): boolean {
    return this.targets.has(id);
  }

  public setCategoryPreset(category: FocusCategory, preset: FocusPreset): void {
    this.customPresets.set(category, preset);
  }

  public getPresetForCategory(category: FocusCategory): FocusPreset {
    return this.customPresets.get(category) || DEFAULT_CATEGORY_PRESETS[category];
  }

  public clear(): void {
    this.targets.clear();
    this.customPresets.clear();
  }
}

export const FocusRegistry = new FocusRegistryClass();
