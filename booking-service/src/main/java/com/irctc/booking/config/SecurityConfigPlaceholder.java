package com.irctc.booking.config;

import org.springframework.context.annotation.Configuration;

/**
 * Security Configuration Placeholder.
 * 
 * To enable Spring Security and JWT Authentication:
 * 1. Uncomment spring-boot-starter-security dependency in pom.xml
 * 2. Annotate this class with @EnableWebSecurity
 * 3. Define SecurityFilterChain Bean to authorize requests and validate JWTs.
 * 
 * Example setup:
 * 
 * @Configuration
 * @EnableWebSecurity
 * public class SecurityConfig {
 *     @Bean
 *     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         http
 *             .csrf(AbstractHttpConfigurer::disable)
 *             .authorizeHttpRequests(auth -> auth
 *                 .requestMatchers("/actuator/**", "/api/v1/booking/health").permitAll()
 *                 .anyRequest().authenticated()
 *             )
 *             .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 *             .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
 *         return http.build();
 *     }
 * }
 */
@Configuration
public class SecurityConfigPlaceholder {
    // Left empty deliberately as a template for Spring Security/JWT configuration.
}
