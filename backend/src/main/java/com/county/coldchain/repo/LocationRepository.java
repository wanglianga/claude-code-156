package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByZoneId(Long zoneId);
}
