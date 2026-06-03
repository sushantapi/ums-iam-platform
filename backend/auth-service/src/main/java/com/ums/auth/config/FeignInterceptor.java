
  package com.ums.auth.config;
  
  import java.time.Instant;
  
  import org.springframework.security.oauth2.jwt.JwtClaimsSet; import
  org.springframework.security.oauth2.jwt.JwtEncoder; import
  org.springframework.security.oauth2.jwt.JwtEncoderParameters; import
  org.springframework.stereotype.Component;
  
  import feign.RequestInterceptor; import feign.RequestTemplate; import
  lombok.RequiredArgsConstructor;
  
  @Component
  
  @RequiredArgsConstructor public class FeignInterceptor implements
  RequestInterceptor {
  
  private final JwtEncoder jwtEncoder;
  
  @Override public void apply(RequestTemplate template) {
  
  Instant now = Instant.now();
  
  JwtClaimsSet claims =
  JwtClaimsSet.builder().issuer("auth-service").subject("AUTH-SERVICE").
  issuedAt(now) .expiresAt(now.plusSeconds(300)).claim("service",
  true).build();
  
  String token =
  jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  
  template.header("Authorization", "Bearer " + token); } }
 