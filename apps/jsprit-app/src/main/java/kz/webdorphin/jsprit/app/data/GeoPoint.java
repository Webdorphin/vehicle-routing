package kz.webdorphin.jsprit.app.data;

import lombok.Data;

@Data
public class GeoPoint {

    /**
     * Point identifier (name).
     */
    private String id;

    /**
     * Latitude.
     */
    private Double lat;

    /**
     * Longitude.
     */
    private Double lon;
}
