package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Preset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresetRepository extends JpaRepository<Preset, Long> {
    List<Preset> findByActiveTrue();
}
