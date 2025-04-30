package kz.webdorphin.jsprit.app.service.impl;

import kz.webdorphin.jsprit.app.data.GeoPoint;
import kz.webdorphin.jsprit.app.data.RouteCostMatrixDto;
import kz.webdorphin.jsprit.app.data.response.GraphHopperRouteResponse;
import kz.webdorphin.jsprit.app.service.GraphHopperService;
import kz.webdorphin.jsprit.app.service.RoutingCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingCostServiceImpl implements RoutingCostService {
    private final GraphHopperService graphHopperService;

    @Override
    public RouteCostMatrixDto getRouteCostMatrix(List<GeoPoint> deliveryPoints) {

        // delivery points sum + 1 warehouse (starting point).
//        int matrixSize = deliveryPoints.size() + 1;

        int matrixSize = deliveryPoints.size();

        double[][] costMatrix = new double[matrixSize][matrixSize];

        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (i == j) {
                    costMatrix[i][j] = 0;
                    continue;
                }

                GeoPoint from = deliveryPoints.get(i);
                GeoPoint to = deliveryPoints.get(j);

                GraphHopperRouteResponse route = graphHopperService.getRoute(from, to);
                costMatrix[i][j] = route.getDistanceMeters();
            }
        }

        return new RouteCostMatrixDto(costMatrix);
    }
}
