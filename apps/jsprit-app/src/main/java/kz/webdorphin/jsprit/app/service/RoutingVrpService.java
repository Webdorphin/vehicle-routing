package kz.webdorphin.jsprit.app.service;

import kz.webdorphin.jsprit.app.data.request.RouteRequest;
import kz.webdorphin.jsprit.app.data.response.RouteResponse;

public interface RoutingVrpService {

    RouteResponse solveVRP(RouteRequest payload);
}
