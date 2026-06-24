package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Preset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PresetRepository extends JpaRepository<Preset, Long> {
    List<Preset> findByActiveTrueOrderByGameNameAsc();

    Optional<Preset> findByIdAndActiveTrue(Long id);

    @Query("""
            SELECT p
            FROM Preset p
            WHERE (:activeOnly = false OR p.active = true)
              AND (
                LOWER(p.gameName) LIKE LOWER(CONCAT(:query, '%'))
                OR LOWER(COALESCE(p.platform, '')) LIKE LOWER(CONCAT(:query, '%'))
              )
            ORDER BY p.gameName ASC
            """)
    List<Preset> searchByText(@Param("query") String query, @Param("activeOnly") boolean activeOnly);
}
