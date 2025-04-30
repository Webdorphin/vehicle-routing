package kz.webdorphin.jsprit.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class GraphHopperConfiguration {

    @Value("${integration.graphhopper.base-url}")
    private String graphHopperBaseUrl;

    @Bean(name = "graphHopperTemplate")
    public RestTemplate graphHopperTemplate() {
        var restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(graphHopperBaseUrl));
        return restTemplate;
    }
}
