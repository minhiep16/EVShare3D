import { describe, it, expect, beforeEach } from 'vitest';
import * as THREE from 'three';
import { PERFORMANCE_PROFILES } from './performanceProfiles';
import { usePerformanceStore } from './usePerformanceStore';
import { AssetCache } from './AssetCache';

describe('Performance Foundation Subsystem (08-AD)', () => {
  beforeEach(() => {
    usePerformanceStore.getState().setTier('HIGH');
    usePerformanceStore.getState().setAutoDegradeEnabled(true);
    AssetCache.disposeAll();
  });

  describe('1. Performance Profiles (HIGH, MEDIUM, LOW)', () => {
    it('configures HIGH tier for maximum fidelity', () => {
      const high = PERFORMANCE_PROFILES.HIGH;
      expect(high.tier).toBe('HIGH');
      expect(high.shadowsEnabled).toBe(true);
      expect(high.shadowMapSize).toBe(2048);
      expect(high.antialias).toBe(true);
      expect(high.maxAnisotropy).toBe(16);
      expect(high.dprRange).toEqual([1.0, 2.0]);
      expect(high.lodBias).toBe(1.0);
    });

    it('configures MEDIUM tier for balanced performance', () => {
      const med = PERFORMANCE_PROFILES.MEDIUM;
      expect(med.tier).toBe('MEDIUM');
      expect(med.shadowsEnabled).toBe(true);
      expect(med.shadowMapSize).toBe(1024);
      expect(med.antialias).toBe(true);
      expect(med.maxAnisotropy).toBe(4);
      expect(med.dprRange).toEqual([0.85, 1.5]);
      expect(med.lodBias).toBe(1.2);
    });

    it('configures LOW tier for maximum throughput and mobile capability', () => {
      const low = PERFORMANCE_PROFILES.LOW;
      expect(low.tier).toBe('LOW');
      expect(low.shadowsEnabled).toBe(false);
      expect(low.shadowMapSize).toBe(0);
      expect(low.antialias).toBe(false);
      expect(low.maxAnisotropy).toBe(1);
      expect(low.dprRange).toEqual([0.65, 1.0]);
      expect(low.lodBias).toBe(1.5);
    });
  });

  describe('2. Adaptive DPR & Performance Store', () => {
    it('switches performance tiers dynamically', () => {
      const { setTier } = usePerformanceStore.getState();

      setTier('MEDIUM');
      expect(usePerformanceStore.getState().currentTier).toBe('MEDIUM');
      expect(usePerformanceStore.getState().activeProfile.shadowMapSize).toBe(1024);

      setTier('LOW');
      expect(usePerformanceStore.getState().currentTier).toBe('LOW');
      expect(usePerformanceStore.getState().activeProfile.shadowsEnabled).toBe(false);
    });

    it('clamps adaptive DPR within the active tier range', () => {
      const { setTier, setAdaptiveDpr } = usePerformanceStore.getState();

      setTier('MEDIUM'); // dprRange: [0.85, 1.5]

      setAdaptiveDpr(0.5); // Below min
      expect(usePerformanceStore.getState().adaptiveDpr).toBe(0.85);

      setAdaptiveDpr(2.5); // Above max
      expect(usePerformanceStore.getState().adaptiveDpr).toBe(1.5);

      setAdaptiveDpr(1.2); // Within range
      expect(usePerformanceStore.getState().adaptiveDpr).toBe(1.2);
    });

    it('auto-degrades tier when low FPS is sustained for >= 3 seconds', () => {
      const { recordFpsSample } = usePerformanceStore.getState();

      expect(usePerformanceStore.getState().currentTier).toBe('HIGH');

      // Sustained 20 FPS for 1.5s
      recordFpsSample(20, 1.5);
      expect(usePerformanceStore.getState().currentTier).toBe('HIGH');

      // Sustained 20 FPS for another 1.6s (total > 3.0s)
      recordFpsSample(20, 1.6);
      expect(usePerformanceStore.getState().currentTier).toBe('MEDIUM');
    });

    it('ignores auto-degrade when autoDegradeEnabled=false', () => {
      const { setAutoDegradeEnabled, recordFpsSample } = usePerformanceStore.getState();

      setAutoDegradeEnabled(false);
      recordFpsSample(15, 4.0);

      expect(usePerformanceStore.getState().currentTier).toBe('HIGH');
    });
  });

  describe('3. Asset Cache & Memory Management', () => {
    it('caches and reuses BufferGeometry instances by key', () => {
      const geom1 = AssetCache.getOrRegisterGeometry('test_box', () => new THREE.BoxGeometry(1, 1, 1));
      const geom2 = AssetCache.getOrRegisterGeometry('test_box', () => new THREE.BoxGeometry(2, 2, 2));

      expect(geom1).toBe(geom2);
      expect(AssetCache.getCacheStats().geometriesCount).toBe(1);
    });

    it('caches and reuses Material instances by key', () => {
      const mat1 = AssetCache.getOrRegisterMaterial('cyber_mat', () => new THREE.MeshStandardMaterial({ color: 0x00e5ff }));
      const mat2 = AssetCache.getOrRegisterMaterial('cyber_mat', () => new THREE.MeshStandardMaterial({ color: 0xff0000 }));

      expect(mat1).toBe(mat2);
      expect(AssetCache.getCacheStats().materialsCount).toBe(1);
    });

    it('applies texture quality limits based on performance profile', () => {
      const texture = new THREE.Texture();
      AssetCache.applyTextureProfile(texture, PERFORMANCE_PROFILES.LOW);

      expect(texture.anisotropy).toBe(1);
      expect(texture.minFilter).toBe(THREE.LinearFilter);

      AssetCache.applyTextureProfile(texture, PERFORMANCE_PROFILES.HIGH);
      expect(texture.anisotropy).toBe(16);
      expect(texture.minFilter).toBe(THREE.LinearMipmapLinearFilter);
    });

    it('clears and disposes all assets cleanly', () => {
      AssetCache.getOrRegisterGeometry('g1', () => new THREE.BoxGeometry());
      AssetCache.getOrRegisterMaterial('m1', () => new THREE.MeshBasicMaterial());

      expect(AssetCache.getCacheStats().geometriesCount).toBe(1);
      expect(AssetCache.getCacheStats().materialsCount).toBe(1);

      AssetCache.disposeAll();
      expect(AssetCache.getCacheStats().geometriesCount).toBe(0);
      expect(AssetCache.getCacheStats().materialsCount).toBe(0);
    });
  });
});
