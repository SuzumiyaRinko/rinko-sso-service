package suzumiya.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import suzumiya.filter.StatisticsFilter;

@EnableWebSecurity // 标识为Security配置类
@EnableGlobalMethodSecurity(prePostEnabled = true) // 开启SpringSecurity权限控制
public class SecurityConfig {

    @Autowired
    private StatisticsFilter statisticsFilter;

    @Bean("authenticationManager")
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean("passwordEncoder")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable() // 关闭csrf
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and() // 不通过Session获取SecurityContext（前后端分离的情况下就要设置这个）
                .authorizeRequests(authorize -> authorize
                        .anyRequest().permitAll().and()
                        .addFilterAfter(statisticsFilter, FilterSecurityInterceptor.class)
                ); // 允许跨域访问
        return http.build();
    }
}
