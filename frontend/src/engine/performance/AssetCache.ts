import * as THREE from 'three';
import { PerformanceProfile } from './performanceTypes';

/**
 * Central Asset Cache & Memory Manager
 * Deduplicates in-flight fetch requests, caches reusable Three.js objects,
 * and handles memory cleanup to prevent GPU leaks.
 */
class AssetCacheManager {
  private textures = new Map<string, THREE.Texture>();
  private geometries = new Map<string, THREE.BufferGeometry>();
  private materials = new Map<string, THREE.Material>();
  private pendingPromises = new Map<string, Promise<any>>();
  private textureLoader = new THREE.TextureLoader();

  /**
   * Lazily loads a texture with promise deduplication.
   */
  async loadTextureLazy(url: string, profile?: PerformanceProfile): Promise<THREE.Texture> {
    if (this.textures.has(url)) {
      return this.textures.get(url)!;
    }

    if (this.pendingPromises.has(url)) {
      return this.pendingPromises.get(url)!;
    }

    const promise = new Promise<THREE.Texture>((resolve, reject) => {
      this.textureLoader.load(
        url,
        (texture) => {
          if (profile) {
            this.applyTextureProfile(texture, profile);
          }
          this.textures.set(url, texture);
          this.pendingPromises.delete(url);
          resolve(texture);
        },
        undefined,
        (err) => {
          this.pendingPromises.delete(url);
          reject(err);
        }
      );
    });

    this.pendingPromises.set(url, promise);
    return promise;
  }

  /**
   * Applies performance tier texture limits (anisotropy and filtering).
   */
  applyTextureProfile(texture: THREE.Texture, profile: PerformanceProfile): void {
    texture.anisotropy = profile.maxAnisotropy;
    texture.minFilter = profile.tier === 'LOW' ? THREE.LinearFilter : THREE.LinearMipmapLinearFilter;
    texture.magFilter = THREE.LinearFilter;
    texture.needsUpdate = true;
  }

  /**
   * Retrieves or instantiates a cached BufferGeometry by key.
   */
  getOrRegisterGeometry<T extends THREE.BufferGeometry>(key: string, factory: () => T): T {
    if (!this.geometries.has(key)) {
      this.geometries.set(key, factory());
    }
    return this.geometries.get(key) as T;
  }

  /**
   * Retrieves or instantiates a cached Material by key.
   */
  getOrRegisterMaterial<T extends THREE.Material>(key: string, factory: () => T): T {
    if (!this.materials.has(key)) {
      this.materials.set(key, factory());
    }
    return this.materials.get(key) as T;
  }

  /**
   * Returns current cache statistics.
   */
  getCacheStats() {
    return {
      texturesCount: this.textures.size,
      geometriesCount: this.geometries.size,
      materialsCount: this.materials.size,
    };
  }

  /**
   * Frees all cached assets and releases GPU memory.
   */
  disposeAll(): void {
    this.textures.forEach((tex) => tex.dispose());
    this.textures.clear();

    this.geometries.forEach((geom) => geom.dispose());
    this.geometries.clear();

    this.materials.forEach((mat) => mat.dispose());
    this.materials.clear();

    this.pendingPromises.clear();
  }
}

export const AssetCache = new AssetCacheManager();
