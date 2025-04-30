package kz.webdorphin.jsprit.app.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GraphHopperRouteResponse {

    private double distanceMeters;
    private long timeMillis;
}
