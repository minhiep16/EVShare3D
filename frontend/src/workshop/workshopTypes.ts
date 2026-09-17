export type SubsystemId =
  | 'BRAKE_SYSTEM'
  | 'HIGH_VOLTAGE_BATTERY'
  | 'LIDAR_ADAS'
  | 'SUSPENSION_TIRES'
  | 'THERMAL_COOLANT';

export type SubsystemStatus =
  | 'NOMINAL'
  | 'WARNING'
  | 'CRITICAL_FAULT'
  | 'REPAIRED';

export interface VehicleConditionSubsystem {
  subsystemId: SubsystemId;
  name: string;
  category: 'CHASSIS' | 'POWERTRAIN' | 'AVIONICS_SENSORS' | 'THERMAL';
  healthPercentage: number; // 0 to 100
  faultCode: string | null; // e.g. "DTC-P1A24"
  faultDescription: string | null;
  status: SubsystemStatus;
  replacementPartName: string;
  repairCostVnd: number;
  positionOffset: [number, number, number]; // Offset relative to car origin on lift
}

export type WorkOrderStatus =
  | 'PENDING_INSPECTION'
  | 'IN_SERVICE'
  | 'WAITING_PARTS'
  | 'READY_FOR_RELEASE'
  | 'RELEASED';

export interface MaintenanceWorkOrder {
  workOrderId: string;
  vehicleId: number;
  vehicleModel: string;
  licensePlate: string;
  odometerKm: number;
  status: WorkOrderStatus;
  assignedTechnician: string;
  serviceBay: string;
  totalRepairCostVnd: number;
  backendExpenseId: number | null;
  invoiceReference: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface WorkshopCameraPreset {
  name: string;
  position: [number, number, number];
  target: [number, number, number];
}

export type WorkshopStation =
  | 'HYDRAULIC_LIFT'
  | 'DIAGNOSTIC_CART'
  | 'PARTS_RACK'
  | 'WORK_ORDER_STELA';

export type WorkshopTab =
  | 'OVERVIEW'
  | 'DIAGNOSTICS'
  | 'LIFT_CONTROL'
  | 'PARTS_STAGING'
  | 'WORK_ORDERS';

export interface WorkshopState {
  // Navigation & tabs
  activeTab: WorkshopTab;
  activeStation: WorkshopStation;

  // Active Vehicle & Work Order
  selectedVehicleId: number;
  workOrder: MaintenanceWorkOrder;
  subsystems: Record<SubsystemId, VehicleConditionSubsystem>;
  selectedSubsystemId: SubsystemId | null;

  // Hydraulic Lift State
  isElevated: boolean;
  liftHeight: number; // 0.2m (ground) to 1.8m (undercarriage inspection height)
  targetLiftHeight: number;
  isMovingLift: boolean;

  // Diagnostic State
  isScanningObd: boolean;
  diagnosticCompleted: boolean;

  // Repair & Parts State
  isRepairing: boolean;
  activeRepairPart: string | null;

  // Backend synchronization & Ledger
  isSubmittingExpense: boolean;
  isCertifyingRelease: boolean;
  feedbackMessage: string | null;
  errorMessage: string | null;
  safetyViolationNotice: string | null;
}
