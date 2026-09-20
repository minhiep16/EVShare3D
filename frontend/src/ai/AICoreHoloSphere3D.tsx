import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';

export const AICoreHoloSphere3D: React.FC = () => {
  const isAnalyzing = useAIStore((s) => s.isAnalyzing);
  const modelStatus = useAIStore((s) => s.modelStatus);
  const disclosureNotice = useAIStore((s) => s.disclosureNotice);

  const ring1Ref = useRef<THREE.Group>(null);
  const ring2Ref = useRef<THREE.Group>(null);
  const ring3Ref = useRef<THREE.Group>(null);
  const coreRef = useRef<THREE.Mesh>(null);

  useFrame((_, delta) => {
    const speedMult = isAnalyzing ? 3.5 : modelStatus === 'NOT_AVAILABLE' ? 0.6 : 1.0;

    if (ring1Ref.current) {
      ring1Ref.current.rotation.x += delta * 0.45 * speedMult;
      ring1Ref.current.rotation.y += delta * 0.2 * speedMult;
    }
    if (ring2Ref.current) {
      ring2Ref.current.rotation.y += delta * 0.6 * speedMult;
      ring2Ref.current.rotation.z += delta * 0.3 * speedMult;
    }
    if (ring3Ref.current) {
      ring3Ref.current.rotation.z += delta * 0.35 * speedMult;
      ring3Ref.current.rotation.x -= delta * 0.25 * speedMult;
    }
    if (coreRef.current) {
      const pulse = 1.0 + Math.sin(Date.now() * 0.003 * speedMult) * 0.12;
      coreRef.current.scale.set(pulse, pulse, pulse);
    }
  });

  return (
    <group name="AICoreHoloSphere" position={AI_LAYOUT.aiCorePosition}>
      {/* 1. Inner Synaptic Core Sphere */}
      <mesh ref={coreRef} castShadow>
        <icosahedronGeometry args={[0.75, 1]} />
        <meshStandardMaterial
          color={isAnalyzing ? '#f59e0b' : modelStatus === 'NOT_AVAILABLE' ? '#64748b' : '#8b5cf6'}
          emissive={isAnalyzing ? '#d97706' : modelStatus === 'NOT_AVAILABLE' ? '#334155' : '#6d28d9'}
          emissiveIntensity={isAnalyzing ? 2.5 : modelStatus === 'NOT_AVAILABLE' ? 0.8 : 1.4}
          roughness={0.2}
          metalness={0.8}
          wireframe={false}
        />
      </mesh>

      {/* Wireframe outer envelope */}
      <mesh scale={1.15}>
        <dodecahedronGeometry args={[0.78, 0]} />
        <meshBasicMaterial
          color={AI_LAYOUT.colors.cyanLight}
          wireframe
          transparent
          opacity={0.5}
        />
      </mesh>

      {/* 2. Gyroscopic Concentric Rings */}
      <group ref={ring1Ref}>
        <mesh>
          <torusGeometry args={[1.25, 0.025, 16, 48]} />
          <meshStandardMaterial
            color={AI_LAYOUT.colors.cyberCyan}
            emissive={AI_LAYOUT.colors.cyberCyan}
            emissiveIntensity={0.8}
            metalness={0.8}
          />
        </mesh>
      </group>

      <group ref={ring2Ref}>
        <mesh>
          <torusGeometry args={[1.5, 0.025, 16, 48]} />
          <meshStandardMaterial
            color={AI_LAYOUT.colors.neuralViolet}
            emissive={AI_LAYOUT.colors.neuralViolet}
            emissiveIntensity={0.8}
            metalness={0.8}
          />
        </mesh>
      </group>

      <group ref={ring3Ref}>
        <mesh>
          <torusGeometry args={[1.75, 0.02, 16, 48]} />
          <meshStandardMaterial
            color={AI_LAYOUT.colors.goldWarning}
            emissive={AI_LAYOUT.colors.goldWarning}
            emissiveIntensity={0.6}
            metalness={0.8}
          />
        </mesh>
      </group>

      {/* 3. Floating 3D Core Identity & Honest Disclosure Badge */}
      <group position={[0, 2.4, 0]}>
        {/* Plaque Background */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[3.6, 0.85]} />
          <meshBasicMaterial color="#03040c" opacity={0.88} transparent side={THREE.DoubleSide} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(3.6, 0.85)]} />
          <lineBasicMaterial color={AI_LAYOUT.colors.neuralViolet} />
        </lineSegments>

        {/* Title */}
        <Text
          position={[0, 0.24, 0.02]}
          fontSize={0.16}
          color={AI_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          LÕI TRÍ TUỆ NHÂN TẠO AI
        </Text>

        {/* Honest Disclosure Banner */}
        <Text
          position={[0, 0.02, 0.02]}
          fontSize={0.105}
          color={AI_LAYOUT.colors.goldWarning}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.04}
        >
          {disclosureNotice
            .replace('STATUS: NOT_AVAILABLE (AI API UNCONFIGURED) — ADVISORY HEURISTICS ACTIVE', 'TRẠNG THÁI: CHƯA KÍCH HOẠT (CHƯA CẤU HÌNH API AI) — MÔ HÌNH SUY LUẬN TƯ VẤN ĐANG HOẠT ĐỘNG')
            .replace('STATUS: NOT_AVAILABLE (NO DEDICATED AI SERVICE) — ADVISORY HEURISTICS ACTIVE', 'TRẠNG THÁI: CHƯA KÍCH HOẠT (KHÔNG CÓ DỊCH VỤ AI RIÊNG) — MÔ HÌNH SUY LUẬN TƯ VẤN ĐANG HOẠT ĐỘNG')
            .replace('STATUS: ONLINE', 'TRẠNG THÁI: TRỰC TUYẾN')
            .replace('ADVISORY ONLY', 'CHỈ MANG TÍNH TƯ VẤN')}
        </Text>

        {/* Safety Boundary Subtitle */}
        <Text
          position={[0, -0.22, 0.02]}
          fontSize={0.09}
          color={AI_LAYOUT.colors.cyanLight}
          anchorX="center"
          anchorY="middle"
        >
          CHỈ MANG TÍNH TƯ VẤN • KHÔNG TỰ ĐỘNG THANH TOÁN HAY KÝ HỢP ĐỒNG (BR-AI-SAFE-01)
        </Text>
      </group>
    </group>
  );
};
