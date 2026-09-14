/**
 * Audio Engine Types
 * Pure Web Audio API foundation supporting procedural sound synthesis
 * and lazy-loaded assets without any copyrighted audio files.
 */

export type SoundCategory = 'UI' | 'AMBIENT' | 'TRANSITION' | 'NOTIFICATION';

export type ProceduralSoundId =
  | 'UI_HOVER'
  | 'UI_CLICK'
  | 'UI_BACK'
  | 'WARP_TRANSITION'
  | 'CAMERA_WHOOSH'
  | 'NOTIF_SUCCESS'
  | 'NOTIF_WARNING'
  | 'NOTIF_ERROR'
  | 'AMBIENT_CYBER_DRONE';

export interface SoundOptions {
  volume?: number;      // 0.0 - 1.0 multiplier
  playbackRate?: number; // Pitch/speed multiplier (default: 1.0)
  loop?: boolean;
}

export interface AudioSettings {
  masterVolume: number;
  uiVolume: number;
  ambientVolume: number;
  transitionVolume: number;
  notificationVolume: number;
  isMuted: boolean;
}

export interface ActiveAudioNode {
  id: string;
  source: AudioNode;
  gain: GainNode;
  category: SoundCategory;
  stop: () => void;
}
