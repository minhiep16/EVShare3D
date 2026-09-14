import React, { useRef } from 'react';
import { useTouchStore } from './useTouchStore';

const JOYSTICK_RADIUS = 50;

export const VirtualTouchJoystick: React.FC = () => {
  const isTouchDevice = useTouchStore((state) => state.isTouchDevice);
  const isVisible = useTouchStore((state) => state.isTouchControlsVisible);
  const joystick = useTouchStore((state) => state.joystickState);
  const updateJoystick = useTouchStore((state) => state.updateJoystick);
  const resetJoystick = useTouchStore((state) => state.resetJoystick);

  const containerRef = useRef<HTMLDivElement>(null);
  const touchIdRef = useRef<number | null>(null);

  if (!isTouchDevice && !isVisible) {
    return null;
  }

  const handleTouchStart = (e: React.TouchEvent<HTMLDivElement>) => {
    if (touchIdRef.current !== null) return;

    const touch = e.changedTouches[0];
    touchIdRef.current = touch.identifier;

    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const originX = rect.left + rect.width / 2;
    const originY = rect.top + rect.height / 2;

    const deltaX = touch.clientX - originX;
    const deltaY = touch.clientY - originY;
    const dist = Math.hypot(deltaX, deltaY);
    const clampedDist = Math.min(dist, JOYSTICK_RADIUS);
    const angle = Math.atan2(deltaY, deltaX);

    const normX = (clampedDist / JOYSTICK_RADIUS) * Math.cos(angle);
    const normY = -(clampedDist / JOYSTICK_RADIUS) * Math.sin(angle); // Invert Y so up is forward

    updateJoystick(
      [originX, originY],
      [originX + clampedDist * Math.cos(angle), originY + clampedDist * Math.sin(angle)],
      [normX, normY]
    );
  };

  const handleTouchMove = (e: React.TouchEvent<HTMLDivElement>) => {
    if (touchIdRef.current === null) return;

    for (let i = 0; i < e.changedTouches.length; i++) {
      const touch = e.changedTouches[i];
      if (touch.identifier === touchIdRef.current) {
        const rect = containerRef.current?.getBoundingClientRect();
        if (!rect) return;

        const originX = rect.left + rect.width / 2;
        const originY = rect.top + rect.height / 2;

        const deltaX = touch.clientX - originX;
        const deltaY = touch.clientY - originY;
        const dist = Math.hypot(deltaX, deltaY);
        const clampedDist = Math.min(dist, JOYSTICK_RADIUS);
        const angle = Math.atan2(deltaY, deltaX);

        const normX = (clampedDist / JOYSTICK_RADIUS) * Math.cos(angle);
        const normY = -(clampedDist / JOYSTICK_RADIUS) * Math.sin(angle);

        updateJoystick(
          [originX, originY],
          [originX + clampedDist * Math.cos(angle), originY + clampedDist * Math.sin(angle)],
          [normX, normY]
        );
        break;
      }
    }
  };

  const handleTouchEnd = (e: React.TouchEvent<HTMLDivElement>) => {
    if (touchIdRef.current === null) return;

    for (let i = 0; i < e.changedTouches.length; i++) {
      if (e.changedTouches[i].identifier === touchIdRef.current) {
        touchIdRef.current = null;
        resetJoystick();
        break;
      }
    }
  };

  const stickOffsetX = joystick.active ? joystick.vector[0] * JOYSTICK_RADIUS : 0;
  const stickOffsetY = joystick.active ? -joystick.vector[1] * JOYSTICK_RADIUS : 0;

  return (
    <div
      ref={containerRef}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onTouchCancel={handleTouchEnd}
      style={{
        position: 'absolute',
        bottom: '2.5rem',
        left: '2.5rem',
        width: `${JOYSTICK_RADIUS * 2 + 24}px`,
        height: `${JOYSTICK_RADIUS * 2 + 24}px`,
        borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(0, 229, 255, 0.12) 0%, rgba(10, 14, 26, 0.6) 80%)',
        border: '1.5px solid rgba(0, 229, 255, 0.4)',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.6), 0 0 16px rgba(0, 229, 255, 0.2)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        touchAction: 'none',
        userSelect: 'none',
        zIndex: 50,
        pointerEvents: 'auto',
      }}
    >
      {/* Inner crosshair reticle */}
      <div
        style={{
          position: 'absolute',
          width: '40px',
          height: '40px',
          border: '1px dashed rgba(0, 229, 255, 0.25)',
          borderRadius: '50%',
          pointerEvents: 'none',
        }}
      />

      {/* Dynamic Thumb Stick */}
      <div
        style={{
          width: '52px',
          height: '52px',
          borderRadius: '50%',
          background: joystick.active
            ? 'radial-gradient(circle, #00e5ff 0%, #0091ea 100%)'
            : 'radial-gradient(circle, rgba(0, 229, 255, 0.6) 0%, rgba(10, 14, 26, 0.8) 100%)',
          border: '2px solid #ffffff',
          boxShadow: joystick.active
            ? '0 0 20px #00e5ff, 0 4px 12px rgba(0,0,0,0.6)'
            : '0 0 10px rgba(0, 229, 255, 0.4)',
          transform: `translate(${stickOffsetX}px, ${stickOffsetY}px)`,
          transition: joystick.active ? 'none' : 'transform 0.2s cubic-bezier(0.18, 0.89, 0.32, 1.28)',
          pointerEvents: 'none',
        }}
      />
    </div>
  );
};
