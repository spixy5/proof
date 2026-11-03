package com.proofapp.api.auth;

import com.proofapp.api.auth.AuthController.AuthResponse;
import com.proofapp.api.auth.AuthController.LoginRequest;
import com.proofapp.api.auth.AuthController.RegisterRequest;
import com.proofapp.api.common.Errors.ConflictException;
import com.proofapp.api.user.MeController.UserDto;
import com.proofapp.api.user.User;
import com.proofapp.api.user.UserRepository;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalise(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        return respond(user);
    }

    private AuthResponse respond(User user) {
        TokenService.IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), UserDto.from(user));
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
