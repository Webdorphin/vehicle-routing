package kz.webdorphin.jsprit.app.service;

import kz.webdorphin.jsprit.app.data.GeoPoint;
import kz.webdorphin.jsprit.app.data.RouteCostMatrixDto;

import java.util.List;

public interface RoutingCostService {

    RouteCostMatrixDto getRouteCostMatrix(List<GeoPoint> points);
}
