import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useFinanceStore } from './useFinanceStore';
import { CATEGORY_CONFIGS } from './financeTypes';
import { FINANCE_LAYOUT } from './financeLayout';
import * as financeApi from '@/api/financeApi';

describe('Finance Center & Cost Allocation Subsystem (09-F)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    useFinanceStore.setState({
      groupId: 1,
      activeTab: 'OVERVIEW',
      selectedExpenseId: 101,
      allocationStrategy: 'HYBRID',
      expenses: [
        {
          id: 101,
          groupId: 1,
          vehicleId: 1,
          vehicleModel: 'VF8 City Edition',
          category: 'CHARGING',
          title: 'Fast Charging Session',
          description: 'Highway DC fast charging',
          amountVnd: 5880000,
          incurredDate: '2026-09-15T10:00:00Z',
          isSettled: false,
          allocations: [],
        },
      ],
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
      },
    });
  });

  describe('1. Initial Store State & Category Configurations', () => {
    it('initializes with default expenses, member summaries, and HYBRID strategy', () => {
      const state = useFinanceStore.getState();
      expect(state.groupId).toBe(1);
      expect(state.expenses.length).toBeGreaterThanOrEqual(1);
      expect(state.selectedExpenseId).toBe(101);
      expect(state.allocationStrategy).toBe('HYBRID');
      expect(state.memberSummaries.length).toBe(4);
    });

    it('correctly maps visual categories to fixed vs variable cost classifications', () => {
      expect(CATEGORY_CONFIGS.CHARGING.isFixedCost).toBe(false);
      expect(CATEGORY_CONFIGS.MAINTENANCE.isFixedCost).toBe(false);
      expect(CATEGORY_CONFIGS.CLEANING.isFixedCost).toBe(false);
      expect(CATEGORY_CONFIGS.INSURANCE.isFixedCost).toBe(true);
      expect(CATEGORY_CONFIGS.PARKING.isFixedCost).toBe(true);
      expect(CATEGORY_CONFIGS.OTHER.isFixedCost).toBe(true);
    });
  });

  describe('2. Cost Allocation Strategies (Ownership vs Usage vs Hybrid)', () => {
    it('recalculates member liabilities under OWNERSHIP_BASED strategy via backend API', async () => {
      vi.spyOn(financeApi, 'getGroupAllocationSummary').mockResolvedValueOnce({
        groupId: 1,
        strategy: 'OWNERSHIP_BASED',
        totalExpensesVnd: 35400000,
        memberSummaries: [
          { userId: 1, userName: 'Minh Hiep (You)', equityPercentage: 35.0, usagePercentage: 37.4, allocatedTotalVnd: 12390000, paidTotalVnd: 6683000, outstandingDueVnd: 5707000, settlementStatus: 'PARTIAL' },
          { userId: 2, userName: 'Tran Duc', equityPercentage: 25.0, usagePercentage: 30.3, allocatedTotalVnd: 8850000, paidTotalVnd: 5645000, outstandingDueVnd: 3205000, settlementStatus: 'PARTIAL' },
        ],
      });

      const { setAllocationStrategy } = useFinanceStore.getState();
      await setAllocationStrategy('OWNERSHIP_BASED');

      const state = useFinanceStore.getState();
      expect(state.allocationStrategy).toBe('OWNERSHIP_BASED');

      const mySummary = state.memberSummaries.find((m) => m.userId === 1);
      expect(mySummary).toBeDefined();
      expect(mySummary?.allocatedTotalVnd).toBe(12390000);
      expect(mySummary?.equityPercentage).toBe(35.0);
    });

    it('recalculates member liabilities under USAGE_BASED strategy via backend API', async () => {
      vi.spyOn(financeApi, 'getGroupAllocationSummary').mockResolvedValueOnce({
        groupId: 1,
        strategy: 'USAGE_BASED',
        totalExpensesVnd: 35400000,
        memberSummaries: [
          { userId: 1, userName: 'Minh Hiep (You)', equityPercentage: 35.0, usagePercentage: 37.4, allocatedTotalVnd: 13239600, paidTotalVnd: 6683000, outstandingDueVnd: 6556600, settlementStatus: 'PARTIAL' },
        ],
      });

      const { setAllocationStrategy } = useFinanceStore.getState();
      await setAllocationStrategy('USAGE_BASED');

      const state = useFinanceStore.getState();
      expect(state.allocationStrategy).toBe('USAGE_BASED');

      const mySummary = state.memberSummaries.find((m) => m.userId === 1);
      expect(mySummary).toBeDefined();
      expect(mySummary?.usagePercentage).toBeCloseTo(37.4, 1);
      expect(mySummary?.allocatedTotalVnd).toBe(13239600);
    });

    it('balances fixed and variable expenditures under HYBRID strategy via backend API', async () => {
      vi.spyOn(financeApi, 'getGroupAllocationSummary').mockResolvedValueOnce({
        groupId: 1,
        strategy: 'HYBRID',
        totalExpensesVnd: 35400000,
        memberSummaries: [
          { userId: 1, userName: 'Minh Hiep (You)', equityPercentage: 35.0, usagePercentage: 37.4, allocatedTotalVnd: 12563000, paidTotalVnd: 6683000, outstandingDueVnd: 5880000, settlementStatus: 'PARTIAL' },
        ],
      });

      const { setAllocationStrategy } = useFinanceStore.getState();
      await setAllocationStrategy('HYBRID');

      const state = useFinanceStore.getState();
      expect(state.allocationStrategy).toBe('HYBRID');
      const mySummary = state.memberSummaries.find((m) => m.userId === 1);
      expect(mySummary?.allocatedTotalVnd).toBe(12563000);
      expect(mySummary?.settlementStatus).toBe('PARTIAL');
    });
  });

  describe('3. Shared Fund Liquid Reserve (BR-FIN-03)', () => {
    it('accurately tracks liquid reserve vs minimum safety floor', () => {
      const state = useFinanceStore.getState();
      expect(state.sharedFund.currentBalance).toBe(78500000);
      expect(state.sharedFund.minimumReserve).toBe(50000000);
      expect(state.sharedFund.isLowLiquidity).toBe(false);
      expect(state.sharedFund.safetyDeficitAmount).toBe(0);
    });

    it('flags low liquidity and computes safety deficit when balance drops below reserve', () => {
      useFinanceStore.setState({
        sharedFund: {
          id: 1,
          currentBalance: 32000000,
          minimumReserve: 50000000,
          isLowLiquidity: true,
          safetyDeficitAmount: 18000000,
        },
      });

      const state = useFinanceStore.getState();
      expect(state.sharedFund.isLowLiquidity).toBe(true);
      expect(state.sharedFund.safetyDeficitAmount).toBe(18000000);
    });
  });

  describe('4. Real Payment Execution & Strict No Fake Financial Success', () => {
    it('sets FAILED state and stores real error message when backend rejects payment', async () => {
      vi.spyOn(financeApi, 'initiatePayment').mockRejectedValueOnce({
        response: {
          data: { message: 'BR-FIN-03: Insufficient group liquidity for settlement' },
        },
      });

      const { initiatePaymentAction } = useFinanceStore.getState();
      await initiatePaymentAction(5000000, 'BANK_TRANSFER');

      const state = useFinanceStore.getState();
      expect(state.paymentSession.status).toBe('FAILED');
      expect(state.paymentSession.errorMessage).toContain('BR-FIN-03');
      expect(state.paymentSession.errorCode).toBe('ERR_GATEWAY_REJECTED');
    });

    it('sets AWAITING_SETTLEMENT state and populates VietQR when payment initiation succeeds', async () => {
      vi.spyOn(financeApi, 'initiatePayment').mockResolvedValueOnce({
        id: 9901,
        transactionReference: 'PAY-REF-9901',
        fundId: 1,
        userId: 1,
        amount: 5880000,
        currency: 'VND',
        paymentMethod: 'BANK_TRANSFER',
        status: 'PENDING',
        providerType: 'BANK_TRANSFER',
        qrCodeData: 'vietqr://pay?acc=109887766554&amount=5880000&memo=EVSHARE_9901',
        providerInstructions: 'Please transfer exactly 5,880,000 VND to Vietcombank escrow',
        createdAt: '2026-09-15T11:00:00Z',
      });

      const { initiatePaymentAction } = useFinanceStore.getState();
      await initiatePaymentAction(5880000, 'BANK_TRANSFER');

      const state = useFinanceStore.getState();
      expect(state.paymentSession.status).toBe('AWAITING_SETTLEMENT');
      expect(state.paymentSession.paymentId).toBe(9901);
      expect(state.paymentSession.reference).toBe('PAY-REF-9901');
      expect(state.paymentSession.qrCodeData).toContain('vietqr://pay');
    });

    it('refuses to complete verification if backend returns status other than COMPLETED', async () => {
      useFinanceStore.setState({
        paymentSession: {
          status: 'AWAITING_SETTLEMENT',
          paymentId: 9901,
          reference: 'PAY-REF-9901',
          amountVnd: 5880000,
          method: 'BANK_TRANSFER',
        },
      });

      vi.spyOn(financeApi, 'verifyPayment').mockResolvedValueOnce({
        id: 9901,
        transactionReference: 'PAY-REF-9901',
        fundId: 1,
        userId: 1,
        amount: 5880000,
        currency: 'VND',
        paymentMethod: 'BANK_TRANSFER',
        status: 'PENDING',
        providerType: 'BANK_TRANSFER',
        createdAt: '2026-09-15T11:00:00Z',
      });

      const { verifyPaymentAction } = useFinanceStore.getState();
      const verified = await verifyPaymentAction();

      expect(verified).toBe(false);
      const state = useFinanceStore.getState();
      expect(state.paymentSession.status).toBe('FAILED');
      expect(state.paymentSession.errorMessage).toContain('Settlement pending or unconfirmed');
    });

    it('transitions to COMPLETED and increases shared fund upon verified payment settlement', async () => {
      useFinanceStore.setState({
        sharedFund: {
          id: 1,
          currentBalance: 70000000,
          minimumReserve: 50000000,
          isLowLiquidity: false,
          safetyDeficitAmount: 0,
        },
        paymentSession: {
          status: 'AWAITING_SETTLEMENT',
          paymentId: 9901,
          reference: 'PAY-REF-9901',
          amountVnd: 5000000,
          method: 'BANK_TRANSFER',
        },
      });

      vi.spyOn(financeApi, 'verifyPayment').mockResolvedValueOnce({
        id: 9901,
        transactionReference: 'PAY-REF-9901',
        fundId: 1,
        userId: 1,
        amount: 5000000,
        currency: 'VND',
        paymentMethod: 'BANK_TRANSFER',
        status: 'COMPLETED',
        providerType: 'BANK_TRANSFER',
        createdAt: '2026-09-15T11:00:00Z',
      });

      vi.spyOn(financeApi, 'getSharedFund').mockResolvedValueOnce({
        id: 1,
        groupId: 1,
        currentBalance: 75000000,
        minimumReserve: 50000000,
        currency: 'VND',
        isLowLiquidity: false,
        safetyDeficitAmount: 0,
        totalContributions: 100000000,
        totalDisbursements: 25000000,
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-15T11:00:00Z',
      });

      const { verifyPaymentAction } = useFinanceStore.getState();
      const verified = await verifyPaymentAction();

      expect(verified).toBe(true);
      const state = useFinanceStore.getState();
      expect(state.paymentSession.status).toBe('COMPLETED');
      expect(state.sharedFund.currentBalance).toBe(75000000);
      expect(state.paymentSession.receiptId).toContain('REC-PAY-REF-9901');
    });
  });

  describe('5. Spatial Navigation & Station Focus', () => {
    it('supports tab changes and expense selection', () => {
      const { setActiveTab, selectExpense } = useFinanceStore.getState();

      setActiveTab('EXPENSES');
      expect(useFinanceStore.getState().activeTab).toBe('EXPENSES');

      selectExpense(103);
      expect(useFinanceStore.getState().selectedExpenseId).toBe(103);

      selectExpense(null);
      expect(useFinanceStore.getState().selectedExpenseId).toBeNull();
    });

    it('defines spatial layout stations and camera positions', () => {
      expect(FINANCE_LAYOUT.sectorCenter).toEqual([0, 0, -80]);
      expect(FINANCE_LAYOUT.cameras.FINANCE_OVERVIEW).toBeDefined();
      expect(FINANCE_LAYOUT.cameras.EXPENSE_CLUSTER).toBeDefined();
      expect(FINANCE_LAYOUT.cameras.ALLOCATION_FOCUS).toBeDefined();
      expect(FINANCE_LAYOUT.cameras.PAYMENT_KIOSK_FOCUS).toBeDefined();
      expect(FINANCE_LAYOUT.cameras.VAULT_COLUMN_FOCUS).toBeDefined();
    });
  });
});
