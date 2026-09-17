import type { Vector3Tuple } from 'three';
import type { SectorId, SectorBounds, SectorMetadata, WorldPortalDefinition } from './worldTypes';

export const SECTOR_CENTERS: Record<SectorId, Vector3Tuple> = {
  SECURITY_CHECKPOINT: [0, 0, 80],
  CENTRAL_GARAGE: [0, 0, 0],
  CO_OWNERSHIP_HALL: [40, 0, 40],
  BOOKING_CHAMBER: [0, 0, -40],
  ENERGY_FINANCE_CENTER: [0, 0, -80],
  SHARED_FUND_VAULT: [-40, 0, -80],
  DIGITAL_CONTRACT_ROOM: [40, 0, -80],
  DECISION_CHAMBER: [40, 0, -40],
  AI_INTELLIGENCE_CENTER: [40, 0, 0],
  OPERATIONS_CENTER: [-40, 0, 0],
  SERVICE_WORKSHOP: [-40, 0, 40],
  DISPUTE_ROOM: [-40, 0, 80],
  ADMIN_COMMAND_CENTER: [0, 25, 0],
};

function createSectorBounds(center: Vector3Tuple, halfWidth: number, halfDepth: number, height = 12): SectorBounds {
  return {
    minX: center[0] - halfWidth,
    maxX: center[0] + halfWidth,
    minY: center[1],
    maxY: center[1] + height,
    minZ: center[2] - halfDepth,
    maxZ: center[2] + halfDepth,
    centerX: center[0],
    centerY: center[1],
    centerZ: center[2],
    radius: Math.sqrt(halfWidth * halfWidth + halfDepth * halfDepth),
  };
}

