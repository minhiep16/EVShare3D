import React, { useEffect, useRef } from 'react';
import { useFrame, useThree } from '@react-three/fiber';
import { PerspectiveCamera, OrbitControls } from '@react-three/drei';
import * as THREE from 'three';
import type { OrbitControls as OrbitControlsImpl } from 'three-stdlib';
import type { PerspectiveCamera as PerspectiveCameraImpl } from 'three';
import { useCameraStore } from '@/stores/useCameraStore';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { useEngineStore } from '../engineStore';

export const CameraManager: React.FC = () => {
  const cameraRef = useRef<PerspectiveCameraImpl>(null);
  const controlsRef = useRef<OrbitControlsImpl>(null);
  const { size } = useThree();

  // Temporary working vectors to avoid per-frame GC allocations
  const tempPos = useRef(new THREE.Vector3());
  const tempTarget = useRef(new THREE.Vector3());
  const desiredPosVec = useRef(new THREE.Vector3());
  const desiredTgtVec = useRef(new THREE.Vector3());

  // Store subscriptions
  const mode = useCameraStore((state) => state.mode);
  const desiredPosition = useCameraStore((state) => state.desiredPosition);
  const desiredTarget = useCameraStore((state) => state.desiredTarget);
  const desiredFov = useCameraStore((state) => state.desiredFov);
  const immediateSnap = useCameraStore((state) => state.immediateSnap);
  const isTransitioning = useCameraStore((state) => state.isTransitioning);
  const interpolationSpeed = useCameraStore((state) => state.interpolationSpeed);
  const updateRenderedTransform = useCameraStore((state) => state.updateRenderedTransform);

  // Cinematic parameters
  const cinematicKeyframes = useCameraStore((state) => state.cinematicKeyframes);
  const currentCinematicIndex = useCameraStore((state) => state.currentCinematicIndex);
  const stopCinematic = useCameraStore((state) => state.stopCinematic);

  // Player state for 1st / 3rd person
  const playerPosition = usePlayerStore((state) => state.position);
  const playerRotation = usePlayerStore((state) => state.rotation);

  // Synchronize canvas size to engine store
  const setDimensions = useEngineStore((state) => state.setDimensions);
  useEffect(() => {
    setDimensions(size.width, size.height);
  }, [size.width, size.height, setDimensions]);

  // Responsive FOV adjustment on resize
  useEffect(() => {
    if (cameraRef.current) {
      cameraRef.current.aspect = size.width / size.height;
      if (size.width < size.height) {
        cameraRef.current.fov = desiredFov * (size.height / size.width) * 0.7;
      } else {
        cameraRef.current.fov = desiredFov;
      }
      cameraRef.current.updateProjectionMatrix();
    }
  }, [size.width, size.height, desiredFov]);

  useFrame((_, delta) => {
    const camera = cameraRef.current;
    const controls = controlsRef.current;
    if (!camera) return;

    // 1. Immediate Hard Snap (Only when explicitly commanded)
    if (immediateSnap) {
      camera.position.set(...desiredPosition);
      if (controls) {
        controls.target.set(...desiredTarget);
        controls.update();
      }
      camera.fov = desiredFov;
      camera.updateProjectionMatrix();
      updateRenderedTransform(desiredPosition, desiredTarget, desiredFov, true);
      return;
    }

    // 2. Exponential smooth interpolation factor (Frame-rate independent)
    const factor = 1 - Math.exp(-interpolationSpeed * Math.min(delta, 0.1));

    // 3. FIRST PERSON CAMERA MODE
    if (mode === 'FIRST_PERSON') {
      if (controls) controls.enabled = false;

      // Eye position at player head level (+1.7 units)
      const eyeX = playerPosition[0];
      const eyeY = playerPosition[1] + 1.7;
      const eyeZ = playerPosition[2];

      // Forward look target based on player yaw
      const yaw = playerRotation[1];
      const lookX = eyeX - Math.sin(yaw) * 5;
      const lookY = eyeY;
      const lookZ = eyeZ - Math.cos(yaw) * 5;

      desiredPosVec.current.set(eyeX, eyeY, eyeZ);
      desiredTgtVec.current.set(lookX, lookY, lookZ);

      camera.position.lerp(desiredPosVec.current, factor);
      tempTarget.current.lerp(desiredTgtVec.current, factor);
      camera.lookAt(tempTarget.current);

      updateRenderedTransform(
        [camera.position.x, camera.position.y, camera.position.z],
        [tempTarget.current.x, tempTarget.current.y, tempTarget.current.z],
        camera.fov,
        true
      );
      return;
    }

    // 4. THIRD PERSON CHASE CAMERA MODE
    if (mode === 'THIRD_PERSON') {
      if (controls) controls.enabled = false;

      const chaseDistance = 4.5;
      const chaseHeight = 2.2;
      const yaw = playerRotation[1];

      // Calculate camera position trailing behind the player
      const chaseX = playerPosition[0] + Math.sin(yaw) * chaseDistance;
      const chaseY = playerPosition[1] + chaseHeight;
      const chaseZ = playerPosition[2] + Math.cos(yaw) * chaseDistance;

      // Look target at player upper torso (+1.3 units)
      const tgtX = playerPosition[0];
      const tgtY = playerPosition[1] + 1.3;
      const tgtZ = playerPosition[2];

      desiredPosVec.current.set(chaseX, chaseY, chaseZ);
      desiredTgtVec.current.set(tgtX, tgtY, tgtZ);

      camera.position.lerp(desiredPosVec.current, factor);
      tempTarget.current.lerp(desiredTgtVec.current, factor);
      camera.lookAt(tempTarget.current);

      updateRenderedTransform(
        [camera.position.x, camera.position.y, camera.position.z],
        [tempTarget.current.x, tempTarget.current.y, tempTarget.current.z],
        camera.fov,
        true
      );
      return;
    }

    // 5. CINEMATIC WAYPOINT SEQUENCER
    if (mode === 'CINEMATIC') {
      if (controls) controls.enabled = false;

      if (cinematicKeyframes.length > 0) {
        const kf = cinematicKeyframes[currentCinematicIndex];
        desiredPosVec.current.set(...kf.position);
        desiredTgtVec.current.set(...kf.target);

        camera.position.lerp(desiredPosVec.current, factor);
        tempTarget.current.lerp(desiredTgtVec.current, factor);
        camera.lookAt(tempTarget.current);

        const dist = camera.position.distanceTo(desiredPosVec.current);
        if (dist < 0.1) {
          if (currentCinematicIndex + 1 < cinematicKeyframes.length) {
            useCameraStore.setState({
              currentCinematicIndex: currentCinematicIndex + 1,
            });
          } else {
            // Sequence complete -> revert to ORBIT
            stopCinematic();
          }
        }
      }
      return;
    }

    // 6. ORBIT & INSPECTION MODES (With Smooth Transition Gliding)
    if (controls) {
      controls.enabled = true;

      if (isTransitioning) {
        desiredPosVec.current.set(...desiredPosition);
        desiredTgtVec.current.set(...desiredTarget);

        camera.position.lerp(desiredPosVec.current, factor);
        controls.target.lerp(desiredTgtVec.current, factor);
        controls.update();

        // Interpolate FOV smoothly
        if (Math.abs(camera.fov - desiredFov) > 0.05) {
          camera.fov = THREE.MathUtils.lerp(camera.fov, desiredFov, factor);
          camera.updateProjectionMatrix();
        }

        const posDist = camera.position.distanceTo(desiredPosVec.current);
        const tgtDist = controls.target.distanceTo(desiredTgtVec.current);

        if (posDist < 0.04 && tgtDist < 0.04) {
          camera.position.copy(desiredPosVec.current);
          controls.target.copy(desiredTgtVec.current);
          controls.update();
          updateRenderedTransform(desiredPosition, desiredTarget, desiredFov, true);
        } else {
          updateRenderedTransform(
            [camera.position.x, camera.position.y, camera.position.z],
            [controls.target.x, controls.target.y, controls.target.z],
            camera.fov,
            false
          );
        }
      }
    }
  });

  return (
    <>
      <PerspectiveCamera
        ref={cameraRef}
        makeDefault
        fov={desiredFov}
        near={0.1}
        far={1000}
        position={desiredPosition}
      />
      <OrbitControls
        ref={controlsRef}
        target={desiredTarget}
        enableDamping
        dampingFactor={0.05}
        minDistance={1.5}
        maxDistance={60}
        maxPolarAngle={Math.PI / 2 + 0.05}
        makeDefault
      />
    </>
  );
};
