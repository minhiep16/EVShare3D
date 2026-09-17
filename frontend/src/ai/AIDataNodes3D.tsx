import React, { useState } from 'react';
import { Text, Line } from '@react-three/drei';
import * as THREE from 'three';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';
import type { AIDataNodeModel } from './aiTypes';

export const AIDataNodes3D: React.FC = () => {
  const dataNodes = useAIStore((s) => s.dataNodes);
  const selectedNodeId = useAIStore((s) => s.selectedNodeId);
  const selectNode = useAIStore((s) => s.selectNode);

  return (
    <group name="AIDataNodes">
      {dataNodes.map((node) => {
        const isSelected = selectedNodeId === node.id;
        return (
          <SingleDataNode
            key={node.id}
            node={node}
            isSelected={isSelected}
            onSelect={() => selectNode(node.id)}
          />
        );
      })}
    </group>
  );
};

interface SingleDataNodeProps {
  node: AIDataNodeModel;
  isSelected: boolean;
  onSelect: () => void;
}

const SingleDataNode: React.FC<SingleDataNodeProps> = ({
  node,
  isSelected,
  onSelect,
}) => {
  const [hovered, setHovered] = useState(false);
  const corePos = AI_LAYOUT.aiCorePosition;

  return (
    <group position={node.position}>
      {/* 1. Laser Data Conduit Beam to Central Core */}
      <Line
        points={[
          [0, 0, 0],
          [corePos[0] - node.position[0], corePos[1] - node.position[1], corePos[2] - node.position[2]],
        ]}
        color={isSelected ? '#fbbf24' : node.color}
        lineWidth={isSelected ? 2 : 1}
        transparent
        opacity={isSelected ? 0.75 : 0.35}
      />

      {/* 2. Interactive Satellite Node Object */}
      <group
        onClick={(e) => {
          e.stopPropagation();
          onSelect();
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
        {/* Node Polyhedron */}
        <mesh castShadow scale={isSelected ? 1.25 : hovered ? 1.15 : 1.0}>
          <octahedronGeometry args={[0.35, 0]} />
          <meshStandardMaterial
            color={isSelected ? '#fbbf24' : node.color}
            emissive={isSelected ? '#d97706' : node.color}
            emissiveIntensity={isSelected ? 1.5 : hovered ? 1.0 : 0.4}
            roughness={0.2}
            metalness={0.8}
          />
        </mesh>

        {/* Outer Pulsing Wireframe Cage */}
        <mesh scale={isSelected ? 1.45 : hovered ? 1.3 : 1.15}>
          <dodecahedronGeometry args={[0.38, 0]} />
          <meshBasicMaterial
            color={isSelected ? '#ffffff' : node.color}
            wireframe
            transparent
            opacity={isSelected ? 0.8 : 0.4}
          />
        </mesh>

        {/* 3. Floating Telemetry Card Above Node */}
        <group position={[0, 1.1, 0]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[2.2, 1.1]} />
            <meshBasicMaterial
              color="#040612"
              opacity={isSelected ? 0.92 : 0.8}
              transparent
              side={THREE.DoubleSide}
            />
          </mesh>
          <lineSegments position={[0, 0, 0.005]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(2.2, 1.1)]} />
            <lineBasicMaterial color={isSelected ? '#fbbf24' : hovered ? '#ffffff' : node.color} />
          </lineSegments>

          {/* Node Header */}
          <Text
            position={[0, 0.38, 0.02]}
            fontSize={0.11}
            color={isSelected ? '#fbbf24' : node.color}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.05}
          >
            {node.name.toUpperCase()}
          </Text>

          {/* Metrics List */}
          {node.metrics.slice(0, 3).map((m, idx) => {
            const yOffset = 0.14 - idx * 0.22;
            return (
              <group key={idx} position={[0, yOffset, 0.02]}>
                <Text
                  position={[-0.95, 0, 0]}
                  fontSize={0.08}
                  color={AI_LAYOUT.colors.textMuted}
                  anchorX="left"
                  anchorY="middle"
                >
                  {m.label}
                </Text>
                <Text
                  position={[0.95, 0, 0]}
                  fontSize={0.085}
                  color={AI_LAYOUT.colors.textWhite}
                  anchorX="right"
                  anchorY="middle"
                >
                  {m.value}
                </Text>
              </group>
            );
          })}
        </group>
      </group>
    </group>
  );
};
