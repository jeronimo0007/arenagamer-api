package com.arenagamer.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 30)
    private String phoneNumber;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 500)
    private String instagramUrl;

    @Size(max = 500)
    private String youtubeUrl;

    @Size(max = 500)
    private String twitchUrl;
}
