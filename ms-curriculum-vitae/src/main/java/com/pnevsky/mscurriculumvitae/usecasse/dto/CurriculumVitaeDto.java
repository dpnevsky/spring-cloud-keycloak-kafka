package com.pnevsky.mscurriculumvitae.usecasse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CurriculumVitaeDto {
    private UUID uuid;
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 100)
    private String surname;
    @NotBlank @Size(max = 50)
    private String countryName;
    @Size(max = 100)
    private String city;
    private Boolean isReadyToRelocate;
    private Boolean isReadyForRemoteWork;
    @Size(max = 50)
    private String status;
}
