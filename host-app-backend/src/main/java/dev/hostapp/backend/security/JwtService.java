package dev.hostapp.backend.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.UserRepository;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final byte[] key;
    private final String issuer;
    private final Duration lifetime;
    private final UserRepository users;

    public JwtService(@Value("${app.jwt.secret-base64}") String secret,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.access-minutes}") int minutes,
                      UserRepository users) {
        key = secret.isBlank() ? Base64.getUrlDecoder().decode(Tokens.random()) : Base64.getDecoder().decode(secret);
        if (key.length < 32 || issuer.isBlank() || minutes < 1 || minutes > 60) {
            throw new IllegalArgumentException("JWT: ключ от 32 байт, непустой issuer и срок от 1 до 60 минут");
        }
        this.issuer = issuer;
        this.lifetime = Duration.ofMinutes(minutes);
        this.users = users;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(lifetime)))
                .claim("purpose", "access")
                .claim("ver", user.getAuthVersion())
                .build());
        try {
            jwt.sign(new MACSigner(key));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Не удалось подписать JWT", exception);
        }
    }

    public long expiresInSeconds() { return lifetime.toSeconds(); }

    public User resolve(String token) {
        if (token.length() > 2048) {
            return null;
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(key))) {
                return null;
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expires = claims.getExpirationTime();
            Long version = claims.getLongClaim("ver");
            if (!issuer.equals(claims.getIssuer()) || !"access".equals(claims.getStringClaim("purpose"))
                    || expires == null || !expires.toInstant().isAfter(Instant.now())
                    || version == null || claims.getSubject() == null) {
                return null;
            }
            return users.findById(UUID.fromString(claims.getSubject()))
                    .filter(user -> user.isActive() && user.isEmailVerified() && user.getAuthVersion() == version)
                    .orElse(null);
        } catch (ParseException | JOSEException | IllegalArgumentException exception) {
            return null;
        }
    }
}
