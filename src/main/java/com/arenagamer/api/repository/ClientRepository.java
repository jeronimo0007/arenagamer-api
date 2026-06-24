package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Integer> {

    @Query("""
            SELECT c
            FROM Client c
            WHERE c.userId = :id
              AND c.visibility IN :visibilities
              AND c.active = 1
            """)
    Optional<Client> findDiscoverableById(
            @Param("id") Integer id,
            @Param("visibilities") Collection<Visibility> visibilities);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Client c
            WHERE LOWER(c.nickname) = LOWER(:nickname)
            """)
    boolean existsByNicknameIgnoreCase(@Param("nickname") String nickname);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Client c
            WHERE LOWER(c.nickname) = LOWER(:nickname)
              AND c.userId <> :userId
            """)
    boolean existsByNicknameIgnoreCaseAndUserIdNot(
            @Param("nickname") String nickname,
            @Param("userId") Integer userId);

    @Query("""
            SELECT c
            FROM Client c
            WHERE LOWER(c.nickname) = LOWER(:nickname)
            """)
    Optional<Client> findByNicknameIgnoreCase(@Param("nickname") String nickname);
}
