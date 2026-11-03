package com.proofapp.api.user;

import com.proofapp.api.common.Errors.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Me")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    public record UserDto(Long id, String email, String displayName) {
        public static UserDto from(User user) {
            return new UserDto(user.getId(), user.getEmail(), user.getDisplayName());
        }
    }

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "The signed-in user")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(Long.valueOf(jwt.getSubject()))
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
