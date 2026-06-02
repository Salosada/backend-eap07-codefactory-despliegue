package com.codefactory.appstripe.identity.api.dto;

import com.codefactory.appstripe.identity.domain.Merchant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterMerchantResponse {
    String id;
    String businessName;
    String businessId;
    String email;
    String businessType;
    String status;
    String permission;
    String invitationToken;

    public static RegisterMerchantResponse fromDomain(Merchant merchant, String invitationToken) {
        return RegisterMerchantResponse.builder()
                .id(merchant.getId())
                .businessName(merchant.getBusinessName())
                .businessId(merchant.getBusinessId())
                .email(merchant.getEmail())
                .businessType(merchant.getBusinessType())
                .status(merchant.getStatus().name())
                .permission(merchant.getPermission().name())
                .invitationToken(invitationToken)
                .build();
    }
}
