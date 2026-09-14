import { describe, it, expect } from 'vitest';
import {
  getVisualStateSoundId,
  resolveVisualState,
  resolveVisualStateParams,
} from './visualStateEngine';
import { VisualState } from './visualStateTypes';

describe('Shared 3D Visual State System (08-AC)', () => {
  describe('1. State Resolution Hierarchy', () => {
    it('defaults to IDLE when no flags are active', () => {
      expect(resolveVisualState({})).toBe('IDLE');
    });

    it('resolves HOVER when isHovered=true', () => {
      expect(resolveVisualState({ isHovered: true })).toBe('HOVER');
    });

    it('prioritizes SELECTED over HOVER', () => {
      expect(resolveVisualState({ isHovered: true, isSelected: true })).toBe('SELECTED');
    });

    it('prioritizes ACTIVE (pressed) over SELECTED and HOVER', () => {
      expect(resolveVisualState({ isHovered: true, isSelected: true, isPressed: true })).toBe('ACTIVE');
    });

    it('prioritizes LOADING over ACTIVE, SELECTED, and HOVER', () => {
      expect(
        resolveVisualState({
          isHovered: true,
          isSelected: true,
          isPressed: true,
          loading: true,
        })
      ).toBe('LOADING');
    });

    it('prioritizes DISABLED over LOADING, ACTIVE, SELECTED, and HOVER', () => {
      expect(
        resolveVisualState({
          disabled: true,
          loading: true,
          isPressed: true,
          isSelected: true,
          isHovered: true,
        })
      ).toBe('DISABLED');
    });

    it('prioritizes explicit controlledState over all other flags', () => {
      expect(
        resolveVisualState({
          controlledState: 'SUCCESS',
          disabled: true,
          loading: true,
          isPressed: true,
        })
      ).toBe('SUCCESS');

      expect(
        resolveVisualState({
          controlledState: 'ERROR',
          disabled: true,
        })
      ).toBe('ERROR');
    });
  });

  describe('2. PBR Visual Parameters across all 8 States', () => {
    const states: VisualState[] = [
      'IDLE',
      'HOVER',
      'ACTIVE',
      'SELECTED',
      'DISABLED',
      'LOADING',
      'SUCCESS',
      'ERROR',
    ];

    it('resolves valid visual parameters for all 8 states without throwing', () => {
      states.forEach((state) => {
        const params = resolveVisualStateParams(state, 'cyan');
        expect(params.state).toBe(state);
        expect(params.primaryColor).toBeDefined();
        expect(params.backgroundColor).toBeDefined();
        expect(params.borderColor).toBeDefined();
        expect(params.textColor).toBeDefined();
        expect(params.emissiveColor).toBeDefined();
        expect(typeof params.emissiveIntensity).toBe('number');
        expect(typeof params.scale).toBe('number');
        expect(typeof params.elevationZ).toBe('number');
        expect(typeof params.opacity).toBe('number');
        expect(typeof params.isInteractive).toBe('boolean');
      });
    });

    it('enforces tactile physical Z-depression (-0.018m) in ACTIVE state', () => {
      const active = resolveVisualStateParams('ACTIVE');
      expect(active.elevationZ).toBe(-0.018);
      expect(active.emissiveIntensity).toBeGreaterThan(2.0);
      expect(active.isInteractive).toBe(true);
      expect(active.cursor).toBe('POINTER');
    });

    it('enforces hover levitation (+0.025m) and scale punch (1.04x) in HOVER state', () => {
      const hover = resolveVisualStateParams('HOVER');
      expect(hover.elevationZ).toBe(0.025);
      expect(hover.scale).toBe(1.04);
      expect(hover.isInteractive).toBe(true);
      expect(hover.cursor).toBe('POINTER');
    });

    it('enforces high-elevation scan (+0.035m) and gold highlight in SELECTED state', () => {
      const selected = resolveVisualStateParams('SELECTED');
      expect(selected.elevationZ).toBe(0.035);
      expect(selected.scale).toBe(1.06);
      expect(selected.primaryColor).toBe('#ffab00');
      expect(selected.isInteractive).toBe(true);
    });

    it('enforces non-interactive guard and NOT_ALLOWED cursor in DISABLED state', () => {
      const disabled = resolveVisualStateParams('DISABLED');
      expect(disabled.isInteractive).toBe(false);
      expect(disabled.cursor).toBe('NOT_ALLOWED');
      expect(disabled.emissiveIntensity).toBe(0.0);
      expect(disabled.opacity).toBeLessThan(0.6);
    });

    it('enforces non-interactive guard and WAIT cursor in LOADING state', () => {
      const loading = resolveVisualStateParams('LOADING');
      expect(loading.isInteractive).toBe(false);
      expect(loading.cursor).toBe('WAIT');
      expect(loading.opacity).toBeLessThan(0.9);
    });

    it('enforces emerald burst (#00e676) and scale pop (1.08x) in SUCCESS state', () => {
      const success = resolveVisualStateParams('SUCCESS');
      expect(success.primaryColor).toBe('#00e676');
      expect(success.scale).toBe(1.08);
      expect(success.elevationZ).toBe(0.015);
      expect(success.isInteractive).toBe(true);
    });

    it('enforces crimson alert (#ff1744) and high emissive in ERROR state', () => {
      const error = resolveVisualStateParams('ERROR');
      expect(error.primaryColor).toBe('#ff1744');
      expect(error.emissiveColor).toBe('#ff1744');
      expect(error.emissiveIntensity).toBeGreaterThan(2.0);
      expect(error.isInteractive).toBe(true);
    });
  });

  describe('3. Spatial Procedural Sound Mappings', () => {
    it('maps HOVER to UI_HOVER sound', () => {
      expect(getVisualStateSoundId('HOVER')).toBe('UI_HOVER');
    });

    it('maps ACTIVE to UI_CLICK sound', () => {
      expect(getVisualStateSoundId('ACTIVE')).toBe('UI_CLICK');
    });

    it('maps SUCCESS to NOTIF_SUCCESS sound', () => {
      expect(getVisualStateSoundId('SUCCESS')).toBe('NOTIF_SUCCESS');
    });

    it('maps ERROR to NOTIF_ERROR sound', () => {
      expect(getVisualStateSoundId('ERROR')).toBe('NOTIF_ERROR');
    });

    it('returns null for silent states (IDLE, DISABLED, LOADING, SELECTED)', () => {
      expect(getVisualStateSoundId('IDLE')).toBeNull();
      expect(getVisualStateSoundId('DISABLED')).toBeNull();
      expect(getVisualStateSoundId('LOADING')).toBeNull();
      expect(getVisualStateSoundId('SELECTED')).toBeNull();
    });
  });
});
