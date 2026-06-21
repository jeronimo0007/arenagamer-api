package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblstaff")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @Column(name = "staffid")
    private Integer staffId;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String firstname;

    @Column(nullable = false, length = 50)
    private String lastname;

    @Column(length = 30)
    private String phonenumber;

    @Column(nullable = false, length = 250)
    private String password;

    @Column(name = "datecreated", nullable = false)
    private LocalDateTime datecreated;

    @Column(name = "profile_image", length = 191)
    private String profileImage;

    @Column(nullable = false)
    @Builder.Default
    private Integer admin = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer active = 1;
}
