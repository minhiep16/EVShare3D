import React, { useRef, useState } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group, Mesh } from 'three';
import { useOwnershipStore } from './useOwnershipStore';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';
import { formatCurrencyVND, formatPercentageVN } from '@/i18n';

export const SyndicateOverviewPillar3D: React.FC = () => {
  const coreRef = useRef<Mesh>(null);
  const crestRef = useRef<Group>(null);
  const [hoveredButtonId, setHoveredButtonId] = useState<number | null>(null);

  const groups = useOwnershipStore((s) => s.groups);
  const activeGroupId = useOwnershipStore((s) => s.activeGroupId);
  const setActiveGroup = useOwnershipStore((s) => s.setActiveGroup);

  const activeGroup = groups.find((g) => g.id === activeGroupId) || groups[0];

  useFrame((_, delta) => {
    if (coreRef.current) {
      coreRef.current.rotation.y += delta * 0.4;
    }
    if (crestRef.current) {
      crestRef.current.rotation.y -= delta * 0.2;
    }
  });

  const { THEME, CENTRAL_DAIS_HEIGHT } = CO_OWNERSHIP_HALL_LAYOUT;

  return (
    <group position={[0, CENTRAL_DAIS_HEIGHT, 0]} name="SyndicateOverviewPillar3D">
      {/* 1. Base Column Plinth */}
      <mesh position={[0, 0.4, 0]} receiveShadow>
        <cylinderGeometry args={[0.9, 1.1, 0.8, 32]} />
        <meshStandardMaterial
          color="#0f172a"
          roughness={0.2}
          metalness={0.8}
        />
      </mesh>

      {/* 2. Rotating Holographic Gold Core */}
      <mesh ref={coreRef} position={[0, 1.6, 0]}>
        <octahedronGeometry args={[0.55, 0]} />
        <meshStandardMaterial
          color={THEME.GOLD_ACCENT_PRIMARY}
          emissive={THEME.GOLD_ACCENT_GLOW}
          emissiveIntensity={0.8}
          wireframe
        />
      </mesh>

      {/* Internal Core Light */}
      <pointLight
        color={THEME.GOLD_ACCENT_PRIMARY}
        intensity={1.0}
        distance={6}
        decay={2}
        position={[0, 1.6, 0]}
      />

      {/* 3. Outer Glass/Energy Protective Cylinder */}
      <mesh position={[0, 1.6, 0]}>
        <cylinderGeometry args={[0.8, 0.8, 1.6, 32, 1, true]} />
        <meshStandardMaterial
          color="#1e293b"
          roughness={0.1}
          metalness={0.9}
          transparent
          opacity={0.35}
        />
      </mesh>

      {/* 4. Top Crown & Rotating Syndicate Crest */}
      <mesh position={[0, 2.45, 0]}>
        <cylinderGeometry args={[0.95, 0.8, 0.15, 32]} />
        <meshStandardMaterial
          color="#030712"
          roughness={0.2}
          metalness={0.9}
        />
      </mesh>

      {/* Rotating Floating Crest */}
      <group ref={crestRef} position={[0, 2.9, 0]}>
        <mesh>
          <torusGeometry args={[0.32, 0.04, 16, 32]} />
          <meshBasicMaterial color={THEME.NEON_GOLD} />
        </mesh>
        <mesh rotation={[0, 0, Math.PI / 4]}>
          <boxGeometry args={[0.22, 0.22, 0.04]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} />
        </mesh>
      </group>

      {/* 5. In-World Front Holographic Display (Facing South/Entry: +Z) */}
      <group position={[0, 1.8, 0.95]}>
        {/* Holographic Backing Slab */}
        <mesh position={[0, 0, -0.05]}>
          <planeGeometry args={[2.0, 1.8]} />
          <meshStandardMaterial
            color="#050811"
            transparent
            opacity={0.85}
            roughness={0.1}
            metalness={0.9}
          />
        </mesh>

        {/* Outer Gold Border */}
        <mesh position={[0, 0, -0.04]}>
          <ringGeometry args={[1.05, 1.08, 4]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} />
        </mesh>

        {/* Syndicate Header */}
        <Text
          position={[0, 0.72, 0]}
          fontSize={0.13}
          color={THEME.TEXT_GOLD_BRIGHT}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          {activeGroup.name.toUpperCase()}
        </Text>

        <Text
          position={[0, 0.54, 0]}
          fontSize={0.08}
          color={THEME.TEXT_MUTED}
          anchorX="center"
          anchorY="middle"
        >
          {`${activeGroup.vehicleModelName} • ${activeGroup.vehiclePlate}`}
        </Text>

        {/* Separator Line */}
        <mesh position={[0, 0.44, 0]}>
          <planeGeometry args={[1.7, 0.006]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_GLOW} transparent opacity={0.6} />
        </mesh>

        {/* Syndicate Metrics Grid */}
        <group position={[-0.8, 0.28, 0]}>
          <Text fontSize={0.065} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
            ĐỊNH GIÁ XE:
          </Text>
          <Text position={[1.6, 0, 0]} fontSize={0.065} color="#f8fafc" anchorX="right" anchorY="middle">
            {formatCurrencyVND(activeGroup.totalValuationVnd)}
          </Text>
        </group>

        <group position={[-0.8, 0.15, 0]}>
          <Text fontSize={0.065} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
            QUỸ DỰ PHÒNG:
          </Text>
          <Text position={[1.6, 0, 0]} fontSize={0.065} color={THEME.CYBER_EMERALD} anchorX="right" anchorY="middle">
            {formatCurrencyVND(activeGroup.operatingReserveBalanceVnd)}
          </Text>
        </group>

        <group position={[-0.8, 0.02, 0]}>
          <Text fontSize={0.065} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
            TỔNG CỔ PHẦN:
          </Text>
          <Text position={[1.6, 0, 0]} fontSize={0.065} color={THEME.GOLD_ACCENT_PRIMARY} anchorX="right" anchorY="middle">
            {`${formatPercentageVN(activeGroup.totalSharesPercent)} ĐÃ PHÂN BỔ`}
          </Text>
        </group>

        <group position={[-0.8, -0.11, 0]}>
          <Text fontSize={0.065} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
            MÃ ĐĂNG KÝ:
          </Text>
          <Text position={[1.6, 0, 0]} fontSize={0.065} color={THEME.CYBER_CYAN} anchorX="right" anchorY="middle">
            {activeGroup.legalRegistrationCode}
          </Text>
        </group>

        {/* 6. Interactive Syndicate Switcher Buttons */}
        <group position={[0, -0.45, 0]}>
          <Text position={[0, 0.14, 0]} fontSize={0.06} color={THEME.TEXT_MUTED} anchorX="center" anchorY="middle">
            CHỌN NHÓM ĐỒNG SỞ HỮU:
          </Text>

          {groups.map((grp, idx) => {
            const totalBtns = groups.length;
            const btnWidth = 0.52;
            const spacing = 0.58;
            const xOffset = (idx - (totalBtns - 1) / 2) * spacing;
            const isCurrent = grp.id === activeGroupId;
            const isHovered = hoveredButtonId === grp.id;

            return (
              <group
                key={grp.id}
                position={[xOffset, -0.05, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveGroup(grp.id);
                }}
                onPointerEnter={(e) => {
                  e.stopPropagation();
                  setHoveredButtonId(grp.id);
                  document.body.style.cursor = 'pointer';
                }}
                onPointerLeave={(e) => {
                  e.stopPropagation();
                  setHoveredButtonId(null);
                  document.body.style.cursor = 'auto';
                }}
              >
                {/* Button Box */}
                <mesh>
                  <boxGeometry args={[btnWidth, 0.16, 0.02]} />
                  <meshStandardMaterial
                    color={isCurrent ? THEME.AMBER_DARK : isHovered ? '#1e293b' : '#090d16'}
                    emissive={isCurrent ? THEME.GOLD_ACCENT_PRIMARY : isHovered ? '#334155' : '#000000'}
                    emissiveIntensity={isCurrent ? 0.6 : 0.2}
                    roughness={0.3}
                    metalness={0.7}
                  />
                </mesh>

                {/* Button Label */}
                <Text
                  position={[0, 0, 0.02]}
                  fontSize={0.055}
                  color={isCurrent ? '#ffffff' : isHovered ? THEME.TEXT_GOLD_BRIGHT : THEME.TEXT_MUTED}
                  anchorX="center"
                  anchorY="middle"
                >
                  {grp.name.split(' ')[0].toUpperCase()}
                </Text>
              </group>
            );
          })}
        </group>
      </group>
    </group>
  );
};
