import type { SectorId, SpawnPoint } from './worldTypes';

export const SPAWN_POINTS: Record<string, SpawnPoint> = {
  SPAWN_SECURITY_ENTRY: {
    id: 'SPAWN_SECURITY_ENTRY',
    sectorId: 'SECURITY_CHECKPOINT',
    name: 'Security Gate Entry',
    position: [0, 0, 88],
    rotation: [0, Math.PI, 0],
    cameraPitch: 0,
    cameraYaw: Math.PI,
    isDefault: true,
  },
  SPAWN_GARAGE_CENTER: {
    id: 'SPAWN_GARAGE_CENTER',
    sectorId: 'CENTRAL_GARAGE',
    name: 'Showroom Fleet Hub',
    position: [0, 0, 15],
    rotation: [0, Math.PI, 0],
    cameraPitch: -0.05,
    cameraYaw: Math.PI,
  },
  SPAWN_CO_OWNERSHIP_ENTRY: {
    id: 'SPAWN_CO_OWNERSHIP_ENTRY',
    sectorId: 'CO_OWNERSHIP_HALL',
    name: 'Co-Ownership Amphitheater Entry',
    position: [40, 0, 48],
    rotation: [0, Math.PI, 0],
    requiredRole: 'ROLE_CO_OWNER',
  },
  SPAWN_BOOKING_ENTRY: {
    id: 'SPAWN_BOOKING_ENTRY',
    sectorId: 'BOOKING_CHAMBER',
    name: 'Booking Chamber Arch',
    position: [0, 0, -32],
    rotation: [0, 0, 0],
    requiredRole: 'ROLE_CO_OWNER',
  },
  SPAWN_FINANCE_ENTRY: {
    id: 'SPAWN_FINANCE_ENTRY',
    sectorId: 'ENERGY_FINANCE_CENTER',
    name: 'Finance Lab Entry',
    position: [0, 0, -72],
    rotation: [0, 0, 0],
    requiredRole: 'ROLE_CO_OWNER',
  },
  SPAWN_VAULT_ENTRY: {
    id: 'SPAWN_VAULT_ENTRY',
    sectorId: 'SHARED_FUND_VAULT',
    name: 'Shared Fund Vault Doorway',
    position: [-40, 0, -72],
    rotation: [0, 0, 0],
    requiredRole: 'ROLE_CO_OWNER',
  },
  SPAWN_CONTRACT_ENTRY: {
    id: 'SPAWN_CONTRACT_ENTRY',
    sectorId: 'DIGITAL_CONTRACT_ROOM',
    name: 'Digital Contract Room Portal',
    position: [40, 0, -32],
    rotation: [0, 0, 0],
    requiredRole: 'ROLE_CO_OWNER',
  },
  SPAWN_DECISION_ENTRY: {
    id: 'SPAWN_DECISION_ENTRY',
    sectorId: 'DECISION_CHAMBER',
    name: 'Decision Chamber Floor',
    position: [40, 0, -72],
    rotation: [0, 0, 0],
    requiredRole: 'ROLE_CO_OWNER',
  },
  SPAWN_AI_ENTRY: {
    id: 'SPAWN_AI_ENTRY',
    sectorId: 'AI_INTELLIGENCE_CENTER',
    name: 'AI Neural Nexus Entry',
    position: [40, 0, 8],
    rotation: [0, Math.PI, 0],
    requiredRole: 'ROLE_CO_OWNER',
  },
  SPAWN_OPERATIONS_ENTRY: {
    id: 'SPAWN_OPERATIONS_ENTRY',
    sectorId: 'OPERATIONS_CENTER',
    name: 'Operations Dispatch Hangar',
    position: [-40, 0, 8],
    rotation: [0, Math.PI, 0],
    requiredRole: 'ROLE_STAFF',
  },
  SPAWN_WORKSHOP_ENTRY: {
    id: 'SPAWN_WORKSHOP_ENTRY',
    sectorId: 'SERVICE_WORKSHOP',
    name: 'Service Bay Inspection Pad',
    position: [-40, 0, 48],
    rotation: [0, Math.PI, 0],
    requiredRole: 'ROLE_STAFF',
  },
  SPAWN_DISPUTE_ENTRY: {
    id: 'SPAWN_DISPUTE_ENTRY',
    sectorId: 'DISPUTE_ROOM',
    name: 'Dispute Room Antechamber',
    position: [-40, 0, 88],
    rotation: [0, Math.PI, 0],
  },
  SPAWN_ADMIN_ENTRY: {
    id: 'SPAWN_ADMIN_ENTRY',
    sectorId: 'ADMIN_COMMAND_CENTER',
    name: 'Orbital Elevator Arrival Pad',
    position: [0, 25, 12],
    rotation: [0, Math.PI, 0],
    requiredRole: 'ROLE_ADMIN',
  },
};

export function getSpawnPoint(spawnId: string): SpawnPoint {
  const spawn = SPAWN_POINTS[spawnId];
  if (!spawn) {
    console.warn(`[SpawnPoints] Unknown spawn point ID "${spawnId}". Falling back to default.`);
    return SPAWN_POINTS.SPAWN_SECURITY_ENTRY;
  }
  return spawn;
}

export function getDefaultSpawnPoint(): SpawnPoint {
  return SPAWN_POINTS.SPAWN_SECURITY_ENTRY;
}

export function getSectorSpawnPoint(sectorId: SectorId): SpawnPoint {
  for (const spawn of Object.values(SPAWN_POINTS)) {
    if (spawn.sectorId === sectorId) {
      return spawn;
    }
  }
  return getDefaultSpawnPoint();
}

export function getAllSpawnPoints(): SpawnPoint[] {
  return Object.values(SPAWN_POINTS);
}
