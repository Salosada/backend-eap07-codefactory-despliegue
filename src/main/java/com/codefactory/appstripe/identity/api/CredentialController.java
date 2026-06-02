package com.codefactory.appstripe.identity.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codefactory.appstripe.identity.api.dto.CredentialItemResponse;
import com.codefactory.appstripe.identity.api.dto.CredentialResponse;
import com.codefactory.appstripe.identity.api.dto.GenerateCredentialRequest;
import com.codefactory.appstripe.identity.api.dto.RevokedCredentialResponse;
import com.codefactory.appstripe.identity.application.CredentialApplicationService;
import com.codefactory.appstripe.identity.domain.ApiCredential;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/credentials")
public class CredentialController {

    private final CredentialApplicationService credentialApplicationService;

    public CredentialController(CredentialApplicationService credentialApplicationService) {
        this.credentialApplicationService = credentialApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<CredentialItemResponse>> list() {
        List<CredentialItemResponse> credentials = credentialApplicationService.listAllCredentials().stream()
                .map(CredentialItemResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(credentials);
    }

    @PostMapping("/generate")
    public ResponseEntity<CredentialResponse> generate(@Valid @RequestBody GenerateCredentialRequest request) {
        ApiCredential generated = credentialApplicationService.generateCredentials(request.getMerchantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CredentialResponse.fromDomain(generated));
    }

    @PatchMapping("/{publicId}/revoke")
    public ResponseEntity<RevokedCredentialResponse> revoke(
            @PathVariable String publicId,
            Authentication authentication) {

        String merchantId = extractMerchantId(authentication);

        ApiCredential revoked = (merchantId != null)
                ? credentialApplicationService.revokeCredential(publicId, merchantId)
                : credentialApplicationService.revokeCredentialAsAdmin(publicId);

        return ResponseEntity.ok(RevokedCredentialResponse.fromDomain(revoked));
    }

    private String extractMerchantId(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }
        String cred = authentication.getCredentials().toString();
        return (cred.isBlank() || cred.equals("null")) ? null : cred;
    }
}
