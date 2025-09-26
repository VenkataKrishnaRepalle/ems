package com.learning.emsmybatisliquibase.security;

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

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expireDate)
                .signWith(key())
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        String username = authentication.getName();
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + refreshTokenExpirationDate);
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expireDate)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshTokenSecretKey)))
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
        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String getUsernameForRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) refreshKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

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
        try {
            Jwts.parser()
                    .setSigningKey(key())
                    .build()
                    .parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(refreshKey())
                    .build()
                    .parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
