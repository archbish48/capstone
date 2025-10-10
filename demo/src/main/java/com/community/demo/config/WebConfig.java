package com.community.demo.config;


import com.community.demo.controller.UserBusyBlockInterceptor;
import com.community.demo.service.notice.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;


// MVC 관련 설정
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {    //정적 리소스 매핑 ( 파일을 브라우저에서 바로 볼 수 있게 하려면 필요함)

//    @Value("${file.dir}")
//    private String uploadDir;

    private final FileStorageService storage;
    private final UserBusyBlockInterceptor busyInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.getRootDir().toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(busyInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // /route 를 추가하여 인터셉터 제외
                        "/route/files/**",
                        "/route/swagger-ui/**",
                        "/route/v3/api-docs/**",
                        "/route/actuator/**",

                        //  기존 경로도 일단 유지 (안전을 위해)
                        "/files/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**"
                );
    }
}
