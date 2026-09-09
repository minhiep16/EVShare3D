package com.example.evshare.service;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.ExpenseAllocation;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.service.allocation.AllocationResult;
import com.example.evshare.service.allocation.CostAllocationStrategy;

import java.util.List;

/**
 * High-level orchestration engine for syndicate cost allocations.
 * Coordinates strategy lookup (OWNERSHIP_BASED, USAGE_BASED, HYBRID),
 * calculation execution, and database persistence of allocation obligations.
 */
public interface CostAllocationService {

    /**
     * Calculates itemized member cost allocation for an expense in-memory without persisting.
     *
     * @param expense the expense entity
     * @return the reconciled and explained allocation result
     */
    AllocationResult calculateAllocation(Expense expense);

    /**
     * Calculates itemized member cost allocation for an expense loaded by ID.
     *
     * @param expenseId the expense ID
     * @return the reconciled and explained allocation result
     */
    AllocationResult calculateAllocation(Long expenseId);

    /**
     * Calculates and persists ExpenseAllocation ledger entries for the given expense.
     * Reconciles allocations to guarantee 100% mathematical equality with total expense amount.
     *
     * @param expense the expense entity
     * @return the saved ExpenseAllocation entities
     */
    List<ExpenseAllocation> applyAndPersistAllocations(Expense expense);

    /**
     * Calculates and persists ExpenseAllocation ledger entries for the expense loaded by ID.
     *
     * @param expenseId the expense ID
     * @return the saved ExpenseAllocation entities
     */
    List<ExpenseAllocation> applyAndPersistAllocations(Long expenseId);

    /**
     * Resolves the concrete strategy bean for a specified allocation mode.
     *
     * @param strategy the allocation mode enum
     * @return the matching CostAllocationStrategy implementation
     */
    CostAllocationStrategy getStrategy(AllocationStrategy strategy);
}
