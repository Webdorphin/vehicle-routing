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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kz.webdorphin.jsprit.app.data.GeoPoint;
import kz.webdorphin.jsprit.app.data.RouteCostMatrixDto;
import kz.webdorphin.jsprit.app.data.request.RouteRequest;
import kz.webdorphin.jsprit.app.data.response.GraphHopperRouteResponse;
import kz.webdorphin.jsprit.app.data.response.RouteResponse;
import kz.webdorphin.jsprit.app.service.GraphHopperService;
import kz.webdorphin.jsprit.app.service.RoutingCostService;
import kz.webdorphin.jsprit.app.service.RoutingVrpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingVrpServiceImpl implements RoutingVrpService {
    private static final String VRP_VEHICLE_TYPE = "courier-type";
    private static final String VRP_VEHICLE_NAME = "courier-1";

    private final RoutingCostService routingCostService;
    private final GraphHopperService graphHopperService;

    @Override
    public RouteResponse solveGreedy(RouteRequest payload) {
        List<GeoPoint> points = new ArrayList<>(payload.getPoints());
        int n = points.size();
        boolean[] visited = new boolean[n];
        List<Integer> routeIndices = new ArrayList<>();

        int current = 0; // Start at depot
        visited[current] = true;
        routeIndices.add(current);

        // --- Nearest neighbor search using Haversine ---
        for (int step = 1; step < n; step++) {
            double minDistance = Double.MAX_VALUE;
            int next = -1;
            for (int j = 0; j < n; j++) {
                if (!visited[j]) {
                    double distance = haversine(points.get(current), points.get(j));
                    if (distance < minDistance) {
                        minDistance = distance;
                        next = j;
                    }
                }
            }
            if (next != -1) {
                visited[next] = true;
                routeIndices.add(next);
                current = next;
            }
        }

        // Return to depot
        routeIndices.add(0);

        // --- Compute total real distance using GraphHopper ---
        double totalDistance = 0;
        long totalTime = 0;
        for (int i = 0; i < routeIndices.size() - 1; i++) {
            GeoPoint from = points.get(routeIndices.get(i));
            GeoPoint to = points.get(routeIndices.get(i + 1));
            GraphHopperRouteResponse route = graphHopperService.getRoute(from, to);
            totalDistance += route.getDistanceMeters();
            totalTime += route.getTimeMillis();
        }

        List<String> stopSequence = routeIndices.stream()
            .map(i -> points.get(i).getId())
            .collect(Collectors.toList());

        return RouteResponse.builder()
            .optimizedOrder(stopSequence)
            .totalDistanceMeters(totalDistance)
            .totalTimeSeconds((double) totalTime / 1000)
            .build();
    }


    @Override
    public RouteResponse computeDistance(RouteRequest payload) {
        List<GeoPoint> points = payload.getPoints();
        RouteCostMatrixDto matrixDto = routingCostService.getRouteCostMatrix(points);
        double[][] matrix = matrixDto.getMatrix();

        double totalDistance = 0;

        // Map point ID to matrix index
        Map<String, Integer> pointIndexMap = new HashMap<>();
        for (int i = 0; i < points.size(); i++) {
            pointIndexMap.put(points.get(i).getId(), i);
        }

        // Iterate through consecutive points in the given order
        for (int i = 0; i < points.size() - 1; i++) {
            int fromIdx = i;
            int toIdx = i + 1;
            totalDistance += matrix[fromIdx][toIdx];
        }

        // Optionally return to start (for closed loop)
        int lastIdx = points.size() - 1;
        int firstIdx = 0;
        totalDistance += matrix[lastIdx][firstIdx]; // include return leg

        // Extract IDs for response
        List<String> orderedIds = points.stream()
            .map(GeoPoint::getId)
            .collect(Collectors.toList());
        orderedIds.add(points.getFirst().getId()); // append return leg

        return RouteResponse.builder()
            .optimizedOrder(orderedIds)
            .totalDistanceMeters(totalDistance)
            .totalTimeSeconds(0) // optional: add time computation if needed
            .build();
    }

    @Override
    public RouteResponse solveVRP(RouteRequest payload) {
        long t0 = System.nanoTime();                 // ──► start overall timer

        long t1 = System.nanoTime();
        /* ---------- 1. build cost matrix ---------- */
        List<GeoPoint> points = payload.getPoints();
        RouteCostMatrixDto matrixDto = routingCostService.getRouteCostMatrix(points);

        VehicleRoutingTransportCostsMatrix.Builder costBuilder =
            buildCostMatrix(points, matrixDto);

        long elapsedMs1 = (System.nanoTime() - t1) / 1_000_000;   // ──► stop timer
        log.info("Cost matrix built in {} ms", elapsedMs1);

        /* ---------- 2. create VRP model ---------- */
        long t2 = System.nanoTime();
        VehicleRoutingProblem problem = buildProblem(payload, costBuilder);

        /* ---------- 3. run optimisation ---------- */
        VehicleRoutingAlgorithm algo = Jsprit.createAlgorithm(problem);
        algo.setMaxIterations(200);
        Collection<VehicleRoutingProblemSolution> solutions = algo.searchSolutions();
        VehicleRoutingProblemSolution best = Solutions.bestOf(solutions);

        long elapsedMs2 = (System.nanoTime() - t2) / 1_000_000;   // ──► stop timer
        log.info("VRP solved for {} points in {} ms", points.size(), elapsedMs2);


        /* ---------- 4. extract stop sequence ----- */
        List<String> stopSequence = new ArrayList<>();
        for (VehicleRoute route : best.getRoutes()) {
            stopSequence.add(route.getStart().getLocation().getId());
            for (TourActivity act : route.getActivities()) {
                stopSequence.add(act.getLocation().getId());
            }
            stopSequence.add(route.getEnd().getLocation().getId());
        }

        double routeDistance = 0;
        double[][] matrix = matrixDto.getMatrix();

        // Map point ID to matrix index
        Map<String, Integer> pointIndexMap = new HashMap<>();
        for (int i = 0; i < points.size(); i++) {
            pointIndexMap.put(points.get(i).getId(), i);
        }

        for (int i = 0; i < stopSequence.size() - 1; i++) {
            String fromId = stopSequence.get(i);
            String toId = stopSequence.get(i + 1);
            int fromIdx = pointIndexMap.get(fromId);
            int toIdx = pointIndexMap.get(toId);
            routeDistance += matrix[fromIdx][toIdx];
        }

        long elapsedMs0 = (System.nanoTime() - t0) / 1_000_000;   // ──► stop timer
        log.info("Overall time elapsed for {} points in {} ms", points.size(), elapsedMs0);

        return RouteResponse.builder()
            .optimizedOrder(stopSequence)
            .totalDistanceMeters(routeDistance)   // TODO distance aggregation
            .totalTimeSeconds(0)      // TODO time aggregation
            .build();
    }

    private double haversine(GeoPoint p1, GeoPoint p2) {
        final int R = 6371000; // Earth radius in meters
        double lat1 = Math.toRadians(p1.getLat());
        double lon1 = Math.toRadians(p1.getLon());
        double lat2 = Math.toRadians(p2.getLat());
        double lon2 = Math.toRadians(p2.getLon());
        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
            Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /* ---------------------------------------------------------------------- */

    private VehicleRoutingTransportCostsMatrix.Builder buildCostMatrix(
        List<GeoPoint> points, RouteCostMatrixDto matrixDto) {

        VehicleRoutingTransportCostsMatrix.Builder costBuilder =
            VehicleRoutingTransportCostsMatrix.Builder.newInstance(false);

        double[][] dist = matrixDto.getMatrix();
        for (int i = 0; i < points.size(); i++) {
            for (int j = 0; j < points.size(); j++) {
                if (i != j) {
                    costBuilder.addTransportDistance(points.get(i).getId(),
                        points.get(j).getId(),
                        dist[i][j]);
                    costBuilder.addTransportTime(points.get(i).getId(),
                        points.get(j).getId(),
                        dist[i][j]);   // same metric
                }
            }
        }
        return costBuilder;
    }

    private VehicleRoutingProblem buildProblem(
        RouteRequest payload,
        VehicleRoutingTransportCostsMatrix.Builder costBuilder) {

        VehicleRoutingProblem.Builder pb = VehicleRoutingProblem.Builder.newInstance();

        VehicleType type = VehicleTypeImpl.Builder.newInstance(VRP_VEHICLE_TYPE)
            .addCapacityDimension(0, 100)
            .build();

        GeoPoint depot = payload.getPoints().getFirst();             // warehouse first
        List<GeoPoint> customers = payload.getPoints().stream().skip(1).toList();

        VehicleImpl vehicle = VehicleImpl.Builder.newInstance(VRP_VEHICLE_NAME)
            .setStartLocation(Location.newInstance(depot.getId()))
            .setType(type)
            .build();
        pb.addVehicle(vehicle);

        pb.addAllJobs(customers.stream()
            .map(this::asDelivery).collect(Collectors.toList()));

        pb.setRoutingCost(costBuilder.build());
        return pb.build();
    }

    private com.graphhopper.jsprit.core.problem.job.Service asDelivery(GeoPoint p) {
        return com.graphhopper.jsprit.core.problem.job.Service.Builder
            .newInstance(p.getId())
            .setLocation(Location.newInstance(p.getId()))
            .build();
    }
}
