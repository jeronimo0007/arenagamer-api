package com.arenagamer.api.repository;

import com.arenagamer.api.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    Optional<Contact> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Contact> findByUserid(Integer userid);
    Optional<Contact> findByUseridAndIsPrimary(Integer userid, Integer isPrimary);
}
