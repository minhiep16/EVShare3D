import { apiClient } from './apiClient';
import type { ApiResponse, PagedData } from './vehiclesApi';

export type ExpenseCategory =
  | 'CHARGING'
  | 'MAINTENANCE'
  | 'INSURANCE'
  | 'CLEANING'
  | 'PARKING'
  | 'REPAIR'
  | 'OTHER';

export type AllocationStrategy =
  | 'OWNERSHIP_BASED'
  | 'USAGE_BASED'
  | 'HYBRID';

export type PaymentMethod =
  | 'BANK_TRANSFER'
  | 'E_WALLET'
  | 'CREDIT_CARD'
  | 'MOCK'
  | 'GATEWAY';

export type PaymentStatus =
  | 'PENDING'
  | 'COMPLETED'
  | 'FAILED'
  | 'REFUNDED';

export type FundTransactionType =
  | 'CONTRIBUTION'
  | 'EXPENSE_PAYOUT'
  | 'REFUND'
  | 'SAFETY_RESERVE_TOPUP';

export interface ExpenseAllocationDTO {
  id: number;
  expenseId: number;
  userId: number;
  userName: string;
  sharePercentage: number;
  allocatedAmount: number;
  isPaid: boolean;
  paidAt?: string | null;
  paymentReference?: string | null;
}

export interface MemberCostSummaryDTO {
  userId: number;
  userName: string;
  equityPercentage: number;
  usagePercentage: number;
  allocatedTotalVnd: number;
  paidTotalVnd: number;
  outstandingDueVnd: number;
  settlementStatus: 'SETTLED' | 'PARTIAL' | 'OVERDUE';
}

export interface GroupAllocationSummaryDTO {
  groupId: number;
  groupName: string;
  strategy: AllocationStrategy;
  totalExpenseAmount: number;
  totalAllocatedAmount: number;
  isReconciled: boolean;
  memberSummaries: MemberCostSummaryDTO[];
}

export interface ExpenseResponseDTO {
  id: number;
  groupId: number;
  vehicleId: number;
  vehicleModel?: string;
  creatorId: number;
  creatorName: string;
  category: ExpenseCategory;
  title: string;
  description: string;
  amount: number;
  currency: string;
  incurredDate: string;
  receiptEvidenceUrl?: string;
  isSettled: boolean;
  allocations: ExpenseAllocationDTO[];
  createdAt: string;
}

export interface CreateExpensePayload {
  groupId: number;
  vehicleId: number;
  category: ExpenseCategory;
  title: string;
  description?: string;
  amount: number;
  currency?: string;
  incurredDate: string;
  receiptEvidenceUrl?: string;
}

export interface SharedFundDTO {
  id: number;
  groupId: number;
  groupName: string;
  currentBalance: number;
  minimumReserve: number;
  currency: string;
  isLowLiquidity: boolean;
  safetyDeficitAmount: number;
  updatedAt: string;
}

export interface FundContributionPayload {
  amount: number;
  paymentMethod: PaymentMethod;
  description?: string;
  reference?: string;
}

export interface FundTransactionDTO {
  id: number;
  fundId: number;
  userId: number;
  userName: string;
  type: FundTransactionType;
  amount: number;
  currency: string;
  balanceAfter: number;
  description: string;
  reference: string;
  createdAt: string;
}

export interface InitiatePaymentPayload {
  userId: number;
  fundId: number;
  expenseAllocationId?: number;
  amount: number;
  paymentMethod: PaymentMethod;
  description?: string;
  idempotencyKey?: string;
  metadata?: Record<string, unknown>;
}

