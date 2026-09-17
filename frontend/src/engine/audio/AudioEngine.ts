import { ProceduralSoundId, SoundCategory, SoundOptions } from './audioTypes';
import {
  createAmbientDrone,
  playClickSnap,
  playErrorBuzz,
  playHoverBlip,
  playSuccessChime,
  playWarningBeep,
  playWarpTransition,
} from './proceduralSynth';
import { useAudioStore } from './useAudioStore';

class AudioEngineClass {
  private ctx: AudioContext | null = null;
  private masterGain: GainNode | null = null;
  private categoryGains = new Map<SoundCategory, GainNode>();
  private bufferCache = new Map<string, AudioBuffer>();
  private activeAmbientStop: (() => void) | null = null;
  private isUnlocked = false;

  private initContext(): AudioContext | null {
    if (!this.ctx) {
      const g = typeof window !== 'undefined' ? window : (globalThis as unknown as Window);
      const AudioCtx =
        g.AudioContext ||
        (g as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;

      if (!AudioCtx) {
        return null;
      }

      try {
        this.ctx = new AudioCtx();
        if (typeof this.ctx.createGain !== 'function') {
          return null;
        }

        // Master Gain
        this.masterGain = this.ctx.createGain();
        this.masterGain.connect(this.ctx.destination);

        // Category Gains
        const categories: SoundCategory[] = ['UI', 'AMBIENT', 'TRANSITION', 'NOTIFICATION'];
        categories.forEach((cat) => {
          const gainNode = this.ctx!.createGain();
          gainNode.connect(this.masterGain!);
          this.categoryGains.set(cat, gainNode);
        });

        this.syncVolumes();
        this.listenToUserGesture();
      } catch {
        return null;
      }
    }
    return this.ctx;
  }

  private listenToUserGesture(): void {
    if (this.isUnlocked || typeof window === 'undefined') return;

    const unlock = () => {
      if (this.ctx && this.ctx.state === 'suspended') {
        this.ctx.resume().then(() => {
          this.isUnlocked = true;
        });
      } else {
        this.isUnlocked = true;
      }
      window.removeEventListener('pointerdown', unlock);
      window.removeEventListener('keydown', unlock);
      window.removeEventListener('click', unlock);
    };

    window.addEventListener('pointerdown', unlock, { once: true });
    window.addEventListener('keydown', unlock, { once: true });
    window.addEventListener('click', unlock, { once: true });
  }

  public syncVolumes(): void {
    const ctx = this.initContext();
    if (!ctx || !this.masterGain) return;

    const { masterVolume, uiVolume, ambientVolume, transitionVolume, notificationVolume, isMuted } =
      useAudioStore.getState();

    const now = this.ctx.currentTime;
    const effectiveMaster = isMuted ? 0 : masterVolume;

    this.masterGain.gain.setValueAtTime(effectiveMaster, now);

    this.categoryGains.get('UI')?.gain.setValueAtTime(uiVolume, now);
    this.categoryGains.get('AMBIENT')?.gain.setValueAtTime(ambientVolume, now);
    this.categoryGains.get('TRANSITION')?.gain.setValueAtTime(transitionVolume, now);
    this.categoryGains.get('NOTIFICATION')?.gain.setValueAtTime(notificationVolume, now);
  }

  /**
   * Plays a procedural sound effect without requiring any external audio files.
   */
  public play(id: ProceduralSoundId, options?: SoundOptions): void {
    const ctx = this.initContext();
    if (!ctx || typeof ctx.createOscillator !== 'function') return;

    if (ctx.state === 'suspended') {
      ctx.resume().catch(() => {});
    }

    this.syncVolumes();

    let dest: AudioNode = this.masterGain!;
    const vol = options?.volume ?? 1.0;

    switch (id) {
      case 'UI_HOVER':
        dest = this.categoryGains.get('UI') || dest;
        playHoverBlip(ctx, dest, vol);
        break;
      case 'UI_CLICK':
      case 'UI_BACK':
        dest = this.categoryGains.get('UI') || dest;
        playClickSnap(ctx, dest, vol);
        break;
      case 'WARP_TRANSITION':
      case 'CAMERA_WHOOSH':
        dest = this.categoryGains.get('TRANSITION') || dest;
        playWarpTransition(ctx, dest, vol);
        break;
      case 'NOTIF_SUCCESS':
        dest = this.categoryGains.get('NOTIFICATION') || dest;
        playSuccessChime(ctx, dest, vol);
        break;
      case 'NOTIF_WARNING':
        dest = this.categoryGains.get('NOTIFICATION') || dest;
        playWarningBeep(ctx, dest, vol);
        break;
      case 'NOTIF_ERROR':
        dest = this.categoryGains.get('NOTIFICATION') || dest;
        playErrorBuzz(ctx, dest, vol);
        break;
      case 'AMBIENT_CYBER_DRONE':
        this.startAmbientDrone(vol);
        break;
    }
  }

  /**
   * Plays a 3D positioned procedural sound effect.
   */
  public playSpatial(id: ProceduralSoundId, _position?: [number, number, number], options?: SoundOptions): void {
    this.play(id, options);
  }

  /**
   * Starts continuous ambient background cyber drone.
   */
  public startAmbientDrone(volume = 0.2): void {
    this.stopAmbient();
    const ctx = this.initContext();
    if (!ctx || typeof ctx.createOscillator !== 'function') return;
    const dest = this.categoryGains.get('AMBIENT') || this.masterGain!;
    this.activeAmbientStop = createAmbientDrone(ctx, dest, volume);
  }

  /**
   * Stops continuous ambient audio with graceful fadeout.
   */
  public stopAmbient(): void {
    if (this.activeAmbientStop) {
      this.activeAmbientStop();
      this.activeAmbientStop = null;
    }
  }

  /**
   * Lazy loads and caches an external audio buffer.
   */
  public async loadAudioBuffer(url: string): Promise<AudioBuffer | null> {
    if (this.bufferCache.has(url)) {
      return this.bufferCache.get(url)!;
    }

    const ctx = this.initContext();
    try {
      const response = await fetch(url);
      const arrayBuffer = await response.arrayBuffer();
      const audioBuffer = await ctx.decodeAudioData(arrayBuffer);
      this.bufferCache.set(url, audioBuffer);
      return audioBuffer;
    } catch (err) {
      console.warn(`[AudioEngine] Failed to lazy-load audio buffer from: ${url}`, err);
      return null;
    }
  }

  /**
   * Plays a cached audio buffer.
   */
  public playBuffer(buffer: AudioBuffer, category: SoundCategory = 'UI', options?: SoundOptions): AudioBufferSourceNode {
    const ctx = this.initContext();
    const dest = this.categoryGains.get(category) || this.masterGain!;

    const source = ctx.createBufferSource();
    source.buffer = buffer;
    source.loop = options?.loop ?? false;
    if (options?.playbackRate) source.playbackRate.value = options.playbackRate;

    source.connect(dest);
    source.start();
    return source;
  }

  public dispose(): void {
    this.stopAmbient();
    this.bufferCache.clear();
    if (this.ctx && this.ctx.state !== 'closed') {
      this.ctx.close().catch(() => {});
      this.ctx = null;
      this.masterGain = null;
      this.categoryGains.clear();
    }
  }
}

export const AudioEngine = new AudioEngineClass();
