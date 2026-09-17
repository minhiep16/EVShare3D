import { create } from 'zustand';
import type { EnvironmentPreset, SectorId } from './worldTypes';
import { SECTOR_METADATA_REGISTRY } from './worldCoordinates';

export const ENVIRONMENT_PRESETS: Record<string, EnvironmentPreset> = {
  SECURITY_GATE: {
    id: 'SECURITY_GATE',
    name: 'Security Checkpoint Gate',
    ambientColor: '#0a101f',
    ambientIntensity: 0.6,
    skyColor: '#00e5ff',
    groundColor: '#040711',
    keyLightColor: '#cceeff',
    keyLightIntensity: 1.8,
    keyLightPosition: [0, 15, 90],
    fillLightColor: '#00e5ff',
    fillLightIntensity: 0.8,
    rimLightColor: '#0088cc',
    rimLightIntensity: 0.6,
    fogColor: '#030712',
    fogNear: 10,
    fogFar: 60,
    floorColor: '#0d1117',
    gridColor: '#00e5ff',
    accentColor: '#00e5ff',
  },
  GARAGE_DAYLIGHT: {
    id: 'GARAGE_DAYLIGHT',
    name: 'Central Garage Showroom',
    ambientColor: '#121826',
    ambientIntensity: 0.9,
    skyColor: '#e0f2fe',
    groundColor: '#0f172a',
    keyLightColor: '#ffffff',
    keyLightIntensity: 2.2,
    keyLightPosition: [10, 20, 15],
    fillLightColor: '#38bdf8',
    fillLightIntensity: 0.9,
    rimLightColor: '#00e5ff',
    rimLightIntensity: 0.7,
    fogColor: '#080d1a',
    fogNear: 25,
    fogFar: 90,
    floorColor: '#0f172a',
    gridColor: '#1e293b',
    accentColor: '#00e5ff',
  },
  GOLDEN_OBSIDIAN: {
    id: 'GOLDEN_OBSIDIAN',
    name: 'Co-Ownership Golden Obsidian',
    ambientColor: '#1c1917',
    ambientIntensity: 0.7,
    skyColor: '#fef3c7',
    groundColor: '#0c0a09',
    keyLightColor: '#fef08a',
    keyLightIntensity: 2.0,
    keyLightPosition: [40, 18, 15],
    fillLightColor: '#f59e0b',
    fillLightIntensity: 0.8,
    rimLightColor: '#d97706',
    rimLightIntensity: 0.5,
    fogColor: '#0c0a09',
    fogNear: 15,
    fogFar: 50,
    floorColor: '#1c1917',
    gridColor: '#78350f',
    accentColor: '#ffab00',
  },
  CHRONO_CYAN: {
    id: 'CHRONO_CYAN',
    name: 'Booking Chrono-Cyan',
    ambientColor: '#082f49',
    ambientIntensity: 0.8,
    skyColor: '#bae6fd',
    groundColor: '#031726',
    keyLightColor: '#7dd3fc',
    keyLightIntensity: 2.0,
    keyLightPosition: [0, 18, -40],
    fillLightColor: '#0284c7',
    fillLightIntensity: 0.7,
    rimLightColor: '#00e5ff',
    rimLightIntensity: 0.6,
    fogColor: '#031726',
    fogNear: 15,
    fogFar: 55,
    floorColor: '#071e33',
    gridColor: '#0369a1',
    accentColor: '#00e5ff',
  },
  FINANCE_EMERALD: {
    id: 'FINANCE_EMERALD',
    name: 'Energy & Finance Emerald',
    ambientColor: '#064e3b',
    ambientIntensity: 0.8,
    skyColor: '#a7f3d0',
    groundColor: '#022c22',
    keyLightColor: '#6ee7b7',
    keyLightIntensity: 2.1,
    keyLightPosition: [0, 18, -80],
    fillLightColor: '#10b981',
    fillLightIntensity: 0.9,
    rimLightColor: '#059669',
    rimLightIntensity: 0.7,
    fogColor: '#022c22',
    fogNear: 15,
    fogFar: 55,
    floorColor: '#064e3b',
    gridColor: '#047857',
    accentColor: '#00e676',
  },
  VAULT_AMBER: {
    id: 'VAULT_AMBER',
    name: 'Shared Fund Vault Amber',
    ambientColor: '#451a03',
    ambientIntensity: 0.75,
    skyColor: '#fed7aa',
    groundColor: '#1c0a00',
    keyLightColor: '#fbbf24',
    keyLightIntensity: 2.2,
    keyLightPosition: [-40, 18, -80],
    fillLightColor: '#d97706',
    fillLightIntensity: 0.8,
    rimLightColor: '#b45309',
    rimLightIntensity: 0.6,
    fogColor: '#1c0a00',
    fogNear: 12,
    fogFar: 48,
    floorColor: '#291102',
    gridColor: '#78350f',
    accentColor: '#ffab00',
  },
  EXECUTIVE_SLATE: {
    id: 'EXECUTIVE_SLATE',
    name: 'Digital Contract Executive Slate',
    ambientColor: '#0f172a',
    ambientIntensity: 0.8,
    skyColor: '#cbd5e1',
    groundColor: '#020617',
    keyLightColor: '#f1f5f9',
    keyLightIntensity: 2.0,
    keyLightPosition: [40, 18, -40],
    fillLightColor: '#64748b',
    fillLightIntensity: 0.7,
    rimLightColor: '#3b82f6',
    rimLightIntensity: 0.5,
    fogColor: '#020617',
    fogNear: 15,
    fogFar: 50,
    floorColor: '#0f172a',
    gridColor: '#334155',
    accentColor: '#60a5fa',
  },
  PARLIAMENT_INDIGO: {
    id: 'PARLIAMENT_INDIGO',
    name: 'Decision Chamber Parliament Indigo',
    ambientColor: '#1e1b4b',
    ambientIntensity: 0.8,
    skyColor: '#c7d2fe',
    groundColor: '#080720',
    keyLightColor: '#a5b4fc',
    keyLightIntensity: 2.1,
    keyLightPosition: [40, 18, -80],
    fillLightColor: '#6366f1',
    fillLightIntensity: 0.8,
    rimLightColor: '#818cf8',
    rimLightIntensity: 0.6,
    fogColor: '#080720',
    fogNear: 15,
    fogFar: 55,
    floorColor: '#1e1b4b',
    gridColor: '#4338ca',
    accentColor: '#818cf8',
  },
  NEURAL_PURPLE: {
    id: 'NEURAL_PURPLE',
    name: 'AI Intelligence Neural Nexus',
    ambientColor: '#3b0764',
    ambientIntensity: 0.7,
    skyColor: '#f5d0fe',
    groundColor: '#18022a',
    keyLightColor: '#d8b4fe',
    keyLightIntensity: 2.2,
    keyLightPosition: [40, 18, 0],
    fillLightColor: '#c084fc',
    fillLightIntensity: 0.9,
    rimLightColor: '#e879f9',
    rimLightIntensity: 0.8,
    fogColor: '#120220',
    fogNear: 14,
    fogFar: 50,
    floorColor: '#240438',
    gridColor: '#7e22ce',
    accentColor: '#c084fc',
  },
  OPERATIONS_ORANGE: {
    id: 'OPERATIONS_ORANGE',
    name: 'Operations Center Diagnostic',
    ambientColor: '#431407',
    ambientIntensity: 0.85,
    skyColor: '#ffedd5',
    groundColor: '#1c0702',
    keyLightColor: '#fb923c',
    keyLightIntensity: 2.2,
    keyLightPosition: [-40, 18, 0],
    fillLightColor: '#ea580c',
    fillLightIntensity: 0.9,
    rimLightColor: '#f97316',
    rimLightIntensity: 0.7,
    fogColor: '#1c0702',
    fogNear: 15,
    fogFar: 55,
    floorColor: '#270e06',
    gridColor: '#9a3412',
    accentColor: '#f97316',
  },
  WORKSHOP_STEEL: {
    id: 'WORKSHOP_STEEL',
    name: 'Service Workshop Inspection',
    ambientColor: '#18181b',
    ambientIntensity: 0.9,
    skyColor: '#f4f4f5',
    groundColor: '#09090b',
    keyLightColor: '#ffffff',
    keyLightIntensity: 2.4,
    keyLightPosition: [-40, 18, 40],
    fillLightColor: '#a1a1aa',
    fillLightIntensity: 0.8,
    rimLightColor: '#e4e4e7',
    rimLightIntensity: 0.6,
    fogColor: '#09090b',
    fogNear: 15,
    fogFar: 55,
    floorColor: '#18181b',
    gridColor: '#3f3f46',
    accentColor: '#38bdf8',
  },
  DISPUTE_CRIMSON: {
    id: 'DISPUTE_CRIMSON',
    name: 'Dispute Resolution Neutral Deliberation',
    ambientColor: '#450a0a',
    ambientIntensity: 0.75,
    skyColor: '#fecaca',
    groundColor: '#1a0303',
    keyLightColor: '#f87171',
    keyLightIntensity: 2.1,
    keyLightPosition: [-40, 18, 80],
    fillLightColor: '#dc2626',
    fillLightIntensity: 0.8,
    rimLightColor: '#ef4444',
    rimLightIntensity: 0.7,
    fogColor: '#1a0303',
    fogNear: 14,
    fogFar: 50,
    floorColor: '#2b0707',
    gridColor: '#991b1b',
    accentColor: '#ff1744',
  },
  COMMAND_HORIZON: {
    id: 'COMMAND_HORIZON',
    name: 'Admin Command Orbital Horizon',
    ambientColor: '#020617',
    ambientIntensity: 0.65,
    skyColor: '#38bdf8',
    groundColor: '#010309',
    keyLightColor: '#e0f2fe',
    keyLightIntensity: 2.5,
    keyLightPosition: [0, 45, 0],
    fillLightColor: '#0284c7',
    fillLightIntensity: 0.9,
    rimLightColor: '#38bdf8',
    rimLightIntensity: 0.8,
    fogColor: '#010309',
    fogNear: 20,
    fogFar: 120,
    floorColor: '#020617',
    gridColor: '#1e3a8a',
    accentColor: '#38bdf8',
  },
};

