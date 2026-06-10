package com.irctc.admin.controller;

import com.irctc.admin.dto.RouteRequest;
import com.irctc.admin.dto.RouteResponse;
import com.irctc.admin.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoute(@Valid @RequestBody RouteRequest request) {
        RouteResponse response = routeService.createRoute(request);
        return success(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRouteById(@PathVariable Long id) {
        RouteResponse response = routeService.getRouteById(id);
        return success(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRoutes() {
        List<RouteResponse> response = routeService.getAllRoutes();
        return success(response, HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> success(Object data, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return new ResponseEntity<>(response, status);
    }
}
