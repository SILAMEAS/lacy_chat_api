package com.lacy.chat.config;

import com.lacy.chat.modules.user.service.CustomOAuth2UserService;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
//  Scan properties from yml
  private final AppProperties appProperties;
//  Custom OAuth2 + Save User to DB
  private final CustomOAuth2UserService oAuth2UserService;
//  Handle when OAuth2 Success
  private final OAuth2SuccessHandler successHandler;
//  Implement JWT
  private final JwtAuthenticationFilter jwtFilter;

//  Config Source : (cor, method, header-request, ... )
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(appProperties.getCors_origin()));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {

//  Apply CorsConfigurationSource
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth
                    // Public paths
                    .requestMatchers("/", "/oauth2/**", "/login/**", "/h2-console/**","/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    // API endpoints require authentication
                    .requestMatchers("/api/**")
                    .authenticated()
                    // Any other paths are public
                    .anyRequest()
                    .permitAll())
        .oauth2Login(
            oauth ->
                oauth
                    .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                    .successHandler(successHandler))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (HttpServletRequest request,
                     HttpServletResponse response,
                     AuthenticationException
                            authException) -> {
                      String uri = request.getRequestURI();

                      // API call: return 401
                      if (uri.startsWith("/api/")) {
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Unauthorized");
                      } else {
                        // Web page: redirect to OAuth login
                        response.sendRedirect("/oauth2/authorization/google");
                      }
                    }))
//  Apply jwtFilter
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    // Disable frame options for H2 console
    http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

    return http.build();
  }
}
