/*
 * package com.ums.gateway.security;
 * 
 * import javax.crypto.SecretKey;
 * 
 * import org.springframework.beans.factory.annotation.Value; import
 * org.springframework.stereotype.Service;
 * 
 * import io.jsonwebtoken.Claims; import io.jsonwebtoken.Jwts; import
 * io.jsonwebtoken.security.Keys;
 * 
 * @Service public class JwtService {
 * 
 * @Value("${jwt.secret}") private String secretKey;
 * 
 * public Claims extractClaims(String token) {
 * 
 * return Jwts.parserBuilder()
 * 
 * .setSigningKey(getSigningKey())
 * 
 * .build()
 * 
 * .parseClaimsJws(token)
 * 
 * .getBody(); }
 * 
 * private SecretKey getSigningKey() {
 * 
 * return Keys.hmacShaKeyFor(secretKey.getBytes()); } }
 */