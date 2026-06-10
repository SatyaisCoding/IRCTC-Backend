package com.irctc.admin.controller;

import com.irctc.admin.dto.ScheduleRequest;
import com.irctc.admin.dto.ScheduleResponse;
import com.irctc.admin.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return success(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSchedules() {
        List<ScheduleResponse> response = scheduleService.getAllSchedules();
        return success(response, HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> success(Object data, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return new ResponseEntity<>(response, status);
    }
}
