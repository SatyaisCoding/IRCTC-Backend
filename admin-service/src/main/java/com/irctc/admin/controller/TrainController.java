package com.irctc.admin.controller;

import com.irctc.admin.dto.TrainRequest;
import com.irctc.admin.dto.TrainResponse;
import com.irctc.admin.exception.BadRequestException;
import com.irctc.admin.service.TrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/trains/train")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTrain(@Valid @RequestBody TrainRequest request) {
        TrainResponse response = trainService.createTrain(request);
        return success(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTrains() {
        List<TrainResponse> response = trainService.getAllTrains();
        return success(response, HttpStatus.OK);
    }

    @PatchMapping("/{trainId}/route")
    public ResponseEntity<Map<String, Object>> assignRouteToTrain(
            @PathVariable Long trainId,
            @RequestBody Map<String, Long> requestBody) {
        
        Long routeId = requestBody.get("routeId");
        if (routeId == null) {
            throw new BadRequestException("routeId parameter is required in request body");
        }

        TrainResponse response = trainService.assignRoute(trainId, routeId);
        return success(response, HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> success(Object data, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return new ResponseEntity<>(response, status);
    }
}
