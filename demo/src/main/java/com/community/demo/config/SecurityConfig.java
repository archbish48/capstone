package com.community.demo.config;


import com.community.demo.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity  // @PreAuthorize 활성화
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // notices (공지사항) 권한 규칙
                        .requestMatchers(HttpMethod.GET, "/notices/**").permitAll()               // 열람 자유
                        .requestMatchers("/notices/**").hasAnyRole("MANAGER", "ADMIN")     // 작성, 수정, 삭제는 MANAGER, ADMIN 등급만 가능 (권한 등급 범위는 나중에 조정 가능)

                        // 댓글·리액션은 로그인 필요
                        .requestMatchers("/community/*/reactions").authenticated()
                        .requestMatchers("/community/*/comments/**").authenticated()

                        // 나머지 커뮤니티 GET 은 열람 자유
                        .requestMatchers(HttpMethod.GET, "/community/**").permitAll()


                        // 그 외 모든 요청은 전부 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
