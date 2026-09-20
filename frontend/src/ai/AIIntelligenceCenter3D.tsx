import React, { useEffect, useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';
import { AIChamberFloor3D } from './AIChamberFloor3D';
import { AICoreHoloSphere3D } from './AICoreHoloSphere3D';
import { AIDataNodes3D } from './AIDataNodes3D';
import { AIRecommendationHolograms3D } from './AIRecommendationHolograms3D';
import { AIResultVisualization3D } from './AIResultVisualization3D';
import { AITerminal3D } from './AITerminal3D';
import { useCameraStore } from '@/stores/useCameraStore';

interface AIIntelligenceCenter3DProps {
  position?: [number, number, number];
}

export const AIIntelligenceCenter3D: React.FC<AIIntelligenceCenter3DProps> = ({
  position = [0, 0, 0],
}) => {
  const fetchAIData = useAIStore((s) => s.fetchAIData);

  useEffect(() => {
    fetchAIData();
  }, [fetchAIData]);

  const cameraButtons = [
    { key: 'NEXUS_OVERVIEW', label: 'TỔNG QUAN', preset: AI_LAYOUT.cameraPresets.NEXUS_OVERVIEW },
    { key: 'CORE_FOCUS', label: 'LÕI TRÍ TUỆ AI', preset: AI_LAYOUT.cameraPresets.CORE_FOCUS },
    { key: 'TERMINAL_FOCUS', label: 'BÀN ĐIỀU KHIỂN', preset: AI_LAYOUT.cameraPresets.TERMINAL_FOCUS },
    { key: 'HOLOGRAPHIC_RECOMMENDATIONS', label: 'KHUYẾN NGHỊ AI', preset: AI_LAYOUT.cameraPresets.HOLOGRAPHIC_RECOMMENDATIONS },
    { key: 'DATA_NODES_FOCUS', label: 'NÚT DỮ LIỆU', preset: AI_LAYOUT.cameraPresets.DATA_NODES_FOCUS },
    { key: 'SAFETY_AUDIT_FOCUS', label: 'KHÓA AN TOÀN', preset: AI_LAYOUT.cameraPresets.SAFETY_AUDIT_FOCUS },
  ];

  const handleCameraChange = (preset: typeof AI_LAYOUT.cameraPresets.NEXUS_OVERVIEW) => {
    const targetPos: [number, number, number] = [
      preset.position[0] + position[0],
      preset.position[1] + position[1],
      preset.position[2] + position[2],
    ];
    const targetLook: [number, number, number] = [
      preset.target[0] + position[0],
      preset.target[1] + position[1],
      preset.target[2] + position[2],
    ];

    useCameraStore.getState().transitionTo(targetPos, targetLook, 1.2);
  };

  return (
    <group name="AIIntelligenceCenterSector" position={position}>
      {/* Neural Purple Environment Lighting */}
      <ambientLight color="#1e1438" intensity={0.9} />
      <directionalLight
        position={[8, 14, 10]}
        intensity={1.4}
        color="#c084fc"
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
      />
      <pointLight position={[0, 3.5, 0]} intensity={1.8} color="#06b6d4" distance={12} />
      <pointLight position={[0, 3.5, -5.2]} intensity={1.5} color="#a855f7" distance={14} />

      {/* 1. Base Arena Floor & Peripheral Neural Pylons */}
      <AIChamberFloor3D />

      {/* 2. Central Neural AI Core Holosphere */}
      <AICoreHoloSphere3D />

      {/* 3. Satellite Data Nodes with Laser Beams */}
      <AIDataNodes3D />

      {/* 4. Recommendation & Insights Hologram Screen */}
      <AIRecommendationHolograms3D />

      {/* 5. Result Visualization Stela */}
      <AIResultVisualization3D />

      {/* 6. Interaction Console & Safety Interlock Terminal */}
      <AITerminal3D />

      {/* 7. Floating 3D Navigation Bar for Station Quick-Teleport */}
      <group position={[0, 0.4, 5.2]} rotation={[-Math.PI * 0.15, 0, 0]}>
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[7.2, 0.65]} />
          <meshBasicMaterial color="#020309" opacity={0.88} transparent side={THREE.DoubleSide} />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(7.2, 0.65)]} />
          <lineBasicMaterial color={AI_LAYOUT.colors.neuralPurple} />
        </lineSegments>

        {cameraButtons.map((btn, idx) => {
          const spacing = 1.16;
          const xPos = (idx - 2.5) * spacing;
          return (
            <StationNavButton3D
              key={btn.key}
              position={[xPos, 0, 0.02]}
              label={btn.label}
              onClick={() => handleCameraChange(btn.preset)}
            />
          );
        })}
      </group>
    </group>
  );
};

interface StationNavButton3DProps {
  position: [number, number, number];
  label: string;
  onClick: () => void;
}

const StationNavButton3D: React.FC<StationNavButton3DProps> = ({
  position,
  label,
  onClick,
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <group
      position={position}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
      onPointerOver={(e) => {
        e.stopPropagation();
        setHovered(true);
        document.body.style.cursor = 'pointer';
      }}
      onPointerOut={() => {
        setHovered(false);
        document.body.style.cursor = 'auto';
      }}
    >
      <mesh position={[0, 0, 0]}>
        <planeGeometry args={[1.08, 0.38]} />
        <meshStandardMaterial
          color={hovered ? '#4c1d95' : '#0e1224'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(1.08, 0.38)]} />
        <lineBasicMaterial color={hovered ? '#c084fc' : '#6d28d9'} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.075}
        color={hovered ? '#ffffff' : '#ddd6fe'}
        anchorX="center"
        anchorY="middle"
        letterSpacing={0.04}
      >
        {label}
      </Text>
    </group>
  );
};
