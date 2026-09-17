import type { ExpenseCategory, PaymentMethod, PaymentStatus } from '@/api/financeApi';

export type AllocationStrategy = 'OWNERSHIP_BASED' | 'USAGE_BASED' | 'HYBRID';

export type FinanceTab = 'OVERVIEW' | 'EXPENSES' | 'ALLOCATION' | 'PAYMENT' | 'AUDIT';

export interface CategoryVisualConfig {
  category: ExpenseCategory;
  label: string;
  color: string;
  emissive: string;
  icon: string;
  isFixedCost: boolean;
}

export const CATEGORY_CONFIGS: Record<ExpenseCategory, CategoryVisualConfig> = {
  CHARGING: {
    category: 'CHARGING',
    label: 'EV Charging & Power',
    color: '#00e5ff',
    emissive: '#00838f',
    icon: '⚡',
    isFixedCost: false,
  },
  MAINTENANCE: {
    category: 'MAINTENANCE',
    label: 'Scheduled Maintenance',
    color: '#ffab00',
    emissive: '#ff6f00',
    icon: '🔧',
    isFixedCost: false,
  },
  INSURANCE: {
    category: 'INSURANCE',
    label: 'Comprehensive Coverage',
    color: '#38bdf8',
    emissive: '#0284c7',
    icon: '🛡️',
    isFixedCost: true,
  },
  CLEANING: {
    category: 'CLEANING',
    label: 'Detailing & Sanitation',
    color: '#00e676',
    emissive: '#00a152',
    icon: '✨',
    isFixedCost: false,
  },
  PARKING: {
    category: 'PARKING',
    label: 'Dedicated Bay Stabling',
    color: '#b388ff',
    emissive: '#7c4dff',
    icon: '🅿️',
    isFixedCost: true,
  },
  REPAIR: {
    category: 'REPAIR',
    label: 'Corrective Overhaul',
    color: '#ff5252',
    emissive: '#d50000',
    icon: '⚠️',
    isFixedCost: false,
  },
  OTHER: {
    category: 'OTHER',
    label: 'General Operating',
    color: '#90a4ae',
    emissive: '#455a64',
    icon: '📋',
    isFixedCost: true,
  },
};

export interface MemberAllocationShare {
  userId: number;
  userName: string;
  equityPercentage: number;
  usageKilometers: number;
  usageHours: number;
  allocatedAmountVnd: number;
  isPaid: boolean;
  paidAt?: string | null;
  paymentReference?: string | null;
}

export interface ExpenseItemModel {
  id: number;
  groupId: number;
  vehicleId: number;
  vehicleModel: string;
  category: ExpenseCategory;
  title: string;
  description: string;
  amountVnd: number;
  incurredDate: string;
  isSettled: boolean;
  allocations: MemberAllocationShare[];
}

export interface MemberCostSummary {
  userId: number;
  userName: string;
  equityPercentage: number;
  usagePercentage: number;
  allocatedTotalVnd: number;
  paidTotalVnd: number;
  outstandingDueVnd: number;
  settlementStatus: 'SETTLED' | 'PARTIAL' | 'OVERDUE';
}

export interface PaymentTransactionSession {
  status: 'IDLE' | 'SELECTING' | 'INITIATING' | 'AWAITING_SETTLEMENT' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  paymentId?: number;
  reference?: string;
  amountVnd: number;
  method: PaymentMethod;
  paymentUrl?: string;
  qrCodeData?: string;
  providerInstructions?: string;
  errorMessage?: string;
  errorCode?: string;
  completedAt?: string;
  receiptId?: string;
}
