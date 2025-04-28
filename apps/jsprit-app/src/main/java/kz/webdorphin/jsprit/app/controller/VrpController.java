package kz.webdorphin.jsprit.app.controller;

import kz.webdorphin.jsprit.app.data.request.RouteRequest;
import kz.webdorphin.jsprit.app.data.response.RouteResponse;
import kz.webdorphin.jsprit.app.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
public class VrpController {
    private final RoutingService routingService;

    @GetMapping("/vrp")
    public RouteResponse solveVrp(@RequestBody RouteRequest request) {
        return routingService.solveVRP(request);
    }
}
