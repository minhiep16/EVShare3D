import React, { useMemo } from 'react';
import { Text } from '@react-three/drei';
import { useOwnershipStore } from './useOwnershipStore';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';

export const EquityDistributionRing3D: React.FC = () => {
  const groups = useOwnershipStore((s) => s.groups);
  const activeGroupId = useOwnershipStore((s) => s.activeGroupId);
  const selectedMemberId = useOwnershipStore((s) => s.selectedMemberId);
  const hoveredMemberId = useOwnershipStore((s) => s.hoveredMemberId);
  const selectMember = useOwnershipStore((s) => s.selectMember);
  const setHoveredMember = useOwnershipStore((s) => s.setHoveredMember);
  const authoritativeValidation = useOwnershipStore((s) => s.authoritativeValidation);

  const activeGroup = groups.find((g) => g.id === activeGroupId) || groups[0];
  const members = activeGroup.members;

  const {
    EQUITY_RING_RADIUS,
    EQUITY_RING_ELEVATION,
    THEME,
  } = CO_OWNERSHIP_HALL_LAYOUT;

  // Calculate angular partitions for each member
  const slices = useMemo(() => {
    let currentAngle = 0;
    return members.map((m) => {
      const angleSpan = (m.sharePercentage / 100) * Math.PI * 2;
      const startAngle = currentAngle;
      const midAngle = currentAngle + angleSpan / 2;
      currentAngle += angleSpan;

      return {
        member: m,
        startAngle,
        angleSpan,
        midAngle,
      };
    });
  }, [members]);

  const innerRadius = EQUITY_RING_RADIUS - 0.45;
  const outerRadius = EQUITY_RING_RADIUS + 0.45;

  return (
    <group position={[0, EQUITY_RING_ELEVATION, 0]} name="EquityDistributionRing3D">
      {/* Central Rotating Equity Pool Summary Disc */}
      <group position={[0, 0.05, 0]}>
        <mesh rotation={[-Math.PI / 2, 0, 0]}>
          <circleGeometry args={[innerRadius - 0.08, 32]} />
          <meshStandardMaterial
            color="#090d16"
            metalness={0.9}
            roughness={0.2}
            transparent
            opacity={0.7}
          />
        </mesh>

        <mesh rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[innerRadius - 0.12, innerRadius - 0.08, 32]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} transparent opacity={0.6} />
        </mesh>

        {/* Central Overview Text */}
        <Text
          position={[0, 0.15, -0.12]}
          rotation={[-Math.PI / 2, 0, 0]}
          fontSize={0.14}
          color={THEME.TEXT_GOLD_BRIGHT}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.06}
        >
          EQUITY ALLOCATION
        </Text>
        <Text
          position={[0, 0.15, 0.08]}
          rotation={[-Math.PI / 2, 0, 0]}
          fontSize={0.12}
          color={THEME.CYBER_EMERALD}
          anchorX="center"
          anchorY="middle"
        >
          {`${activeGroup.totalSharesPercent.toFixed(2)}% TOTAL`}
        </Text>
        <Text
          position={[0, 0.15, 0.26]}
          rotation={[-Math.PI / 2, 0, 0]}
          fontSize={0.065}
          color={authoritativeValidation?.isValid ? THEME.CYBER_EMERALD : '#fbbf24'}
          anchorX="center"
          anchorY="middle"
        >
          {authoritativeValidation?.isValid ? '✓ BACKEND VERIFIED' : 'SPRING BOOT VALIDATING'}
        </Text>
      </group>

      {/* Segmented Slices for Each Co-Owner */}
      {slices.map(({ member, startAngle, angleSpan, midAngle }) => {
        const isSelected = selectedMemberId === member.id;
        const isHovered = hoveredMemberId === member.id;
        const isActive = isSelected || isHovered;

        // Elevation and radius boost when active
        const yOffset = isActive ? 0.14 : 0.02;
        const currentOuter = isActive ? outerRadius + 0.15 : outerRadius;
        const currentInner = isActive ? innerRadius - 0.05 : innerRadius;

        // Midpoint coordinates for floating label badge
        const labelDist = currentOuter + 0.65;
        const lx = Math.cos(midAngle) * labelDist;
        const lz = Math.sin(midAngle) * labelDist;

        return (
          <group key={member.id} name={`EquitySlice_${member.id}`}>
            {/* 3D Segmented Arc Mesh */}
            <mesh
              position={[0, yOffset, 0]}
              rotation={[-Math.PI / 2, 0, startAngle]}
              onClick={(e) => {
                e.stopPropagation();
                selectMember(isSelected ? null : member.id);
              }}
              onPointerEnter={(e) => {
                e.stopPropagation();
                setHoveredMember(member.id);
                document.body.style.cursor = 'pointer';
              }}
              onPointerLeave={(e) => {
                e.stopPropagation();
                setHoveredMember(null);
                document.body.style.cursor = 'auto';
              }}
            >
              <ringGeometry args={[currentInner, currentOuter, 32, 1, 0, angleSpan]} />
              <meshStandardMaterial
                color={member.color}
                emissive={member.color}
                emissiveIntensity={isActive ? 0.7 : 0.25}
                roughness={0.2}
                metalness={0.6}
                side={2}
              />
            </mesh>

            {/* Glowing Edge Border on the Arc */}
            <mesh
              position={[0, yOffset + 0.01, 0]}
              rotation={[-Math.PI / 2, 0, startAngle]}
            >
              <ringGeometry args={[currentOuter - 0.04, currentOuter, 32, 1, 0, angleSpan]} />
              <meshBasicMaterial color="#ffffff" transparent opacity={isActive ? 0.9 : 0.4} />
            </mesh>

            {/* Radial Connector Guide Line to floating label */}
            <mesh
              position={[Math.cos(midAngle) * ((currentOuter + labelDist - 0.25) / 2), yOffset + 0.02, Math.sin(midAngle) * ((currentOuter + labelDist - 0.25) / 2)]}
              rotation={[-Math.PI / 2, 0, -midAngle]}
            >
              <planeGeometry args={[0.02, labelDist - currentOuter]} />
              <meshBasicMaterial color={member.color} transparent opacity={0.6} />
            </mesh>

            {/* Floating 3D Share Label Badge */}
            <group
              position={[lx, yOffset + 0.35, lz]}
              onClick={(e) => {
                e.stopPropagation();
                selectMember(isSelected ? null : member.id);
              }}
              onPointerEnter={(e) => {
                e.stopPropagation();
                setHoveredMember(member.id);
                document.body.style.cursor = 'pointer';
              }}
              onPointerLeave={(e) => {
                e.stopPropagation();
                setHoveredMember(null);
                document.body.style.cursor = 'auto';
              }}
            >
              {/* Badge Backing Box */}
              <mesh>
                <boxGeometry args={[1.35, 0.52, 0.02]} />
                <meshStandardMaterial
                  color="#050811"
                  emissive={isActive ? member.color : '#0f172a'}
                  emissiveIntensity={isActive ? 0.5 : 0.15}
                  roughness={0.2}
                  metalness={0.8}
                />
              </mesh>

              {/* Member Short Name */}
              <Text
                position={[0, 0.13, 0.02]}
                fontSize={0.075}
                color="#ffffff"
                anchorX="center"
                anchorY="middle"
              >
                {member.name.split(' ').slice(-2).join(' ')}
              </Text>

              {/* Share Percentage */}
              <Text
                position={[0, -0.04, 0.02]}
                fontSize={0.105}
                color={member.color}
                anchorX="center"
                anchorY="middle"
              >
                {`${member.sharePercentage.toFixed(1)}% EQUITY`}
              </Text>

              {/* Share Certificate */}
              <Text
                position={[0, -0.18, 0.02]}
                fontSize={0.045}
                color={THEME.TEXT_MUTED}
                anchorX="center"
                anchorY="middle"
              >
                {member.shareCertificateNumber || `CERT-G${activeGroup.id}-M${member.id}`}
              </Text>
            </group>
          </group>
        );
      })}
    </group>
  );
};