export const SECTOR_METADATA_REGISTRY: Record<SectorId, SectorMetadata> = {
  SECURITY_CHECKPOINT: {
    id: 'SECURITY_CHECKPOINT',
    name: 'Security Checkpoint',
    subtitle: 'Gateway & Biometric Identity Verification',
    description: 'High-tech entry security gate with atmospheric volumetric fog and laser barriers.',
    centerCoordinates: SECTOR_CENTERS.SECURITY_CHECKPOINT,
    bounds: createSectorBounds(SECTOR_CENTERS.SECURITY_CHECKPOINT, 14, 14),
    primaryEnvironmentPreset: 'SECURITY_GATE',
    spawnPointId: 'SPAWN_SECURITY_ENTRY',
  },
  CENTRAL_GARAGE: {
    id: 'CENTRAL_GARAGE',
    name: 'EV Central Garage',
    subtitle: 'Metaverse Showroom & Digital Twin Fleet Hub',
    description: 'Sleek, futuristic architectural showroom with polished reflective flooring, ambient skylights, and active charging stalls.',
    centerCoordinates: SECTOR_CENTERS.CENTRAL_GARAGE,
    bounds: createSectorBounds(SECTOR_CENTERS.CENTRAL_GARAGE, 16, 16),
    primaryEnvironmentPreset: 'GARAGE_DAYLIGHT',
    spawnPointId: 'SPAWN_GARAGE_CENTER',
  },
  CO_OWNERSHIP_HALL: {
    id: 'CO_OWNERSHIP_HALL',
    name: 'Co-Ownership Hall',
    subtitle: 'Fractional Syndicate Identity & Trust Registry',
    description: 'Cylindrical amphitheater with dark obsidian finishes and golden neon accents displaying member equity pedestals.',
    centerCoordinates: SECTOR_CENTERS.CO_OWNERSHIP_HALL,
    bounds: createSectorBounds(SECTOR_CENTERS.CO_OWNERSHIP_HALL, 14, 14),
    primaryEnvironmentPreset: 'GOLDEN_OBSIDIAN',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_CO_OWNERSHIP_ENTRY',
  },
  BOOKING_CHAMBER: {
    id: 'BOOKING_CHAMBER',
    name: 'Booking Chamber',
    subtitle: 'Chrono-Spatial Timeline & Reservation Arc',
    description: 'Surrounded by a sweeping panoramic calendar aura with interactive draggable time handles.',
    centerCoordinates: SECTOR_CENTERS.BOOKING_CHAMBER,
    bounds: createSectorBounds(SECTOR_CENTERS.BOOKING_CHAMBER, 14, 14),
    primaryEnvironmentPreset: 'CHRONO_CYAN',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_BOOKING_ENTRY',
  },
  ENERGY_FINANCE_CENTER: {
    id: 'ENERGY_FINANCE_CENTER',
    name: 'Energy & Finance Center',
    subtitle: 'Expense Allocation Laboratory & Payment Kiosk',
    description: 'Clean financial laboratory with emerald data pipelines, floating expense crystals, and contactless payment.',
    centerCoordinates: SECTOR_CENTERS.ENERGY_FINANCE_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.ENERGY_FINANCE_CENTER, 14, 14),
    primaryEnvironmentPreset: 'FINANCE_EMERALD',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_FINANCE_ENTRY',
  },
  SHARED_FUND_VAULT: {
    id: 'SHARED_FUND_VAULT',
    name: 'Shared Fund Vault',
    subtitle: 'Syndicate Treasury & Liquid Reserve Column',
    description: 'Cybernetic bank vault with heavy titanium locking mechanisms, liquid balance column, and scrolling ribbon.',
    centerCoordinates: SECTOR_CENTERS.SHARED_FUND_VAULT,
    bounds: createSectorBounds(SECTOR_CENTERS.SHARED_FUND_VAULT, 14, 14),
    primaryEnvironmentPreset: 'VAULT_AMBER',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_VAULT_ENTRY',
  },
  DIGITAL_CONTRACT_ROOM: {
    id: 'DIGITAL_CONTRACT_ROOM',
    name: 'Digital Contract Room',
    subtitle: 'Executive Legal Suite & Holographic Signatures',
    description: 'Executive legal chamber with floating 3D manuscripts and biometric signature touch plates.',
    centerCoordinates: SECTOR_CENTERS.DIGITAL_CONTRACT_ROOM,
    bounds: createSectorBounds(SECTOR_CENTERS.DIGITAL_CONTRACT_ROOM, 14, 14),
    primaryEnvironmentPreset: 'EXECUTIVE_SLATE',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_CONTRACT_ENTRY',
  },
  DECISION_CHAMBER: {
    id: 'DECISION_CHAMBER',
    name: 'Decision Chamber',
    subtitle: 'Democratic Governance & Parliamentary Quorum Dais',
    description: 'Futuristic parliamentary arena with proposal hologram pod, voting pedestals, and live quorum fluid column.',
    centerCoordinates: SECTOR_CENTERS.DECISION_CHAMBER,
    bounds: createSectorBounds(SECTOR_CENTERS.DECISION_CHAMBER, 14, 14),
    primaryEnvironmentPreset: 'PARLIAMENT_INDIGO',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_DECISION_ENTRY',
  },
  AI_INTELLIGENCE_CENTER: {
    id: 'AI_INTELLIGENCE_CENTER',
    name: 'AI Mobility Intelligence Center',
    subtitle: 'Neural Core & Predictive Analytics Nexus',
    description: 'Neural nexus with spinning concentric gyroscopic rings and floating advisory recommendation nodes.',
    centerCoordinates: SECTOR_CENTERS.AI_INTELLIGENCE_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.AI_INTELLIGENCE_CENTER, 14, 14),
    primaryEnvironmentPreset: 'NEURAL_PURPLE',
    requiredRole: 'ROLE_CO_OWNER',
    spawnPointId: 'SPAWN_AI_ENTRY',
  },
  OPERATIONS_CENTER: {
    id: 'OPERATIONS_CENTER',
    name: 'Operations Center',
    subtitle: 'Staff Diagnostic Bench & QR Station',
    description: 'Industrial control hangar with 3D QR scanner station and real-time OBD-II telemetry diagnostic bench.',
    centerCoordinates: SECTOR_CENTERS.OPERATIONS_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.OPERATIONS_CENTER, 14, 14),
    primaryEnvironmentPreset: 'OPERATIONS_ORANGE',
    requiredRole: 'ROLE_STAFF',
    spawnPointId: 'SPAWN_OPERATIONS_ENTRY',
  },
  SERVICE_WORKSHOP: {
    id: 'SERVICE_WORKSHOP',
    name: 'Service Workshop',
    subtitle: 'Maintenance Bay & Hydraulic Lift',
    description: 'Automotive service bay with hydraulic vehicle lift, robotic arms, and part replacement stations.',
    centerCoordinates: SECTOR_CENTERS.SERVICE_WORKSHOP,
    bounds: createSectorBounds(SECTOR_CENTERS.SERVICE_WORKSHOP, 14, 14),
    primaryEnvironmentPreset: 'WORKSHOP_STEEL',
    requiredRole: 'ROLE_STAFF',
    spawnPointId: 'SPAWN_WORKSHOP_ENTRY',
  },
  DISPUTE_ROOM: {
    id: 'DISPUTE_ROOM',
    name: 'Dispute Resolution Room',
    subtitle: 'Neutral Deliberation Chamber & Evidence Holotank',
    description: 'Neutral arbitration chamber with floating crimson dispute crystal and 3D defect coordinate holotank.',
    centerCoordinates: SECTOR_CENTERS.DISPUTE_ROOM,
    bounds: createSectorBounds(SECTOR_CENTERS.DISPUTE_ROOM, 14, 14),
    primaryEnvironmentPreset: 'DISPUTE_CRIMSON',
    spawnPointId: 'SPAWN_DISPUTE_ENTRY',
  },
  ADMIN_COMMAND_CENTER: {
    id: 'ADMIN_COMMAND_CENTER',
    name: 'Admin Command Center',
    subtitle: 'Orbital Oversight Deck & The 7 Command Cores',
    description: 'Elevated observation deck overlooking the metaverse, housing the 7 interactive administrative monolithic cores.',
    centerCoordinates: SECTOR_CENTERS.ADMIN_COMMAND_CENTER,
    bounds: createSectorBounds(SECTOR_CENTERS.ADMIN_COMMAND_CENTER, 16, 16, 15),
    primaryEnvironmentPreset: 'COMMAND_HORIZON',
    requiredRole: 'ROLE_ADMIN',
    spawnPointId: 'SPAWN_ADMIN_ENTRY',
  },
};

