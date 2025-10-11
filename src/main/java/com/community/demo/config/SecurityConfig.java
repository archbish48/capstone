package com.community.demo.config;


import com.community.demo.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableMethodSecurity  // @PreAuthorize 활성화
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                // /route/v3/api-docs (API 명세 경로)
                "/route/v3/api-docs/**",
                "/v3/api-docs/**",

                // /route/swagger-ui/ (Swagger UI HTML, JS, CSS 등 모든 정적 파일)
                "/route/swagger-ui/**",
                "/swagger-ui/**",

                // /route/swagger-ui.html 및 /swagger-ui.html
                "/route/swagger-ui.html",
                "/swagger-ui.html"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()    //인증 관련 경로만 남기기
                        .requestMatchers("/files/**", "/profiles/**").permitAll() //프로필 이미지 접근 허용
                        .requestMatchers("/error").permitAll()  // error 열기

                        // notices (공지사항) 관련 규칙
                        .requestMatchers(HttpMethod.GET, "/notices/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/notices/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/notices/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/notices/**").hasAnyRole("STAFF","MANAGER","ADMIN")

                        // notifications 알림창 관련 규칙
                        .requestMatchers("/notifications/**").authenticated()


                        // 댓글·리액션은 로그인 필요
                        .requestMatchers("/community/*/reactions").authenticated()
                        .requestMatchers("/community/*/comments/**").authenticated()

                        // 커뮤니티 글: GET 은 전체 공개, 나머지는 로그인만 통과 (세부 권한은 @PreAuthorize 에서)
                        .requestMatchers(HttpMethod.GET, "/community/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/community/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/community/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/community/**").authenticated()

                        //  enroll-timer: 평균은 공개, 나머지는 인증 필요
                        .requestMatchers(HttpMethod.GET, "/enroll-timer/stats/average").permitAll()
                        .requestMatchers("/enroll-timer/**").authenticated()

                        // (임시) 디버그: 매핑/보안 확인용(인증필요)
                        .requestMatchers("/debug/**").authenticated()

                        // 챗봇 인증 필요
                        .requestMatchers(HttpMethod.POST, "/chatbot/ask").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/chatbot/history/**").authenticated()

                        //credits 학점정보컨트롤러
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/credits/upload").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,  "/credits").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/credits/flat").authenticated()

                        //마이페이지 컨트롤러 권한 설정 전부 인증 필요
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/mypage/**").authenticated()

                        //FAQ 권한 규칙 모든 유저가 쓸 수 있음
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/inquiries").hasAnyRole("STUDENT","STAFF","MANAGER","ADMIN")

                        
                        //관리자 페이지 관리자만 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN")



                        // 그 외 모든 요청은 전부 인증 필요
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            // 401
                            System.out.printf("401 %s %s : %s%n", req.getMethod(), req.getRequestURI(), e.getMessage());
                            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            // 403
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            System.out.printf("403 %s %s : auth=%s, authorities=%s, msg=%s%n",
                                    req.getMethod(), req.getRequestURI(),
                                    (auth == null ? null : auth.getPrincipal()),
                                    (auth == null ? null : auth.getAuthorities()),
                                    e.getMessage());
                            res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
