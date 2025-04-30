package kz.webdorphin.jsprit.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.webdorphin.jsprit.app.data.GeoPoint;
import kz.webdorphin.jsprit.app.data.response.GraphHopperRouteResponse;
import kz.webdorphin.jsprit.app.service.GraphHopperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GraphHopperServiceImpl implements GraphHopperService {
    private final RestTemplate graphHopperTemplate;
    private final ObjectMapper objectMapper;

    public GraphHopperRouteResponse getRoute(GeoPoint from, GeoPoint to) {
        try {
            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("points", List.of(
                List.of(from.getLon(), from.getLat()),
                List.of(to.getLon(), to.getLat())
            ));

            requestBody.put("profile", "car");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = graphHopperTemplate.exchange(
                "/route", HttpMethod.POST, entity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode path = root.get("paths").get(0);

            double distance = path.get("distance").asDouble();
            long time = path.get("time").asLong();

            return new GraphHopperRouteResponse(distance, time);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call GraphHopper", e);
        }
    }
}
