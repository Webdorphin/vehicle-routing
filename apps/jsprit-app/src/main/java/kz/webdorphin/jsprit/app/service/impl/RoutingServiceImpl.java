package kz.webdorphin.jsprit.app.service.impl;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import kz.webdorphin.jsprit.app.data.GeoPoint;
import kz.webdorphin.jsprit.app.data.request.RouteRequest;
import kz.webdorphin.jsprit.app.data.response.RouteResponse;
import kz.webdorphin.jsprit.app.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutingServiceImpl implements RoutingService {
    private static final String VRP_VEHICLE_TYPE = "courier-type";
    private static final String VRP_VEHICLE_NAME = "courier-1";

    @Override
    public RouteResponse solveVRP(RouteRequest payload) {
        // 1. Build cost matrix
        VehicleRoutingTransportCostsMatrix.Builder costBuilder = buildCostMatrix(payload);

        // 2. Build VRP Problem
        VehicleRoutingProblem problem = getVehicleRoutingProblem(payload, costBuilder);

        // 3. Solve
        VehicleRoutingAlgorithm algo = Jsprit.createAlgorithm(problem);
        Collection<VehicleRoutingProblemSolution> solutions = algo.searchSolutions();
        VehicleRoutingProblemSolution best = Solutions.bestOf(solutions);

        // 4. Extract solution
        List<String> stopSequence = new ArrayList<>();
        for (VehicleRoute route : best.getRoutes()) {
            stopSequence.add(route.getStart().getLocation().getId());
            for (TourActivity act : route.getActivities()) {
                stopSequence.add(act.getLocation().getId());
            }
            stopSequence.add(route.getEnd().getLocation().getId());
        }

        return RouteResponse.builder()
            .optimizedOrder(stopSequence)
            .totalDistanceMeters(0) // ??? calculate later
            .totalTimeSeconds(0) // ??? calculate later
            .build();
    }

    private VehicleRoutingTransportCostsMatrix.Builder buildCostMatrix(RouteRequest payload) {
        VehicleRoutingTransportCostsMatrix.Builder costBuilder =
            VehicleRoutingTransportCostsMatrix.Builder.newInstance(false);

        List<String> pointIds = new ArrayList<>();
        for (GeoPoint point : payload.getPoints()) {
            pointIds.add(point.getId());
        }

        for (int i = 0; i < pointIds.size(); i++) {
            for (int j = 0; j < pointIds.size(); j++) {
                if (i != j) {
                    costBuilder.addTransportDistance(pointIds.get(i), pointIds.get(j), payload.getDistances()[i][j]);
                    costBuilder.addTransportTime(pointIds.get(i), pointIds.get(j), payload.getTimes()[i][j]);
                }
            }
        }

        return costBuilder;
    }

    private VehicleRoutingProblem getVehicleRoutingProblem(RouteRequest payload, VehicleRoutingTransportCostsMatrix.Builder costBuilder) {
        VehicleRoutingProblem.Builder problemBuilder = VehicleRoutingProblem.Builder.newInstance();

        VehicleType type = VehicleTypeImpl.Builder.newInstance(VRP_VEHICLE_TYPE)
            .addCapacityDimension(0, 100)
            .build();

        // Assume first point is warehouse
        GeoPoint warehouse = payload.getPoints().get(0);
        List<GeoPoint> deliveryPoints = payload.getPoints().stream()
            .skip(1)
            .toList();

        VehicleImpl vehicle = VehicleImpl.Builder.newInstance(VRP_VEHICLE_NAME)
            .setStartLocation(Location.newInstance(warehouse.getLon(), warehouse.getLat()))
            .setType(type)
            .build();

        problemBuilder.addVehicle(vehicle);

        var deliveryJobs = buildDeliveryJobs(deliveryPoints);
        problemBuilder.addAllJobs(deliveryJobs);

        problemBuilder.setRoutingCost(costBuilder.build());
        return problemBuilder.build();
    }

    private List<com.graphhopper.jsprit.core.problem.job.Service> buildDeliveryJobs(List<GeoPoint> points) {
        return points.stream()
            .map(this::buildDeliveryJob)
            .collect(Collectors.toList());
    }

    private com.graphhopper.jsprit.core.problem.job.Service buildDeliveryJob(GeoPoint point) {
        return com.graphhopper.jsprit.core.problem.job.Service.Builder
            .newInstance(point.getId())
            .setLocation(Location.newInstance(point.getLat(), point.getLon()))
            .build();
    }
}
