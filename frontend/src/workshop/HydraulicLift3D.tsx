import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useWorkshopStore } from './useWorkshopStore';
import { WORKSHOP_THEME } from './workshopLayout';
import { VehicleConditionDisplay3D } from './VehicleConditionDisplay3D';

export const HydraulicLift3D: React.FC = () => {
  const liftGroupRef = useRef<THREE.Group>(null);
  const currentHeightRef = useRef<number>(0.2);

  const {
    liftHeight,
    isElevated,
    elevateLift,
    lowerLift,
    workOrder,
  } = useWorkshopStore();

  const [hoverUp, setHoverUp] = useState(false);
  const [hoverDown, setHoverDown] = useState(false);

  // Smooth lerp animation for lift elevation
  useFrame((_, delta) => {
    currentHeightRef.current = THREE.MathUtils.damp(
      currentHeightRef.current,
      liftHeight,
      4.0,
      delta
    );
    if (liftGroupRef.current) {
      liftGroupRef.current.position.y = currentHeightRef.current;
    }
  });

  return (
    <group name="HydraulicLift">
      {/* 1. Static Dual Hydraulic Columns (x = -1.8 and x = +1.8) */}
      {[-1.8, 1.8].map((x) => (
        <group key={`col-${x}`} position={[x, 0, 0]}>
          {/* Base plate */}
          <mesh position={[0, 0.08, 0]} castShadow>
            <boxGeometry args={[0.8, 0.16, 0.9]} />
            <meshStandardMaterial color="#18181b" metalness={0.8} roughness={0.3} />
          </mesh>
          {/* Main vertical column */}
          <mesh position={[0, 1.8, 0]} castShadow>
            <boxGeometry args={[0.42, 3.6, 0.42]} />
            <meshStandardMaterial color="#27272a" metalness={0.7} roughness={0.3} />
          </mesh>
          {/* Inner hydraulic cylinder piston track */}
          <mesh position={[x > 0 ? -0.18 : 0.18, 1.8, 0]}>
            <cylinderGeometry args={[0.08, 0.08, 3.4, 16]} />
            <meshStandardMaterial
              color="#e4e4e7"
              metalness={0.95}
              roughness={0.1}
            />
          </mesh>
          {/* Top safety cap */}
          <mesh position={[0, 3.68, 0]}>
            <boxGeometry args={[0.48, 0.16, 0.48]} />
            <meshStandardMaterial
              color={WORKSHOP_THEME.secondary}
              emissive={WORKSHOP_THEME.secondary}
              emissiveIntensity={0.5}
            />
          </mesh>
        </group>
      ))}

      {/* 2. Top Crossbeam between columns */}
      <mesh position={[0, 3.65, 0]}>
        <boxGeometry args={[3.8, 0.2, 0.3]} />
        <meshStandardMaterial color="#18181b" metalness={0.8} roughness={0.3} />
      </mesh>

      {/* 3. Moving Platform & Vehicle Group (Height controlled by currentHeightRef) */}
      <group ref={liftGroupRef} position={[0, 0.2, 0]}>
        {/* Carriage sleeves sliding along columns */}
        {[-1.8, 1.8].map((x) => (
          <group key={`carr-${x}`} position={[x, 0, 0]}>
            <mesh position={[0, 0, 0]}>
              <boxGeometry args={[0.5, 0.45, 0.5]} />
              <meshStandardMaterial color={WORKSHOP_THEME.secondary} />
            </mesh>
            {/* Telescoping swing arms extending to vehicle lift pads */}
            <mesh position={[x > 0 ? -0.55 : 0.55, -0.05, -1.0]}>
              <boxGeometry args={[0.9, 0.08, 0.12]} />
              <meshStandardMaterial color="#3f3f46" metalness={0.8} />
            </mesh>
            <mesh position={[x > 0 ? -0.55 : 0.55, -0.05, 1.0]}>
              <boxGeometry args={[0.9, 0.08, 0.12]} />
              <meshStandardMaterial color="#3f3f46" metalness={0.8} />
            </mesh>
          </group>
        ))}

        {/* Heavy Lift Pad Crossbars */}
        <mesh position={[0, -0.05, -1.0]}>
          <boxGeometry args={[2.8, 0.08, 0.22]} />
          <meshStandardMaterial color="#27272a" metalness={0.8} roughness={0.3} />
        </mesh>
        <mesh position={[0, -0.05, 1.0]}>
          <boxGeometry args={[2.8, 0.08, 0.22]} />
          <meshStandardMaterial color="#27272a" metalness={0.8} roughness={0.3} />
        </mesh>

        {/* 4. Digital Twin EV Vehicle Model (VinFast VF6 Eco) */}
        <group position={[0, 0.45, 0]}>
          {/* Main Lower Chassis Body */}
          <mesh castShadow receiveShadow>
            <boxGeometry args={[2.1, 0.65, 4.3]} />
            <meshStandardMaterial
              color="#0284c7" // VinFast Oceanic Blue
              metalness={0.8}
              roughness={0.25}
            />
          </mesh>

          {/* Cabin Glass / Roof */}
          <mesh position={[0, 0.55, -0.2]}>
            <boxGeometry args={[1.7, 0.52, 2.4]} />
            <meshStandardMaterial
              color="#09090b"
              metalness={0.9}
              roughness={0.1}
              transparent
              opacity={0.85}
            />
          </mesh>

          {/* 4 Wheels */}
          {[
            [-0.98, -0.15, -1.3],
            [0.98, -0.15, -1.3],
            [-0.98, -0.15, 1.3],
            [0.98, -0.15, 1.3],
          ].map((wPos, idx) => (
            <mesh
              key={`wheel-${idx}`}
              position={wPos as [number, number, number]}
              rotation={[0, 0, Math.PI / 2]}
            >
              <cylinderGeometry args={[0.34, 0.34, 0.22, 24]} />
              <meshStandardMaterial color="#18181b" roughness={0.7} />
            </mesh>
          ))}

          {/* Front Grille Plate */}
          <mesh position={[0, 0.05, -2.16]}>
            <planeGeometry args={[1.5, 0.25]} />
            <meshBasicMaterial color="#000000" />
          </mesh>
          <Text
            position={[0, 0.05, -2.17]}
            rotation={[0, Math.PI, 0]}
            fontSize={0.08}
            color="#ffffff"
            anchorX="center"
            anchorY="middle"
          >
            VF6 • {workOrder.licensePlate}
          </Text>

          {/* 5. Subsystem Condition Diagnostic Markers (Attached to vehicle coordinates) */}
          <VehicleConditionDisplay3D />
        </group>
      </group>

      {/* 6. Hydraulic Lift Control Pedestal at [-2.2, 0, 1.8] */}
      <group position={[-2.2, 0, 1.8]}>
        {/* Pedestal Stand */}
        <mesh position={[0, 0.55, 0]} castShadow>
          <cylinderGeometry args={[0.22, 0.28, 1.1, 16]} />
          <meshStandardMaterial color="#18181b" metalness={0.7} roughness={0.3} />
        </mesh>
        <mesh position={[0, 0.05, 0]}>
          <cylinderGeometry args={[0.32, 0.35, 0.1, 16]} />
          <meshStandardMaterial color={WORKSHOP_THEME.secondary} />
        </mesh>

        {/* Angled Control Box Top */}
        <group position={[0, 1.15, 0]} rotation={[-Math.PI * 0.15, 0, 0]}>
          <mesh position={[0, 0, 0]}>
            <boxGeometry args={[0.85, 0.65, 0.1]} />
            <meshStandardMaterial color="#09090b" metalness={0.8} roughness={0.2} />
          </mesh>
          <lineSegments position={[0, 0, 0.055]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(0.85, 0.65)]} />
            <lineBasicMaterial color={WORKSHOP_THEME.secondary} />
          </lineSegments>

          {/* Header */}
          <Text
            position={[0, 0.22, 0.06]}
            fontSize={0.055}
            color={WORKSHOP_THEME.secondary}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.05}
          >
            HYDRAULIC LIFT CONTROLLER
          </Text>
          <Text
            position={[0, 0.12, 0.06]}
            fontSize={0.045}
            color="#94a3b8"
            anchorX="center"
            anchorY="middle"
          >
            HEIGHT: {liftHeight.toFixed(2)}M • 220 BAR
          </Text>

          {/* Button 1: Elevate */}
          <group
            position={[0, -0.02, 0.06]}
            onClick={(e) => {
              e.stopPropagation();
              elevateLift();
            }}
            onPointerOver={(e) => {
              e.stopPropagation();
              setHoverUp(true);
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoverUp(false);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh>
              <boxGeometry args={[0.65, 0.16, 0.04]} />
              <meshStandardMaterial
                color={hoverUp ? '#0284c7' : isElevated ? '#0369a1' : '#1e293b'}
                emissive={hoverUp || isElevated ? WORKSHOP_THEME.primary : '#000000'}
                emissiveIntensity={hoverUp ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.025]}
              fontSize={0.05}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              ▲ ELEVATE (1.80M)
            </Text>
          </group>

          {/* Button 2: Lower */}
          <group
            position={[0, -0.22, 0.06]}
            onClick={(e) => {
              e.stopPropagation();
              lowerLift();
            }}
            onPointerOver={(e) => {
              e.stopPropagation();
              setHoverDown(true);
              document.body.style.cursor = 'pointer';
            }}
            onPointerOut={() => {
              setHoverDown(false);
              document.body.style.cursor = 'auto';
            }}
          >
            <mesh>
              <boxGeometry args={[0.65, 0.16, 0.04]} />
              <meshStandardMaterial
                color={hoverDown ? '#d97706' : !isElevated ? '#78350f' : '#1e293b'}
                emissive={hoverDown || !isElevated ? WORKSHOP_THEME.secondary : '#000000'}
                emissiveIntensity={hoverDown ? 0.6 : 0.2}
              />
            </mesh>
            <Text
              position={[0, 0, 0.025]}
              fontSize={0.05}
              color="#ffffff"
              anchorX="center"
              anchorY="middle"
            >
              ▼ LOWER (0.20M)
            </Text>
          </group>
        </group>
      </group>
    </group>
  );
};