export interface PaymentResponseDTO {
  id: number;
  transactionReference: string;
  fundId: number;
  userId: number;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  providerType: string;
  paymentUrl?: string;
  qrCodeData?: string;
  providerInstructions?: string;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────
// REST API Callers
// ─────────────────────────────────────────────────────────────

export const getGroupExpenses = async (
  groupId: number,
  category?: ExpenseCategory,
  page = 0,
  size = 20
): Promise<PagedData<ExpenseResponseDTO>> => {
  const params: Record<string, unknown> = { page, size };
  if (category) params.category = category;
  const res = await apiClient.get<ApiResponse<PagedData<ExpenseResponseDTO>>>(
    `/expenses/group/${groupId}`,
    { params }
  );
  return res.data.data;
};

export const getExpenseById = async (id: number): Promise<ExpenseResponseDTO> => {
  const res = await apiClient.get<ApiResponse<ExpenseResponseDTO>>(`/expenses/${id}`);
  return res.data.data;
};

export const createExpense = async (
  payload: CreateExpensePayload
): Promise<ExpenseResponseDTO> => {
  const res = await apiClient.post<ApiResponse<ExpenseResponseDTO>>('/expenses', payload);
  return res.data.data;
};

export const getSharedFund = async (groupId: number): Promise<SharedFundDTO> => {
  const res = await apiClient.get<ApiResponse<SharedFundDTO>>(
    `/ownership-groups/${groupId}/fund`
  );
  return res.data.data;
};

export const getFundBalance = async (groupId: number): Promise<SharedFundDTO> => {
  const res = await apiClient.get<ApiResponse<SharedFundDTO>>(
    `/ownership-groups/${groupId}/fund/balance`
  );
  return res.data.data;
};

export const contributeToFund = async (
  groupId: number,
  payload: FundContributionPayload
): Promise<FundTransactionDTO> => {
  const res = await apiClient.post<ApiResponse<FundTransactionDTO>>(
    `/ownership-groups/${groupId}/fund/contributions`,
    payload
  );
  return res.data.data;
};

export const getFundTransactions = async (
  groupId: number
): Promise<FundTransactionDTO[]> => {
  const res = await apiClient.get<ApiResponse<FundTransactionDTO[]>>(
    `/ownership-groups/${groupId}/fund/transactions`
  );
  return res.data.data;
};

export const initiatePayment = async (
  payload: InitiatePaymentPayload
): Promise<PaymentResponseDTO> => {
  const res = await apiClient.post<ApiResponse<PaymentResponseDTO>>(
    '/payments/initiate',
    payload
  );
  return res.data.data;
};

export const verifyPayment = async (
  paymentId: number,
  idempotencyKey?: string
): Promise<PaymentResponseDTO> => {
  const headers: Record<string, string> = {};
  if (idempotencyKey) headers['X-Idempotency-Key'] = idempotencyKey;
  const res = await apiClient.post<ApiResponse<PaymentResponseDTO>>(
    `/payments/${paymentId}/verify`,
    {},
    { headers }
  );
  return res.data.data;
};

export interface FundWithdrawalPayload {
  amount: number;
  paymentMethod: PaymentMethod;
  description: string;
  allowOverdraft?: boolean;
}

export interface FundAuditLogDTO {
  id: number;
  fundId: number;
  action: string;
  actorId: number;
  actorName: string;
  detailsJson: string;
  ipAddress: string;
  createdAt: string;
}

export interface FundReconciliationDTO {
  fundId: number;
  groupId: number;
  currentBalance: number;
  calculatedLedgerBalance: number;
  totalCredits: number;
  totalDebits: number;
  creditCount: number;
  debitCount: number;
  transactionCount: number;
  isReconciled: boolean;
  reconciliationDelta: number;
  reconciledAt: string;
  summary: string;
}

export const withdrawFromFund = async (
  groupId: number,
  payload: FundWithdrawalPayload
): Promise<FundTransactionDTO> => {
  const res = await apiClient.post<ApiResponse<FundTransactionDTO>>(
    `/ownership-groups/${groupId}/fund/withdrawals`,
    payload
  );
  return res.data.data;
};

export const getFundAuditHistory = async (
  groupId: number
): Promise<FundAuditLogDTO[]> => {
  const res = await apiClient.get<ApiResponse<FundAuditLogDTO[]>>(
    `/ownership-groups/${groupId}/fund/audit`
  );
  return res.data.data;
};

export const reconcileFund = async (
  groupId: number
): Promise<FundReconciliationDTO> => {
  const res = await apiClient.get<ApiResponse<FundReconciliationDTO>>(
    `/ownership-groups/${groupId}/fund/reconciliation`
  );
  return res.data.data;
};

export const getTransactionByReference = async (
  groupId: number,
  reference: string
): Promise<FundTransactionDTO> => {
  const res = await apiClient.get<ApiResponse<FundTransactionDTO>>(
    `/ownership-groups/${groupId}/fund/transactions/${reference}`
  );
  return res.data.data;
};

export const getGroupAllocationSummary = async (
  groupId: number,
  strategy: AllocationStrategy = 'HYBRID'
): Promise<GroupAllocationSummaryDTO> => {
  const res = await apiClient.get<ApiResponse<GroupAllocationSummaryDTO>>(
    `/expenses/group/${groupId}/allocation-summary`,
    { params: { strategy } }
  );
  return res.data.data;
};

export const getExpenseAllocations = async (
  expenseId: number
): Promise<ExpenseAllocationDTO[]> => {
  const res = await apiClient.get<ApiResponse<ExpenseAllocationDTO[]>>(
    `/expenses/${expenseId}/allocations`
  );
  return res.data.data;
};

export const allocateExpense = async (
  expenseId: number
): Promise<ExpenseResponseDTO> => {
  const res = await apiClient.post<ApiResponse<ExpenseResponseDTO>>(
    `/expenses/${expenseId}/allocate`
  );
  return res.data.data;
};
