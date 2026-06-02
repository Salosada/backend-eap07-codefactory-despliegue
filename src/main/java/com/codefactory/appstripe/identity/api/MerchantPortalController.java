package com.codefactory.appstripe.identity.api;

import java.time.LocalDate;
import java.util.List;

import com.codefactory.appstripe.transactions.api.dto.PaymentStatusDistributionResponse;
import com.codefactory.appstripe.transactions.application.TransactionApplicationService;
import com.codefactory.appstripe.transactions.application.query.PaymentStatusDistribution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import com.codefactory.appstripe.transactions.api.dto.TransactionVolumeReportResponse;
import com.codefactory.appstripe.transactions.application.query.TransactionVolumeReport;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codefactory.appstripe.identity.api.dto.CredentialItemResponse;
import com.codefactory.appstripe.identity.api.dto.MerchantResponse;
import com.codefactory.appstripe.identity.api.dto.UpdateMerchantProfileRequest;
import com.codefactory.appstripe.identity.application.CommerceApplicationService;
import com.codefactory.appstripe.identity.application.port.IApiCredentialRepositoryPort;
import com.codefactory.appstripe.identity.domain.Merchant;
import com.codefactory.appstripe.transactions.api.dto.TransactionResponse;
import com.codefactory.appstripe.transactions.application.port.ITransactionRepositoryPort;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/merchant-portal")
public class MerchantPortalController {

    private final CommerceApplicationService commerceApplicationService;
    private final IApiCredentialRepositoryPort credentialRepository;
    private final ITransactionRepositoryPort transactionRepository;
    private final TransactionApplicationService transactionApplicationService;

    public MerchantPortalController(
            CommerceApplicationService commerceApplicationService,
            IApiCredentialRepositoryPort credentialRepository,
            ITransactionRepositoryPort transactionRepository,
            TransactionApplicationService transactionApplicationService
    ) {
        this.commerceApplicationService = commerceApplicationService;
        this.credentialRepository = credentialRepository;
        this.transactionRepository = transactionRepository;
        this.transactionApplicationService = transactionApplicationService;
    }

    @GetMapping("/profile")
    public ResponseEntity<MerchantResponse> getProfile(Authentication authentication) {
        String merchantId = extractMerchantId(authentication);
        Merchant merchant = commerceApplicationService.getMerchantProfile(merchantId);
        return ResponseEntity.ok(MerchantResponse.fromDomain(merchant));
    }

    @GetMapping("/credentials")
    public ResponseEntity<List<CredentialItemResponse>> getCredentials(Authentication authentication) {
        String merchantId = extractMerchantId(authentication);
        List<CredentialItemResponse> credentials = credentialRepository.findByMerchantIdAndActiveTrue(merchantId).stream()
                .map(CredentialItemResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(credentials);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(Authentication authentication) {
        String merchantId = extractMerchantId(authentication);
        List<TransactionResponse> transactions = transactionRepository.findByMerchantId(merchantId).stream()
                .map(TransactionResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(transactions);
    }

    @PatchMapping("/profile")
    @PutMapping("/update-profile")
    public ResponseEntity<MerchantResponse> putProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateMerchantProfileRequest request) {

        String merchantId = extractMerchantId(authentication);
        Merchant merchant = commerceApplicationService.updateMerchant(
                merchantId,
                request.getBusinessName(),
                request.getEmail(),
                request.getBusinessType()
        );

        return ResponseEntity.ok(MerchantResponse.fromDomain(merchant));
    }

    private String extractMerchantId(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalStateException("El token no tiene un comercio asociado");
        }

        String merchantId = authentication.getCredentials().toString();

        if (merchantId.isBlank()) {
            throw new IllegalStateException("El token no tiene un comercio asociado");
        }

        return merchantId;
    }
    @GetMapping("/dashboard/payment-status-distribution")
    public ResponseEntity<PaymentStatusDistributionResponse> getPaymentStatusDistribution(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String merchantId = extractMerchantId(authentication);

        PaymentStatusDistribution dashboard =
                transactionApplicationService.getPaymentStatusDistribution(merchantId, from, to);

        return ResponseEntity.ok(PaymentStatusDistributionResponse.fromApplication(dashboard));
    }

    @GetMapping("/reports/transaction-volume")
    public ResponseEntity<TransactionVolumeReportResponse> getTransactionVolumeReport(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String groupBy
    ) {
        String merchantId = extractMerchantId(authentication);

        TransactionVolumeReport report = transactionApplicationService.getTransactionVolumeReport(
                merchantId,
                from,
                to,
                groupBy
        );

        return ResponseEntity.ok(TransactionVolumeReportResponse.fromApplication(report));
    }
}