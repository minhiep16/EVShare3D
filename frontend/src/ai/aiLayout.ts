import type { AICameraPreset } from './aiTypes';

export const AI_LAYOUT = {
  // Sector center offset in world coordinates
  sectorCenter: [40, 0, 0] as [number, number, number],

  // Station positions relative to sector origin [0, 0, 0]
  aiCorePosition: [0, 2.5, 0] as [number, number, number],
  interactionTerminalPosition: [0, 0, 3.2] as [number, number, number],
  recommendationHoloPosition: [0, 2.2, -3.2] as [number, number, number],
  resultVisualizationPosition: [0, 0.2, -5.4] as [number, number, number],

  // Data Node satellite positions
  dataNodePositions: {
    NODE_MOBILITY: [-3.6, 2.2, -0.8] as [number, number, number],
    NODE_FAIRNESS: [3.6, 2.2, -0.8] as [number, number, number],
    NODE_FINANCIAL: [-2.8, 3.2, 1.8] as [number, number, number],
    NODE_GOVERNANCE: [2.8, 3.2, 1.8] as [number, number, number],
  },

  // Floor radius
  arenaRadius: 11.5,

  // Visual Theme Tokens
  colors: {
    arenaDark: '#080612',
    gridTrack: '#1e1438',
    neuralPurple: '#8b5cf6',
    neuralViolet: '#c084fc',
    cyberCyan: '#06b6d4',
    cyanLight: '#67e8f9',
    goldWarning: '#fbbf24',
    anomalyAmber: '#f59e0b',
    anomalyRed: '#ef4444',
    safetyCrimson: '#dc2626',
    successGreen: '#10b981',
    textWhite: '#f8fafc',
    textMuted: '#94a3b8',
  },

  cameraPresets: {
    NEXUS_OVERVIEW: {
      name: 'Toàn Cảnh Trung Tâm Nơ-ron',
      position: [0, 6.5, 9.8],
      target: [0, 2.0, 0],
    } as AICameraPreset,
    CORE_FOCUS: {
      name: 'Lõi Toàn Ảnh AI',
      position: [0, 2.8, 4.2],
      target: [0, 2.5, 0],
    } as AICameraPreset,
    TERMINAL_FOCUS: {
      name: 'Bàn Tương Tác Cố Vấn',
      position: [0, 2.2, 4.8],
      target: [0, 1.1, 3.2],
    } as AICameraPreset,
    HOLOGRAPHIC_RECOMMENDATIONS: {
      name: 'Khuyến Nghị Toàn Ảnh',
      position: [0, 3.0, -0.6],
      target: [0, 2.2, -3.2],
    } as AICameraPreset,
    DATA_NODES_FOCUS: {
      name: 'Nút Dữ Liệu Vệ Tinh',
      position: [3.8, 3.2, 2.0],
      target: [2.5, 2.2, -0.5],
    } as AICameraPreset,
    SAFETY_AUDIT_FOCUS: {
      name: 'Khóa Bảo Vệ Ranh Giới An Toàn',
      position: [-3.8, 3.2, 2.0],
      target: [-2.5, 2.2, -0.5],
    } as AICameraPreset,
  },
};
