import React, { useMemo, useEffect } from 'react';
import { useFrame, useThree } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';
import { useOwnershipStore } from './useOwnershipStore';
import { CO_OWNERSHIP_HALL_LAYOUT } from './ownershipLayout';
import { AmphitheaterCentralDais3D } from './AmphitheaterCentralDais3D';
import { MemberPedestal3D } from './MemberPedestal3D';
import { ContractConsole3D } from './ContractConsole3D';
import { RulesHoloStela3D } from './RulesHoloStela3D';

export const CoOwnershipAmphitheater3D: React.FC = () => {
  const { camera } = useThree();

  const groups = useOwnershipStore((s) => s.groups);
  const activeGroupId = useOwnershipStore((s) => s.activeGroupId);
  const setActiveGroup = useOwnershipStore((s) => s.setActiveGroup);
  const cameraPreset = useOwnershipStore((s) => s.cameraPreset);
  const setCameraPreset = useOwnershipStore((s) => s.setCameraPreset);
  const selectedMemberId = useOwnershipStore((s) => s.selectedMemberId);
  const fetchOwnershipData = useOwnershipStore((s) => s.fetchOwnershipData);
  const authoritativeValidation = useOwnershipStore((s) => s.authoritativeValidation);
  const isLoading = useOwnershipStore((s) => s.isLoading);

  useEffect(() => {
    fetchOwnershipData();
  }, [fetchOwnershipData]);

  const activeGroup = useMemo(
    () => groups.find((g) => g.id === activeGroupId) || groups[0],
    [groups, activeGroupId]
  );

  const {
    AMPHITHEATER_RADIUS,
    MEMBER_PEDESTAL_RADIUS,
    CAMERA_PRESETS,
    THEME,
  } = CO_OWNERSHIP_HALL_LAYOUT;

  // Perimeter architectural pillars for ambient hall enclosure
  const perimeterPillars = useMemo(() => {
    const count = 16;
    const radius = 11.5;
    return Array.from({ length: count }).map((_, i) => {
      const angle = (i / count) * Math.PI * 2;
      return {
        x: Math.cos(angle) * radius,
        z: Math.sin(angle) * radius,
        rotY: -angle,
      };
    });
  }, []);

  // Smooth camera positioning based on preset or selected member
  useFrame((_, delta) => {
    let targetPos: [number, number, number] = CAMERA_PRESETS.HALL_OVERVIEW.position;
    let lookTarget: [number, number, number] = CAMERA_PRESETS.HALL_OVERVIEW.target;

    if (selectedMemberId !== null) {
      const member = activeGroup.members.find((m) => m.id === selectedMemberId);
      if (member) {
        const pedX = Math.cos(member.pedestalAngle) * MEMBER_PEDESTAL_RADIUS;
        const pedZ = Math.sin(member.pedestalAngle) * MEMBER_PEDESTAL_RADIUS;
        // Position camera in front of pedestal looking at member info
        targetPos = [pedX * 0.7, 2.2, pedZ * 0.7];
        lookTarget = [pedX, 1.8, pedZ];
      }
    } else if (cameraPreset === 'EQUITY_CORE') {
      targetPos = CAMERA_PRESETS.EQUITY_CORE.position;
      lookTarget = CAMERA_PRESETS.EQUITY_CORE.target;
    } else if (cameraPreset === 'CONTRACT_TERMINAL') {
      targetPos = CAMERA_PRESETS.CONTRACT_TERMINAL.position;
      lookTarget = CAMERA_PRESETS.CONTRACT_TERMINAL.target;
    } else if (cameraPreset === 'RULES_STELA') {
      targetPos = CAMERA_PRESETS.RULES_STELA.position;
      lookTarget = CAMERA_PRESETS.RULES_STELA.target;
    } else if (cameraPreset === 'HISTORY_STELA') {
      targetPos = CAMERA_PRESETS.HISTORY_STELA.position;
      lookTarget = CAMERA_PRESETS.HISTORY_STELA.target;
    }

    const tVec = new THREE.Vector3(...targetPos);
    // Smooth lerp camera position
    camera.position.lerp(tVec, delta * 2.5);
  });

  return (
    <group name="CoOwnershipAmphitheater3D">
      {/* 1. Grand Amphitheater Tiered Floor */}
      <mesh position={[0, -0.05, 0]} receiveShadow>
        <cylinderGeometry args={[AMPHITHEATER_RADIUS, AMPHITHEATER_RADIUS + 0.5, 0.1, 48]} />
        <meshStandardMaterial
          color="#060913"
          roughness={0.4}
          metalness={0.7}
        />
      </mesh>

      {/* Outer Golden Border Rim */}
      <mesh position={[0, 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[AMPHITHEATER_RADIUS - 0.2, AMPHITHEATER_RADIUS, 48]} />
        <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} transparent opacity={0.6} />
      </mesh>

      {/* Mid Amphitheater Ring Accent */}
      <mesh position={[0, 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[MEMBER_PEDESTAL_RADIUS - 0.05, MEMBER_PEDESTAL_RADIUS + 0.05, 48]} />
        <meshBasicMaterial color={THEME.AMBER_DARK} transparent opacity={0.4} />
      </mesh>

      {/* Inner Central Ring Accent */}
      <mesh position={[0, 0.01, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[3.2, 3.25, 48]} />
        <meshBasicMaterial color={THEME.GOLD_ACCENT_GLOW} transparent opacity={0.5} />
      </mesh>

      {/* 2. Perimeter Colonnade Pillars & Sconces */}
      {perimeterPillars.map((p, idx) => (
        <group key={idx} position={[p.x, 0, p.z]} rotation={[0, p.rotY, 0]}>
          <mesh position={[0, 3.5, 0]} castShadow receiveShadow>
            <cylinderGeometry args={[0.25, 0.35, 7.0, 12]} />
            <meshStandardMaterial
              color="#090d16"
              roughness={0.3}
              metalness={0.8}
            />
          </mesh>

          {/* Pillar Golden Collar */}
          <mesh position={[0, 6.8, 0]}>
            <cylinderGeometry args={[0.35, 0.3, 0.2, 12]} />
            <meshStandardMaterial
              color={THEME.GOLD_ACCENT_PRIMARY}
              roughness={0.2}
              metalness={0.9}
            />
          </mesh>

          {/* Sconce Light Beacon */}
          <mesh position={[0, 4.2, 0.35]}>
            <boxGeometry args={[0.15, 0.4, 0.08]} />
            <meshStandardMaterial
              color={THEME.GOLD_ACCENT_PRIMARY}
              emissive={THEME.GOLD_ACCENT_PRIMARY}
              emissiveIntensity={0.8}
            />
          </mesh>
        </group>
      ))}

      {/* 3. Grand Overhead Syndicate Header Banner */}
      <group position={[0, 6.2, -6.5]} rotation={[0.15, 0, 0]}>
        <mesh position={[0, 0, -0.05]}>
          <planeGeometry args={[8.0, 1.55]} />
          <meshStandardMaterial
            color="#040711"
            roughness={0.1}
            metalness={0.9}
            transparent
            opacity={0.92}
          />
        </mesh>
        <mesh position={[0, 0, -0.045]}>
          <ringGeometry args={[3.98, 4.0, 4]} />
          <meshBasicMaterial color={THEME.GOLD_ACCENT_PRIMARY} />
        </mesh>

        <Text
          position={[0, 0.46, 0]}
          fontSize={0.17}
          color={THEME.TEXT_GOLD_BRIGHT}
          anchorX="center"
          anchorY="middle"
          letterSpacing={0.08}
        >
          ✦ EVSHARE CO-OWNERSHIP COUNCIL ✦
        </Text>

        <Text
          position={[0, 0.2, 0]}
          fontSize={0.11}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
        >
          {activeGroup.name.toUpperCase()}
        </Text>

        <Text
          position={[0, -0.04, 0]}
          fontSize={0.072}
          color={THEME.TEXT_MUTED}
          anchorX="center"
          anchorY="middle"
        >
          {`VEHICLE: ${activeGroup.vehicleModelName || 'EV Model'} (${activeGroup.vehiclePlate || 'N/A'}) • VALUATION: ${(activeGroup.totalValuationVnd || 0).toLocaleString()} VND`}
        </Text>

        {/* Backend Authoritative Equity Invariant Indicator */}
        <Text
          position={[0, -0.22, 0]}
          fontSize={0.065}
          color={
            authoritativeValidation?.isValid
              ? THEME.CYBER_EMERALD
              : isLoading
              ? THEME.TEXT_GOLD_BRIGHT
              : '#ef4444'
          }
          anchorX="center"
          anchorY="middle"
        >
          {isLoading
            ? '⏳ SYNCING WITH SPRING BOOT AUTHORITATIVE SERVICE...'
            : authoritativeValidation?.isValid
            ? `[ ✓ AUTHORITATIVE EQUITY VERIFIED: ${authoritativeValidation.totalEquity.toFixed(2)}% VIA BACKEND ]`
            : '⚠ BACKEND EQUITY INVARIANT CHECK FAILED'}
        </Text>

        {/* 3D Syndicate Group Selector Buttons */}
        <group position={[0, -0.48, 0]}>
          {groups.map((grp, gIdx) => {
            const isGrpActive = grp.id === activeGroupId;
            const xOffset = (gIdx - (groups.length - 1) / 2) * 2.8;

            return (
              <group
                key={grp.id}
                position={[xOffset, 0, 0]}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveGroup(grp.id);
                }}
                onPointerEnter={(e) => {
                  e.stopPropagation();
                  document.body.style.cursor = 'pointer';
                }}
                onPointerLeave={(e) => {
                  e.stopPropagation();
                  document.body.style.cursor = 'auto';
                }}
              >
                <mesh>
                  <boxGeometry args={[2.5, 0.18, 0.02]} />
                  <meshStandardMaterial
                    color={isGrpActive ? THEME.AMBER_DARK : '#090e1a'}
                    emissive={isGrpActive ? THEME.GOLD_ACCENT_PRIMARY : '#000000'}
                    emissiveIntensity={isGrpActive ? 0.6 : 0}
                  />
                </mesh>
                <Text
                  position={[0, 0, 0.02]}
                  fontSize={0.062}
                  color={isGrpActive ? '#ffffff' : THEME.TEXT_MUTED}
                  anchorX="center"
                  anchorY="middle"
                >
                  {isGrpActive ? `▶ ${grp.name}` : grp.name}
                </Text>
              </group>
            );
          })}
        </group>
      </group>

      {/* 4. Spatial View Preset Switcher Dock (Floating in front floor) */}
      <group position={[0, 0.6, 6.8]} rotation={[-0.45, 0, 0]}>
        <mesh position={[0, 0, -0.02]}>
          <planeGeometry args={[6.2, 0.38]} />
          <meshStandardMaterial
            color="#040711"
            transparent
            opacity={0.88}
            roughness={0.2}
            metalness={0.8}
          />
        </mesh>

        <Text position={[-2.8, 0, 0]} fontSize={0.062} color={THEME.TEXT_GOLD_BRIGHT} anchorX="left" anchorY="middle">
          GÓC NHÌN:
        </Text>

        {[
          { id: 'HALL_OVERVIEW' as const, label: 'TOÀN CẢNH' },
          { id: 'EQUITY_CORE' as const, label: 'LÕI CỔ PHẦN' },
          { id: 'CONTRACT_TERMINAL' as const, label: 'BÀN HỢP ĐỒNG' },
          { id: 'RULES_STELA' as const, label: 'BIA QUY TẮC' },
          { id: 'HISTORY_STELA' as const, label: 'SỔ CÁI' },
        ].map((view, vIdx) => {
          const isSelected = cameraPreset === view.id;
          const xPos = -1.75 + vIdx * 0.95;

          return (
            <group
              key={view.id}
              position={[xPos, 0, 0]}
              onClick={(e) => {
                e.stopPropagation();
                setCameraPreset(view.id);
              }}
              onPointerEnter={(e) => {
                e.stopPropagation();
                document.body.style.cursor = 'pointer';
              }}
              onPointerLeave={(e) => {
                e.stopPropagation();
                document.body.style.cursor = 'auto';
              }}
            >
              <mesh>
                <boxGeometry args={[0.88, 0.22, 0.02]} />
                <meshStandardMaterial
                  color={isSelected ? THEME.AMBER_DARK : '#090d16'}
                  emissive={isSelected ? THEME.GOLD_ACCENT_PRIMARY : '#000000'}
                  emissiveIntensity={isSelected ? 0.6 : 0}
                />
              </mesh>
              <Text
                position={[0, 0, 0.02]}
                fontSize={0.048}
                color={isSelected ? '#ffffff' : THEME.TEXT_MUTED}
                anchorX="center"
                anchorY="middle"
              >
                {view.label}
              </Text>
            </group>
          );
        })}
      </group>

      {/* 5. Central Dais & 3D Pie / Torus Equity Visualization */}
      <AmphitheaterCentralDais3D group={activeGroup} />

      {/* 6. Co-Owner Pedestals Distributed Along Amphitheater Semi-Circle */}
      {activeGroup.members.map((member) => (
        <MemberPedestal3D key={member.id} member={member} />
      ))}

      {/* 7. Contract Console Lectern (Left Sector) */}
      <ContractConsole3D />

      {/* 8. Syndicate Governance Rules Stela (Right Sector) */}
      <RulesHoloStela3D />
    </group>
  );
};
