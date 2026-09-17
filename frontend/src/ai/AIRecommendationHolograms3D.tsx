import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';
import type { AIIntelligenceCategory } from './aiTypes';

export const AIRecommendationHolograms3D: React.FC = () => {
  const activeCategory = useAIStore((s) => s.activeCategory);
  const setActiveCategory = useAIStore((s) => s.setActiveCategory);
  const recommendations = useAIStore((s) => s.recommendations);
  const selectedRecommendationId = useAIStore((s) => s.selectedRecommendationId);

  const categories: { key: AIIntelligenceCategory; label: string; color: string }[] = [
    { key: 'RECOMMENDATION', label: 'RECOMMENDATIONS', color: '#06b6d4' },
    { key: 'INSIGHT', label: 'MOBILITY INSIGHTS', color: '#a855f7' },
    { key: 'ANOMALY_INDICATOR', label: 'ANOMALY ALERTS', color: '#ef4444' },
    { key: 'FAIRNESS_SUGGESTION', label: 'FAIRNESS QUOTAS', color: '#10b981' },
  ];

  const currentItem =
    recommendations.find((r) => r.id === selectedRecommendationId) ||
    recommendations.find((r) => r.category === activeCategory) ||
    recommendations[0];

  const getSeverityColor = (sev: string) => {
    switch (sev) {
      case 'CRITICAL':
        return '#ef4444';
      case 'WARNING':
        return '#f59e0b';
      case 'INFO':
        return '#06b6d4';
      default:
        return '#10b981';
    }
  };

  return (
    <group name="AIRecommendationHolograms" position={AI_LAYOUT.recommendationHoloPosition}>
      {/* 1. Category Switcher Tabs */}
      <group position={[0, 1.45, 0]}>
        {categories.map((cat, idx) => {
          const spacing = 1.15;
          const xPos = (idx - 1.5) * spacing;
          const isActive = activeCategory === cat.key;

          return (
            <CategoryTabButton3D
              key={cat.key}
              position={[xPos, 0, 0]}
              label={cat.label}
              color={cat.color}
              isActive={isActive}
              onClick={() => setActiveCategory(cat.key)}
            />
          );
        })}
      </group>

      {/* 2. Main Holographic Recommendation Screen */}
      <group position={[0, 0, 0]}>
        {/* Holographic Panel Glass */}
        <mesh position={[0, 0, 0]}>
          <planeGeometry args={[4.4, 2.3]} />
          <meshBasicMaterial
            color="#040614"
            opacity={0.9}
            transparent
            side={THREE.DoubleSide}
          />
        </mesh>
        <lineSegments position={[0, 0, 0.005]}>
          <edgesGeometry args={[new THREE.PlaneGeometry(4.4, 2.3)]} />
          <lineBasicMaterial color={getSeverityColor(currentItem.severity)} linewidth={1} />
        </lineSegments>

        {/* Top Category Badge & Confidence Score */}
        <group position={[0, 0.88, 0.02]}>
          <Text
            position={[-1.9, 0, 0]}
            fontSize={0.105}
            color={getSeverityColor(currentItem.severity)}
            anchorX="left"
            anchorY="middle"
            letterSpacing={0.06}
          >
            {`[ ${currentItem.category} • ${currentItem.severity} ]`}
          </Text>
          <Text
            position={[1.9, 0, 0]}
            fontSize={0.105}
            color={AI_LAYOUT.colors.cyanLight}
            anchorX="right"
            anchorY="middle"
          >
            {`CONFIDENCE: ${currentItem.confidenceScore}% (DEV MOCK)`}
          </Text>
        </group>

        {/* Recommendation Title */}
        <Text
          position={[0, 0.52, 0.02]}
          fontSize={0.16}
          color={AI_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          maxWidth={4.0}
          textAlign="center"
        >
          {currentItem.title}
        </Text>

        {/* Detailed Narrative */}
        <Text
          position={[0, 0.16, 0.02]}
          fontSize={0.11}
          color={AI_LAYOUT.colors.textWhite}
          anchorX="center"
          anchorY="middle"
          maxWidth={3.9}
          textAlign="center"
          lineHeight={1.35}
        >
          {currentItem.description}
        </Text>

        {/* Impact Metric Badge */}
        <group position={[0, -0.32, 0.02]}>
          <mesh position={[0, 0, 0]}>
            <planeGeometry args={[3.8, 0.32]} />
            <meshBasicMaterial color="#0b1329" opacity={0.95} transparent side={THREE.DoubleSide} />
          </mesh>
          <lineSegments position={[0, 0, 0.005]}>
            <edgesGeometry args={[new THREE.PlaneGeometry(3.8, 0.32)]} />
            <lineBasicMaterial color={AI_LAYOUT.colors.goldWarning} />
          </lineSegments>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.11}
            color={AI_LAYOUT.colors.goldWarning}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.04}
          >
            {`PROJECTED IMPACT: ${currentItem.impactMetric}`}
          </Text>
        </group>

        {/* Suggested Action & Advisory Disclaimer */}
        <group position={[0, -0.74, 0.02]}>
          <Text
            position={[0, 0.08, 0]}
            fontSize={0.10}
            color={AI_LAYOUT.colors.cyanLight}
            anchorX="center"
            anchorY="middle"
            maxWidth={3.9}
            textAlign="center"
          >
            {`ACTION: ${currentItem.suggestedAction}`}
          </Text>
          <Text
            position={[0, -0.16, 0]}
            fontSize={0.085}
            color={AI_LAYOUT.colors.textMuted}
            anchorX="center"
            anchorY="middle"
            letterSpacing={0.04}
          >
            ADVISORY ONLY • REQUIRES CO-OWNER RATIFICATION IN RELEVANT SECTOR (BR-AI-SAFE-01)
          </Text>
        </group>
      </group>
    </group>
  );
};

interface CategoryTabButton3DProps {
  position: [number, number, number];
  label: string;
  color: string;
  isActive: boolean;
  onClick: () => void;
}

const CategoryTabButton3D: React.FC<CategoryTabButton3DProps> = ({
  position,
  label,
  color,
  isActive,
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
        <planeGeometry args={[1.05, 0.32]} />
        <meshStandardMaterial
          color={isActive ? color : hovered ? '#1e1b4b' : '#0b0f19'}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.005]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(1.05, 0.32)]} />
        <lineBasicMaterial color={isActive ? '#ffffff' : color} />
      </lineSegments>
      <Text
        position={[0, 0, 0.02]}
        fontSize={0.08}
        color={isActive ? '#ffffff' : hovered ? '#ffffff' : color}
        anchorX="center"
        anchorY="middle"
        letterSpacing={0.03}
      >
        {label}
      </Text>
    </group>
  );
};
