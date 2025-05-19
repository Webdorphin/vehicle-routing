package kz.webdorphin.jsprit.app.controller;

import kz.webdorphin.jsprit.app.data.request.RouteRequest;
import kz.webdorphin.jsprit.app.data.response.RouteResponse;
import kz.webdorphin.jsprit.app.service.RoutingVrpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicle-routing")
@RequiredArgsConstructor
public class VrpController {
    private final RoutingVrpService routingVrpService;

    @PostMapping
    public RouteResponse solveVrp(@RequestBody RouteRequest request) {
        return routingVrpService.solveVRP(request);
    }

    @PostMapping("/greedy")
    public RouteResponse solveGreedy(@RequestBody RouteRequest request) {
        return routingVrpService.solveGreedy(request);
    }

    @PostMapping("/distance")
    public RouteResponse computeDistance(@RequestBody RouteRequest request) {
        return routingVrpService.computeDistance(request);
    }
}
