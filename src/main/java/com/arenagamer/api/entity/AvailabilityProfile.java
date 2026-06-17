package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.TimeWindow;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "availability_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ElementCollection(targetClass = TimeWindow.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "availability_time_windows",
        joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "time_window")
    @Builder.Default
    private Set<TimeWindow> windows = new HashSet<>();

    @OneToMany(mappedBy = "availabilityProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PreciseSlot> preciseSlots = new ArrayList<>();

    @Column(name = "prefer_weekends", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean preferWeekends = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
