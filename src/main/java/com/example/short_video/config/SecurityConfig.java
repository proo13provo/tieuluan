package com.example.short_video.config;

import com.example.short_video.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/user/*/profile", "/api/users/*/profile").permitAll()
                        .requestMatchers("/api/user/*/videos", "/api/users/*/videos").permitAll()
                        .requestMatchers("/api/user/*/followers", "/api/users/*/followers").permitAll()
                        .requestMatchers("/api/user/*/following", "/api/users/*/following").permitAll()
                        .requestMatchers("/api/user/*/follow-status/**", "/api/users/*/follow-status/**", "/api/user/follow-status/**", "/api/users/follow-status/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/follow/**", "/api/follows/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/follow/status/batch", "/api/follow/batch-status", "/api/follows/status/batch", "/api/user/follow-status/batch", "/api/users/follow-status/batch").permitAll()
                        .requestMatchers("/api/video/hashtags/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/feed/**", "/api/videos/feed").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/search/**", "/api/search", "/api/videos/search/**", "/api/videos/search", "/api/users/search/**", "/api/users/search").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/videos/*/share").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/live/feed").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/live/user/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/live/*").permitAll()
                        .requestMatchers(
                                "/api/admin/users/*/roles/**",
                                "/api/admin/users/*/grant-moderator",
                                "/api/admin/users/*/revoke-moderator",
                                "/api/admin/users/*/moderator",
                                "/api/admin/roles/**",
                                "/api/moderation/users/*/roles/**",
                                "/api/moderation/users/*/grant-moderator",
                                "/api/moderation/users/*/revoke-moderator",
                                "/api/moderation/users/*/moderator",
                                "/api/moderation/roles/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/admin/categories/**", "/api/moderation/categories/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/admin/categories/**", "/api/moderation/categories/**", "/api/admin/hashtags/**", "/api/moderation/hashtags/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/admin/categories/**", "/api/moderation/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**", "/api/moderation/**").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers("/api/reports/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
