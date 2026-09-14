import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAudioStore } from './useAudioStore';
import { AudioEngine } from './AudioEngine';

describe('AudioManager & Web Audio Foundation Subsystem', () => {
  beforeEach(() => {
    useAudioStore.setState({
      masterVolume: 0.8,
      uiVolume: 0.7,
      ambientVolume: 0.4,
      transitionVolume: 0.8,
      notificationVolume: 0.8,
      isMuted: false,
    });
  });

  describe('useAudioStore Volume & Mute Controls', () => {
    it('sets and clamps master volume within [0, 1]', () => {
      const { setMasterVolume } = useAudioStore.getState();

      setMasterVolume(0.5);
      expect(useAudioStore.getState().masterVolume).toBe(0.5);

      // Clamping upper bound
      setMasterVolume(1.8);
      expect(useAudioStore.getState().masterVolume).toBe(1.0);

      // Clamping lower bound
      setMasterVolume(-0.4);
      expect(useAudioStore.getState().masterVolume).toBe(0.0);
    });

    it('sets per-category volumes independently (UI, AMBIENT, TRANSITION, NOTIFICATION)', () => {
      const { setCategoryVolume } = useAudioStore.getState();

      setCategoryVolume('UI', 0.9);
      setCategoryVolume('AMBIENT', 0.2);
      setCategoryVolume('TRANSITION', 0.65);
      setCategoryVolume('NOTIFICATION', 0.85);

      const state = useAudioStore.getState();
      expect(state.uiVolume).toBe(0.9);
      expect(state.ambientVolume).toBe(0.2);
      expect(state.transitionVolume).toBe(0.65);
      expect(state.notificationVolume).toBe(0.85);
    });

    it('toggles master mute status correctly', () => {
      const { toggleMute } = useAudioStore.getState();

      expect(useAudioStore.getState().isMuted).toBe(false);

      toggleMute();
      expect(useAudioStore.getState().isMuted).toBe(true);

      toggleMute();
      expect(useAudioStore.getState().isMuted).toBe(false);
    });
  });

  describe('AudioEngine Mocked Web Audio API Integration', () => {
    let mockGainNode: {
      gain: { value: number; setValueAtTime: ReturnType<typeof vi.fn> };
      connect: ReturnType<typeof vi.fn>;
      disconnect: ReturnType<typeof vi.fn>;
    };

    let mockOscillatorNode: {
      type: string;
      frequency: { setValueAtTime: ReturnType<typeof vi.fn>; exponentialRampToValueAtTime: ReturnType<typeof vi.fn> };
      connect: ReturnType<typeof vi.fn>;
      start: ReturnType<typeof vi.fn>;
      stop: ReturnType<typeof vi.fn>;
    };

    let mockAudioContext: {
      currentTime: number;
      state: string;
      destination: object;
      createGain: ReturnType<typeof vi.fn>;
      createOscillator: ReturnType<typeof vi.fn>;
      createBiquadFilter: ReturnType<typeof vi.fn>;
      resume: ReturnType<typeof vi.fn>;
      close: ReturnType<typeof vi.fn>;
      decodeAudioData: ReturnType<typeof vi.fn>;
    };

    beforeEach(() => {
      AudioEngine.dispose();

      mockGainNode = {
        gain: {
          value: 1.0,
          setValueAtTime: vi.fn(),
          linearRampToValueAtTime: vi.fn(),
          exponentialRampToValueAtTime: vi.fn(),
        },
        connect: vi.fn(),
        disconnect: vi.fn(),
      };

      mockOscillatorNode = {
        type: 'sine',
        frequency: {
          setValueAtTime: vi.fn(),
          exponentialRampToValueAtTime: vi.fn(),
        },
        connect: vi.fn(),
        start: vi.fn(),
        stop: vi.fn(),
      };

      mockAudioContext = {
        currentTime: 10.0,
        state: 'running',
        destination: {},
        createGain: vi.fn(() => mockGainNode),
        createOscillator: vi.fn(() => mockOscillatorNode),
        createBiquadFilter: vi.fn(() => ({
          type: 'lowpass',
          frequency: {
            setValueAtTime: vi.fn(),
            exponentialRampToValueAtTime: vi.fn(),
          },
          Q: { value: 1 },
          connect: vi.fn(),
          disconnect: vi.fn(),
        })),
        resume: vi.fn().mockResolvedValue(undefined),
        close: vi.fn().mockResolvedValue(undefined),
        decodeAudioData: vi.fn().mockResolvedValue({ duration: 1.5, numberOfChannels: 2 }),
      };

      // Stub global AudioContext for Node/Vitest environment
      (globalThis as unknown as Record<string, unknown>).AudioContext = vi.fn(() => mockAudioContext);
    });

    it('plays procedural UI sound effects without external audio dependencies', () => {
      expect(() => {
        AudioEngine.play('UI_HOVER');
        AudioEngine.play('UI_CLICK');
        AudioEngine.play('WARP_TRANSITION');
        AudioEngine.play('NOTIF_SUCCESS');
      }).not.toThrow();

      expect(mockAudioContext.createGain).toHaveBeenCalled();
    });

    it('sets effective master gain to 0 when muted', () => {
      useAudioStore.setState({ isMuted: true, masterVolume: 0.8 });

      AudioEngine.syncVolumes();

      // Master gain should be assigned 0
      expect(mockGainNode.gain.setValueAtTime).toHaveBeenCalledWith(0, expect.any(Number));
    });

    it('sets effective master gain to masterVolume when unmuted', () => {
      useAudioStore.setState({ isMuted: false, masterVolume: 0.75 });

      AudioEngine.syncVolumes();

      expect(mockGainNode.gain.setValueAtTime).toHaveBeenCalledWith(0.75, expect.any(Number));
    });

    it('properly disposes active ambient audio and audio context on cleanup', () => {
      AudioEngine.startAmbientDrone();
      expect(() => AudioEngine.dispose()).not.toThrow();
      expect(mockAudioContext.close).toHaveBeenCalled();
    });
  });
});
