package kz.webdorphin.jsprit.app.service;

import kz.webdorphin.jsprit.app.data.request.RouteRequest;
import kz.webdorphin.jsprit.app.data.response.RouteResponse;

public interface RoutingService {

    RouteResponse solveVRP(RouteRequest payload);
}
