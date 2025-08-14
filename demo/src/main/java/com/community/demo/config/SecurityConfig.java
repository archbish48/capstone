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

                        // notices (공지사항) 관련 규칙
                        .requestMatchers(HttpMethod.GET, "/notices/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/notices/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/notices/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/notices/**").hasAnyRole("STAFF","MANAGER","ADMIN")


                        // 댓글·리액션은 로그인 필요
                        .requestMatchers("/community/*/reactions").authenticated()
                        .requestMatchers("/community/*/comments/**").authenticated()

                        // 커뮤니티 글: GET 은 전체 공개, 나머지는 로그인만 통과 (세부 권한은 @PreAuthorize 에서)
                        .requestMatchers(HttpMethod.GET, "/community/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/community/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/community/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/community/**").authenticated()


                        // 그 외 모든 요청은 전부 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
