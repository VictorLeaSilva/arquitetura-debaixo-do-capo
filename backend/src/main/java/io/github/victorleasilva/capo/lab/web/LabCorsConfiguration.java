package io.github.victorleasilva.capo.lab.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * O CORS da API (03 §5; AC-RNF-009.4; Q-045): só a tela do laboratório, em {@code http://localhost:5173}. Outra origem
 * recebe o 403 padrão do Spring, sem {@code Access-Control-Allow-Origin}.
 */
@Configuration
public class LabCorsConfiguration implements WebMvcConfigurer {

    static final String LAB_ORIGIN = "http://localhost:5173";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(LAB_ORIGIN)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type")
                .exposedHeaders("Location");
    }
}
