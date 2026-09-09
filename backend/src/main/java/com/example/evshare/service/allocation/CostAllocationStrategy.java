package com.example.evshare.service.allocation;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.enums.AllocationStrategy;

/**
 * Strategy interface for allocating syndicate vehicle expenses among co-owners.
 * Concrete implementations must be deterministic, unit-testable, explainable,
 * and guarantee exact mathematical reconciliation with the original expense total.
 */
public interface CostAllocationStrategy {

    /**
     * @return the allocation strategy mode implemented by this strategy
     */
    AllocationStrategy getStrategyMode();

    /**
     * Allocates the given expense among eligible syndicate members.
     *
     * @param expense the expense entity to allocate
     * @return the reconciled and fully explained allocation result
     */
    AllocationResult allocate(Expense expense);
}
