package com.kidbank.controller;

import com.kidbank.dto.Dtos.*;
import com.kidbank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse addTransaction(@PathVariable Long userId,
                                              @Valid @RequestBody TransactionRequest req) {
        return transactionService.addTransaction(userId, req);
    }

    @GetMapping
    public List<TransactionResponse> getTransactions(@PathVariable Long userId,
                                                     @RequestParam(required = false) String range) {
        return transactionService.getTransactions(userId, range);
    }

    @GetMapping("/summary")
    public SummaryResponse getSummary(@PathVariable Long userId,
                                      @RequestParam(required = false) String range) {
        return transactionService.getSummary(userId, range);
    }

    @PostMapping("/{txId}/void")
    public TransactionResponse voidTransaction(@PathVariable Long userId, @PathVariable Long txId) {
        return transactionService.voidTransaction(userId, txId);
    }
}
