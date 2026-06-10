package com.irctc.admin.controller;

import com.irctc.admin.dto.StationRequest;
import com.irctc.admin.dto.StationResponse;
import com.irctc.admin.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stations/station")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createStation(@Valid @RequestBody StationRequest request) {
        StationResponse response = stationService.createStation(request);
        return success(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllStations() {
        List<StationResponse> response = stationService.getAllStations();
        return success(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStationById(@PathVariable Long id) {
        StationResponse response = stationService.getStationById(id);
        return success(response, HttpStatus.OK);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Map<String, Object>> getStationByCode(@PathVariable String code) {
        StationResponse response = stationService.getStationByCode(code);
        return success(response, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateStation(
            @PathVariable Long id, 
            @Valid @RequestBody StationRequest request) {
        StationResponse response = stationService.updateStation(id, request);
        return success(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Station deleted successfully");
        return success(body, HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> success(Object data, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return new ResponseEntity<>(response, status);
    }
}
