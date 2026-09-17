import type { Vector3Tuple } from 'three';

/**
 * EVShare 3D Metaverse Sector Identifiers
 * 12 Functional Sectors + 1 Security Checkpoint as defined in docs/WORLD_ARCHITECTURE.md
 */
export type SectorId =
  | 'SECURITY_CHECKPOINT'
  | 'CENTRAL_GARAGE'
  | 'CO_OWNERSHIP_HALL'
  | 'BOOKING_CHAMBER'
  | 'ENERGY_FINANCE_CENTER'
  | 'SHARED_FUND_VAULT'
  | 'DIGITAL_CONTRACT_ROOM'
  | 'DECISION_CHAMBER'
  | 'AI_INTELLIGENCE_CENTER'
  | 'OPERATIONS_CENTER'
  | 'SERVICE_WORKSHOP'
  | 'DISPUTE_ROOM'
  | 'ADMIN_COMMAND_CENTER';

export type UserRole = 'ROLE_CO_OWNER' | 'ROLE_STAFF' | 'ROLE_ADMIN' | 'GUEST';

export interface SectorBounds {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
  minZ: number;
  maxZ: number;
  centerX: number;
  centerY: number;
  centerZ: number;
  radius: number;
}

export interface SpawnPoint {
  id: string;
  sectorId: SectorId;
  name: string;
  position: Vector3Tuple;
  rotation: Vector3Tuple;
  cameraPitch?: number;
  cameraYaw?: number;
  requiredRole?: UserRole;
  isDefault?: boolean;
}

export interface EnvironmentPreset {
  id: string;
  name: string;
  ambientColor: string;
  ambientIntensity: number;
  skyColor: string;
  groundColor: string;
  keyLightColor: string;
  keyLightIntensity: number;
  keyLightPosition: Vector3Tuple;
  fillLightColor: string;
  fillLightIntensity: number;
  rimLightColor: string;
  rimLightIntensity: number;
  fogColor: string;
  fogNear: number;
  fogFar: number;
  floorColor: string;
  gridColor: string;
  accentColor: string;
}

export interface WorldPortalDefinition {
  id: string;
  name: string;
  fromSector: SectorId;
  toSector: SectorId;
  position: Vector3Tuple;
  destinationPosition: Vector3Tuple;
  color: string;
  requiredRole?: UserRole;
  label: string;
}

export interface SectorMetadata {
  id: SectorId;
  name: string;
  subtitle: string;
  description: string;
  centerCoordinates: Vector3Tuple;
  bounds: SectorBounds;
  primaryEnvironmentPreset: string;
  requiredRole?: UserRole;
  spawnPointId: string;
}

export interface WorldInteractiveEntity {
  id: string;
  sectorId: SectorId;
  name: string;
  category: 'VEHICLE' | 'TERMINAL' | 'PEDESTAL' | 'PORTAL' | 'HOLOGRAM' | 'KIOSK';
  position: Vector3Tuple;
  interactionDistance: number;
  requiredRole?: UserRole;
  focusFramingDistance?: number;
  metadata?: Record<string, unknown>;
}
