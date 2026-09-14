import { create } from 'zustand';
import { usePerformanceStore } from '../performance/usePerformanceStore';
import { checkWebGLSupport, WebGLSupportReport } from './webglDetector';

export type WebGLRecoveryStatus =
  | 'OK'
  | 'CONTEXT_LOST'
  | 'INIT_FAILED'
  | 'RENDER_ERROR'
  | 'UNSUPPORTED';

interface WebGLRecoveryState {
  status: WebGLRecoveryStatus;
  retryCount: number;
  lastError: Error | null;
  diagnosticReport: WebGLSupportReport | null;
  safeModeActive: boolean;

  // Actions
  triggerContextLost: () => void;
  triggerContextRestored: () => void;
  triggerInitFailed: (error: Error) => void;
  triggerRenderError: (error: Error) => void;
  retry: () => void;
  launchSafeMode: () => void;
  runDiagnostics: () => WebGLSupportReport;
  reset: () => void;
}

export const useWebGLRecoveryStore = create<WebGLRecoveryState>((set, get) => ({
  status: 'OK',
  retryCount: 0,
  lastError: null,
  diagnosticReport: null,
  safeModeActive: false,

  triggerContextLost: () => {
    console.warn('[useWebGLRecoveryStore] WebGL Context Lost event caught.');
    set({ status: 'CONTEXT_LOST' });
  },

  triggerContextRestored: () => {
    console.info('[useWebGLRecoveryStore] WebGL Context Restored.');
    set({ status: 'OK', lastError: null });
  },

  triggerInitFailed: (error: Error) => {
    console.error('[useWebGLRecoveryStore] WebGL Engine Initialization Failed:', error);
    const report = checkWebGLSupport();
    set({
      status: report.isSupported ? 'INIT_FAILED' : 'UNSUPPORTED',
      lastError: error,
      diagnosticReport: report,
    });
  },

  triggerRenderError: (error: Error) => {
    console.error('[useWebGLRecoveryStore] Rendering Error Boundary Triggered:', error);
    const report = checkWebGLSupport();
    set({
      status: 'RENDER_ERROR',
      lastError: error,
      diagnosticReport: report,
    });
  },

  retry: () => {
    const nextCount = get().retryCount + 1;
    set({
      status: 'OK',
      lastError: null,
      retryCount: nextCount,
    });
  },

  launchSafeMode: () => {
    console.info('[useWebGLRecoveryStore] Activating Safe Mode (LOW Tier Profile).');
    usePerformanceStore.getState().setTier('LOW');
    set({
      safeModeActive: true,
      status: 'OK',
      lastError: null,
      retryCount: get().retryCount + 1,
    });
  },

  runDiagnostics: () => {
    const report = checkWebGLSupport();
    set({ diagnosticReport: report });
    return report;
  },

  reset: () => {
    set({
      status: 'OK',
      retryCount: 0,
      lastError: null,
      diagnosticReport: null,
      safeModeActive: false,
    });
  },
}));
