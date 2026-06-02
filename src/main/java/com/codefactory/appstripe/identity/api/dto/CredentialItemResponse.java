package com.codefactory.appstripe.identity.api.dto;

import com.codefactory.appstripe.identity.domain.ApiCredential;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CredentialItemResponse {
    String publicId;
    String merchantId;
    boolean active;

    public static CredentialItemResponse fromDomain(ApiCredential credential) {
        return CredentialItemResponse.builder()
                .publicId(credential.getPublicId())
                .merchantId(credential.getMerchantId())
                .active(credential.isActive())
                .build();
    }
}
