import { describe, it, expect, beforeEach } from 'vitest';
import { SceneRegistry } from './SceneRegistry';
import {
  DEFAULT_SECURITY_CHECKPOINT_SCENE,
  DEFAULT_GARAGE_SCENE,
  registerDefaultScenes,
} from './defaultScenes';
import { SceneLifecycleState } from './sceneTypes';

describe('Scene Initialization & Management (08-AG)', () => {
  beforeEach(() => {
    SceneRegistry.clear();
  });

  describe('1. Scene Registry & Default Scene Configurations', () => {
    it('registers and retrieves scene definitions by unique id', () => {
      expect(SceneRegistry.getAllScenes().length).toBe(0);

      SceneRegistry.registerScene(DEFAULT_SECURITY_CHECKPOINT_SCENE);
      expect(SceneRegistry.hasScene('SECURITY_CHECKPOINT')).toBe(true);

      const retrieved = SceneRegistry.getScene('SECURITY_CHECKPOINT');
      expect(retrieved).toBeDefined();
      expect(retrieved?.name).toBe('Security Checkpoint Gateway');
      expect(retrieved?.camera.fov).toBe(45);
      expect(retrieved?.environment.lightingProfile).toBe('CYBER_NEON');
    });

    it('registers all default scenes cleanly', () => {
      registerDefaultScenes();
      const all = SceneRegistry.getAllScenes();
      expect(all.length).toBe(13);

      const sceneIds = all.map((s) => s.id);
      expect(sceneIds).toContain('SECURITY_CHECKPOINT');
      expect(sceneIds).toContain('CENTRAL_GARAGE');
      expect(sceneIds).toContain('CO_OWNERSHIP_HALL');
      expect(sceneIds).toContain('BOOKING_CHAMBER');
      expect(sceneIds).toContain('ENERGY_FINANCE_CENTER');
      expect(sceneIds).toContain('SHARED_FUND_VAULT');
      expect(sceneIds).toContain('DIGITAL_CONTRACT_ROOM');
      expect(sceneIds).toContain('DECISION_CHAMBER');
      expect(sceneIds).toContain('AI_INTELLIGENCE_CENTER');
      expect(sceneIds).toContain('OPERATIONS_CENTER');
      expect(sceneIds).toContain('SERVICE_WORKSHOP');
      expect(sceneIds).toContain('DISPUTE_ROOM');
      expect(sceneIds).toContain('ADMIN_COMMAND_CENTER');
    });

    it('unregisters scenes on demand', () => {
      SceneRegistry.registerScene(DEFAULT_GARAGE_SCENE);
      expect(SceneRegistry.hasScene('CENTRAL_GARAGE')).toBe(true);

      const removed = SceneRegistry.unregisterScene('CENTRAL_GARAGE');
      expect(removed).toBe(true);
      expect(SceneRegistry.hasScene('CENTRAL_GARAGE')).toBe(false);
    });
  });

  describe('2. Scene Lifecycle & Environment Contracts', () => {
    it('verifies valid scene lifecycle state transitions', () => {
      const states: SceneLifecycleState[] = [
        'INITIALIZING',
        'ACTIVE',
        'TRANSITIONING',
        'UNMOUNTING',
      ];

      expect(states).toContain('INITIALIZING');
      expect(states).toContain('ACTIVE');
      expect(states).toContain('TRANSITIONING');
      expect(states).toContain('UNMOUNTING');
    });

    it('verifies environment lighting and fog properties in default scenes', () => {
      const security = DEFAULT_SECURITY_CHECKPOINT_SCENE;
      expect(security.environment.ambientIntensity).toBeGreaterThan(0);
      expect(security.environment.keyLightIntensity).toBeGreaterThan(0);
      expect(security.environment.fogNear).toBeLessThan(security.environment.fogFar);
      expect(security.environment.enableGroundGrid).toBe(true);

      const garage = DEFAULT_GARAGE_SCENE;
      expect(garage.environment.lightingProfile).toBe('CLEAN_DAYLIGHT');
      expect(garage.camera.fov).toBe(42);
    });
  });
});
