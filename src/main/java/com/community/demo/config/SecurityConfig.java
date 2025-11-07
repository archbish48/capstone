package com.community.demo.config;


import com.community.demo.jwt.ApiKeyAuthFilter;
import com.community.demo.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
    private final ApiKeyAuthFilter apiKeyAuthFilter;


    // JwtAuthenticationFilter 가 @Component 로 자동 등록되는 것을 방지합니다.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // 👈 [핵심] 자동 등록 비활성화
        return registration;
    }

    // ApiKeyAuthFilter 가 @Component 로 자동 등록되는 것을 방지합니다.
    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration(ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // 👈 [핵심] 자동 등록 비활성화
        return registration;
    }



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
                "/swagger-ui.html",

                // files 경로를 보안 필터 체인에서 완전히 제외
                "/files/**",
                "/route/files/**"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // [수정] /auth/** 뿐만 아니라 /route/auth/** 도 permitAll
                        .requestMatchers("/auth/**", "/route/auth/**").permitAll()
                        // [수정] 다른 permitAll 경로들에도 /route/ 버전을 추가
                        .requestMatchers("/files/**", "/route/files/**", "/profiles/**", "/route/profiles/**").permitAll()
                        .requestMatchers("/error", "/route/error").permitAll()

                        //  (FastAPI 크롤러용)
                        // [신규] 'ROLE_CRAWLER'만 /notices/school 에 POST 허용
                        .requestMatchers(HttpMethod.POST, "/notices/school").hasRole("CRAWLER")

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
                //.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                // 경로에 따라 JWT 필터와 API Key 필터를 분기
                .addFilterBefore(
                        (request, response, chain) -> {

                            HttpServletRequest httpRequest = (HttpServletRequest) request;
                            String uri = httpRequest.getRequestURI();
                            String method = httpRequest.getMethod();

                            // 람다는 '전체 경로' ( '/route' 있음 )
                            String crawlerPath = "/route/notices/school";

                            if (uri.equals(crawlerPath) && method.equals(HttpMethod.POST.name())) {
                                // 크롤러 경로는 ApiKeyAuthFilter 실행
                                apiKeyAuthFilter.doFilter(request, response, chain);
                            } else {
                                // 그 외 경로는 JwtAuthenticationFilter 실행
                                jwtFilter.doFilter(request, response, chain);
                            }
                        },
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
//        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
//        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
//        config.setAllowCredentials(true);
//        config.setMaxAge(Duration.ofHours(1));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }

}
