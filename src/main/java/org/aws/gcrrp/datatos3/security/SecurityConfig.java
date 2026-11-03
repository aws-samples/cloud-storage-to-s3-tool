package org.aws.gcrrp.datatos3.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                // Only the liveness endpoints are public; everything else requires a login.
                .antMatchers("/ping", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            .and()
                .formLogin()
            .and()
                .httpBasic()
            .and()
                .logout()
            .and()
                // CSRF stays enabled. The token is exposed in the XSRF-TOKEN cookie so the page's AJAX calls can send it.
                .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
    }
}
