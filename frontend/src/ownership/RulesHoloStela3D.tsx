import React, { useState } from 'react';
import { Text } from '@react-three/drei';
import { useOwnershipStore } from './useOwnershipStore';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';

interface SyndicateRule {
  id: number;
  category: 'USAGE' | 'CHARGING' | 'MAINTENANCE' | 'PENALTY';
  severity: 'STRICT' | 'NORMAL' | 'WARNING';
  title: string;
  description: string;
}

const DEFAULT_SYNDICATE_RULES: SyndicateRule[] = [
  {
    id: 1,
    category: 'USAGE',
    severity: 'STRICT',
    title: 'Reservation Overlap & Quota Cap',
    description: 'Reservations must respect equity quota and 30-min turnaround buffer (BR-BKG-01..02).',
  },
  {
    id: 2,
    category: 'USAGE',
    severity: 'NORMAL',
    title: 'Cancellation Lead Time',
    description: 'Free cancellation permitted up to 12 hours before start window (BR-BKG-03).',
  },
  {
    id: 3,
    category: 'CHARGING',
    severity: 'STRICT',
    title: 'Minimum Return SoC Threshold',
    description: 'Vehicle must be returned with >=20% SoC or connected to active charging stall.',
  },
  {
    id: 4,
    category: 'CHARGING',
    severity: 'NORMAL',
    title: 'Charging Cost Settlement',
    description: 'Direct Supercharger sessions billed to syndicate shared reserve fund.',
  },
  {
    id: 5,
    category: 'MAINTENANCE',
    severity: 'WARNING',
    title: 'Service Interval Notification',
    description: 'Preventive inspection mandated every 10,000 km or 6 operating months.',
  },
  {
    id: 6,
    category: 'MAINTENANCE',
    severity: 'NORMAL',
    title: 'Tire & Brake Pad Inspection',
    description: 'Report telemetry anomalies via 3D diagnostic inspection terminal.',
  },
  {
    id: 7,
    category: 'PENALTY',
    severity: 'STRICT',
    title: 'Late Return Penalties',
    description: 'Returns exceeding scheduled end by >15 mins incur 50,000 VND / 30m block.',
  },
  {
    id: 8,
    category: 'PENALTY',
    severity: 'WARNING',
    title: 'No-Show Fee',
    description: 'Unclaimed slots after 30 mins are marked NO_SHOW and forfeited.',
  },
];

