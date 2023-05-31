package us.dot.its.jpo.ode.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import us.dot.its.jpo.ode.api.accessors.map.ProcessedMapRepository;

import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@EnableWebMvc
@SpringBootApplication
public class ConflictApiApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ConflictApiApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(ConflictApiApplication.class, args);
        System.out.println("Started Conflict Monitor API");
        System.out.println("Conflict Monitor API docs page found here: http://localhost:8081/swagger-ui/index.html");
        System.out.println("Startup Complete");
        
        
    }

    @Bean
    
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                Properties props = new Properties();
                registry.addMapping("/**").allowedOrigins(props.getCors());
                // registry.addMapping("/**").allowedMethods("*");
            }
        };
    }

    
    
}
