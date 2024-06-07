package de.fabiankru.javawings.config;

import de.fabiankru.javawings.JavaWings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null ||!authHeader.startsWith("Bearer")) {
            response.addHeader("WWW-Authenticate", "Bearer");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"The required authorization headers were not present in the request.\"}");
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7); // remove "Bearer."

        if (!JavaWings.WINGS_SECRET.equals(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"You are not authorized to access this endpoint.\"}");
            return;
        }
        UsernamePasswordAuthenticationToken u = new UsernamePasswordAuthenticationToken(token, null, null);
        u.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(u);

        filterChain.doFilter(request, response);

    }

}
