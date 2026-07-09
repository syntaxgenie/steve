/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2026 SteVe Community Team
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.web.api;

import de.rwth.idsg.steve.repository.ChargingProfileRepository;
import de.rwth.idsg.steve.web.api.ApiControllerAdvice.ApiErrorResponse;
import de.rwth.idsg.steve.web.dto.ChargingProfileCreateRequest;
import de.rwth.idsg.steve.web.dto.ChargingProfileForm;
import de.rwth.idsg.steve.web.dto.ChargingProfileOverview;
import de.rwth.idsg.steve.web.dto.ChargingProfileQueryForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for managing OCPP charging profiles.
 * Charging profiles define power limits per time period and are applied
 * to charge point connectors via SetChargingProfile.
 * Typical flow for platform-managed charging:
 * 1. POST /api/v1/chargingProfiles — create profile with desired power limit
 * 2. POST /api/v1/operations/SetChargingProfile — apply to connector
 * 3. DELETE /api/v1/chargingProfiles/{pk} — clean up after session ends
 */
@Tag(
        name = "charging-profile-controller",
        description = """
        Operations for managing OCPP charging profiles.
        Charging profiles control the power limit applied to a connector during a transaction.
        Use TX_PROFILE with RELATIVE kind for per-session power limits.
        The platform uses this to implement Smart Charge and Green Mode scheduling.
        """
)
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/chargingProfiles", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChargingProfileRestController {

    private final ChargingProfileRepository chargingProfileRepository;

    @Operation(description = """
        Returns all charging profiles. Optionally filter by description.
        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))})
    })
    @GetMapping
    public List<ChargingProfileOverview> getAll() {
        log.debug("GET /api/v1/chargingProfiles");
        return ChargingProfileOverview.fromList(
                chargingProfileRepository.getOverview(new ChargingProfileQueryForm()));
    }

    @Operation(description = """
        Creates a new charging profile and returns it with the generated chargingProfilePk.
        The pk is required for SetChargingProfile operations.
        For per-transaction power limits use: purpose=TX_PROFILE, kind=RELATIVE, stackLevel=0.
        Schedule periods define power limits in W or A (set chargingRateUnit accordingly).
        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad Request — validation failed",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))})
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChargingProfileOverview create(@Valid @RequestBody ChargingProfileCreateRequest request) {
        log.debug("POST /api/v1/chargingProfiles: {}", request);

        ChargingProfileForm form = request.toForm();
        int pk = chargingProfileRepository.add(form);

        log.info("Charging profile created with pk={}", pk);

        return new ChargingProfileOverview(
                pk,
                form.getDescription(),
                form.getStackLevel(),
                form.getChargingProfilePurpose() != null
                        ? form.getChargingProfilePurpose().value() : null,
                form.getChargingProfileKind() != null
                        ? form.getChargingProfileKind().value() : null,
                form.getRecurrencyKind() != null
                        ? form.getRecurrencyKind().value() : null,
                form.getChargingRateUnit() != null
                        ? form.getChargingRateUnit().value() : null,
                form.getMinChargingRate(),
                form.getSchedulePeriods() != null
                        ? form.getSchedulePeriods().stream()
                        .map(p -> new ChargingProfileOverview.SchedulePeriodDTO(
                                p.getStartPeriodInSeconds(),
                                p.getPowerLimit(),
                                p.getNumberPhases()))
                        .toList()
                        : null
        );
    }

    @Operation(description = "Deletes a charging profile by its primary key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))})
    })
    @DeleteMapping("/{chargingProfilePk}")
    public void delete(@PathVariable int chargingProfilePk) {
        log.debug("DELETE /api/v1/chargingProfiles/{}", chargingProfilePk);
        chargingProfileRepository.delete(chargingProfilePk);
        log.info("Charging profile deleted: pk={}", chargingProfilePk);
    }
}