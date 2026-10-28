package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;

/**
 * 通用配置。
 * 注意：按需求，用户密码以明文存储，故不再提供 BCrypt PasswordEncoder。
 * 若日后要改回哈希存储，在此恢复 PasswordEncoder Bean，并在 AuthServiceImpl
 * 中把「明文比对 / 明文写入」改回 encode + matches 即可。
 */
@Configuration
public class AppConfig {
}
