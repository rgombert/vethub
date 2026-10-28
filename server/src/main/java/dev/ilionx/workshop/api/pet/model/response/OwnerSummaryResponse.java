package dev.ilionx.workshop.api.pet.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Summary response DTO containing basic owner details for nesting in pet responses.
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "Summary of the owner of a pet")
public class OwnerSummaryResponse {

    @Schema(
        description = "The unique identifier of the owner",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer id;

    @Schema(
        description = "The owner's first name",
        example = "George",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String firstName;

    @Schema(
        description = "The owner's last name",
        example = "Franklin",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String lastName;
}
