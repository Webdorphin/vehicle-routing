package kz.webdorphin.jsprit.app.data.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteResponse {

    private List<String> optimizedOrder;
    private double totalDistanceMeters;
    private double totalTimeSeconds;
    private String polyline;
}
