package com.codefactory.appstripe.security.api.dto;

import com.codefactory.appstripe.security.domain.User;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountStatusResponse {
    String email;
    String role;
    String merchantId;
    boolean accountActivated;
    String invitationToken;

    public static AccountStatusResponse fromDomain(User user) {
        return AccountStatusResponse.builder()
                .email(user.getEmail())
                .role("ROLE_" + user.getRole())
                .merchantId(user.getMerchantId())
                .accountActivated(user.isAccountActivated())
                .invitationToken(user.isAccountActivated() ? null : user.getInvitationToken())
                .build();
    }
}
