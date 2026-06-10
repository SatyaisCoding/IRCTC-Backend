package com.irctc.admin.service;

import com.irctc.admin.dto.ScheduleRequest;
import com.irctc.admin.dto.ScheduleResponse;

import java.util.List;

public interface ScheduleService {
    ScheduleResponse createSchedule(ScheduleRequest request);
    List<ScheduleResponse> getAllSchedules();
}