interface WorldEnvironmentState {
  activeSectorId: SectorId;
  activePreset: EnvironmentPreset;
  transitioning: boolean;
  transitionProgress: number;
  targetPreset: EnvironmentPreset | null;

  // Actions
  setActiveSector: (sectorId: SectorId) => void;
  setEnvironmentPreset: (presetId: string) => void;
  startTransition: (toSectorId: SectorId) => void;
  setTransitionProgress: (progress: number) => void;
  completeTransition: () => void;
}

export const useWorldEnvironmentStore = create<WorldEnvironmentState>((set, get) => ({
  activeSectorId: 'SECURITY_CHECKPOINT',
  activePreset: ENVIRONMENT_PRESETS.SECURITY_GATE,
  transitioning: false,
  transitionProgress: 1.0,
  targetPreset: null,

  setActiveSector: (sectorId: SectorId) => {
    const meta = SECTOR_METADATA_REGISTRY[sectorId];
    if (!meta) return;
    const preset = ENVIRONMENT_PRESETS[meta.primaryEnvironmentPreset] || ENVIRONMENT_PRESETS.GARAGE_DAYLIGHT;
    set({
      activeSectorId: sectorId,
      activePreset: preset,
      transitioning: false,
      transitionProgress: 1.0,
      targetPreset: null,
    });
  },

  setEnvironmentPreset: (presetId: string) => {
    const preset = ENVIRONMENT_PRESETS[presetId];
    if (preset) {
      set({ activePreset: preset });
    }
  },

  startTransition: (toSectorId: SectorId) => {
    const meta = SECTOR_METADATA_REGISTRY[toSectorId];
    if (!meta) return;
    const targetPreset = ENVIRONMENT_PRESETS[meta.primaryEnvironmentPreset] || ENVIRONMENT_PRESETS.GARAGE_DAYLIGHT;
    set({
      transitioning: true,
      transitionProgress: 0.0,
      targetPreset,
    });
  },

  setTransitionProgress: (progress: number) => {
    set({ transitionProgress: Math.min(1.0, Math.max(0.0, progress)) });
  },

  completeTransition: () => {
    const { targetPreset } = get();
    if (targetPreset) {
      // Find sector matching target preset
      let resolvedSector: SectorId = 'CENTRAL_GARAGE';
      for (const [sId, meta] of Object.entries(SECTOR_METADATA_REGISTRY)) {
        if (meta.primaryEnvironmentPreset === targetPreset.id) {
          resolvedSector = sId as SectorId;
          break;
        }
      }
      set({
        activeSectorId: resolvedSector,
        activePreset: targetPreset,
        transitioning: false,
        transitionProgress: 1.0,
        targetPreset: null,
      });
    }
  },
}));
