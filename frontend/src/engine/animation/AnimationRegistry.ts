import { ActiveAnimationTrack, AnimationConfig, AnimationState } from './animationTypes';

export const TRANSIENT_DURATIONS: Partial<Record<AnimationState, number>> = {
  ACTIVE: 350,   // 350ms activation punch
  SUCCESS: 750,  // 750ms green pulse & settle
  ERROR: 500,    // 500ms rapid shake & decay
};

export const IS_LOOPING_STATE: Record<AnimationState, boolean> = {
  IDLE: false,
  HOVER: true,
  SELECTED: true,
  LOADING: true,
  ACTIVE: false,
  SUCCESS: false,
  ERROR: false,
};

class AnimationRegistryClass {
  private tracks = new Map<string, ActiveAnimationTrack>();
  private configs = new Map<string, AnimationConfig>();
  private listeners = new Map<string, Set<(state: AnimationState) => void>>();

  public register(config: AnimationConfig): () => void {
    this.configs.set(config.id, config);
    this.setTrackState(config.id, config.state);

    return () => {
      this.tracks.delete(config.id);
      this.configs.delete(config.id);
      this.listeners.delete(config.id);
    };
  }

  public setTrackState(id: string, newState: AnimationState): void {
    const isLooping = IS_LOOPING_STATE[newState];
    const durationMs = TRANSIENT_DURATIONS[newState] ?? 0;

    this.tracks.set(id, {
      id,
      state: newState,
      elapsedMs: 0,
      durationMs,
      isLooping,
      isFinished: false,
    });

    this.notifyListeners(id, newState);
  }

  public getTrack(id: string): ActiveAnimationTrack | undefined {
    return this.tracks.get(id);
  }

  public getConfig(id: string): AnimationConfig | undefined {
    return this.configs.get(id);
  }

  public getAllTracks(): ActiveAnimationTrack[] {
    return Array.from(this.tracks.values());
  }

  public subscribe(id: string, callback: (state: AnimationState) => void): () => void {
    if (!this.listeners.has(id)) {
      this.listeners.set(id, new Set());
    }
    this.listeners.get(id)!.add(callback);
    return () => {
      this.listeners.get(id)?.delete(callback);
    };
  }

  private notifyListeners(id: string, state: AnimationState): void {
    this.listeners.get(id)?.forEach((cb) => cb(state));
  }

  /**
   * Centralized tick for all active tracks in the engine.
   * Automatically terminates transient animations and prevents uncontrolled loops.
   */
  public tickAll(deltaMs: number): void {
    for (const [id, track] of this.tracks.entries()) {
      if (track.isFinished) continue;

      track.elapsedMs += deltaMs;

      // Check transient one-shot completion
      if (!track.isLooping && track.durationMs > 0) {
        if (track.elapsedMs >= track.durationMs) {
          track.isFinished = true;
          const config = this.configs.get(id);
          config?.onAnimationComplete?.(track.state);

          // Auto-settle to IDLE to ensure animation stops
          this.setTrackState(id, 'IDLE');
        }
      }
    }
  }

  /**
   * Advances a single track time (fallback or individual update).
   */
  public advanceTrack(id: string, deltaMs: number): void {
    const track = this.tracks.get(id);
    if (!track || track.isFinished) return;

    track.elapsedMs += deltaMs;

    if (!track.isLooping && track.durationMs > 0) {
      if (track.elapsedMs >= track.durationMs) {
        track.isFinished = true;
        const config = this.configs.get(id);
        config?.onAnimationComplete?.(track.state);

        // Auto-settle to IDLE after one-shot completion
        this.setTrackState(id, 'IDLE');
      }
    }
  }

  public clear(): void {
    this.tracks.clear();
    this.configs.clear();
    this.listeners.clear();
  }
}

export const AnimationRegistry = new AnimationRegistryClass();
