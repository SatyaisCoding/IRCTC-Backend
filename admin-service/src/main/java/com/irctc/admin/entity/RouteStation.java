package com.irctc.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "route_stations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"route_id", "sequence_number"}),
        @UniqueConstraint(columnNames = {"route_id", "station_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonIgnore
    private Route route;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "arrival_time", nullable = false)
    private String arrivalTime;

    @Column(name = "departure_time", nullable = false)
    private String departureTime;

    @Column(name = "distance_from_origin", nullable = false)
    private Double distanceFromOrigin;
}
