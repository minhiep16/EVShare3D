/**
 * Procedural Web Audio API Synthesizer
 * 100% rights-free, generated in real-time without any external copyrighted files.
 */

export const playHoverBlip = (ctx: AudioContext, destination: AudioNode, volume = 0.3): void => {
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();

  osc.type = 'sine';
  const now = ctx.currentTime;

  // Rapid micro-pitch chirp 850Hz -> 1250Hz
  osc.frequency.setValueAtTime(850, now);
  osc.frequency.exponentialRampToValueAtTime(1250, now + 0.035);

  gain.gain.setValueAtTime(volume * 0.4, now);
  gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);

  osc.connect(gain);
  gain.connect(destination);

  osc.start(now);
  osc.stop(now + 0.045);
};

export const playClickSnap = (ctx: AudioContext, destination: AudioNode, volume = 0.5): void => {
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();

  osc.type = 'triangle';
  const now = ctx.currentTime;

  // Snappy tech pop
  osc.frequency.setValueAtTime(540, now);
  osc.frequency.exponentialRampToValueAtTime(180, now + 0.05);

  gain.gain.setValueAtTime(volume * 0.6, now);
  gain.gain.exponentialRampToValueAtTime(0.001, now + 0.055);

  osc.connect(gain);
  gain.connect(destination);

  osc.start(now);
  osc.stop(now + 0.06);
};

export const playWarpTransition = (ctx: AudioContext, destination: AudioNode, volume = 0.6): void => {
  const osc = ctx.createOscillator();
  const filter = ctx.createBiquadFilter();
  const gain = ctx.createGain();

  osc.type = 'sawtooth';
  filter.type = 'lowpass';
  filter.Q.value = 4.0;

  const now = ctx.currentTime;
  const dur = 0.45;

  // Upward frequency and filter sweep
  osc.frequency.setValueAtTime(140, now);
  osc.frequency.exponentialRampToValueAtTime(780, now + dur);

  filter.frequency.setValueAtTime(300, now);
  filter.frequency.exponentialRampToValueAtTime(3200, now + dur);

  gain.gain.setValueAtTime(0.01, now);
  gain.gain.linearRampToValueAtTime(volume * 0.5, now + 0.15);
  gain.gain.exponentialRampToValueAtTime(0.001, now + dur);

  osc.connect(filter);
  filter.connect(gain);
  gain.connect(destination);

  osc.start(now);
  osc.stop(now + dur);
};

export const playSuccessChime = (ctx: AudioContext, destination: AudioNode, volume = 0.6): void => {
  const notes = [523.25, 659.25, 783.99]; // C5, E5, G5 Major Triad
  const now = ctx.currentTime;

  notes.forEach((freq, idx) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    const noteStart = now + idx * 0.07;
    const noteDur = 0.28;

    osc.frequency.setValueAtTime(freq, noteStart);

    gain.gain.setValueAtTime(0.001, noteStart);
    gain.gain.linearRampToValueAtTime(volume * 0.4, noteStart + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.001, noteStart + noteDur);

    osc.connect(gain);
    gain.connect(destination);

    osc.start(noteStart);
    osc.stop(noteStart + noteDur);
  });
};

export const playWarningBeep = (ctx: AudioContext, destination: AudioNode, volume = 0.5): void => {
  const now = ctx.currentTime;
  const tones = [520, 420];

  tones.forEach((freq, idx) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'square';
    const start = now + idx * 0.1;
    const dur = 0.08;

    osc.frequency.setValueAtTime(freq, start);

    gain.gain.setValueAtTime(volume * 0.25, start);
    gain.gain.exponentialRampToValueAtTime(0.001, start + dur);

    osc.connect(gain);
    gain.connect(destination);

    osc.start(start);
    osc.stop(start + dur);
  });
};

export const playErrorBuzz = (ctx: AudioContext, destination: AudioNode, volume = 0.5): void => {
  const osc = ctx.createOscillator();
  const filter = ctx.createBiquadFilter();
  const gain = ctx.createGain();

  osc.type = 'sawtooth';
  filter.type = 'lowpass';
  filter.frequency.value = 600;

  const now = ctx.currentTime;
  const dur = 0.26;

  // Downward frequency drop buzz
  osc.frequency.setValueAtTime(220, now);
  osc.frequency.exponentialRampToValueAtTime(75, now + dur);

  gain.gain.setValueAtTime(volume * 0.5, now);
  gain.gain.exponentialRampToValueAtTime(0.001, now + dur);

  osc.connect(filter);
  filter.connect(gain);
  gain.connect(destination);

  osc.start(now);
  osc.stop(now + dur);
};

export const createAmbientDrone = (
  ctx: AudioContext,
  destination: AudioNode,
  volume = 0.18
): (() => void) => {
  const osc1 = ctx.createOscillator();
  const osc2 = ctx.createOscillator();
  const filter = ctx.createBiquadFilter();
  const gain = ctx.createGain();

  osc1.type = 'sine';
  osc1.frequency.value = 55; // A1 low sub-bass drone

  osc2.type = 'triangle';
  osc2.frequency.value = 110.5; // Slight detune for cyber acoustic chorusing

  filter.type = 'lowpass';
  filter.frequency.value = 240;

  gain.gain.setValueAtTime(0.001, ctx.currentTime);
  gain.gain.linearRampToValueAtTime(volume * 0.3, ctx.currentTime + 1.5); // Smooth 1.5s fade-in

  osc1.connect(filter);
  osc2.connect(filter);
  filter.connect(gain);
  gain.connect(destination);

  osc1.start();
  osc2.start();

  // Return clean disposal function
  return () => {
    try {
      const stopTime = ctx.currentTime + 0.8;
      gain.gain.linearRampToValueAtTime(0.001, stopTime);
      setTimeout(() => {
        try {
          osc1.stop();
          osc2.stop();
          osc1.disconnect();
          osc2.disconnect();
          filter.disconnect();
          gain.disconnect();
        } catch {
          // Already stopped
        }
      }, 850);
    } catch {
      // AudioContext closed
    }
  };
};
