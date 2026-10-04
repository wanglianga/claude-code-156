package com.county.coldchain.repo;

import com.county.coldchain.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
}
