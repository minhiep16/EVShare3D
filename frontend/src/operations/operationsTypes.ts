import type {
  UsageSessionDTO,
  QrValidationDTO,
  VehicleStatus,
} from '../api/usageSessionsApi';

export type OperationsTab =
  | 'OVERVIEW'
  | 'QR_STATION'
  | 'CHECK_IN_OUT'
  | 'FLEET_STATUS'
  | 'NOTIFICATIONS';

export type OperationsStation =
  | 'QR_DESK'
  | 'DISPATCH_CONSOLE'
  | 'FLEET_STELA'
  | 'NOTIFICATION_BOARD'
  | 'INSPECTION_BAY';

export type CheckInOutMode = 'CHECK_IN' | 'CHECK_OUT';

export interface OperationalAlertItem {
  id: string;
  title: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  timestamp: string;
  message: string;
  isAcknowledged: boolean;
  targetVehicle: string;
  targetBay: string;
}

export interface VehicleOperationStatus {
  vehicleId: number;
  modelName: string;
  licensePlate: string;
  status: VehicleStatus;
  batterySoc: number;
  odometerKm: number;
  currentBay: string;
  activeUser: string | null;
  activeBookingId: number | null;
  activeSessionId: number | null;
}

export interface OperationsCameraPreset {
  name: string;
  position: [number, number, number];
  target: [number, number, number];
}

export interface OperationsState {
  // Navigation & tabs
  activeTab: OperationsTab;
  activeStation: OperationsStation;
  checkInOutMode: CheckInOutMode;

  // Fleet state
  fleet: VehicleOperationStatus[];
  selectedVehicleId: number;

  // Session & Check-in/out
  activeSession: UsageSessionDTO | null;
  inputOdometer: number;
  inputBattery: number;
  hasDamageReported: boolean;
  inspectionNotes: string;

  // QR Station
  qrToken: string | null;
  qrExpiresAt: string | null;
  qrValidationResult: QrValidationDTO | null;
  isScanningQr: boolean;

  // Feedback & alerts
  isSubmitting: boolean;
  operationMessage: string | null;
  operationError: string | null;
  alerts: OperationalAlertItem[];
}
