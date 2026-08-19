package com.ums.hrms.leave.config;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
@Component
public class TrustedGatewayAuthenticationFilter extends OncePerRequestFilter {
 static final String AUTHENTICATED_USER_HEADER="X-Authenticated-User";
 static final String USER_ROLES_HEADER="X-User-Roles";
 static final String USER_PERMISSIONS_HEADER="X-User-Permissions";
 static final String INTERNAL_GATEWAY_SECRET_HEADER="X-Internal-Gateway-Secret";
 private final String secret;
 public TrustedGatewayAuthenticationFilter(@Value("${internal.gateway.secret}") String secret){this.secret=secret;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String user=req.getHeader(AUTHENTICATED_USER_HEADER), supplied=req.getHeader(INTERNAL_GATEWAY_SECRET_HEADER);
  if(StringUtils.hasText(secret)&&secret.equals(supplied)&&StringUtils.hasText(user)&&isUuid(user)){
   List<SimpleGrantedAuthority> a=new ArrayList<>(); values(req.getHeader(USER_ROLES_HEADER)).stream().map(r->r.startsWith("ROLE_")?r:"ROLE_"+r).map(SimpleGrantedAuthority::new).forEach(a::add); values(req.getHeader(USER_PERMISSIONS_HEADER)).stream().map(SimpleGrantedAuthority::new).forEach(a::add);
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,a));
  }
  try{chain.doFilter(req,res);}finally{SecurityContextHolder.clearContext();}
 }
 private boolean isUuid(String value){try{UUID.fromString(value);return true;}catch(IllegalArgumentException e){return false;}}
 private List<String> values(String header){if(!StringUtils.hasText(header))return List.of();return Arrays.stream(header.replace("[","").replace("]","").split(",")).map(String::trim).filter(StringUtils::hasText).toList();}
}
