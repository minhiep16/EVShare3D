import { describe, it, expect, beforeEach } from 'vitest';
import { useUI3DStore } from '@/stores/useUI3DStore';
import { useInputStore } from '../input/useInputStore';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { VARIANT_PALETTES } from './ui3dTypes';

describe('UI3DManager & WebGL Spatial Interface Subsystem', () => {
  beforeEach(() => {
    useUI3DStore.setState({
      activeModalId: null,
      modalStack: [],
      activeTerminalId: null,
      notifications: [],
      isHudVisible: true,
    });
    useInputStore.setState({
      isTypingMode: false,
    });
    useInteractionStore.setState({
      focusedInputId: null,
      selectedObjectId: null,
      hoveredObjectId: null,
    });
  });

  describe('3D Modal Stack Operations (Strictly in WebGL)', () => {
    it('opens 3D modal and pushes onto modal stack', () => {
      useUI3DStore.getState().openModal('vehicle_specs_modal');

      const state = useUI3DStore.getState();
      expect(state.activeModalId).toBe('vehicle_specs_modal');
      expect(state.modalStack).toEqual(['vehicle_specs_modal']);
    });

    it('manages nested 3D modal stack and closes sequentially', () => {
      const { openModal, closeActiveModal } = useUI3DStore.getState();

      openModal('garage_modal');
      openModal('vehicle_confirm_modal');

      expect(useUI3DStore.getState().activeModalId).toBe('vehicle_confirm_modal');
      expect(useUI3DStore.getState().modalStack.length).toBe(2);

      // Close top modal -> restores garage_modal
      closeActiveModal();
      expect(useUI3DStore.getState().activeModalId).toBe('garage_modal');
      expect(useUI3DStore.getState().modalStack).toEqual(['garage_modal']);

      // Close last modal -> resets to null
      closeActiveModal();
      expect(useUI3DStore.getState().activeModalId).toBeNull();
      expect(useUI3DStore.getState().modalStack).toEqual([]);
    });

    it('closes specific modal from anywhere in stack', () => {
      const { openModal, closeModal } = useUI3DStore.getState();

      openModal('modal_1');
      openModal('modal_2');
      openModal('modal_3');

      closeModal('modal_2');
      expect(useUI3DStore.getState().modalStack).toEqual(['modal_1', 'modal_3']);
      expect(useUI3DStore.getState().activeModalId).toBe('modal_3');
    });
  });

  describe('3D Terminal Management', () => {
    it('opens and closes interactive terminals', () => {
      const { openTerminal, closeTerminal } = useUI3DStore.getState();

      openTerminal('security_kiosk_01');
      expect(useUI3DStore.getState().activeTerminalId).toBe('security_kiosk_01');

      closeTerminal();
      expect(useUI3DStore.getState().activeTerminalId).toBeNull();
    });
  });

  describe('Input Typing Mode & Focus Synchronization', () => {
    it('typing mode disables WASD camera/player movement when 3D input is active', () => {
      useInteractionStore.getState().setFocusedInputId('pincode_input');
      useInputStore.getState().setTypingMode(true);

      expect(useInteractionStore.getState().focusedInputId).toBe('pincode_input');
      expect(useInputStore.getState().isTypingMode).toBe(true);

      // Blurring input
      useInteractionStore.getState().setFocusedInputId(null);
      useInputStore.getState().setTypingMode(false);

      expect(useInteractionStore.getState().focusedInputId).toBeNull();
      expect(useInputStore.getState().isTypingMode).toBe(false);
    });
  });

  describe('UI3D Design Palette Tokens', () => {
    it('provides high-contrast cybernetic color variants', () => {
      expect(VARIANT_PALETTES.cyan.primary).toBe('#00e5ff');
      expect(VARIANT_PALETTES.emerald.primary).toBe('#00e676');
      expect(VARIANT_PALETTES.amber.primary).toBe('#ffab00');
      expect(VARIANT_PALETTES.crimson.primary).toBe('#ff1744');
      expect(VARIANT_PALETTES.neutral.primary).toBe('#8a94a6');
    });
  });
});
