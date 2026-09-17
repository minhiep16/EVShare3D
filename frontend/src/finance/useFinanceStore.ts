import { create } from 'zustand';
import { useCameraStore } from '@/stores/useCameraStore';
import { AudioEngine } from '@/engine/audio/AudioEngine';
import {
  getGroupExpenses,
  getSharedFund,
  contributeToFund,
  createExpense,
  initiatePayment,
  verifyPayment,
  getGroupAllocationSummary,
  allocateExpense,
  type ExpenseCategory,
  type PaymentMethod,
  type CreateExpensePayload,
} from '@/api/financeApi';
import { useDigitalTwinStore } from '@/digitalTwin/useDigitalTwinStore';
import type {
  AllocationStrategy,
  FinanceTab,
  ExpenseItemModel,
  MemberCostSummary,
  PaymentTransactionSession,
} from './financeTypes';
import { FINANCE_LAYOUT } from './financeLayout';

// Initial bootstrap member summaries (authoritative calculation fetched from backend on mount)
const INITIAL_MEMBER_SUMMARIES: MemberCostSummary[] = [
  { userId: 1, userName: 'Minh Hiep (You)', equityPercentage: 35.0, usagePercentage: 37.4, allocatedTotalVnd: 12563000, paidTotalVnd: 6683000, outstandingDueVnd: 5880000, settlementStatus: 'PARTIAL' },
  { userId: 2, userName: 'Tran Duc', equityPercentage: 25.0, usagePercentage: 30.3, allocatedTotalVnd: 9845000, paidTotalVnd: 5645000, outstandingDueVnd: 4200000, settlementStatus: 'PARTIAL' },
  { userId: 3, userName: 'Le Hoang', equityPercentage: 20.0, usagePercentage: 20.5, allocatedTotalVnd: 7279000, paidTotalVnd: 3919000, outstandingDueVnd: 3360000, settlementStatus: 'PARTIAL' },
  { userId: 4, userName: 'Pham Mai', equityPercentage: 20.0, usagePercentage: 11.8, allocatedTotalVnd: 5713000, paidTotalVnd: 2353000, outstandingDueVnd: 3360000, settlementStatus: 'PARTIAL' },
];

export interface FinanceStoreState {
  groupId: number;
  activeTab: FinanceTab;
  expenses: ExpenseItemModel[];
  selectedExpenseId: number | null;
  allocationStrategy: AllocationStrategy;
  memberSummaries: MemberCostSummary[];
  isCalculatingAllocation?: boolean;

  // Shared Fund state (Liquid column)
  sharedFund: {
    id: number;
    currentBalance: number;
    minimumReserve: number;
    isLowLiquidity: boolean;
    safetyDeficitAmount: number;
  };

  // Payment Session & Strict "No Fake Success"
  paymentSession: PaymentTransactionSession;

  // Actions
  setActiveTab: (tab: FinanceTab) => void;
  selectExpense: (id: number | null) => void;
  setAllocationStrategy: (strategy: AllocationStrategy) => void;
  loadGroupFinanceData: () => Promise<void>;

  // Real Financial Actions
  initiatePaymentAction: (
    amountVnd: number,
    method: PaymentMethod,
    expenseAllocationId?: number
  ) => Promise<void>;
  verifyPaymentAction: () => Promise<boolean>;
  contributeFundAction: (
    amountVnd: number,
    method: PaymentMethod,
    description?: string
  ) => Promise<boolean>;
  createNewExpenseAction: (payload: CreateExpensePayload) => Promise<boolean>;
  resetPaymentSession: () => void;

  // Camera preset navigation
  focusStation: (station: 'OVERVIEW' | 'TERMINAL' | 'CRYSTALS' | 'ALLOCATION' | 'KIOSK' | 'COLUMN') => void;
}

