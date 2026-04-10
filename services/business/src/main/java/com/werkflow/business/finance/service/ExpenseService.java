package com.werkflow.business.finance.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.context.UserContext;
import com.werkflow.business.finance.dto.ExpenseRequest;
import com.werkflow.business.finance.dto.ExpenseResponse;
import com.werkflow.business.finance.entity.Expense;
import com.werkflow.business.finance.repository.ExpenseRepository;
import com.werkflow.business.finance.repository.BudgetCategoryRepository;
import com.werkflow.business.finance.repository.BudgetLineItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final BudgetLineItemRepository lineItemRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getAllExpenses(Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching all expenses for tenant: {}", tenantId);
        return expenseRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpensesUnscoped() {
        return expenseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        String tenantId = getTenantId();
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));
        if (!expense.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this Expense");
        }
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        String tenantId = getTenantId();
        var category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new EntityNotFoundException("BudgetCategory not found with id: " + request.getCategoryId()));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetCategory");
        }

        Expense expense = Expense.builder()
            .tenantId(tenantId)
            .departmentId(request.getDepartmentId())
            .expenseDate(request.getExpenseDate())
            .amount(request.getAmount())
            .category(category)
            .vendorName(request.getVendorName())
            .description(request.getDescription())
            .receiptUrl(request.getReceiptUrl())
            .status(Expense.ExpenseStatus.SUBMITTED)
            .submittedByUserId(request.getSubmittedByUserId())
            .metadata(request.getMetadata())
            .build();

        if (request.getBudgetLineItemId() != null) {
            var lineItem = lineItemRepository.findById(request.getBudgetLineItemId())
                .orElseThrow(() -> new EntityNotFoundException("BudgetLineItem not found with id: " + request.getBudgetLineItemId()));
            if (!lineItem.getTenantId().equals(tenantId)) {
                throw new AccessDeniedException("Not authorized to access this BudgetLineItem");
            }
            expense.setBudgetLineItem(lineItem);
        }

        return toResponse(expenseRepository.save(expense));
    }

    private ExpenseResponse toResponse(Expense expense) {
        ExpenseResponse response = ExpenseResponse.builder()
            .id(expense.getId())
            .budgetLineItemId(expense.getBudgetLineItem() != null ? expense.getBudgetLineItem().getId() : null)
            .departmentId(expense.getDepartmentId())
            .expenseDate(expense.getExpenseDate())
            .amount(expense.getAmount())
            .categoryId(expense.getCategory().getId())
            .categoryName(expense.getCategory().getName())
            .vendorName(expense.getVendorName())
            .description(expense.getDescription())
            .receiptUrl(expense.getReceiptUrl())
            .status(expense.getStatus())
            .submittedByUserId(expense.getSubmittedByUserId())
            .approvedByUserId(expense.getApprovedByUserId())
            .approvedDate(expense.getApprovedDate())
            .processInstanceId(expense.getProcessInstanceId())
            .rejectionReason(expense.getRejectionReason())
            .metadata(expense.getMetadata())
            .createdAt(expense.getCreatedAt())
            .updatedAt(expense.getUpdatedAt())
            .build();

        try {
            String displayName = UserContext.getDisplayName();
            response.setCreatedByDisplayName(displayName);
            response.setUpdatedByDisplayName(displayName);
        } catch (IllegalStateException e) {
            // UserContext not available — leave as null
        }

        return response;
    }
}
