package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 通用配置。
 * 注意：按需求，用户密码以明文存储，故不再提供 BCrypt PasswordEncoder。
 * 若日后要改回哈希存储，在此恢复 PasswordEncoder Bean，并在 AuthServiceImpl
 * 中把「明文比对 / 明文写入」改回 encode + matches 即可。
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    /**
     * 全局 CORS 兜底：各 Controller 虽有 @CrossOrigin(origins = "http://192.168.68.47:5173")，
     * 但浏览器对同源 POST 也会携带 Origin 头（如 127.0.0.1:5174 / 局域网 IP），
     * 端口或主机一变就会被 403 Invalid CORS request 拒掉。
     * 这里放行所有来源（令牌走 Authorization 头、不用 Cookie，无 CSRF 风险）。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