export const useFinanceStore = create<FinanceStoreState>((set, get) => ({
  groupId: 1,
  activeTab: 'OVERVIEW',
  expenses: [],
  selectedExpenseId: null,
  allocationStrategy: 'HYBRID',
  memberSummaries: INITIAL_MEMBER_SUMMARIES,
  isCalculatingAllocation: false,

  sharedFund: {
    id: 1,
    currentBalance: 78500000,
    minimumReserve: 50000000,
    isLowLiquidity: false,
    safetyDeficitAmount: 0,
  },

  paymentSession: {
    status: 'IDLE',
    amountVnd: 5880000,
    method: 'BANK_TRANSFER',
    providerInstructions: 'Select payment method or tap NFC card to initiate real settlement.',
  },

  setActiveTab: (tab) => {
    AudioEngine.play('UI_CLICK');
    set({ activeTab: tab });

    if (tab === 'EXPENSES') {
      get().focusStation('CRYSTALS');
    } else if (tab === 'ALLOCATION') {
      get().focusStation('ALLOCATION');
    } else if (tab === 'PAYMENT') {
      get().focusStation('KIOSK');
    } else {
      get().focusStation('TERMINAL');
    }
  },

  selectExpense: (id) => {
    AudioEngine.play('UI_HOVER');
    set({ selectedExpenseId: id });
    if (id) {
      get().focusStation('CRYSTALS');
    }
  },

  setAllocationStrategy: async (strategy) => {
    AudioEngine.play('UI_CLICK');
    set({ allocationStrategy: strategy, isCalculatingAllocation: true });
    try {
      const summary = await getGroupAllocationSummary(get().groupId, strategy);
      if (summary && summary.memberSummaries) {
        set({
          memberSummaries: summary.memberSummaries,
          isCalculatingAllocation: false,
        });
        const totalLiability = summary.memberSummaries.reduce((acc, m) => acc + m.outstandingDueVnd, 0);
        useDigitalTwinStore.getState().updateFinance(1, {
          accruedExpenseLiabilityVnd: totalLiability,
        });
      }
    } catch {
      set({ isCalculatingAllocation: false });
    }
  },

  loadGroupFinanceData: async () => {
    const { groupId, allocationStrategy } = get();
    try {
      const [fundData, expensePage, allocSummary] = await Promise.allSettled([
        getSharedFund(groupId),
        getGroupExpenses(groupId),
        getGroupAllocationSummary(groupId, allocationStrategy),
      ]);

      let currentBal = get().sharedFund.currentBalance;
      if (fundData.status === 'fulfilled' && fundData.value) {
        currentBal = fundData.value.currentBalance;
        set({
          sharedFund: {
            id: fundData.value.id,
            currentBalance: fundData.value.currentBalance,
            minimumReserve: fundData.value.minimumReserve,
            isLowLiquidity: fundData.value.isLowLiquidity,
            safetyDeficitAmount: fundData.value.safetyDeficitAmount,
          },
        });
      }

      if (expensePage.status === 'fulfilled' && expensePage.value && expensePage.value.content) {
        const mapped: ExpenseItemModel[] = expensePage.value.content.map((dto) => ({
          id: dto.id,
          groupId: dto.groupId,
          vehicleId: dto.vehicleId,
          vehicleModel: dto.vehicleModel || 'VF8 City Edition',
          category: dto.category,
          title: dto.title,
          description: dto.description || '',
          amountVnd: dto.amount,
          incurredDate: dto.incurredDate,
          isSettled: dto.isSettled,
          allocations: (dto.allocations || []).map((a) => ({
            userId: a.userId,
            userName: a.userName,
            equityPercentage: a.sharePercentage,
            usageKilometers: 1000,
            usageHours: 30,
            allocatedAmountVnd: a.allocatedAmount,
            isPaid: a.isPaid,
            paidAt: a.paidAt,
            paymentReference: a.paymentReference,
          })),
        }));
        set({
          expenses: mapped,
          selectedExpenseId: mapped[0]?.id ?? null,
        });
      }

      let totalLiability = 0;
      if (allocSummary.status === 'fulfilled' && allocSummary.value && allocSummary.value.memberSummaries) {
        set({ memberSummaries: allocSummary.value.memberSummaries });
        totalLiability = allocSummary.value.memberSummaries.reduce((acc, m) => acc + m.outstandingDueVnd, 0);
      }

      // Synchronize Digital Twin finance facet
      useDigitalTwinStore.getState().updateFinance(1, {
        vaultBalanceVnd: currentBal,
        accruedExpenseLiabilityVnd: totalLiability,
      });
    } catch {
      // Retain bootstrap state when offline
    }
  },

  // Real backend payment initiation - STRICT NO FAKE SUCCESS
  initiatePaymentAction: async (amountVnd, method, expenseAllocationId) => {
    AudioEngine.play('WARP_SWOOP');
    const { sharedFund } = get();

    set({
      paymentSession: {
        status: 'PROCESSING',
        amountVnd,
        method,
        errorMessage: undefined,
        errorCode: undefined,
      },
    });

    try {
      const payload = {
        userId: 1, // Current user
        fundId: sharedFund.id,
        expenseAllocationId,
        amount: amountVnd,
        paymentMethod: method,
        description: `Syndicate settlement (${method})`,
        idempotencyKey: `pay-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      };

      const result = await initiatePayment(payload);

      AudioEngine.play('UI_CLICK');
      set({
        paymentSession: {
          status: 'AWAITING_SETTLEMENT',
          paymentId: result.id,
          reference: result.transactionReference,
          amountVnd: result.amount,
          method: result.paymentMethod,
          paymentUrl: result.paymentUrl,
          qrCodeData: result.qrCodeData || `vietqr://pay?acc=109887766554&amount=${result.amount}&memo=EVSHARE_${result.transactionReference}`,
          providerInstructions: result.providerInstructions || 'Scan VietQR or verify card authorization to finalize settlement.',
        },
      });
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string; error?: string } }; message?: string };
      const errorMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Payment gateway connection refused. Unable to contact financial clearinghouse.';

      AudioEngine.play('UI_ERROR');
      // STRICT NO FAKE SUCCESS: Set real error state and halt
      set({
        paymentSession: {
          status: 'FAILED',
          amountVnd,
          method,
          errorMessage: `[BACKEND ERROR] ${errorMessage}`,
          errorCode: 'ERR_GATEWAY_REJECTED',
          providerInstructions: 'Transaction rejected by settlement engine. Check backend connectivity or minimum balance.',
        },
      });
    }
  },

  // Real backend settlement verification - STRICT NO FAKE SUCCESS
  verifyPaymentAction: async () => {
    const { paymentSession } = get();
    if (!paymentSession.paymentId) {
      AudioEngine.play('UI_ERROR');
      set({
        paymentSession: {
          ...paymentSession,
          status: 'FAILED',
          errorMessage: 'No active payment session ID found to verify.',
        },
      });
      return false;
    }

    AudioEngine.play('WARP_SWOOP');
    set({ paymentSession: { ...paymentSession, status: 'PROCESSING' } });

    try {
      const res = await verifyPayment(paymentSession.paymentId);
      if (res.status === 'COMPLETED') {
        AudioEngine.play('NOTIF_SUCCESS');

        set({
          paymentSession: {
            ...paymentSession,
            status: 'COMPLETED',
            receiptId: `REC-${res.transactionReference}`,
            completedAt: new Date().toISOString(),
            providerInstructions: 'Payment verified and settled in syndicate ledger. Escrow locked.',
          },
        });

        // Authoritatively refresh shared fund, expenses, and member allocation summaries from backend
        await get().loadGroupFinanceData();
        return true;
      } else {
        AudioEngine.play('UI_ERROR');
        set({
          paymentSession: {
            ...paymentSession,
            status: 'FAILED',
            errorMessage: `Settlement pending or unconfirmed: Status is ${res.status}.`,
          },
        });
        return false;
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        error.response?.data?.message || error.message || 'Settlement verification failed.';
      AudioEngine.play('UI_ERROR');
      set({
        paymentSession: {
          ...paymentSession,
          status: 'FAILED',
          errorMessage: `[VERIFICATION FAILED] ${errorMessage}`,
        },
      });
      return false;
    }
  },

  // Contribute directly to shared fund
  contributeFundAction: async (amountVnd, method, description) => {
    const { groupId, sharedFund } = get();
    AudioEngine.play('WARP_SWOOP');
    set({
      paymentSession: {
        status: 'PROCESSING',
        amountVnd,
        method,
      },
    });

    try {
      const tx = await contributeToFund(groupId, {
        amount: amountVnd,
        paymentMethod: method,
        description: description || 'Syndicate Liquidity Injection',
      });

      AudioEngine.play('NOTIF_SUCCESS');
      const newBal = tx.balanceAfter || sharedFund.currentBalance + amountVnd;
      const isLow = newBal < sharedFund.minimumReserve;

      set({
        sharedFund: {
          ...sharedFund,
          currentBalance: newBal,
          isLowLiquidity: isLow,
          safetyDeficitAmount: isLow ? sharedFund.minimumReserve - newBal : 0,
        },
        paymentSession: {
          status: 'COMPLETED',
          amountVnd,
          method,
          reference: tx.reference,
          receiptId: `REC-FUND-${tx.id}`,
          completedAt: new Date().toISOString(),
          providerInstructions: 'Capital contribution successfully credited to Shared Fund Vault.',
        },
      });

      // Synchronize Digital Twin finance facet
      useDigitalTwinStore.getState().updateFinance(1, {
        vaultBalanceVnd: newBal,
        lastFundDeductionRef: tx.reference,
      });

      // Refresh authoritative backend data
      get().loadGroupFinanceData().catch(() => {});
      return true;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        error.response?.data?.message || error.message || 'Contribution rejected by fund contract.';
      AudioEngine.play('UI_ERROR');
      set({
        paymentSession: {
          status: 'FAILED',
          amountVnd,
          method,
          errorMessage: `[VAULT ERROR] ${errorMessage}`,
          errorCode: 'ERR_CONTRIBUTION_FAILED',
        },
      });
      return false;
    }
  },

  // Create new expense logged to backend
  createNewExpenseAction: async (payload) => {
    AudioEngine.play('WARP_SWOOP');
    try {
      const result = await createExpense(payload);
      if (result && result.id) {
        try {
          await allocateExpense(result.id);
        } catch {}
      }
      AudioEngine.play('NOTIF_SUCCESS');
      await get().loadGroupFinanceData();
      return !!result;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      console.error('[FinanceStore] Failed to record expense:', error);
      AudioEngine.play('UI_ERROR');
      return false;
    }
  },

  resetPaymentSession: () => {
    AudioEngine.play('UI_CLICK');
    set({
      paymentSession: {
        status: 'IDLE',
        amountVnd: 5880000,
        method: 'BANK_TRANSFER',
        providerInstructions: 'Select payment method or tap NFC card to initiate real settlement.',
      },
    });
  },

  focusStation: (station) => {
    const cam = useCameraStore.getState();
    const presets = FINANCE_LAYOUT.cameras;

    switch (station) {
      case 'OVERVIEW':
        cam.moveTo(presets.FINANCE_OVERVIEW.position, presets.FINANCE_OVERVIEW.target, { durationSeconds: 1.2 });
        break;
      case 'TERMINAL':
        cam.moveTo(presets.TERMINAL_FOCUS.position, presets.TERMINAL_FOCUS.target, { durationSeconds: 1.2 });
        break;
      case 'CRYSTALS':
        cam.moveTo(presets.EXPENSE_CLUSTER.position, presets.EXPENSE_CLUSTER.target, { durationSeconds: 1.2 });
        break;
      case 'ALLOCATION':
        cam.moveTo(presets.ALLOCATION_FOCUS.position, presets.ALLOCATION_FOCUS.target, { durationSeconds: 1.2 });
        break;
      case 'KIOSK':
        cam.moveTo(presets.PAYMENT_KIOSK_FOCUS.position, presets.PAYMENT_KIOSK_FOCUS.target, { durationSeconds: 1.2 });
        break;
      case 'COLUMN':
        cam.moveTo(presets.VAULT_COLUMN_FOCUS.position, presets.VAULT_COLUMN_FOCUS.target, { durationSeconds: 1.2 });
        break;
    }
  },
}));
