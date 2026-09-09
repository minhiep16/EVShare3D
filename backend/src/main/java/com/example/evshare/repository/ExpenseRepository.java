package com.example.evshare.repository;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupId(Long groupId);

    List<Expense> findByGroupIdOrderByIncurredDateDesc(Long groupId);

    List<Expense> findByGroupIdAndCategory(Long groupId, ExpenseCategory category);

    List<Expense> findByGroupIdAndCategoryOrderByIncurredDateDesc(Long groupId, ExpenseCategory category);

    List<Expense> findByLoggedByUserId(Long loggedByUserId);

    List<Expense> findByVehicleIdOrderByIncurredDateDesc(Long vehicleId);

    Page<Expense> findByGroupId(Long groupId, Pageable pageable);

    Page<Expense> findByGroupIdAndCategory(Long groupId, ExpenseCategory category, Pageable pageable);

    Page<Expense> findByGroupIdAndIncurredDateBetween(Long groupId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Expense> findByGroupIdAndCategoryAndIncurredDateBetween(Long groupId, ExpenseCategory category, LocalDate startDate, LocalDate endDate, Pageable pageable);

    boolean existsByInvoiceReference(String invoiceReference);

    boolean existsByGroupIdAndInvoiceReference(Long groupId, String invoiceReference);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(e) > 0 FROM Expense e WHERE e.group.id = :groupId AND e.vehicle.id = :vehicleId AND e.category = :category AND e.totalAmount = :amount AND e.incurredDate = :incurredDate AND LOWER(e.title) = LOWER(:title)")
    boolean existsDuplicateExpense(
            @org.springframework.data.repository.query.Param("groupId") Long groupId,
            @org.springframework.data.repository.query.Param("vehicleId") Long vehicleId,
            @org.springframework.data.repository.query.Param("category") ExpenseCategory category,
            @org.springframework.data.repository.query.Param("amount") java.math.BigDecimal amount,
            @org.springframework.data.repository.query.Param("incurredDate") LocalDate incurredDate,
            @org.springframework.data.repository.query.Param("title") String title
    );
}
