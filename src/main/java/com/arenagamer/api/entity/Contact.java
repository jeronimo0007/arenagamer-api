package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblcontacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", referencedColumnName = "userid", insertable = false, updatable = false)
    private Client client;

    @Column(nullable = false, length = 191)
    private String firstname;

    @Column(nullable = false, length = 191)
    private String lastname;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String phonenumber = "";

    @Column(length = 255)
    private String password;

    @Column(name = "datecreated", nullable = false)
    private LocalDateTime datecreated;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "profile_image", length = 191)
    private String profileImage;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "twitch_url", length = 500)
    private String twitchUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Integer isPrimary = 1;

    @Column(name = "wallet_view_allowed", nullable = false)
    @Builder.Default
    private Boolean walletViewAllowed = false;

    @Column(name = "wallet_use_allowed", nullable = false)
    @Builder.Default
    private Boolean walletUseAllowed = false;

    @Column(name = "invoice_emails", nullable = false)
    @Builder.Default
    private Boolean invoiceEmails = true;

    @Column(name = "estimate_emails", nullable = false)
    @Builder.Default
    private Boolean estimateEmails = true;

    @Column(name = "credit_note_emails", nullable = false)
    @Builder.Default
    private Boolean creditNoteEmails = true;

    @Column(name = "contract_emails", nullable = false)
    @Builder.Default
    private Boolean contractEmails = true;

    @Column(name = "task_emails", nullable = false)
    @Builder.Default
    private Boolean taskEmails = true;

    @Column(name = "project_emails", nullable = false)
    @Builder.Default
    private Boolean projectEmails = true;

    @Column(name = "ticket_emails", nullable = false)
    @Builder.Default
    private Boolean ticketEmails = true;
}
