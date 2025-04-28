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

    /**
     * matrix[i][j] = distance from point_i to point_j.
     */
    private double[][] distances;

    /**
     *  matrix[i][j] = time from point_i to point_j.
     */
    private double[][] times;
}
