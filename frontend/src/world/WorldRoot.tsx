import React, { useEffect, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import { EnvironmentManager } from './EnvironmentManager';
import { EnvironmentTransitionController } from './EnvironmentTransitionController';
import { WorldLoader } from './WorldLoader';
import { WorldPortal3D } from './WorldPortal3D';
import { WorldAccessNotice3D } from './WorldAccessNotice3D';
import { useWorldEnvironmentStore } from './useWorldEnvironmentStore';
import { useNavigationStore } from './useNavigationStore';
import { SECTOR_METADATA_REGISTRY, getSectorAtPosition } from './worldCoordinates';
import { PORTAL_NETWORK } from './portalNetwork';
import { getSectorSpawnPoint, getDefaultSpawnPoint } from './spawnPoints';
import { usePlayerStore } from '@/stores/usePlayerStore';
import { PlayerController } from '@/engine/player/PlayerController';
import { CentralGarage3D } from '@/garage/CentralGarage3D';
import { BookingChamber3D } from '@/booking/BookingChamber3D';
import { CoOwnershipAmphitheater3D } from '@/ownership/CoOwnershipAmphitheater3D';
import { FinanceCenter3D } from '@/finance/FinanceCenter3D';
import { SharedFundVault3D } from '@/vault/SharedFundVault3D';
import { DigitalContractRoom3D } from '@/contracts/DigitalContractRoom3D';
import { DecisionChamber3D } from '@/decision/DecisionChamber3D';
import { AIIntelligenceCenter3D } from '@/ai/AIIntelligenceCenter3D';
import { OperationsCenter3D } from '@/operations/OperationsCenter3D';
import { ServiceWorkshop3D } from '@/workshop/ServiceWorkshop3D';
import { DisputeRoom3D } from '@/dispute/DisputeRoom3D';
import { AdminCommandCenter3D } from '@/admin/AdminCommandCenter3D';
import type { SectorId } from './worldTypes';

export interface WorldRootProps {
  initialSector?: SectorId;
  showFloorGrid?: boolean;
}

/**
 * WorldRoot: Top-level 3D container component for EVShare's virtual metaverse world.
 * Manages dynamic environment lighting, world coordinate sectors, waypoints,
 * sector boundary detection, inter-sector 3D portals, and RBAC authorization feedback.
 */
export const WorldRoot: React.FC<WorldRootProps> = ({
  initialSector = 'SECURITY_CHECKPOINT',
  showFloorGrid = true,
}) => {
  const activeSectorId = useWorldEnvironmentStore((state) => state.activeSectorId);
  const setActiveSector = useWorldEnvironmentStore((state) => state.setActiveSector);
  const startTransition = useWorldEnvironmentStore((state) => state.startTransition);
  const activePreset = useWorldEnvironmentStore((state) => state.activePreset);

  const playerPosition = usePlayerStore((state) => state.position);
  const teleportTo = usePlayerStore((state) => state.teleportTo);

  const lastDetectedSector = useRef<SectorId>(activeSectorId);

  // Initialize player position to chosen spawn on mount
  useEffect(() => {
    setActiveSector(initialSector);
    useNavigationStore.setState({ currentSector: initialSector });
    const spawn = getSectorSpawnPoint(initialSector) || getDefaultSpawnPoint();
    teleportTo(spawn.position, spawn.rotation);
  }, [initialSector, setActiveSector, teleportTo]);

  // Per-frame sector boundary detection as player walks
  useFrame(() => {
    const currentSector = getSectorAtPosition(playerPosition);
    if (currentSector && currentSector !== lastDetectedSector.current) {
      const isAllowed = useNavigationStore.getState().canAccessSector(currentSector);
      if (!isAllowed) {
        useNavigationStore.getState().handleSectorBoundaryIntrusion(currentSector);
        return;
      }
      lastDetectedSector.current = currentSector;
      startTransition(currentSector);
      useNavigationStore.setState({ currentSector });
    }
  });

  return (
    <group name="EVShareWorldRoot">
      {/* 1. Dynamic Environment Lighting & Fog */}
      <EnvironmentManager />

      {/* 2. Environment Transition Interpolator */}
      <EnvironmentTransitionController />

      {/* 3. 3D Spatial World Loader Indicator */}
      <WorldLoader />

      {/* 4. Spatial Access Authorization Notice */}
      <WorldAccessNotice3D />

      {/* 5. Player Controller & Avatar */}
      <PlayerController />

      {/* 6. Master Metaverse Architectural Ground / Plaza */}
      <group name="MetaverseGroundPlaza">
        {/* Main central reflective ground plane */}
        <mesh position={[0, -0.05, 0]} receiveShadow rotation={[-Math.PI / 2, 0, 0]}>
          <planeGeometry args={[240, 240]} />
          <meshStandardMaterial
            color={activePreset.floorColor}
            roughness={0.25}
            metalness={0.65}
          />
        </mesh>

        {/* Global coordinate grid lines */}
        {showFloorGrid && (
          <gridHelper
            args={[240, 60, activePreset.gridColor, '#1e293b']}
            position={[0, 0.01, 0]}
          />
        )}
      </group>

      {/* 7. Contiguous Sector Foundations & Boundary Markers */}
      <group name="SectorWaypointsAndBoundaries">
        {Object.values(SECTOR_METADATA_REGISTRY).map((meta) => {
          const [cx, cy, cz] = meta.centerCoordinates;
          const isActive = meta.id === activeSectorId;

          const isCentralGarage = meta.id === 'CENTRAL_GARAGE';

          return (
            <group key={meta.id} position={[cx, cy, cz]} name={`Sector_${meta.id}`}>
              {/* Sector Specific 3D Infrastructure */}
              {meta.id === 'CENTRAL_GARAGE' ? (
                <CentralGarage3D />
              ) : meta.id === 'BOOKING_CHAMBER' ? (
                <BookingChamber3D />
              ) : meta.id === 'CO_OWNERSHIP_HALL' ? (
                <CoOwnershipAmphitheater3D />
              ) : meta.id === 'ENERGY_FINANCE_CENTER' ? (
                <FinanceCenter3D />
              ) : meta.id === 'SHARED_FUND_VAULT' ? (
                <SharedFundVault3D />
              ) : meta.id === 'DIGITAL_CONTRACT_ROOM' ? (
                <DigitalContractRoom3D />
              ) : meta.id === 'DECISION_CHAMBER' ? (
                <DecisionChamber3D />
              ) : meta.id === 'AI_INTELLIGENCE_CENTER' ? (
                <AIIntelligenceCenter3D />
              ) : meta.id === 'OPERATIONS_CENTER' ? (
                <OperationsCenter3D />
              ) : meta.id === 'SERVICE_WORKSHOP' ? (
                <ServiceWorkshop3D />
              ) : meta.id === 'DISPUTE_ROOM' ? (
                <DisputeRoom3D />
              ) : meta.id === 'ADMIN_COMMAND_CENTER' ? (
                <AdminCommandCenter3D />
              ) : (
                <>
                  {/* Generic Sector Center Pedestal Beacon */}
                  <mesh position={[0, 0.1, 0]} receiveShadow>
                    <cylinderGeometry args={[2.5, 2.7, 0.2, 32]} />
                    <meshStandardMaterial
                      color="#111827"
                      metalness={0.8}
                      roughness={0.2}
                      emissive={isActive ? (meta.id === 'DISPUTE_ROOM' ? '#ff1744' : '#00e5ff') : '#1e293b'}
                      emissiveIntensity={isActive ? 0.6 : 0.1}
                    />
                  </mesh>

                  {/* Glowing Floor Boundary Ring */}
                  <mesh position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]}>
                    <ringGeometry args={[meta.bounds.radius - 0.4, meta.bounds.radius, 48]} />
                    <meshBasicMaterial
                      color={isActive ? '#00e5ff' : '#334155'}
                      transparent
                      opacity={isActive ? 0.7 : 0.25}
                    />
                  </mesh>
                </>
              )}

              {/* Overhead Floating Sector Hologram Banner */}
              <group position={[0, 6.5, 0]}>
                <Text
                  position={[0, 0.4, 0]}
                  fontSize={0.65}
                  color={isActive ? '#ffffff' : '#94a3b8'}
                  anchorX="center"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/orbitron/v31/yMJRMIlzdpvBhQQL_Qq7dys.woff"
                >
                  {meta.name.toUpperCase()}
                </Text>
                <Text
                  position={[0, -0.25, 0]}
                  fontSize={0.26}
                  color={isActive ? '#00e5ff' : '#64748b'}
                  anchorX="center"
                  anchorY="middle"
                  font="https://fonts.gstatic.com/s/spacegrotesk/v16/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-g.woff"
                >
                  {meta.subtitle}
                </Text>
              </group>
            </group>
          );
        })}
      </group>

      {/* 8. Inter-Sector 3D Architectural Portals */}
      <group name="InterSectorPortals">
        {PORTAL_NETWORK.map((portal) => (
          <WorldPortal3D key={portal.id} portal={portal} />
        ))}
      </group>
    </group>
  );
};
