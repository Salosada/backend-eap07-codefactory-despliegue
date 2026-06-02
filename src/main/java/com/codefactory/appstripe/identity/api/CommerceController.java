package com.codefactory.appstripe.identity.api;

import com.codefactory.appstripe.identity.api.dto.MerchantResponse;
import com.codefactory.appstripe.identity.api.dto.RegisterMerchantRequest;
import com.codefactory.appstripe.identity.api.dto.RegisterMerchantResponse;
import com.codefactory.appstripe.identity.application.CommerceApplicationService;
import com.codefactory.appstripe.identity.application.MerchantRegistrationResult;
import com.codefactory.appstripe.identity.domain.Merchant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/merchants")
public class CommerceController {

    private final CommerceApplicationService commerceApplicationService;

    public CommerceController(CommerceApplicationService commerceApplicationService) {
        this.commerceApplicationService = commerceApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<MerchantResponse>> list() {
        List<MerchantResponse> merchants = commerceApplicationService.listMerchants().stream()
                .map(MerchantResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(merchants);
    }

    @PostMapping
    public ResponseEntity<RegisterMerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request) {
        MerchantRegistrationResult result = commerceApplicationService.registerMerchant(
                request.getBusinessName(),
                request.getBusinessId(),
                request.getEmail(),
                request.getBusinessType());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisterMerchantResponse.fromDomain(result.getMerchant(), result.getInvitationToken()));
    }
}
