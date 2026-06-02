package com.codefactory.appstripe.transactions.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codefactory.appstripe.transactions.api.dto.TransactionResponse;
import com.codefactory.appstripe.transactions.application.TransactionApplicationService;

@RestController
@RequestMapping("/api/v1/admin/transactions")
public class AdminTransactionController {

    private final TransactionApplicationService transactionApplicationService;

    public AdminTransactionController(TransactionApplicationService transactionApplicationService) {
        this.transactionApplicationService = transactionApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list() {
        List<TransactionResponse> transactions = transactionApplicationService.getAllTransactions().stream()
                .map(TransactionResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(transactions);
    }
}
