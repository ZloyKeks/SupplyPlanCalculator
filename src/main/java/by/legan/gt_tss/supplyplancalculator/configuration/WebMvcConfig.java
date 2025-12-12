package by.legan.gt_tss.supplyplancalculator.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Конфигурация для настройки Spring MVC.
 * Добавляет HTTP-заголовки для разрешения использования событий unload (требуется для SockJS).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Настраивает обработку статических ресурсов.
     * Убеждается, что статические ресурсы обрабатываются раньше контроллеров.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Явно регистрируем статические ресурсы из classpath:/static/
        // Это гарантирует, что /index.html будет обслуживаться как статический ресурс
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // Отключаем кеширование для разработки
    }

    /**
     * Регистрирует фильтр для добавления HTTP-заголовка Permissions-Policy.
     * Это разрешает использование события unload, которое требуется библиотеке SockJS.
     *
     * @return FilterRegistrationBean для регистрации фильтра
     */
    @Bean
    public FilterRegistrationBean<Filter> permissionsPolicyFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PermissionsPolicyFilter());
        registration.addUrlPatterns("/*");
        registration.setName("permissionsPolicyFilter");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Фильтр для добавления HTTP-заголовка Permissions-Policy.
     */
    private static class PermissionsPolicyFilter implements Filter {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, 
                           FilterChain chain) throws IOException, ServletException {
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).setHeader("Permissions-Policy", "unload=*");
            }
            chain.doFilter(request, response);
        }
    }
}

