/**
 * Pure mathematical easing and interpolation functions for 3D spatial animations.
 */

export const clamp01 = (val: number): number => Math.max(0, Math.min(1, val));

export const cubicEaseOut = (t: number): number => {
  const clamped = clamp01(t);
  const f = clamped - 1;
  return f * f * f + 1;
};

export const cubicEaseInOut = (t: number): number => {
  const clamped = clamp01(t);
  return clamped < 0.5
    ? 4 * clamped * clamped * clamped
    : (clamped - 1) * (2 * clamped - 2) * (2 * clamped - 2) + 1;
};

/**
 * Exponential frame-rate-independent spring damping towards target.
 */
export const springDamp = (
  current: number,
  target: number,
  lambda: number,
  dt: number
): number => {
  return target + (current - target) * Math.exp(-lambda * dt);
};

/**
 * Oscillating sine pulse between min and max.
 */
export const sinePulse = (
  timeSeconds: number,
  frequencyHz: number,
  min: number,
  max: number
): number => {
  const normalized = (Math.sin(timeSeconds * frequencyHz * Math.PI * 2) + 1) / 2;
  return min + normalized * (max - min);
};

/**
 * Decaying error shake offset. Returns strictly 0 once elapsedMs >= durationMs.
 */
export const computeShakeOffset = (
  elapsedMs: number,
  durationMs: number,
  amplitude: number = 0.08,
  cycles: number = 3
): number => {
  if (elapsedMs >= durationMs || durationMs <= 0) return 0;
  const t = elapsedMs / durationMs;
  const decay = 1 - cubicEaseOut(t);
  return Math.sin(t * cycles * Math.PI * 2) * amplitude * decay;
};

/**
 * Activation punch curve (1.0 -> 0.92 -> 1.08 -> 1.0)
 */
export const computeActivationScale = (elapsedMs: number, durationMs: number): number => {
  if (elapsedMs >= durationMs || durationMs <= 0) return 1.0;
  const t = elapsedMs / durationMs;
  if (t < 0.3) {
    // Compress
    return 1.0 - (0.08 * (t / 0.3));
  } else if (t < 0.7) {
    // Pop overshoot
    const progress = (t - 0.3) / 0.4;
    return 0.92 + (0.16 * cubicEaseOut(progress));
  } else {
    // Settle to 1.0
    const progress = (t - 0.7) / 0.3;
    return 1.08 - (0.08 * cubicEaseOut(progress));
  }
};
