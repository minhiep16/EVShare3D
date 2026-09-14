import React, { useEffect, useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group, Mesh } from 'three';
import { ThreeDButtonProps, VARIANT_PALETTES } from './ui3dTypes';
import { useVisualState } from './visualStates';
import { useInteractable } from '../raycast/useInteractable';
import { useSound } from '../audio/useSound';

export const ThreeDButton: React.FC<ThreeDButtonProps> = ({
  id,
  label,
  width = 1.2,
  height = 0.32,
  depth = 0.04,
  variant = 'cyan',
  disabled = false,
  loading = false,
  state: controlledState,
  ariaLabel,
  shortcutKey,
  isFocused = false,
  onClick,
  position = [0, 0, 0],
  rotation = [0, 0, 0],
}) => {
  const capRef = useRef<Group>(null);
  const spinnerRef = useRef<Mesh>(null);
  const focusRingRef = useRef<Mesh>(null);

  const [isHovered, setIsHovered] = useState(false);
  const [isPressed, setIsPressed] = useState(false);
  const sound = useSound();

  // Shared 8-State Visual State Engine
  const { state: effectiveState, params } = useVisualState({
    disabled,
    loading,
    controlledState,
    isPressed,
    isHovered,
    variant,
    enableAudio: false,
  });

  const isInteractive = params.isInteractive;
  const palette = VARIANT_PALETTES[variant];
  const primaryColor = params.primaryColor;
  const emissiveColor = params.emissiveColor;
  const emissiveIntensity = params.emissiveIntensity;

  // Register interactive raycasting with UI priority (120)
  useInteractable({
    id,
    ref: capRef,
    priority: 120,
    cursor: disabled ? 'NOT_ALLOWED' : loading ? 'WAIT' : 'POINTER',
    onHoverEnter: () => {
      if (isInteractive) {
        setIsHovered(true);
        sound.playHover();
      }
    },
    onHoverLeave: () => {
      setIsHovered(false);
      setIsPressed(false);
    },
    onPointerDown: () => {
      if (isInteractive) setIsPressed(true);
    },
    onPointerUp: () => {
      if (isInteractive) setIsPressed(false);
    },
    onClick: () => {
      if (isInteractive && onClick) {
        sound.playClick();
        onClick();
      }
    },
  });

  // Keyboard accessibility support (Enter / Space / ShortcutKey)
  useEffect(() => {
    if (!isInteractive) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Focus Enter/Space trigger
      if (isFocused && (e.key === 'Enter' || e.key === ' ')) {
        e.preventDefault();
        sound.playClick();
        onClick?.();
        return;
      }

      // Direct shortcut key
      if (shortcutKey && e.key.toLowerCase() === shortcutKey.toLowerCase()) {
        e.preventDefault();
        sound.playClick();
        onClick?.();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isInteractive, isFocused, shortcutKey, onClick, sound]);

  // Frame animation loop: physical Z-depression, hover levitation, loading rotation, and focus ring pulse
  useFrame((r3fState, delta) => {
    const dt = Math.min(delta, 0.1);
    const timeSec = r3fState.clock.elapsedTime;

    if (capRef.current) {
      // 1. Z-Position Depth Depression
      let targetZ = 0;
      if (effectiveState === 'ACTIVE') {
        targetZ = -0.018; // Physical press depression
      } else if (effectiveState === 'HOVER') {
        targetZ = 0.025; // Subtle hover levitation
      } else if (effectiveState === 'SUCCESS') {
        targetZ = 0.015;
      }

      capRef.current.position.z += (targetZ - capRef.current.position.z) * Math.min(1, dt * 22);

      // 2. Scale Pop / Shake
      let targetScale = 1.0;
      if (effectiveState === 'HOVER') targetScale = 1.04;
      if (effectiveState === 'SUCCESS') targetScale = 1.08;

      const currentS = capRef.current.scale.x;
      const nextS = currentS + (targetScale - currentS) * Math.min(1, dt * 18);
      capRef.current.scale.set(nextS, nextS, nextS);

      // Error horizontal shake
      if (effectiveState === 'ERROR') {
        capRef.current.position.x = Math.sin(timeSec * 35) * 0.02;
      } else {
        capRef.current.position.x += (0 - capRef.current.position.x) * Math.min(1, dt * 15);
      }
    }

    // 3. Loading Hologram Spinner Rotation
    if (spinnerRef.current && (effectiveState === 'LOADING' || loading)) {
      spinnerRef.current.rotation.z -= dt * 6.0;
    }

    // 4. Keyboard Focus Ring Glow Pulse
    if (focusRingRef.current && isFocused) {
      const pulseOpacity = 0.5 + Math.sin(timeSec * 6.0) * 0.4;
      const mat = focusRingRef.current.material as { opacity?: number };
      if (mat && 'opacity' in mat) {
        mat.opacity = pulseOpacity;
      }
    }
  });

  return (
    <group position={position} rotation={rotation}>
      {/* 1. Base Mounting Chassis Rim */}
      <mesh position={[0, 0, -depth / 2]}>
        <boxGeometry args={[width + 0.04, height + 0.04, depth]} />
        <meshStandardMaterial color="#080b10" metalness={0.9} roughness={0.3} />
      </mesh>

      {/* 2. Keyboard Focus Indicator Ring (Accessibility) */}
      {isFocused && (
        <mesh ref={focusRingRef} position={[0, 0, 0.02]}>
          <planeGeometry args={[width + 0.1, height + 0.1]} />
          <meshBasicMaterial
            color={palette.primary}
            wireframe
            transparent
            opacity={0.8}
          />
        </mesh>
      )}

      {/* 3. Interactive Pressable Button Cap */}
      <group ref={capRef}>
        {/* Button Face Slab */}
        <mesh castShadow receiveShadow>
          <boxGeometry args={[width, height, depth]} />
          <meshStandardMaterial
            color={
              effectiveState === 'DISABLED'
                ? '#12161f'
                : effectiveState === 'HOVER'
                ? palette.background
                : '#0a0e16'
            }
            emissive={emissiveColor}
            emissiveIntensity={emissiveIntensity}
            metalness={0.88}
            roughness={0.18}
            transparent
            opacity={effectiveState === 'DISABLED' ? 0.45 : 0.95}
          />
        </mesh>

        {/* Luminous Glowing Rim Stroke */}
        <mesh position={[0, 0, depth / 2 + 0.001]}>
          <planeGeometry args={[width, height]} />
          <meshBasicMaterial
            color={primaryColor}
            wireframe
            transparent
            opacity={effectiveState === 'DISABLED' ? 0.25 : isHovered ? 0.95 : 0.45}
          />
        </mesh>

        {/* Loading Spinner Ring */}
        {(effectiveState === 'LOADING' || loading) && (
          <mesh ref={spinnerRef} position={[0, 0, depth / 2 + 0.01]}>
            <torusGeometry args={[height * 0.25, 0.016, 16, 32, Math.PI * 1.5]} />
            <meshBasicMaterial color={palette.primary} />
          </mesh>
        )}

        {/* Signed Distance Field (SDF) 3D Text Label */}
        {!(effectiveState === 'LOADING' || loading) && (
          <Text
            position={[0, 0, depth / 2 + 0.008]}
            fontSize={height * 0.38}
            color={
              effectiveState === 'DISABLED'
                ? '#555e6d'
                : effectiveState === 'HOVER'
                ? '#ffffff'
                : primaryColor
            }
            anchorX="center"
            anchorY="middle"
            font="Orbitron"
            letterSpacing={0.1}
          >
            {label.toUpperCase()}
          </Text>
        )}

        {/* Optional Keyboard Shortcut Badge */}
        {shortcutKey && (
          <group position={[width / 2 - 0.12, -height / 2 + 0.08, depth / 2 + 0.01]}>
            <mesh>
              <planeGeometry args={[0.16, 0.12]} />
              <meshBasicMaterial color="#000000" transparent opacity={0.6} />
            </mesh>
            <Text
              position={[0, 0, 0.002]}
              fontSize={0.065}
              color="#8a94a6"
              anchorX="center"
              anchorY="middle"
              font="JetBrains Mono"
            >
              {shortcutKey.toUpperCase()}
            </Text>
          </group>
        )}

        {/* Accessible Description Tooltip (rendered in WebGL on hover) */}
        {ariaLabel && isHovered && (
          <group position={[0, height / 2 + 0.16, depth / 2 + 0.02]}>
            <mesh>
              <planeGeometry args={[Math.max(width * 0.9, ariaLabel.length * 0.055 + 0.2), 0.14]} />
              <meshBasicMaterial color="#06090e" transparent opacity={0.92} />
            </mesh>
            <mesh position={[0, 0, 0.001]}>
              <planeGeometry args={[Math.max(width * 0.9, ariaLabel.length * 0.055 + 0.2), 0.14]} />
              <meshBasicMaterial color={primaryColor} wireframe transparent opacity={0.4} />
            </mesh>
            <Text
              position={[0, 0, 0.003]}
              fontSize={0.062}
              color="#f0f4fc"
              anchorX="center"
              anchorY="middle"
              font="Space Grotesk"
            >
              {ariaLabel}
            </Text>
          </group>
        )}
      </group>
    </group>
  );
};