export const WORLD_PORTALS: WorldPortalDefinition[] = [
  {
    id: 'PORTAL_GATE_TO_GARAGE',
    name: 'Gateway to Central Garage',
    label: 'EV SHOWROOM',
    fromSector: 'SECURITY_CHECKPOINT',
    toSector: 'CENTRAL_GARAGE',
    position: [0, 0, 70],
    destinationPosition: [0, 0, 18],
    color: '#00e5ff',
  },
  {
    id: 'PORTAL_GARAGE_TO_GATE',
    name: 'Exit to Security Checkpoint',
    label: 'SECURITY EXIT',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'SECURITY_CHECKPOINT',
    position: [0, 0, 22],
    destinationPosition: [0, 0, 72],
    color: '#00e5ff',
  },
  {
    id: 'PORTAL_GARAGE_TO_BOOKING',
    name: 'Portal to Booking Chamber',
    label: 'BOOKING CHAMBER',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'BOOKING_CHAMBER',
    position: [0, 0, -20],
    destinationPosition: [0, 0, -32],
    color: '#00e5ff',
    requiredRole: 'ROLE_CO_OWNER',
  },
  {
    id: 'PORTAL_GARAGE_TO_CO_OWNERSHIP',
    name: 'Portal to Co-Ownership Hall',
    label: 'CO-OWNERSHIP HALL',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'CO_OWNERSHIP_HALL',
    position: [20, 0, 5],
    destinationPosition: [32, 0, 12],
    color: '#ffab00',
    requiredRole: 'ROLE_CO_OWNER',
  },
  {
    id: 'PORTAL_GARAGE_TO_AI',
    name: 'Portal to AI Intelligence',
    label: 'AI NEXUS',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'AI_INTELLIGENCE_CENTER',
    position: [20, 0, -5],
    destinationPosition: [32, 0, -2],
    color: '#b388ff',
    requiredRole: 'ROLE_CO_OWNER',
  },
  {
    id: 'PORTAL_GARAGE_TO_OPERATIONS',
    name: 'Portal to Operations Center',
    label: 'STAFF OPERATIONS',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'OPERATIONS_CENTER',
    position: [-20, 0, 0],
    destinationPosition: [-32, 0, 0],
    color: '#ff9100',
    requiredRole: 'ROLE_STAFF',
  },
  {
    id: 'PORTAL_GARAGE_TO_ADMIN',
    name: 'Orbital Elevator to Admin Command',
    label: 'ADMIN COMMAND DECK',
    fromSector: 'CENTRAL_GARAGE',
    toSector: 'ADMIN_COMMAND_CENTER',
    position: [0, 0, 0],
    destinationPosition: [0, 25, 10],
    color: '#ffffff',
    requiredRole: 'ROLE_ADMIN',
  },
];

/**
 * Returns the sector containing the given 3D position, or null if outside all sectors.
 */
export function getSectorAtPosition(pos: Vector3Tuple): SectorId | null {
  const [x, y, z] = pos;
  for (const [sectorId, meta] of Object.entries(SECTOR_METADATA_REGISTRY)) {
    const { minX, maxX, minY, maxY, minZ, maxZ } = meta.bounds;
    if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
      return sectorId as SectorId;
    }
  }
  return null;
}

/**
 * Returns the nearest sector center to the given 3D coordinates.
 */
export function getNearestSector(pos: Vector3Tuple): SectorId {
  let nearestId: SectorId = 'CENTRAL_GARAGE';
  let minDistanceSq = Number.MAX_VALUE;

  for (const [sectorId, meta] of Object.entries(SECTOR_METADATA_REGISTRY)) {
    const [cx, cy, cz] = meta.centerCoordinates;
    const dx = pos[0] - cx;
    const dy = pos[1] - cy;
    const dz = pos[2] - cz;
    const distSq = dx * dx + dy * dy + dz * dz;

    if (distSq < minDistanceSq) {
      minDistanceSq = distSq;
      nearestId = sectorId as SectorId;
    }
  }

  return nearestId;
}

/**
 * Tests if a given 3D point is within the bounds of a specified sector.
 */
export function isPositionInSector(pos: Vector3Tuple, sectorId: SectorId): boolean {
  const meta = SECTOR_METADATA_REGISTRY[sectorId];
  if (!meta) return false;
  const { minX, maxX, minY, maxY, minZ, maxZ } = meta.bounds;
  return (
    pos[0] >= minX &&
    pos[0] <= maxX &&
    pos[1] >= minY &&
    pos[1] <= maxY &&
    pos[2] >= minZ &&
    pos[2] <= maxZ
  );
}
