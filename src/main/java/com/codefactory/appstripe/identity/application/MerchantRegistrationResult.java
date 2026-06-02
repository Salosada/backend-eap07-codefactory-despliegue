package com.codefactory.appstripe.identity.application;

import com.codefactory.appstripe.identity.domain.Merchant;
import lombok.Value;

@Value
public class MerchantRegistrationResult {
    Merchant merchant;
    String invitationToken;
}
