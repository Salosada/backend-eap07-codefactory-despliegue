package com.codefactory.appstripe.security.api;

import com.codefactory.appstripe.security.api.dto.AccountStatusResponse;
import com.codefactory.appstripe.security.application.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AuthenticationService authenticationService;

    public AdminAccountController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public ResponseEntity<List<AccountStatusResponse>> list() {
        return ResponseEntity.ok(authenticationService.listAccounts());
    }
}
