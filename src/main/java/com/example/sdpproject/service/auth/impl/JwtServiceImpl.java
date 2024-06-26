package com.example.sdpproject.service.auth.impl;

import com.example.sdpproject.entity.enums.RoleEnum;
import com.example.sdpproject.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.sdpproject.util.JwtServiceConstants.*;

@Service
public class JwtServiceImpl implements JwtService {

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JWT_AUTHORITIES_KEY, authorities);

        return generateToken(claims, userDetails);
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(jwtIssuedDate())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    @Override
    public Set<SimpleGrantedAuthority> simpleGrantedAuthorities(Claims claims) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        String role = claims.get(JWT_AUTHORITIES_KEY).toString();

        switch (RoleEnum.valueOf(role)) {
            case ADMIN -> {
                authorities.add(new SimpleGrantedAuthority(JWT_SERVICE_ADMIN));
                authorities.add(new SimpleGrantedAuthority(JWT_SERVICE_USER));
            }
            case USER -> authorities.add(new SimpleGrantedAuthority(JWT_SERVICE_USER));
            default -> authorities.add(new SimpleGrantedAuthority(JWT_SERVICE_UNKNOWN));
        }

        return authorities;
    }

    @Override
    public Set<SimpleGrantedAuthority> simpleGrantedAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        return simpleGrantedAuthorities(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    private Date jwtIssuedDate() {
        return new Date(System.currentTimeMillis());
    }

    private Date jwtExpirationDate() {
        return new Date(System.currentTimeMillis() + 1000 * 60 * 24);
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(JWT_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}