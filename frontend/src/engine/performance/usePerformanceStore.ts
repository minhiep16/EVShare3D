import { create } from 'zustand';
import { PERFORMANCE_PROFILES } from './performanceProfiles';
import { PerformanceProfile, PerformanceTier } from './performanceTypes';

interface PerformanceState {
  currentTier: PerformanceTier;
  activeProfile: PerformanceProfile;
  adaptiveDpr: number;
  measuredFps: number;
  autoDegradeEnabled: boolean;
  lowFpsDurationSec: number;

  // Actions
  setTier: (tier: PerformanceTier) => void;
  setAdaptiveDpr: (dpr: number) => void;
  recordFpsSample: (fps: number, deltaSec: number) => void;
  toggleAutoDegrade: () => void;
  setAutoDegradeEnabled: (enabled: boolean) => void;
}

export const usePerformanceStore = create<PerformanceState>((set, get) => ({
  currentTier: 'HIGH',
  activeProfile: PERFORMANCE_PROFILES.HIGH,
  adaptiveDpr: 1.5,
  measuredFps: 60,
  autoDegradeEnabled: true,
  lowFpsDurationSec: 0,

  setTier: (tier: PerformanceTier) => {
    const profile = PERFORMANCE_PROFILES[tier];
    set({
      currentTier: tier,
      activeProfile: profile,
      adaptiveDpr: profile.dprRange[1],
      lowFpsDurationSec: 0,
    });
  },

  setAdaptiveDpr: (dpr: number) => {
    const { activeProfile } = get();
    const clamped = Math.max(activeProfile.dprRange[0], Math.min(activeProfile.dprRange[1], dpr));
    set({ adaptiveDpr: clamped });
  },

  recordFpsSample: (fps: number, deltaSec: number) => {
    const { currentTier, autoDegradeEnabled, lowFpsDurationSec } = get();
    const smoothedFps = Math.round(fps);

    if (!autoDegradeEnabled) {
      set({ measuredFps: smoothedFps });
      return;
    }

    // Auto-degradation rule: if FPS drops below 28 for >3 consecutive seconds
    if (fps < 28) {
      const newDuration = lowFpsDurationSec + deltaSec;
      if (newDuration >= 3.0) {
        if (currentTier === 'HIGH') {
          get().setTier('MEDIUM');
        } else if (currentTier === 'MEDIUM') {
          get().setTier('LOW');
        }
        return;
      }
      set({ measuredFps: smoothedFps, lowFpsDurationSec: newDuration });
    } else {
      // Reset duration if performing smoothly
      set({ measuredFps: smoothedFps, lowFpsDurationSec: Math.max(0, lowFpsDurationSec - deltaSec * 2) });
    }
  },

  toggleAutoDegrade: () => set((state) => ({ autoDegradeEnabled: !state.autoDegradeEnabled })),
  setAutoDegradeEnabled: (autoDegradeEnabled: boolean) => set({ autoDegradeEnabled }),
}));
