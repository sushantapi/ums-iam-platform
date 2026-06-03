/*
 * package com.ums.auth.security;
 * 
 * import java.io.IOException; import java.util.List; import
 * java.util.stream.Collectors;
 * 
 * import org.springframework.security.authentication.
 * UsernamePasswordAuthenticationToken; import
 * org.springframework.security.core.authority.SimpleGrantedAuthority; import
 * org.springframework.security.core.context.SecurityContextHolder; import
 * org.springframework.security.web.authentication.
 * WebAuthenticationDetailsSource; import
 * org.springframework.stereotype.Component; import
 * org.springframework.web.filter.OncePerRequestFilter;
 * 
 * import com.ums.auth.service.JwtService; import
 * com.ums.auth.service.TokenBlacklistService;
 * 
 * import io.jsonwebtoken.Claims; import jakarta.servlet.FilterChain; import
 * jakarta.servlet.ServletException; import
 * jakarta.servlet.http.HttpServletRequest; import
 * jakarta.servlet.http.HttpServletResponse; import
 * lombok.RequiredArgsConstructor;
 * 
 * @Component
 * 
 * @RequiredArgsConstructor public class JwtAuthenticationFilter extends
 * OncePerRequestFilter {
 * 
 * private final JwtService jwtService; private final TokenBlacklistService
 * blacklistService;
 * 
 * @Override protected void doFilterInternal(HttpServletRequest request,
 * HttpServletResponse response, FilterChain filterChain) throws
 * ServletException, IOException {
 * 
 * String authHeader = request.getHeader("Authorization");
 * 
 * if (authHeader == null || !authHeader.startsWith("Bearer ")) {
 * filterChain.doFilter(request, response); return; }
 * 
 * try {
 * 
 * String token = authHeader.substring(7);
 * 
 * Claims claims = jwtService.validateAndExtract(token);
 * 
 * String jti = claims.getId();
 * 
 * if (blacklistService.isBlacklisted(jti)) {
 * response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
 * "Token has been revoked"); return; }
 * 
 * List<String> roles = claims.get("roles", List.class);
 * 
 * var authorities = roles.stream().map(role -> new
 * SimpleGrantedAuthority("ROLE_" + role)) .collect(Collectors.toList());
 * 
 * UsernamePasswordAuthenticationToken authentication = new
 * UsernamePasswordAuthenticationToken( claims.getSubject(), null, authorities);
 * 
 * authentication.setDetails(new
 * WebAuthenticationDetailsSource().buildDetails(request));
 * 
 * SecurityContextHolder.getContext().setAuthentication(authentication);
 * 
 * } catch (Exception ex) {
 * 
 * response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
 * return; }
 * 
 * filterChain.doFilter(request, response); } }
 */