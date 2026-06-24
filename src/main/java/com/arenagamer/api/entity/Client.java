package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.Visibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblclients", indexes = {
    @Index(name = "idx_clients_visibility", columnList = "visibility")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userid")
    private Integer userId;

    @Column(length = 191)
    private String company;

    @Column(length = 50)
    private String nickname;

    @Column(length = 30)
    private String phonenumber;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 191)
    private String address;

    @Column(nullable = false)
    @Builder.Default
    private Integer country = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer active = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PUBLIC','PRIVATE','PROTECTED')")
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "datecreated", nullable = false)
    private LocalDateTime datecreated;

    @Column(name = "default_currency", nullable = false)
    @Builder.Default
    private Integer defaultCurrency = 0;

    @Column(name = "show_primary_contact", nullable = false)
    @Builder.Default
    private Integer showPrimaryContact = 0;

    @Column(name = "registration_confirmed", nullable = false)
    @Builder.Default
    private Integer registrationConfirmed = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer addedfrom = 0;
}
