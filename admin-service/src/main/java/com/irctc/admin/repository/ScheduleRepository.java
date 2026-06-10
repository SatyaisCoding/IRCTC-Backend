package com.irctc.admin.repository;

import com.irctc.admin.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    boolean existsByTrainIdAndDepartureDate(Long trainId, LocalDate departureDate);
}
