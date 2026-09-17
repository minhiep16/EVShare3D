import type { Vector3Tuple } from 'three';
import type { BackendVehicleStatus } from '@/api/vehiclesApi';

export interface VehicleOwnershipInfo {
  groupId: number;
  groupName: string;
  memberCount: number;
  userSharePercentage: number;
  userVotingPower: number;
  isRepresentative?: boolean;
}

export interface GarageVehicle {
  id: number;
  vin: string;
  licensePlate: string;
  modelName: string;
  manufacturer: string;
  model3dAssetPath?: string;
  status: BackendVehicleStatus;
  batteryLevel: number; // 0 to 100
  odometerKm: number;
  stallLocationCode: string;
  bodyColor: string;
  position: Vector3Tuple;
  rotation: Vector3Tuple;
  ownership?: VehicleOwnershipInfo;
  isLocked: boolean;
  isCharging: boolean;
  chargingPowerKw: number;
}

export type BayType = 'PARKING' | 'CHARGING';

export interface ParkingBay {
  id: string; // e.g. 'BAY-01'
  label: string; // e.g. 'STALL 01'
  position: Vector3Tuple;
  rotation: Vector3Tuple;
  type: BayType;
  isOccupied: boolean;
  occupiedVehicleId?: number;
}

export interface ChargingStation {
  id: string; // e.g. 'CHG-01'
  stallId: string; // e.g. 'BAY-05'
  position: Vector3Tuple;
  rotation: Vector3Tuple;
  maxPowerKw: number;
  currentPowerKw: number;
  status: 'IDLE' | 'CHARGING' | 'OFFLINE';
  connectedVehicleId?: number;
}
