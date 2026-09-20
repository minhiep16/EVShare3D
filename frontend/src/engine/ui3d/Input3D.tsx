import React, { useEffect, useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group } from 'three';
import { Input3DProps, VARIANT_PALETTES } from './ui3dTypes';
import { useInteractable } from '../raycast/useInteractable';
import { useInputStore } from '../input/useInputStore';
import { useInteractionStore } from '@/stores/useInteractionStore';
import { useSound } from '../audio/useSound';

export const Input3D: React.FC<Input3DProps> = ({
  id,
  value,
  onChange,
  onSubmit,
  placeholder = 'NHẬP TẠI ĐÂY...',
  label,
  width = 1.6,
  height = 0.28,
  isPassword = false,
  maxLength = 32,
  position = [0, 0, 0],
}) => {
  const meshRef = useRef<Group>(null);
  const [isFocused, setIsFocused] = useState(false);
  const [cursorVisible, setCursorVisible] = useState(true);

  const sound = useSound();
  const palette = VARIANT_PALETTES.cyan;

  // Register interactive raycasting with UI priority (110)
  useInteractable({
    id,
    ref: meshRef,
    priority: 110,
    cursor: 'TEXT',
    onClick: () => {
      setIsFocused(true);
      useInteractionStore.getState().setFocusedInputId(id);
      useInputStore.getState().setTypingMode(true);
      sound.playClick();
    },
  });

  // Cursor blink cycle (2Hz)
  useFrame((r3fState) => {
    if (isFocused) {
      setCursorVisible(Math.floor(r3fState.clock.elapsedTime * 2.5) % 2 === 0);
    }
  });

  // Global keyboard listener when input is active
  useEffect(() => {
    if (!isFocused) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsFocused(false);
        useInteractionStore.getState().setFocusedInputId(null);
        useInputStore.getState().setTypingMode(false);
        return;
      }

      if (e.key === 'Enter') {
        onSubmit?.(value);
        sound.playSuccess();
        return;
      }

      if (e.key === 'Backspace') {
        e.preventDefault();
        onChange(value.slice(0, -1));
        sound.playHover();
        return;
      }

      // Single printable character
      if (e.key.length === 1 && !e.ctrlKey && !e.altKey && !e.metaKey) {
        if (value.length < maxLength) {
          e.preventDefault();
          onChange(value + e.key);
          sound.playHover();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isFocused, value, onChange, onSubmit, maxLength, sound]);

  // Unfocus when clicking elsewhere
  useEffect(() => {
    const unsub = useInteractionStore.subscribe((state) => {
      if (state.focusedInputId !== id && isFocused) {
        setIsFocused(false);
        useInputStore.getState().setTypingMode(false);
      }
    });

    return () => {
      unsub();
    };
  }, [id, isFocused]);

  const displayString = isPassword ? '*'.repeat(value.length) : value;

  return (
    <group position={position}>
      {/* Optional Top Label */}
      {label && (
        <Text
          position={[-width / 2, height / 2 + 0.08, 0.01]}
          fontSize={0.07}
          color="#8a94a6"
          anchorX="left"
          anchorY="middle"
          letterSpacing={0.05}
        >
          {label.toUpperCase()}
        </Text>
      )}

      <group ref={meshRef}>
        {/* Background Box */}
        <mesh>
          <boxGeometry args={[width, height, 0.02]} />
          <meshStandardMaterial
            color={isFocused ? '#091522' : '#080c12'}
            metalness={0.8}
            roughness={0.2}
          />
        </mesh>

        {/* Outline Frame (cyan glowing when focused) */}
        <mesh position={[0, 0, 0.012]}>
          <planeGeometry args={[width, height]} />
          <meshBasicMaterial
            color={isFocused ? palette.primary : '#303846'}
            wireframe
            transparent
            opacity={isFocused ? 0.9 : 0.4}
          />
        </mesh>

        {/* 3D Rendered Text Content */}
        <Text
          position={[-width / 2 + 0.06, 0, 0.015]}
          fontSize={height * 0.42}
          color={value ? '#ffffff' : '#556072'}
          anchorX="left"
          anchorY="middle"
          font="JetBrains Mono"
          letterSpacing={0.05}
        >
          {value ? displayString : placeholder}
        </Text>

        {/* Blinking 3D Cursor Bar */}
        {isFocused && cursorVisible && (
          <mesh position={[-width / 2 + 0.06 + displayString.length * 0.068, 0, 0.018]}>
            <planeGeometry args={[0.018, height * 0.55]} />
            <meshBasicMaterial color={palette.primary} />
          </mesh>
        )}
      </group>
    </group>
  );
};
