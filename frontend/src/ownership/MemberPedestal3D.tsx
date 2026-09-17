import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import type { Group, Mesh } from 'three';
import type { CoOwnerMember } from './ownershipTypes';
import { useOwnershipStore } from './useOwnershipStore';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';

interface MemberPedestal3DProps {
  member: CoOwnerMember;
}

export const MemberPedestal3D: React.FC<MemberPedestal3DProps> = ({ member }) => {
  const crystalRef = useRef<Mesh>(null);
  const badgeRef = useRef<Group>(null);

  const selectedMemberId = useOwnershipStore((s) => s.selectedMemberId);
  const hoveredMemberId = useOwnershipStore((s) => s.hoveredMemberId);
  const selectMember = useOwnershipStore((s) => s.selectMember);
  const setHoveredMember = useOwnershipStore((s) => s.setHoveredMember);

  const isSelected = selectedMemberId === member.id;
  const isHovered = hoveredMemberId === member.id;
  const isActive = isSelected || isHovered;

  const {
    MEMBER_PEDESTAL_RADIUS,
    MEMBER_PEDESTAL_HEIGHT,
    MEMBER_PEDESTAL_BASE_RADIUS,
    THEME,
  } = CO_OWNERSHIP_HALL_LAYOUT;

  // Compute position on the amphitheater ring
  const posX = Math.cos(member.pedestalAngle) * MEMBER_PEDESTAL_RADIUS;
  const posZ = Math.sin(member.pedestalAngle) * MEMBER_PEDESTAL_RADIUS;

  // Face toward center by calculating rotation Y
  const rotY = Math.atan2(-posX, -posZ);

  useFrame((_, delta) => {
    if (crystalRef.current) {
      crystalRef.current.rotation.y += delta * (isActive ? 1.0 : 0.4);
      crystalRef.current.rotation.x = Math.sin(Date.now() * 0.002) * 0.1;
    }
    if (badgeRef.current) {
      badgeRef.current.position.y = (isActive ? 1.75 : 1.6) + Math.sin(Date.now() * 0.003) * 0.04;
    }
  });

  return (
    <group
      position={[posX, 0, posZ]}
      rotation={[0, rotY, 0]}
      name={`MemberPedestal_${member.id}`}
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
      {/* 1. Floor Inlay Accent Disc */}
      <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <circleGeometry args={[MEMBER_PEDESTAL_BASE_RADIUS + 0.35, 32]} />
        <meshBasicMaterial color={isActive ? member.color : '#0f172a'} transparent opacity={0.5} />
      </mesh>

      <mesh position={[0, 0.03, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[MEMBER_PEDESTAL_BASE_RADIUS + 0.3, MEMBER_PEDESTAL_BASE_RADIUS + 0.35, 32]} />
        <meshBasicMaterial color={member.color} transparent opacity={isActive ? 0.9 : 0.4} />
      </mesh>

      {/* 2. Octagonal Pedestal Column */}
      <mesh position={[0, MEMBER_PEDESTAL_HEIGHT / 2, 0]} receiveShadow>
        <cylinderGeometry
          args={[
            MEMBER_PEDESTAL_BASE_RADIUS * 0.85,
            MEMBER_PEDESTAL_BASE_RADIUS,
            MEMBER_PEDESTAL_HEIGHT,
            8,
          ]}
        />
        <meshStandardMaterial
          color="#090d16"
          roughness={0.25}
          metalness={0.8}
        />
      </mesh>

      {/* 3. Gold Pedestal Top Plate */}
      <mesh position={[0, MEMBER_PEDESTAL_HEIGHT + 0.02, 0]}>
        <cylinderGeometry
          args={[MEMBER_PEDESTAL_BASE_RADIUS * 0.9, MEMBER_PEDESTAL_BASE_RADIUS * 0.85, 0.04, 8]}
        />
        <meshStandardMaterial
          color={THEME.GOLD_ACCENT_PRIMARY}
          roughness={0.2}
          metalness={0.9}
        />
      </mesh>

      {/* 4. Floating Holographic Avatar Crystal */}
      <group ref={badgeRef} position={[0, 1.6, 0]}>
        <mesh ref={crystalRef}>
          <octahedronGeometry args={[0.32, 0]} />
          <meshStandardMaterial
            color={member.color}
            emissive={member.color}
            emissiveIntensity={isActive ? 0.8 : 0.3}
            roughness={0.15}
            metalness={0.7}
            wireframe={!isActive}
          />
        </mesh>

        {/* Member Initial Glyph inside / on Crystal */}
        <Text
          position={[0, 0, 0.35]}
          fontSize={0.22}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
        >
          {member.avatarGlyph}
        </Text>

        {/* Representative Golden Star Banner */}
        {member.isRepresentative && (
          <group position={[0, 0.46, 0]}>
            <mesh>
              <boxGeometry args={[0.85, 0.16, 0.02]} />
              <meshStandardMaterial
                color="#b45309"
                emissive={THEME.GOLD_ACCENT_PRIMARY}
                emissiveIntensity={0.6}
              />
            </mesh>
            <Text
              position={[0, 0, 0.02]}
              fontSize={0.075}
              color={THEME.TEXT_GOLD_BRIGHT}
              anchorX="center"
              anchorY="middle"
            >
              ★ REPRESENTATIVE
            </Text>
          </group>
        )}
      </group>

      {/* 5. 3D Spatial Holographic Info Plaque (Facing User / Center) */}
      <group position={[0, MEMBER_PEDESTAL_HEIGHT + 1.25, 0]}>
        {/* Plaque Backing */}
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[1.9, 1.2]} />
          <meshStandardMaterial
            color="#050811"
            transparent
            opacity={0.88}
            roughness={0.2}
            metalness={0.8}
          />
        </mesh>

        {/* Glowing Border */}
        <mesh position={[0, 0, -0.015]}>
          <ringGeometry args={[0.95, 0.98, 4]} />
          <meshBasicMaterial color={isActive ? member.color : THEME.GOLD_ACCENT_PRIMARY} />
        </mesh>

        {/* Member Full Name */}
        <Text
          position={[0, 0.42, 0]}
          fontSize={0.11}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
        >
          {member.name}
        </Text>

        {/* Role & Trust Tier */}
        <Text
          position={[0, 0.26, 0]}
          fontSize={0.065}
          color={member.isRepresentative ? THEME.TEXT_GOLD_BRIGHT : THEME.TEXT_MUTED}
          anchorX="center"
          anchorY="middle"
        >
          {member.trustTier.replace('_', ' ')}
        </Text>

        {/* Divider */}
        <mesh position={[0, 0.17, 0]}>
          <planeGeometry args={[1.6, 0.005]} />
          <meshBasicMaterial color={member.color} transparent opacity={0.6} />
        </mesh>

        {/* Equity & Voting Power */}
        <group position={[-0.75, 0.05, 0]}>
          <Text fontSize={0.065} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
            EQUITY SHARE:
          </Text>
          <Text position={[1.5, 0, 0]} fontSize={0.075} color={member.color} anchorX="right" anchorY="middle">
            {`${member.sharePercentage.toFixed(1)}%`}
          </Text>
        </group>

        <group position={[-0.75, -0.09, 0]}>
          <Text fontSize={0.065} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
            VOTING POWER:
          </Text>
          <Text position={[1.5, 0, 0]} fontSize={0.075} color="#38bdf8" anchorX="right" anchorY="middle">
            {`${member.votingPowerPercentage.toFixed(1)}%`}
          </Text>
        </group>

        <group position={[-0.75, -0.23, 0]}>
          <Text fontSize={0.065} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
            FAIRNESS SCORE:
          </Text>
          <Text position={[1.5, 0, 0]} fontSize={0.075} color={THEME.CYBER_EMERALD} anchorX="right" anchorY="middle">
            {`${member.fairnessScore}%`}
          </Text>
        </group>

        {/* 6. Interactive Action Button */}
        <group position={[0, -0.42, 0]}>
          <mesh>
            <boxGeometry args={[1.4, 0.18, 0.02]} />
            <meshStandardMaterial
              color={isSelected ? THEME.AMBER_DARK : isHovered ? '#1e293b' : '#0a0f1d'}
              emissive={isSelected ? THEME.GOLD_ACCENT_PRIMARY : isHovered ? member.color : '#000000'}
              emissiveIntensity={isSelected ? 0.6 : 0.3}
            />
          </mesh>
          <Text
            position={[0, 0, 0.02]}
            fontSize={0.065}
            color={isSelected ? '#ffffff' : THEME.TEXT_GOLD_BRIGHT}
            anchorX="center"
            anchorY="middle"
          >
            {isSelected ? 'CLOSE DOSSIER' : 'INSPECT DOSSIER'}
          </Text>
        </group>
      </group>

      {/* 7. Expanded Spatial Dossier when Selected */}
      {isSelected && (
        <group position={[1.8, MEMBER_PEDESTAL_HEIGHT + 1.25, 0]} rotation={[0, -0.3, 0]}>
          {/* Dossier Slab */}
          <mesh position={[0, 0, -0.02]}>
            <planeGeometry args={[2.0, 1.4]} />
            <meshStandardMaterial
              color="#030712"
              transparent
              opacity={0.94}
              roughness={0.1}
              metalness={0.9}
            />
          </mesh>

          {/* Golden Dossier Header */}
          <Text
            position={[0, 0.52, 0]}
            fontSize={0.09}
            color={THEME.TEXT_GOLD_BRIGHT}
            anchorX="center"
            anchorY="middle"
          >
            CO-OWNER DOSSIER
          </Text>

          <mesh position={[0, 0.42, 0]}>
            <planeGeometry args={[1.7, 0.005]} />
            <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} />
          </mesh>

          {/* Detailed Specifications */}
          <group position={[-0.85, 0.28, 0]}>
            <Text fontSize={0.06} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
              CONTACT EMAIL:
            </Text>
            <Text position={[1.7, 0, 0]} fontSize={0.06} color="#ffffff" anchorX="right" anchorY="middle">
              {member.email}
            </Text>
          </group>

          <group position={[-0.85, 0.14, 0]}>
            <Text fontSize={0.06} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
              ENROLLED DATE:
            </Text>
            <Text position={[1.7, 0, 0]} fontSize={0.06} color="#ffffff" anchorX="right" anchorY="middle">
              {member.enrolledDate}
            </Text>
          </group>

          <group position={[-0.85, 0.0, 0]}>
            <Text fontSize={0.06} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
              BOOKINGS COMPLETED:
            </Text>
            <Text position={[1.7, 0, 0]} fontSize={0.06} color={THEME.CYBER_CYAN} anchorX="right" anchorY="middle">
              {`${member.totalBookingsLogged} SESSIONS`}
            </Text>
          </group>

          <group position={[-0.85, -0.14, 0]}>
            <Text fontSize={0.06} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
              SIGNATURE STATUS:
            </Text>
            <Text position={[1.7, 0, 0]} fontSize={0.06} color={THEME.CYBER_EMERALD} anchorX="right" anchorY="middle">
              VERIFIED VALID
            </Text>
          </group>

          <group position={[-0.85, -0.28, 0]}>
            <Text fontSize={0.06} color={THEME.TEXT_MUTED} anchorX="left" anchorY="middle">
              DISPUTE RECORD:
            </Text>
            <Text position={[1.7, 0, 0]} fontSize={0.06} color="#ffffff" anchorX="right" anchorY="middle">
              0 VIOLATIONS
            </Text>
          </group>

          <Text
            position={[0, -0.5, 0]}
            fontSize={0.055}
            color={THEME.TEXT_MUTED}
            anchorX="center"
            anchorY="middle"
          >
            CLICK OUTSIDE OR BUTTON TO DESELECT
          </Text>
        </group>
      )}
    </group>
  );
};
