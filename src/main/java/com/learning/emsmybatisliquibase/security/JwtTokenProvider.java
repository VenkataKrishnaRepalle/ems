package com.learning.emsmybatisliquibase.security;

import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.utils.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecretKey;

    @Value("${app.jwt-expiration-milliseconds}")
    private Long jwtExpirationDate;

    @Value("${app.jwt.refresh-secret}")
    private String refreshTokenSecretKey;

    @Value("${app.refresh-token-expiration-milliseconds}")
    private Long refreshTokenExpirationDate;

    private static final String ACCESS_TOKEN = "access-token";

    private static final String REFRESH_TOKEN = "refresh-token";

    public String generateAccessToken(String sessionId, Authentication authentication) {
        return token(sessionId, authentication.getName(), ACCESS_TOKEN);
    }

    public String generateRefreshToken(String sessionId, Authentication authentication) {
        return token(sessionId, authentication.getName(), REFRESH_TOKEN);
    }

    private String token(String sessionId, String username, String type) {
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);
        String secretKey = jwtSecretKey;
        if (type.equals(REFRESH_TOKEN)) {
            expireDate = new Date(currentDate.getTime() + refreshTokenExpirationDate);
            secretKey = refreshTokenSecretKey;
        }
        return Jwts.builder()
                .claim(Constants.SESSION_ID, sessionId)
                .claim("type", type)
                .subject(username)
                .issuedAt(currentDate)
                .expiration(expireDate)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecretKey)
        );
    }

    private Key refreshKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(refreshTokenSecretKey)
        );
    }

    public String getUsername(String token) {
        var claims = extractTokenAllClaims(token);
        return claims.getSubject();
    }

    public boolean validateToken(String token, HttpServletRequest request) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parse(token);
            return true;
        } catch (ExpiredJwtException ex) {
            request.setAttribute("tokenExpired", true);
            return false;
        } catch (AccessDeniedException exception) {
            request.setAttribute("not_authorized", true);
            return false;
        } catch (Exception ex) {
            request.setAttribute("invalidToken", true);
            return false;
        }
    }

    public boolean validateToken(String token) {
        var claims = extractTokenAllClaims(token);
        return claims != null && claims.getExpiration().before(new Date());
    }

    public boolean validateRefreshToken(String refreshToken) {
        var claims = extractRefreshTokenAllClaims(refreshToken);
        return claims != null && claims.getExpiration().before(new Date());
    }

    public Claims extractTokenAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new IntegrityException("JWT_TOKEN_EXPIRED", exception.getMessage());
        }
    }

    public Claims extractRefreshTokenAllClaims(String refreshToken) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) refreshKey())
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new IntegrityException("JWT_REFRESH_TOKEN_EXPIRED", exception.getMessage());
        }
    }

}
