package kz.webdorphin.jsprit.app.data.request;

import java.util.List;
import kz.webdorphin.jsprit.app.data.GeoPoint;
import lombok.Data;

@Data
public class RouteRequest {

    /**
     * Geo points to traverse, including a starting point at deliveryPoints[0] (warehouse).
     */
    private List<GeoPoint> points;
}