export const RulesHoloStela3D: React.FC = () => {
  const [stelaMode, setStelaMode] = useState<'RULES' | 'AUDIT'>('RULES');
  const [activeCategory, setActiveCategory] = useState<'USAGE' | 'CHARGING' | 'MAINTENANCE' | 'PENALTY'>('USAGE');
  const [hoveredCategory, setHoveredCategory] = useState<string | null>(null);
  const [hoveredMode, setHoveredMode] = useState<string | null>(null);

  const groups = useOwnershipStore((s) => s.groups);
  const activeGroupId = useOwnershipStore((s) => s.activeGroupId);
  const ownershipHistory = useOwnershipStore((s) => s.ownershipHistory);
  const cameraPreset = useOwnershipStore((s) => s.cameraPreset);

  // Sync mode if camera switched to HISTORY_STELA
  React.useEffect(() => {
    if (cameraPreset === 'HISTORY_STELA') {
      setStelaMode('AUDIT');
    } else if (cameraPreset === 'RULES_STELA') {
      setStelaMode('RULES');
    }
  }, [cameraPreset]);

  const activeGroup = groups.find((g) => g.id === activeGroupId) || groups[0];
  const rules: SyndicateRule[] = (activeGroup as { rules?: SyndicateRule[] })?.rules || DEFAULT_SYNDICATE_RULES;
  const filteredRules = rules.filter((r) => r.category === activeCategory);

  const { RULES_STELA_POS, RULES_STELA_ROT, THEME } = CO_OWNERSHIP_HALL_LAYOUT;

  const categories: Array<{ id: 'USAGE' | 'CHARGING' | 'MAINTENANCE' | 'PENALTY'; label: string }> = [
    { id: 'USAGE', label: 'USAGE' },
    { id: 'CHARGING', label: 'CHARGING' },
    { id: 'MAINTENANCE', label: 'SERVICE' },
    { id: 'PENALTY', label: 'PENALTIES' },
  ];

  return (
    <group
      position={RULES_STELA_POS}
      rotation={RULES_STELA_ROT}
      name="RulesHoloStela3D"
    >
      {/* 1. Stela Base Block */}
      <mesh position={[0, 0.45, 0]} receiveShadow>
        <boxGeometry args={[1.4, 0.9, 0.8]} />
        <meshStandardMaterial
          color="#090d16"
          roughness={0.25}
          metalness={0.8}
        />
      </mesh>

      {/* Gold Rim on Base */}
      <mesh position={[0, 0.91, 0]}>
        <boxGeometry args={[1.45, 0.04, 0.85]} />
        <meshStandardMaterial
          color={THEME.GOLD_ACCENT_PRIMARY}
          roughness={0.15}
          metalness={0.9}
        />
      </mesh>

      {/* Light Projection Source */}
      <pointLight
        color={THEME.GOLD_ACCENT_PRIMARY}
        intensity={0.8}
        distance={5}
        position={[0, 1.4, 0.4]}
      />

      {/* 2. Main Holographic Stela Plaque */}
      <group position={[0, 1.7, 0]} rotation={[-0.22, 0, 0]}>
        {/* Plaque Backing */}
        <mesh position={[0, 0, -0.04]}>
          <planeGeometry args={[3.2, 2.2]} />
          <meshStandardMaterial
            color="#040711"
            roughness={0.15}
            metalness={0.85}
            transparent
            opacity={0.92}
          />
        </mesh>

        {/* Gold Border Frame */}
        <mesh position={[0, 0, -0.035]}>
          <ringGeometry args={[1.58, 1.6, 4]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} />
        </mesh>

        {/* Stela Header */}
        <Text
          position={[0, 0.93, 0]}
          fontSize={0.1}
          color={THEME.TEXT_GOLD_BRIGHT}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          {stelaMode === 'RULES' ? 'CO-OWNERSHIP GOVERNANCE RULES' : 'OWNERSHIP AUDIT & EQUITY LEDGER'}
        </Text>

        {/* Stela Subtitle */}
        <Text
          position={[0, 0.81, 0]}
          fontSize={0.058}
          color={THEME.CYBER_CYAN}
          anchorX="center"
          anchorY="middle"
        >
          {stelaMode === 'RULES'
            ? 'SYNDICATE BY-LAWS & FAIR USAGE ETIQUETTE'
            : 'IMMUTABLE CHRONOLOGICAL SHARE ISSUANCE & TRANSFERS'}
        </Text>

        {/* Mode Switcher Toggle (RULES vs AUDIT) */}
        <group position={[0, 0.69, 0]}>
          {[
            { id: 'RULES' as const, label: 'BY-LAWS & RULES' },
            { id: 'AUDIT' as const, label: 'AUDIT LEDGER' },
          ].map((modeItem, mIdx) => {
            const isSelected = stelaMode === modeItem.id;
            const isHover = hoveredMode === modeItem.id;
            const xPos = mIdx === 0 ? -0.72 : 0.72;

            return (
              <group
                key={modeItem.id}
                position={[xPos, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setStelaMode(modeItem.id);
                }}
                onPointerEnter={(e) => {
                  e.stopPropagation();
                  setHoveredMode(modeItem.id);
                  document.body.style.cursor = 'pointer';
                }}
                onPointerLeave={(e) => {
                  e.stopPropagation();
                  setHoveredMode(null);
                  document.body.style.cursor = 'auto';
                }}
              >
                <mesh>
                  <boxGeometry args={[1.3, 0.14, 0.02]} />
                  <meshStandardMaterial
                    color={isSelected ? THEME.AMBER_DARK : isHover ? '#1e293b' : '#0a0f1d'}
                    emissive={isSelected ? THEME.GOLD_ACCENT_PRIMARY : '#000000'}
                    emissiveIntensity={isSelected ? 0.6 : 0}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.02]}
                  fontSize={0.052}
                  color={isSelected ? '#ffffff' : THEME.TEXT_MUTED}
                  anchorX="center"
                  anchorY="middle"
                >
                  {modeItem.label}
                </Text>
              </group>
            );
          })}
        </group>

        {/* Divider */}
        <mesh position={[0, 0.59, 0]}>
          <planeGeometry args={[2.9, 0.006]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_GLOW} transparent opacity={0.6} />
        </mesh>

        {/* VIEW 1: GOVERNANCE RULES */}
        {stelaMode === 'RULES' && (
          <>
            {/* Category Filter Tabs */}
            <group position={[0, 0.49, 0]}>
              {categories.map((cat, idx) => {
                const isTabActive = activeCategory === cat.id;
                const isTabHovered = hoveredCategory === cat.id;
                const tabWidth = 0.68;
                const spacing = 0.72;
                const xPos = (idx - (categories.length - 1) / 2) * spacing;

                return (
                  <group
                    key={cat.id}
                    position={[xPos, 0, 0]}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActiveCategory(cat.id);
                    }}
                    onPointerEnter={(e) => {
                      e.stopPropagation();
                      setHoveredCategory(cat.id);
                      document.body.style.cursor = 'pointer';
                    }}
                    onPointerLeave={(e) => {
                      e.stopPropagation();
                      setHoveredCategory(null);
                      document.body.style.cursor = 'auto';
                    }}
                  >
                    <mesh>
                      <boxGeometry args={[tabWidth, 0.13, 0.02]} />
                      <meshStandardMaterial
                        color={isTabActive ? THEME.AMBER_DARK : isTabHovered ? '#1e293b' : '#0a0f1d'}
                        emissive={isTabActive ? THEME.GOLD_ACCENT_PRIMARY : '#000000'}
                        emissiveIntensity={isTabActive ? 0.5 : 0}
                        roughness={0.3}
                        metalness={0.7}
                      />
                    </mesh>

                    <Text
                      position={[0, 0, 0.02]}
                      fontSize={0.05}
                      color={isTabActive ? '#ffffff' : isTabHovered ? THEME.TEXT_GOLD_BRIGHT : THEME.TEXT_MUTED}
                      anchorX="center"
                      anchorY="middle"
                    >
                      {cat.label}
                    </Text>
                  </group>
                );
              })}
            </group>

            {/* Display Filtered Rules */}
            <group position={[0, 0.22, 0]}>
              {filteredRules.slice(0, 3).map((rule, rIdx) => {
                const yOffset = -rIdx * 0.36;
                const severityColor =
                  rule.severity === 'STRICT'
                    ? '#ef4444'
                    : rule.severity === 'NORMAL'
                    ? THEME.CYBER_CYAN
                    : '#fbbf24';

                return (
                  <group key={rule.id} position={[0, yOffset, 0]}>
                    <mesh position={[0, 0, -0.01]}>
                      <planeGeometry args={[2.9, 0.3]} />
                      <meshStandardMaterial
                        color="#090d16"
                        roughness={0.2}
                        metalness={0.8}
                        transparent
                        opacity={0.8}
                      />
                    </mesh>

                    <mesh position={[-1.42, 0, 0]}>
                      <boxGeometry args={[0.04, 0.28, 0.02]} />
                      <meshStandardMaterial
                        color={severityColor}
                        emissive={severityColor}
                        emissiveIntensity={0.6}
                      />
                    </mesh>

                    <Text
                      position={[-1.34, 0.07, 0]}
                      fontSize={0.065}
                      color="#ffffff"
                      anchorX="left"
                      anchorY="middle"
                    >
                      {rule.title}
                    </Text>

                    <Text
                      position={[1.34, 0.07, 0]}
                      fontSize={0.048}
                      color={severityColor}
                      anchorX="right"
                      anchorY="middle"
                    >
                      {`[${rule.severity}]`}
                    </Text>

                    <Text
                      position={[-1.34, -0.06, 0]}
                      fontSize={0.048}
                      color="#94a3b8"
                      anchorX="left"
                      anchorY="middle"
                      maxWidth={2.65}
                    >
                      {rule.description}
                    </Text>
                  </group>
                );
              })}
            </group>
          </>
        )}

        {/* VIEW 2: OWNERSHIP AUDIT LEDGER */}
        {stelaMode === 'AUDIT' && (
          <group position={[0, 0.4, 0]}>
            {ownershipHistory.slice(0, 4).map((hist, hIdx) => {
              const yOffset = -hIdx * 0.31;
              return (
                <group key={hist.id || hIdx} position={[0, yOffset, 0]}>
                  {/* Ledger Record Plaque */}
                  <mesh position={[0, 0, -0.01]}>
                    <planeGeometry args={[2.9, 0.26]} />
                    <meshStandardMaterial
                      color="#070b14"
                      roughness={0.2}
                      metalness={0.8}
                      transparent
                      opacity={0.85}
                    />
                  </mesh>

                  {/* Left Status Bar */}
                  <mesh position={[-1.42, 0, 0]}>
                    <boxGeometry args={[0.04, 0.24, 0.02]} />
                    <meshStandardMaterial
                      color={THEME.CYBER_EMERALD}
                      emissive={THEME.CYBER_EMERALD}
                      emissiveIntensity={0.5}
                    />
                  </mesh>

                  {/* Action Name */}
                  <Text
                    position={[-1.34, 0.06, 0]}
                    fontSize={0.062}
                    color={THEME.TEXT_GOLD_BRIGHT}
                    anchorX="left"
                    anchorY="middle"
                  >
                    {hist.action.replace(/_/g, ' ')}
                  </Text>

                  {/* Certificate Number */}
                  <Text
                    position={[1.34, 0.06, 0]}
                    fontSize={0.048}
                    color={THEME.CYBER_CYAN}
                    anchorX="right"
                    anchorY="middle"
                  >
                    {hist.newCertificateNumber || 'CERT-RECORDED'}
                  </Text>

                  {/* Percentage Change & Metadata */}
                  <Text
                    position={[-1.34, -0.06, 0]}
                    fontSize={0.05}
                    color="#cbd5e1"
                    anchorX="left"
                    anchorY="middle"
                  >
                    {`SHARE: ${hist.previousPercentage}% ➔ ${hist.newPercentage}%  •  ACTING USER: #${hist.actingUserId}  •  ${hist.effectiveDate || '2026'}`}
                  </Text>

                  {/* Verified Tag */}
                  <Text
                    position={[1.34, -0.06, 0]}
                    fontSize={0.045}
                    color={THEME.CYBER_EMERALD}
                    anchorX="right"
                    anchorY="middle"
                  >
                    ✓ COMMITTED
                  </Text>
                </group>
              );
            })}
          </group>
        )}

        {/* 5. Footer Operational Note */}
        <group position={[0, -0.92, 0]}>
          <Text
            position={[0, 0, 0]}
            fontSize={0.045}
            color={THEME.TEXT_MUTED}
            anchorX="center"
            anchorY="middle"
          >
            {stelaMode === 'RULES'
              ? 'RULE AMENDMENTS REQUIRE 60% MAJORITY RATIFICATION VIA GOVERNANCE COUNCIL'
              : 'IMMUTABLE LEDGER ENTRIES VERIFIED BY SPRING BOOT OWNERSHIP AUDIT SERVICE'}
          </Text>
        </group>
      </group>
    </group>
  );
};
