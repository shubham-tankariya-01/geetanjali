package com.geetanjali.backend.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// SecurityConfig: Defines web application security, route protections, and authentication rules.
// In Express, this corresponds to setting up your authentication middleware (e.g. passport.authenticate) and route guards.
// @Configuration = Tells Spring that this class contains Spring Bean configuration definitions.
@Configuration
// @EnableWebSecurity = Activates Spring Security web filtering for incoming HTTP requests.
@EnableWebSecurity
// @EnableMethodSecurity = Allows using annotations like @PreAuthorize("hasRole('OWNER')") on controller methods.
@EnableMethodSecurity
public class SecurityConfig {

    // PasswordEncoder bean: Configures BCrypt for hashing and verifying passwords.
    // WHY: BCrypt uses a salted adaptive hash algorithm that protects passwords from rainbow table attacks.
    // Input: None.
    // Output: PasswordEncoder bean.
    // @Bean = Tells Spring to manage this BCrypt encoder as a shared application singleton.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // SecurityFilterChain: Defines the chain of security filters applied to each HTTP request.
    // Input: HttpSecurity http - builder object for route rules, login form, and logout behaviors.
    // Output: SecurityFilterChain - compiled filter pipeline.
    // @Bean = Tells Spring to register this filter chain with the servlet container.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Route authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow error and forward dispatches so custom error pages render properly
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                // Allow public access to login, signup, static CSS/JS files, uploaded photos, and error page
                .requestMatchers("/login", "/signup", "/css/**", "/js/**", "/uploads/**", "/error").permitAll()
                // Any other route requires the user to be authenticated
                .anyRequest().authenticated()
            )
            // HTML Form Login configuration
            .formLogin(form -> form
                // Custom GET /login URL instead of the default auto-generated Spring Security login page
                .loginPage("/login")
                // Custom form field name for the login identifier (mobile number instead of "username")
                .usernameParameter("mobile")
                // Form field name for the password
                .passwordParameter("password")
                // Redirect user to home page ("/") upon successful login
                .defaultSuccessUrl("/", true)
                // Permit anyone to access the login endpoint
                .permitAll()
            )
            // Logout configuration: only accepts POST requests to /logout (with CSRF token for security)
            .logout(logout -> logout
                .logoutUrl("/logout")
                // Redirect to login page with a ?logout flag after successfully logging out
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        // CSRF protection remains ON by default in Spring Security
        return http.build();
    }
}
