import { create } from 'zustand';
import { SpatialNotification } from './types';

interface UI3DState {
  // Spatial 3D Modals (Strictly rendered as 3D meshes, not HTML popups)
  activeModalId: string | null;
  modalStack: string[];

  // 3D Terminals & Kiosks
  activeTerminalId: string | null;

  // 3D Virtual Keyboard
  isVirtualKeyboardOpen: boolean;
  virtualKeyboardTargetId: string | null;

  // Spatial Notifications Beacon Queue
  notifications: SpatialNotification[];

  // HUD & Telemetry Overlay
  isHudVisible: boolean;

  // Actions
  openModal: (modalId: string) => void;
  closeModal: (modalId: string) => void;
  closeActiveModal: () => void;

  openTerminal: (terminalId: string) => void;
  closeTerminal: () => void;

  openVirtualKeyboard: (targetInputId: string) => void;
  closeVirtualKeyboard: () => void;

  addNotification: (notification: Omit<SpatialNotification, 'id' | 'timestamp'>) => string;
  dismissNotification: (id: string) => void;
  clearNotifications: () => void;

  toggleHud: () => void;
  setHudVisible: (visible: boolean) => void;
}

export const useUI3DStore = create<UI3DState>((set) => ({
  activeModalId: null,
  modalStack: [],

  activeTerminalId: null,

  isVirtualKeyboardOpen: false,
  virtualKeyboardTargetId: null,

  notifications: [],

  isHudVisible: true,

  openModal: (modalId) =>
    set((state) => ({
      activeModalId: modalId,
      modalStack: [...state.modalStack.filter((id) => id !== modalId), modalId],
    })),

  closeModal: (modalId) =>
    set((state) => {
      const nextStack = state.modalStack.filter((id) => id !== modalId);
      return {
        modalStack: nextStack,
        activeModalId: nextStack.length > 0 ? nextStack[nextStack.length - 1] : null,
      };
    }),

  closeActiveModal: () =>
    set((state) => {
      if (state.modalStack.length === 0) return state;
      const nextStack = state.modalStack.slice(0, -1);
      return {
        modalStack: nextStack,
        activeModalId: nextStack.length > 0 ? nextStack[nextStack.length - 1] : null,
      };
    }),

  openTerminal: (activeTerminalId) => set({ activeTerminalId }),
  closeTerminal: () => set({ activeTerminalId: null }),

  openVirtualKeyboard: (virtualKeyboardTargetId) =>
    set({
      isVirtualKeyboardOpen: true,
      virtualKeyboardTargetId,
    }),

  closeVirtualKeyboard: () =>
    set({
      isVirtualKeyboardOpen: false,
      virtualKeyboardTargetId: null,
    }),

  addNotification: (notification) => {
    const id = `notif_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const newNotification: SpatialNotification = {
      ...notification,
      id,
      timestamp: Date.now(),
    };

    set((state) => ({
      notifications: [...state.notifications, newNotification],
    }));

    if (notification.durationMs && notification.durationMs > 0) {
      setTimeout(() => {
        set((state) => ({
          notifications: state.notifications.filter((n) => n.id !== id),
        }));
      }, notification.durationMs);
    }

    return id;
  },

  dismissNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    })),

  clearNotifications: () => set({ notifications: [] }),

  toggleHud: () => set((state) => ({ isHudVisible: !state.isHudVisible })),
  setHudVisible: (isHudVisible) => set({ isHudVisible }),
}));
