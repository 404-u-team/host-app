package dev.hostapp.backend.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hostapp.backend.domain.Role;
import dev.hostapp.backend.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() {}

    public record Login(@NotBlank @Size(max = 255) String login, @NotBlank @Size(max = 255) String password) {}
    public record Refresh(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String refreshToken) {}
    public record PasswordChange(@NotBlank @Size(max = 255) String currentPassword,
                                 @NotBlank @Size(min = 12, max = 72) String newPassword) {}
    public record TokenPair(String accessToken, String refreshToken, long expiresIn, boolean mustChangePassword) {}
    public record UserInput(@NotBlank @Size(max = 255) String name,
                            @NotBlank @Size(max = 255) String surname,
                            @NotBlank @Size(max = 255) String login,
                            @NotEmpty @Size(max = 3) Set<@NotNull Role> roles,
                            @NotNull Boolean active) {}
    public record UserView(UUID id, String name, String surname, String login, Set<Role> roles,
                           boolean active, boolean mustChangePassword, Instant createdAt, Instant updatedAt) {
        public static UserView from(User user) {
            return new UserView(user.getId(), user.getName(), user.getSurname(), user.getEmail(),
                    user.getRoles(), user.isActive(), user.mustChangePassword(), user.getCreatedAt(), user.getUpdatedAt());
        }
    }
    public record CreateUser(@Valid @NotNull UserInput account,
                             @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
                             @Size(min = 12, max = 72) String temporaryPassword) {}
    public record ResetPassword(@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
                                @Size(min = 12, max = 72) String temporaryPassword) {}
}
