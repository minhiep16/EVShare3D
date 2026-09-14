import { useEffect, useRef } from 'react';
import { useSound } from '../../audio/useSound';
import { UI3DColorVariant } from '../ui3dTypes';
import {
  getVisualStateSoundId,
  resolveVisualState,
  resolveVisualStateParams,
} from './visualStateEngine';
import { VisualState, VisualStateInput, VisualStateParams } from './visualStateTypes';

export interface UseVisualStateResult {
  state: VisualState;
  params: VisualStateParams;
  isInteractive: boolean;
  cursor: 'POINTER' | 'DEFAULT' | 'NOT_ALLOWED' | 'WAIT';
  playSound: () => void;
}

export function useVisualState(
  input: VisualStateInput & { enableAudio?: boolean }
): UseVisualStateResult {
  const { enableAudio = true, variant = 'cyan' } = input;
  const sound = useSound();

  const effectiveState = resolveVisualState(input);
  const params = resolveVisualStateParams(effectiveState, variant);

  const prevStateRef = useRef<VisualState>(effectiveState);

  // Audio trigger on state transition
  useEffect(() => {
    if (!enableAudio) return;
    if (prevStateRef.current !== effectiveState) {
      const soundId = getVisualStateSoundId(effectiveState);
      if (soundId) {
        sound.play(soundId);
      }
      prevStateRef.current = effectiveState;
    }
  }, [effectiveState, enableAudio, sound]);

  const playSound = () => {
    const soundId = getVisualStateSoundId(effectiveState);
    if (soundId) {
      sound.play(soundId);
    }
  };

  return {
    state: effectiveState,
    params,
    isInteractive: params.isInteractive,
    cursor: params.cursor,
    playSound,
  };
}
