import React, { useEffect, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group } from 'three';
import { Modal3DProps, VARIANT_PALETTES } from './ui3dTypes';
import { Button3D } from './Button3D';
import { useInteractable } from '../raycast/useInteractable';
import { InputDispatcher } from '../input/inputDispatcher';
import { useSound } from '../audio/useSound';

export const Modal3D: React.FC<Modal3DProps> = ({
  id,
  title,
  isOpen,
  onClose,
  width = 2.4,
  height = 1.6,
  variant = 'cyan',
  position = [0, 1.6, 0],
  children,
}) => {
  const modalRef = useRef<Group>(null);
  const backdropRef = useRef<Group>(null);

  const sound = useSound();
  const palette = VARIANT_PALETTES[variant];

  // Backdrop click blocker with priority (150)
  useInteractable({
    id: `${id}_backdrop`,
    ref: backdropRef,
    priority: 150,
    cursor: 'POINTER',
    onClick: () => {
      sound.playClick();
      onClose();
    },
  });

  // Smooth pop scale animation (0.7 -> 1.0)
  useFrame((_, delta) => {
    if (!modalRef.current) return;
    const targetScale = isOpen ? 1.0 : 0.001;
    const currentS = modalRef.current.scale.x;
    const nextS = currentS + (targetScale - currentS) * Math.min(1, delta * 18);
    modalRef.current.scale.set(nextS, nextS, nextS);
  });

  // Listen to Escape action
  useEffect(() => {
    if (!isOpen) return;

    const unregister = InputDispatcher.onAction('CANCEL', () => {
      onClose();
    });

    return () => {
      unregister();
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <group position={position}>
      {/* 1. Spatial Dimming Backdrop Plane (dims surrounding 3D world) */}
      <group ref={backdropRef} position={[0, 0, -0.1]}>
        <mesh>
          <planeGeometry args={[40, 40]} />
          <meshBasicMaterial color="#020408" transparent opacity={0.65} />
        </mesh>
      </group>

      {/* 2. Elevated Foreground 3D Modal Box */}
      <group ref={modalRef} scale={[0.8, 0.8, 0.8]}>
        {/* Panel Shell */}
        <mesh castShadow>
          <boxGeometry args={[width, height, 0.06]} />
          <meshStandardMaterial
            color={palette.background}
            metalness={0.88}
            roughness={0.15}
            transparent
            opacity={0.96}
          />
        </mesh>

        {/* Outer Glowing Cyber Rim */}
        <mesh position={[0, 0, 0.032]}>
          <planeGeometry args={[width + 0.02, height + 0.02]} />
          <meshBasicMaterial color={palette.primary} wireframe transparent opacity={0.65} />
        </mesh>

        {/* Top Header Bar */}
        <group position={[0, height / 2 - 0.14, 0.035]}>
          <mesh>
            <planeGeometry args={[width - 0.08, 0.18]} />
            <meshBasicMaterial color={palette.primary} transparent opacity={0.15} />
          </mesh>
          <Text
            position={[-width / 2 + 0.14, 0, 0.002]}
            fontSize={0.095}
            color={palette.primary}
            anchorX="left"
            anchorY="middle"
            letterSpacing={0.12}
          >
            {title.toUpperCase()}
          </Text>

          {/* Close Button 'X' */}
          <Button3D
            id={`${id}_close_btn`}
            label="X"
            width={0.28}
            height={0.14}
            variant="crimson"
            position={[width / 2 - 0.22, 0, 0.005]}
            onClick={onClose}
          />
        </group>

        {/* Modal Interior 3D Content */}
        <group position={[0, -0.08, 0.038]}>{children}</group>
      </group>
    </group>
  );
};
