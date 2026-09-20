import React from 'react';
import { Text } from '@react-three/drei';
import { AI_LAYOUT } from './aiLayout';

export const AIChamberFloor3D: React.FC = () => {
  const pylonCount = 8;
  const pylonRadius = 10.2;

  return (
    <group name="AIChamberFloor">
      {/* 1. Base Neural Floor Disc */}
      <mesh receiveShadow position={[0, -0.15, 0]}>
        <cylinderGeometry args={[11.5, 12, 0.3, 48]} />
        <meshStandardMaterial
          color={AI_LAYOUT.colors.arenaDark}
          roughness={0.55}
          metalness={0.45}
        />
      </mesh>

      {/* 2. Concentric Neon Cybernetic Circuit Rings */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]}>
        <ringGeometry args={[9.8, 9.95, 48]} />
        <meshBasicMaterial color={AI_LAYOUT.colors.neuralPurple} opacity={0.6} transparent />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.015, 0]}>
        <ringGeometry args={[6.8, 6.9, 48]} />
        <meshBasicMaterial color={AI_LAYOUT.colors.cyberCyan} opacity={0.5} transparent />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
        <ringGeometry args={[3.8, 3.9, 48]} />
        <meshBasicMaterial color={AI_LAYOUT.colors.neuralViolet} opacity={0.7} transparent />
      </mesh>

      {/* 3. Central Core Dais Platform */}
      <mesh receiveShadow position={[0, 0.1, 0]}>
        <cylinderGeometry args={[2.8, 3.2, 0.2, 36]} />
        <meshStandardMaterial
          color="#0f1123"
          roughness={0.3}
          metalness={0.7}
        />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.21, 0]}>
        <ringGeometry args={[2.5, 2.7, 36]} />
        <meshBasicMaterial color={AI_LAYOUT.colors.cyberCyan} opacity={0.8} transparent />
      </mesh>

      {/* 4. Peripheral Neural Pylons with Laser Beacons */}
      {Array.from({ length: pylonCount }).map((_, i) => {
        const angle = (i / pylonCount) * Math.PI * 2;
        const px = Math.cos(angle) * pylonRadius;
        const pz = Math.sin(angle) * pylonRadius;

        return (
          <group key={`ai-pylon-${i}`} position={[px, 0, pz]}>
            {/* Column body */}
            <mesh castShadow receiveShadow position={[0, 2.6, 0]}>
              <cylinderGeometry args={[0.2, 0.28, 5.2, 16]} />
              <meshStandardMaterial color="#1a1533" roughness={0.3} metalness={0.8} />
            </mesh>

            {/* Glowing crystal crown */}
            <mesh position={[0, 5.4, 0]}>
              <octahedronGeometry args={[0.28, 0]} />
              <meshStandardMaterial
                color={AI_LAYOUT.colors.neuralViolet}
                emissive={AI_LAYOUT.colors.neuralPurple}
                emissiveIntensity={1.8}
                roughness={0.1}
                metalness={0.9}
              />
            </mesh>

            {/* Base collar */}
            <mesh position={[0, 0.2, 0]}>
              <cylinderGeometry args={[0.4, 0.5, 0.4, 16]} />
              <meshStandardMaterial color="#0c0919" roughness={0.7} metalness={0.4} />
            </mesh>
          </group>
        );
      })}

      {/* 5. Inscribed Floor Labels in 3D */}
      <group position={[0, 0.05, 8.2]} rotation={[-Math.PI / 2, 0, 0]}>
        <Text
          fontSize={0.42}
          color={AI_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.12}
        >
          TRUNG TÂM TRÍ TUỆ NHÂN TẠO AI
        </Text>
        <Text
          position={[0, -0.45, 0]}
          fontSize={0.17}
          color={AI_LAYOUT.colors.neuralViolet}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          MẠNG DỰ BÁO DI CHUYỂN THÔNG MINH • CHỈ MANG TÍNH TƯ VẤN (BR-AI-SAFE-01)
        </Text>
      </group>
    </group>
  );
};
