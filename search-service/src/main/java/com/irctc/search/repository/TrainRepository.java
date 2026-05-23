package com.irctc.search.repository;

import com.irctc.search.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    List<Train> findByStationFromAndStationTo(String stationFrom, String stationTo);
    Optional<Train> findByTrainNumber(String trainNumber);
}
