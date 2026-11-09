package utn.frc.backend.mediturnos.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import utn.frc.backend.mediturnos.common.exception.GlobalExceptionHandler;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdFilter;

/**
 * Auto-configuración de Spring Boot 3 para mediturnos-common-lib.
 * Registra automáticamente el filtro X-Correlation-Id y el GlobalExceptionHandler
 * en ms-catalogo-recursos y ms-reservas al incluir el JAR como dependencia Maven.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonLibAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CorrelationIdFilter.class)
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("correlationIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
