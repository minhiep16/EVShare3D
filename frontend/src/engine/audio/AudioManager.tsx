import React, { useEffect } from 'react';
import { AudioEngine } from './AudioEngine';
import { useAudioStore } from './useAudioStore';
import { useUI3DStore } from '@/stores/useUI3DStore';

export const AudioManager: React.FC = () => {
  const masterVolume = useAudioStore((state) => state.masterVolume);
  const isMuted = useAudioStore((state) => state.isMuted);
  const uiVolume = useAudioStore((state) => state.uiVolume);
  const ambientVolume = useAudioStore((state) => state.ambientVolume);
  const transitionVolume = useAudioStore((state) => state.transitionVolume);
  const notificationVolume = useAudioStore((state) => state.notificationVolume);

  // Sync volume & mute changes to Web Audio API gain nodes
  useEffect(() => {
    AudioEngine.syncVolumes();
  }, [masterVolume, isMuted, uiVolume, ambientVolume, transitionVolume, notificationVolume]);

  // Clean disposal on unmount
  useEffect(() => {
    return () => {
      AudioEngine.dispose();
    };
  }, []);

  // Listen to spatial 3D notifications to play notification sounds automatically
  useEffect(() => {
    let lastCount = useUI3DStore.getState().notifications.length;

    const unsubscribe = useUI3DStore.subscribe((state) => {
      if (state.notifications.length > lastCount) {
        const latest = state.notifications[state.notifications.length - 1];
        if (latest.type === 'SUCCESS') {
          AudioEngine.play('NOTIF_SUCCESS');
        } else if (latest.type === 'WARNING') {
          AudioEngine.play('NOTIF_WARNING');
        } else if (latest.type === 'ERROR') {
          AudioEngine.play('NOTIF_ERROR');
        }
      }
      lastCount = state.notifications.length;
    });

    return () => {
      unsubscribe();
    };
  }, []);

  return null;
};
