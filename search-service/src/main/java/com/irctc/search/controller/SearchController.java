package com.irctc.search.controller;

import com.irctc.search.dto.SearchRequest;
import com.irctc.search.dto.TrainResponse;
import com.irctc.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<List<TrainResponse>> searchTrains(@Valid @RequestBody SearchRequest request) {
        List<TrainResponse> responses = searchService.searchTrains(request);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/train/{trainNumber}")
    public ResponseEntity<TrainResponse> getTrainByNumber(@PathVariable String trainNumber) {
        TrainResponse response = searchService.getTrainByNumber(trainNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/train")
    public ResponseEntity<TrainResponse> addTrain(@Valid @RequestBody TrainResponse request) {
        TrainResponse response = searchService.addTrain(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
