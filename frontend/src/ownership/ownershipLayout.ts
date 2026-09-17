import type { Vector3Tuple } from 'three';

export const CO_OWNERSHIP_HALL_LAYOUT = {
  // Sector Dimensions
  HALL_RADIUS: 12.0,
  AMPHITHEATER_RADIUS: 12.0,
  CENTRAL_DAIS_RADIUS: 3.5,
  CENTRAL_DAIS_HEIGHT: 0.35,

  // Central Syndicate Overview Pillar
  PILLAR_CENTER: [0, 1.8, 0] as Vector3Tuple,
  PILLAR_RADIUS: 0.9,
  PILLAR_HEIGHT: 3.2,

  // Floating Equity Distribution Ring
  EQUITY_RING_RADIUS: 2.5,
  EQUITY_RING_ELEVATION: 1.4,
  EQUITY_RING_TUBE_RADIUS: 0.08,

  // Member Pedestal Radial Configuration
  MEMBER_PEDESTAL_RADIUS: 6.2,
  MEMBER_PEDESTAL_HEIGHT: 0.95,
  MEMBER_PEDESTAL_BASE_RADIUS: 0.65,

  // Digital Contract Plinth & Console Position (Left Sector)
  CONTRACT_CONSOLE_POSITION: [-5.5, 0, 4.2] as Vector3Tuple,
  CONTRACT_CONSOLE_POS: [-5.5, 0, 4.2] as Vector3Tuple,
  CONTRACT_CONSOLE_ROTATION_Y: Math.PI / 4,
  CONTRACT_CONSOLE_ROT: [0, Math.PI / 4, 0] as Vector3Tuple,

  // Syndicate Governance Rules & Audit Stela Position (Right Sector)
  RULES_STELA_POS: [5.5, 0, 4.2] as Vector3Tuple,
  RULES_STELA_ROT: [0, -Math.PI / 4, 0] as Vector3Tuple,

  // Camera Spatial Presets for Pure 3D Navigation
  CAMERA_PRESETS: {
    HALL_OVERVIEW: {
      position: [0, 6.5, 9.5] as Vector3Tuple,
      target: [0, 1.2, 0] as Vector3Tuple,
    },
    EQUITY_CORE: {
      position: [0, 3.2, 4.5] as Vector3Tuple,
      target: [0, 1.4, 0] as Vector3Tuple,
    },
    CONTRACT_TERMINAL: {
      position: [-4.2, 2.2, 5.2] as Vector3Tuple,
      target: [-5.5, 1.6, 4.2] as Vector3Tuple,
    },
    RULES_STELA: {
      position: [4.2, 2.2, 5.2] as Vector3Tuple,
      target: [5.5, 1.6, 4.2] as Vector3Tuple,
    },
    HISTORY_STELA: {
      position: [4.2, 2.2, 5.2] as Vector3Tuple,
      target: [5.5, 1.6, 4.2] as Vector3Tuple,
    },
  },

  // Color & Theme Palette (Dark Obsidian & Radiant Gold)
  THEME: {
    FLOOR_OBSIDIAN: '#090d14',
    FLOOR_STEP_BORDER: '#1e293b',
    GOLD_ACCENT_PRIMARY: '#fbbf24',
    GOLD_ACCENT_GLOW: '#f59e0b',
    AMBER_DARK: '#b45309',
    NEON_GOLD: '#facc15',
    TEXT_GOLD_BRIGHT: '#fef08a',
    TEXT_MUTED: '#94a3b8',
    CYBER_EMERALD: '#10b981',
    CYBER_CYAN: '#06b6d4',
    CYBER_RUBY: '#ef4444',
  },

  // Deterministic Co-Owner Colors
  MEMBER_COLORS: [
    '#fbbf24', // Gold (Representative/Founding)
    '#06b6d4', // Cyan
    '#10b981', // Emerald
    '#a855f7', // Purple
    '#f43f5e', // Rose
    '#38bdf8', // Sky Blue
  ],
};
