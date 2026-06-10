package com.irctc.admin.service;

import com.irctc.admin.dto.RouteRequest;
import com.irctc.admin.dto.RouteResponse;

import java.util.List;

public interface RouteService {
    RouteResponse createRoute(RouteRequest request);
    RouteResponse getRouteById(Long id);
    List<RouteResponse> getAllRoutes();
}
