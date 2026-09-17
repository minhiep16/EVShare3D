import type { Vector3Tuple } from 'three';
import type { BackendVehicleStatus } from '../api/vehiclesApi';

// 1. BATTERY FACET
export interface DigitalTwinBatteryState {
  level: number; // 0 to 100%
  isCharging: boolean;
  chargingPowerKw: number;
  estimatedRangeKm: number;
  healthPercentage: number;
  temperatureCelsius: number;
}

// 2. STATUS FACET
export interface DigitalTwinStatusState {
  status: BackendVehicleStatus;
  isLocked: boolean;
  stallLocationCode: string;
  position: Vector3Tuple;
  rotation: Vector3Tuple;
  lastStatusChangeReason?: string;
}

// 3. OWNERSHIP FACET
export interface DigitalTwinOwnershipState {
  groupId: number;
  groupName: string;
  memberCount: number;
  userSharePercentage: number;
  userVotingPower: number;
  capTableHash: string;
  totalEquityValueVnd: number;
}

// 4. BOOKING FACET
export interface DigitalTwinBookingState {
  activeBookingId: number | null;
  reservedByUserId: number | null;
  reservedByUserName: string | null;
  startTime: string | null;
  endTime: string | null;
  conflictStatus: 'NONE' | 'OVERLAP' | 'RESOLVED';
  purpose: string;
}

// 5. USAGE FACET
export interface DigitalTwinUsageState {
  activeSessionId: number | null;
  driverUserId: number | null;
  driverUserName: string | null;
  sessionStartTime: string | null;
  currentSpeedKmh: number;
  odometerKm: number;
  checkOutDamageReported: boolean;
}

// 6. MAINTENANCE FACET
export interface DigitalTwinMaintenanceSubsystem {
  name: string;
  health: number; // 0-100%
  status: 'NOMINAL' | 'WARNING' | 'CRITICAL_FAULT' | 'REPAIRED';
}

export interface DigitalTwinMaintenanceState {
  serviceStatus: 'NOMINAL' | 'NEEDS_SERVICE' | 'IN_WORKSHOP' | 'CERTIFIED';
  activeWorkOrderId: number | null;
  overallHealthScore: number;
  subsystems: Record<string, DigitalTwinMaintenanceSubsystem>;
  activeDtcCodes: string[];
  lastCertifiedAt?: string;
}

// 7. FINANCE FACET
export interface DigitalTwinFinanceState {
  vaultBalanceVnd: number;
  accruedExpenseLiabilityVnd: number;
  userDepositVnd: number;
  costPerKm: number;
  lastFundDeductionRef: string | null;
  depositStatus: 'HELD' | 'CLEARED' | 'PARTIAL_DEDUCTION';
}

// UNIFIED AGGREGATE DIGITAL TWIN
export interface VehicleDigitalTwin {
  vehicleId: number;
  vin: string;
  licensePlate: string;
  modelName: string;
  manufacturer: string;
  bodyColor: string;

  // The 7 Conceptual Facets
  battery: DigitalTwinBatteryState;
  status: DigitalTwinStatusState;
  ownership: DigitalTwinOwnershipState;
  booking: DigitalTwinBookingState;
  usage: DigitalTwinUsageState;
  maintenance: DigitalTwinMaintenanceState;
  finance: DigitalTwinFinanceState;

  // Pipeline Provenance
  lastSyncTimestamp: string;
  syncSource: 'BACKEND_AUTHORITATIVE' | 'PIPELINE_INTERMEDIATE';
}

export type DigitalTwinFacetTab =
  | 'BATTERY'
  | 'STATUS'
  | 'OWNERSHIP'
  | 'BOOKING'
  | 'USAGE'
  | 'MAINTENANCE'
  | 'FINANCE';
