import { describe, it, expect, beforeEach } from 'vitest';
import { usePerformanceStore } from '../performance/usePerformanceStore';
import { useWebGLRecoveryStore } from './useWebGLRecoveryStore';
import { checkWebGLSupport } from './webglDetector';

describe('WebGL Failure & Recovery Subsystem (08-AE)', () => {
  beforeEach(() => {
    useWebGLRecoveryStore.getState().reset();
    usePerformanceStore.getState().setTier('HIGH');
  });

  describe('1. WebGL Detection & Capability Reporting', () => {
    it('produces a structured WebGLSupportReport without crashing', () => {
      const report = checkWebGLSupport();
      expect(report).toBeDefined();
      expect(typeof report.isSupported).toBe('boolean');
      expect(typeof report.isWebGL1Available).toBe('boolean');
      expect(typeof report.isWebGL2Available).toBe('boolean');
      expect(typeof report.rendererString).toBe('string');
      expect(typeof report.vendorString).toBe('string');
      expect(typeof report.isSoftwareRasterizer).toBe('boolean');
      expect(typeof report.maxTextureSize).toBe('number');
    });

    it('runs diagnostics via recovery store and caches the report', () => {
      const report = useWebGLRecoveryStore.getState().runDiagnostics();
      expect(report).toBeDefined();
      expect(useWebGLRecoveryStore.getState().diagnosticReport).toEqual(report);
    });
  });

  describe('2. Failure Handling & Recovery State Transitions', () => {
    it('handles WebGL context loss and restoration events', () => {
      const store = useWebGLRecoveryStore.getState();
      expect(store.status).toBe('OK');

      store.triggerContextLost();
      expect(useWebGLRecoveryStore.getState().status).toBe('CONTEXT_LOST');

      store.triggerContextRestored();
      expect(useWebGLRecoveryStore.getState().status).toBe('OK');
      expect(useWebGLRecoveryStore.getState().lastError).toBeNull();
    });

    it('captures initialization failures and attaches diagnostic info', () => {
      const initError = new Error('Shader compilation failed: WebGL2 context missing');
      useWebGLRecoveryStore.getState().triggerInitFailed(initError);

      const state = useWebGLRecoveryStore.getState();
      expect(state.status === 'INIT_FAILED' || state.status === 'UNSUPPORTED').toBe(true);
      expect(state.lastError).toBe(initError);
      expect(state.diagnosticReport).toBeDefined();
    });

    it('captures render error boundary faults', () => {
      const renderError = new Error('Mesh geometry buffer overflow');
      useWebGLRecoveryStore.getState().triggerRenderError(renderError);

      const state = useWebGLRecoveryStore.getState();
      expect(state.status).toBe('RENDER_ERROR');
      expect(state.lastError).toBe(renderError);
      expect(state.diagnosticReport).toBeDefined();
    });

    it('retries recovery, clears active error, and increments retryCount', () => {
      useWebGLRecoveryStore.getState().triggerRenderError(new Error('Transient fault'));
      expect(useWebGLRecoveryStore.getState().retryCount).toBe(0);

      useWebGLRecoveryStore.getState().retry();
      const state = useWebGLRecoveryStore.getState();
      expect(state.status).toBe('OK');
      expect(state.lastError).toBeNull();
      expect(state.retryCount).toBe(1);
    });

    it('activates Safe Mode by downgrading performance tier to LOW', () => {
      expect(usePerformanceStore.getState().currentTier).toBe('HIGH');
      useWebGLRecoveryStore.getState().triggerRenderError(new Error('GPU Out of memory'));

      useWebGLRecoveryStore.getState().launchSafeMode();

      const recoveryState = useWebGLRecoveryStore.getState();
      expect(recoveryState.safeModeActive).toBe(true);
      expect(recoveryState.status).toBe('OK');
      expect(recoveryState.lastError).toBeNull();
      expect(recoveryState.retryCount).toBe(1);

      // Performance store should be downgraded to LOW tier
      expect(usePerformanceStore.getState().currentTier).toBe('LOW');
      expect(usePerformanceStore.getState().activeProfile.shadowsEnabled).toBe(false);
    });
  });
});
