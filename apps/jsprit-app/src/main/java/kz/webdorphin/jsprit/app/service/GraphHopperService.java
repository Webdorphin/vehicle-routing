package kz.webdorphin.jsprit.app.service;

import kz.webdorphin.jsprit.app.data.GeoPoint;
import kz.webdorphin.jsprit.app.data.response.GraphHopperRouteResponse;

public interface GraphHopperService {

    GraphHopperRouteResponse getRoute(GeoPoint from, GeoPoint to);
}
