import React from 'react';
import { Text } from '@react-three/drei';
import { DECISION_LAYOUT } from './decisionLayout';

interface DecisionChamberFloor3DProps {
  onStationSelect?: (station: any) => void;
}

export const DecisionChamberFloor3D: React.FC<DecisionChamberFloor3DProps> = () => {
  const pylonCount = 8;
  const pylonRadius = 10.5;

  return (
    <group name="DecisionChamberFloor">
      {/* 1. Base Arena Disc */}
      <mesh receiveShadow position={[0, -0.15, 0]}>
        <cylinderGeometry args={[11.5, 12, 0.3, 48]} />
        <meshStandardMaterial
          color={DECISION_LAYOUT.colors.arenaDark}
          roughness={0.65}
          metalness={0.35}
        />
      </mesh>

      {/* 2. Concentric Neon Parliamentary Floor Rings */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]}>
        <ringGeometry args={[10.2, 10.35, 48]} />
        <meshBasicMaterial color={DECISION_LAYOUT.colors.indigoLight} opacity={0.6} transparent />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.015, 0]}>
        <ringGeometry args={[7.2, 7.3, 48]} />
        <meshBasicMaterial color={DECISION_LAYOUT.colors.violetNeon} opacity={0.5} transparent />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[4.2, 4.3, 48]} />
        <meshBasicMaterial color={DECISION_LAYOUT.colors.cyanQuorum} opacity={0.6} transparent />
      </mesh>

      {/* 3. Central Semicircular Dais */}
      <mesh receiveShadow position={[0, 0.1, -1.5]}>
        <cylinderGeometry args={[6.5, 6.8, 0.2, 36, 1, false, 0, Math.PI]} />
        <meshStandardMaterial
          color="#0f172a"
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>

      {/* 4. Peripheral Architectural Pylons */}
      {Array.from({ length: pylonCount }).map((_, i) => {
        const angle = (i / pylonCount) * Math.PI * 2;
        const px = Math.cos(angle) * pylonRadius;
        const pz = Math.sin(angle) * pylonRadius;

        return (
          <group key={`pylon-${i}`} position={[px, 0, pz]}>
            {/* Pylon column */}
            <mesh castShadow receiveShadow position={[0, 2.8, 0]}>
              <cylinderGeometry args={[0.22, 0.32, 5.6, 16]} />
              <meshStandardMaterial
                color="#1e1b4b"
                roughness={0.3}
                metalness={0.8}
              />
            </mesh>

            {/* Glowing violet crystal crown */}
            <mesh position={[0, 5.8, 0]}>
              <octahedronGeometry args={[0.3, 0]} />
              <meshStandardMaterial
                color={DECISION_LAYOUT.colors.violetGlow}
                emissive={DECISION_LAYOUT.colors.violetNeon}
                emissiveIntensity={1.8}
                roughness={0.1}
                metalness={0.9}
              />
            </mesh>

            {/* Base collar */}
            <mesh position={[0, 0.25, 0]}>
              <cylinderGeometry args={[0.45, 0.55, 0.5, 16]} />
              <meshStandardMaterial color="#090d16" roughness={0.7} metalness={0.4} />
            </mesh>
          </group>
        );
      })}

      {/* 5. Sector Floor Labels in 3D */}
      <group position={[0, 0.05, 8.2]} rotation={[-Math.PI / 2, 0, 0]}>
        <Text
          fontSize={0.42}
          color={DECISION_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.12}
        >
          DECISION CHAMBER
        </Text>
        <Text
          position={[0, -0.45, 0]}
          fontSize={0.2}
          color={DECISION_LAYOUT.colors.indigoLight}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          PARLIAMENTARY GOVERNANCE & DETERMINISTIC EQUITY VOTING
        </Text>
      </group>
    </group>
  );
};
