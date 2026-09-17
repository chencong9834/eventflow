package com.example.eventflow.identity.web;

import com.example.eventflow.identity.AuthService;
import com.example.eventflow.identity.web.LoginResponse.CurrentUserResponse;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.shared.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    return ApiResponse.ok(
        authService.login(request.getUsername(), request.getPassword(), ClientIp.from(httpRequest)));
  }

  @GetMapping("/me")
  public ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
    return ApiResponse.ok(authService.currentUser(principal.getUserId()));
  }
}
