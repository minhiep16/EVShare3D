import React from 'react';

export type UI3DColorVariant = 'cyan' | 'emerald' | 'amber' | 'crimson' | 'neutral';

export interface UI3DVariantColors {
  primary: string;
  background: string;
  border: string;
  text: string;
  glow: string;
}

export const VARIANT_PALETTES: Record<UI3DColorVariant, UI3DVariantColors> = {
  cyan: {
    primary: '#00e5ff',
    background: '#0a1622',
    border: 'rgba(0, 229, 255, 0.6)',
    text: '#ffffff',
    glow: '#00e5ff',
  },
  emerald: {
    primary: '#00e676',
    background: '#081c14',
    border: 'rgba(0, 230, 118, 0.6)',
    text: '#ffffff',
    glow: '#00e676',
  },
  amber: {
    primary: '#ffab00',
    background: '#221908',
    border: 'rgba(255, 171, 0, 0.6)',
    text: '#ffffff',
    glow: '#ffab00',
  },
  crimson: {
    primary: '#ff1744',
    background: '#22080e',
    border: 'rgba(255, 23, 68, 0.6)',
    text: '#ffffff',
    glow: '#ff1744',
  },
  neutral: {
    primary: '#8a94a6',
    background: '#0d1117',
    border: 'rgba(255, 255, 255, 0.2)',
    text: '#f0f4fc',
    glow: '#ffffff',
  },
};

export interface Panel3DProps {
  id?: string;
  title?: string;
  width?: number;
  height?: number;
  depth?: number;
  variant?: UI3DColorVariant;
  children?: React.ReactNode;
  position?: [number, number, number];
  rotation?: [number, number, number];
}

export type ThreeDButtonState = 'IDLE' | 'HOVER' | 'ACTIVE' | 'DISABLED' | 'LOADING' | 'SUCCESS' | 'ERROR';

export interface ThreeDButtonProps {
  id: string;
  label: string;
  width?: number;
  height?: number;
  depth?: number;
  variant?: UI3DColorVariant;
  disabled?: boolean;
  loading?: boolean;
  state?: ThreeDButtonState;
  ariaLabel?: string;
  shortcutKey?: string;
  isFocused?: boolean;
  onClick?: () => void;
  position?: [number, number, number];
  rotation?: [number, number, number];
}

export interface Button3DProps extends ThreeDButtonProps {}

export interface Input3DProps {
  id: string;
  value: string;
  onChange: (val: string) => void;
  onSubmit?: (val: string) => void;
  placeholder?: string;
  label?: string;
  width?: number;
  height?: number;
  isPassword?: boolean;
  maxLength?: number;
  position?: [number, number, number];
}

export interface Terminal3DProps {
  id: string;
  title: string;
  statusLabel?: string;
  statusVariant?: UI3DColorVariant;
  width?: number;
  height?: number;
  position?: [number, number, number];
  rotation?: [number, number, number];
  children?: React.ReactNode;
}

export interface Modal3DProps {
  id: string;
  title: string;
  isOpen: boolean;
  onClose: () => void;
  width?: number;
  height?: number;
  variant?: UI3DColorVariant;
  position?: [number, number, number];
  children?: React.ReactNode;
}
