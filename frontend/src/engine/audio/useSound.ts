import { useCallback } from 'react';
import { ProceduralSoundId, SoundOptions } from './audioTypes';
import { AudioEngine } from './AudioEngine';

export interface UseSoundResult {
  play: (id: ProceduralSoundId, options?: SoundOptions) => void;
  playHover: () => void;
  playClick: () => void;
  playWarp: () => void;
  playSuccess: () => void;
  playWarning: () => void;
  playError: () => void;
  startAmbient: (volume?: number) => void;
  stopAmbient: () => void;
}

export const useSound = (): UseSoundResult => {
  const play = useCallback((id: ProceduralSoundId, options?: SoundOptions) => {
    AudioEngine.play(id, options);
  }, []);

  const playHover = useCallback(() => {
    AudioEngine.play('UI_HOVER');
  }, []);

  const playClick = useCallback(() => {
    AudioEngine.play('UI_CLICK');
  }, []);

  const playWarp = useCallback(() => {
    AudioEngine.play('WARP_TRANSITION');
  }, []);

  const playSuccess = useCallback(() => {
    AudioEngine.play('NOTIF_SUCCESS');
  }, []);

  const playWarning = useCallback(() => {
    AudioEngine.play('NOTIF_WARNING');
  }, []);

  const playError = useCallback(() => {
    AudioEngine.play('NOTIF_ERROR');
  }, []);

  const startAmbient = useCallback((volume?: number) => {
    AudioEngine.startAmbientDrone(volume);
  }, []);

  const stopAmbient = useCallback(() => {
    AudioEngine.stopAmbient();
  }, []);

  return {
    play,
    playHover,
    playClick,
    playWarp,
    playSuccess,
    playWarning,
    playError,
    startAmbient,
    stopAmbient,
  };
};
