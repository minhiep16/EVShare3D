import React, { useRef } from 'react';
import { useFrame, useThree } from '@react-three/fiber';
import * as THREE from 'three';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useInputStore } from '@/engine/input/useInputStore';
import { useWorldStore } from '@/stores/useWorldStore';
import { CollisionEngine } from './collisionEngine';
import { PlayerAvatar } from './PlayerAvatar';
import { PlayerPhysicsConfig } from './movementTypes';

interface PlayerControllerProps {
  config?: Partial<PlayerPhysicsConfig>;
}

const DEFAULT_PHYSICS: PlayerPhysicsConfig = {
  walkSpeed: 4.5,
  runSpeed: 8.5,
  acceleration: 15.0,
  damping: 10.0,
  turnSpeed: 10.0,
  avatarRadius: 0.45,
  avatarHeight: 1.8,
};

export const PlayerController: React.FC<PlayerControllerProps> = ({ config }) => {
  const mergedConfig = { ...DEFAULT_PHYSICS, ...config };
  const { camera } = useThree();

  // Internal working vectors to eliminate per-frame garbage collection
  const velocity = useRef(new THREE.Vector3());
  const inputDir = useRef(new THREE.Vector3());
  const camForward = useRef(new THREE.Vector3());
  const camRight = useRef(new THREE.Vector3());
  const currentYaw = useRef(0);

  // Store hooks
  const playerPosition = usePlayerStore((state) => state.position);
  const setPosition = usePlayerStore((state) => state.setPosition);
  const setRotation = usePlayerStore((state) => state.setRotation);
  const setVelocityStore = usePlayerStore((state) => state.setVelocity);
  const setMovementMode = usePlayerStore((state) => state.setMovementMode);
  const activeRoomBounds = useWorldStore((state) => state.activeRoomBounds);

  const movementInput = useInputStore((state) => state.movement);
  const touchInput = useInputStore((state) => state.touch);
  const isTypingMode = useInputStore((state) => state.isTypingMode);

  useFrame((_, delta) => {
    // If typing text into 3D terminal, disable movement
    if (isTypingMode) {
      velocity.current.set(0, 0, 0);
      setMovementMode('IDLE');
      return;
    }

    const dt = Math.min(delta, 0.1);

    // 1. Compute horizontal camera forward and right directions
    camera.getWorldDirection(camForward.current);
    camForward.current.y = 0;
    camForward.current.normalize();

    camRight.current.crossVectors(camForward.current, new THREE.Vector3(0, 1, 0)).normalize();

    // 2. Synthesize WASD and Touch input vectors
    inputDir.current.set(0, 0, 0);

    let fwd = movementInput.forward;
    let str = movementInput.strafe;

    // Incorporate Touch PAN gesture for mobile navigation
    if (touchInput.gesture === 'PAN' && touchInput.primaryTouch) {
      // Touch drag normalized direction
      fwd += 0.8;
    }

    if (fwd !== 0 || str !== 0) {
      inputDir.current
        .addScaledVector(camForward.current, fwd)
        .addScaledVector(camRight.current, str);

      if (inputDir.current.lengthSq() > 0.001) {
        inputDir.current.normalize();
      }
    }

    // 3. Smooth Acceleration and Damping
    const targetSpeed = movementInput.sprint ? mergedConfig.runSpeed : mergedConfig.walkSpeed;
    const isMoving = inputDir.current.lengthSq() > 0.001;

    if (isMoving) {
      const targetVelX = inputDir.current.x * targetSpeed;
      const targetVelZ = inputDir.current.z * targetSpeed;

      velocity.current.x = THREE.MathUtils.damp(
        velocity.current.x,
        targetVelX,
        mergedConfig.acceleration,
        dt
      );
      velocity.current.z = THREE.MathUtils.damp(
        velocity.current.z,
        targetVelZ,
        mergedConfig.acceleration,
        dt
      );

      // Smoothly rotate avatar towards movement direction
      const targetAngle = Math.atan2(inputDir.current.x, inputDir.current.z);
      // Smooth angle interpolation
      let diff = targetAngle - currentYaw.current;
      while (diff < -Math.PI) diff += Math.PI * 2;
      while (diff > Math.PI) diff -= Math.PI * 2;
      currentYaw.current += diff * Math.min(1, mergedConfig.turnSpeed * dt);
    } else {
      // Smooth friction deceleration to dead stop
      velocity.current.x = THREE.MathUtils.damp(
        velocity.current.x,
        0,
        mergedConfig.damping,
        dt
      );
      velocity.current.z = THREE.MathUtils.damp(
        velocity.current.z,
        0,
        mergedConfig.damping,
        dt
      );
    }

    // 4. Candidate position before collision resolution
    const candidateX = playerPosition[0] + velocity.current.x * dt;
    const candidateY = playerPosition[1];
    const candidateZ = playerPosition[2] + velocity.current.z * dt;

    // 5. Resolve Collisions and Room Boundaries
    const [resolvedX, resolvedY, resolvedZ] = CollisionEngine.resolvePosition(
      [candidateX, candidateY, candidateZ],
      mergedConfig.avatarRadius,
      activeRoomBounds
    );

    // 6. Update Player Store
    const currentSpeed = Math.sqrt(
      velocity.current.x * velocity.current.x + velocity.current.z * velocity.current.z
    );

    setPosition([resolvedX, resolvedY, resolvedZ]);
    setVelocityStore([velocity.current.x, 0, velocity.current.z]);
    setRotation([0, currentYaw.current, 0]);

    if (currentSpeed > 0.2) {
      setMovementMode(movementInput.sprint ? 'SPRINTING' : 'WALKING');
    } else {
      setMovementMode('IDLE');
    }
  });

  return (
    <group name="PlayerController">
      <PlayerAvatar />
    </group>
  );
};
