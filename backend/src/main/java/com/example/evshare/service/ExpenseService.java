package com.example.evshare.service;

import com.example.evshare.dto.request.CreateExpenseRequest;
import com.example.evshare.dto.response.ExpenseAuditLogResponse;
import com.example.evshare.dto.response.ExpenseResponse;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.entity.enums.ExpenseCategory;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    /**
     * Records a new expense in the syndicate ledger, executing multi-dimensional validation
     * (amount, currency, group, vehicle, creator, category, date, evidence), and writing an immutable audit log.
     *
     * @param request the expense creation payload
     * @param currentUserId the authenticated caller ID
     * @param ipAddress the HTTP client IP address for auditing
     * @return the created ExpenseResponse with allocations
     */
    ExpenseResponse createExpense(CreateExpenseRequest request, Long currentUserId, String ipAddress);

    /**
     * Retrieves an expense by its unique identifier.
     *
     * @param id the expense ID
     * @param currentUserId the authenticated caller ID
     * @return the ExpenseResponse
     */
    ExpenseResponse getExpenseById(Long id, Long currentUserId);

    /**
     * Retrieves paginated expenses for an ownership group, optionally filtered by category and date range.
     *
     * @param groupId the ownership group ID
     * @param category optional category filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param pageable pagination parameters
     * @param currentUserId the authenticated caller ID
     * @return paged list of ExpenseResponse
     */
    PagedData<ExpenseResponse> getGroupExpenses(Long groupId, ExpenseCategory category,
                                               LocalDate startDate, LocalDate endDate,
                                               Pageable pageable, Long currentUserId);

    /**
     * Retrieves chronological audit history for a specific expense.
     *
     * @param id the expense ID
     * @param currentUserId the authenticated caller ID
     * @return list of audit log entries
     */
    List<ExpenseAuditLogResponse> getExpenseHistory(Long id, Long currentUserId);
}
