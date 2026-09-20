package com.example.classfit.course.repository;

import com.example.classfit.course.domain.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Optional<Facility> findByBusinessRegistrationNumberAndFacilitySerialNumber(
            String businessRegistrationNumber,
            String facilitySerialNumber
    );
}
