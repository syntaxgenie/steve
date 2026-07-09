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

import de.rwth.idsg.steve.ocpp.OcppVersion;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.service.ChargePointService;
import de.rwth.idsg.steve.web.api.ApiControllerAdvice.ApiErrorResponse;
import de.rwth.idsg.steve.web.dto.ChargeBoxCreateRequest;
import de.rwth.idsg.steve.web.dto.ChargeBoxOverview;
import de.rwth.idsg.steve.web.dto.ChargeBoxUpdateRequest;
import de.rwth.idsg.steve.web.dto.ChargePointFormForCreate;
import de.rwth.idsg.steve.web.dto.ChargePointFormForUpdate;
import de.rwth.idsg.steve.web.dto.ChargePointQueryForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST API for charge box (charge point / charging station) management.
 * <p>
 * Exposes the same operations available through the Thymeleaf web UI
 * ({@link de.rwth.idsg.steve.web.controller.ChargePointsController}) as a
 * JSON REST API, following the conventions of the existing
 * OcppTagRestController and TransactionRestController.
 * <p>
 * Endpoints:
 * GET    /api/v1/chargeBoxes           — list with optional filters
 * GET    /api/v1/chargeBoxes/{pk}      — single charge box (full detail)
 * POST   /api/v1/chargeBoxes           — register a new charge box
 * PUT    /api/v1/chargeBoxes/{pk}      — update metadata (partial)
 * DELETE /api/v1/chargeBoxes/{pk}      — delete
 * <p>
 * Authentication: HTTP Basic Auth using username + api_password from the
 * web_user table (same as all other /api/v1/* endpoints).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/chargeBoxes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "charge-box-controller",
        description = "Operations related to managing charge boxes (charging stations). " +
                      "A charge box represents a physical charging station. Its chargeBoxId " +
                      "must match the last path segment of the WebSocket URL the station connects on: " +
                      "ws://<host>/steve/websocket/CentralSystemService/{chargeBoxId}"
)
public class ChargeBoxRestController {
    private final ChargePointService chargePointService;

    @Operation(
            summary = "List charge boxes",
            description = "Returns charge boxes matching the given filters. All parameters are optional. " +
                          "The response uses ChargePoint.Overview which contains only the columns from " +
                          "the overview query — address and location are not included. " +
                          "Use GET /api/v1/chargeBoxes/{chargeBoxPk} for the full record."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChargeBoxOverview.class)))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public List<ChargeBoxOverview> getChargeBoxes(

            @Parameter(description = "Filter by OCPP charge box identifier (exact match)")
            @RequestParam(required = false) String chargeBoxId,

            @Parameter(description = "Filter by OCPP protocol version prefix, e.g. OCPP1.6")
            @RequestParam(required = false) String ocppVersion,

            @Parameter(description = "Filter by description substring")
            @RequestParam(required = false) String description,

            @Parameter(description = "Filter by heartbeat recency: TODAY, YESTERDAY, EARLIER, or ALL (default)")
            @RequestParam(required = false) ChargePointQueryForm.QueryPeriodType heartbeatPeriod
    ) {
        ChargePointQueryForm params = new ChargePointQueryForm();

        if (chargeBoxId != null) params.setChargeBoxId(chargeBoxId);
        if (description != null) params.setDescription(description);
        if (ocppVersion != null) params.setOcppVersion(OcppVersion.fromValue(ocppVersion));
        if (heartbeatPeriod != null) params.setHeartbeatPeriod(heartbeatPeriod);

        return ChargeBoxOverview.toOverviewList(chargePointService.getOverview(params));
    }

    @Operation(
            summary = "Get a single charge box",
            description = "Returns the full details of a charge box including note, location coordinates, " +
                          "address, and EVSE/connector topology. Uses ChargePoint.Details internally."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = ChargeBoxOverview.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{chargeBoxPk}")
    public ChargeBoxOverview getChargeBox(
            @Parameter(description = "Database primary key of the charge box", required = true)
            @PathVariable int chargeBoxPk
    ) {
        ChargePoint.Details details = chargePointService.getDetails(chargeBoxPk);
        return ChargeBoxOverview.toOverviewFromDetails(details);
    }

    @Operation(
            summary = "Register a new charge box",
            description = "Creates a new charge box record in SteVe. The chargeBoxId must match the " +
                          "identifier the physical station uses in the last segment of its WebSocket URL. " +
                          "If the station has already connected and appears in the 'unknown' list, " +
                          "this call also removes it from that list (same behaviour as the web UI)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(schema = @Schema(implementation = ChargeBoxOverview.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request — validation failed or chargeBoxId already exists",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChargeBoxOverview createChargeBox(@Valid @RequestBody ChargeBoxCreateRequest request) {
        ChargePointFormForCreate form = ChargeBoxCreateRequest.toCreateForm(request);
        chargePointService.addChargePoint(form);

        chargePointService.removeUnknown(Collections.singletonList(request.chargeBoxId()));

        return fetchDetailsByChargeBoxId(request.chargeBoxId());
    }

    @Operation(
            summary = "Update an existing charge box",
            description = "Updates the metadata of a charge box. This is a partial update — " +
                          "fields that are null in the request body keep their current value in the database. " +
                          "The chargeBoxId cannot be changed (it is the WebSocket identity of the station)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK — returns the updated charge box",
                    content = @Content(schema = @Schema(implementation = ChargeBoxOverview.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping(value = "/{chargeBoxPk}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChargeBoxOverview updateChargeBox(
            @Parameter(description = "Database primary key of the charge box", required = true)
            @PathVariable int chargeBoxPk,
            @RequestBody ChargeBoxUpdateRequest request
    ) {
        ChargePoint.Details existing = chargePointService.getDetails(chargeBoxPk);
        ChargePointFormForUpdate form = ChargeBoxUpdateRequest.toUpdateForm(chargeBoxPk, existing, request);
        chargePointService.updateChargePoint(form);

        return ChargeBoxOverview.toOverviewFromDetails(chargePointService.getDetails(chargeBoxPk));
    }

    @Operation(
            summary = "Delete a charge box",
            description = "Permanently removes the charge box and its associated EVSE and connector records. " +
                          "Stop all active transactions on this charge box before deleting to avoid orphaned records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK — returns the deleted charge box",
                    content = @Content(schema = @Schema(implementation = ChargeBoxOverview.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{chargeBoxPk}")
    public ChargeBoxOverview deleteChargeBox(
            @Parameter(description = "Database primary key of the charge box", required = true)
            @PathVariable int chargeBoxPk
    ) {
        ChargePoint.Details details = chargePointService.getDetails(chargeBoxPk);
        ChargeBoxOverview snapshot = ChargeBoxOverview.toOverviewFromDetails(details);

        chargePointService.deleteChargePoint(chargeBoxPk);

        return snapshot;
    }

    /**
     * After a successful insert, fetches the newly created record by chargeBoxId
     * so we can return the generated PK and timestamps in the 201 response.
     * Falls back to a minimal response if the lookup unexpectedly finds nothing.
     */
    private ChargeBoxOverview fetchDetailsByChargeBoxId(String chargeBoxId) {
        ChargePointQueryForm q = new ChargePointQueryForm();
        q.setChargeBoxId(chargeBoxId);
        List<ChargePoint.Overview> results = chargePointService.getOverview(q);

        if (results.isEmpty()) {
            log.warn("Could not find newly created charge box with chargeBoxId={}", chargeBoxId);
            return new ChargeBoxOverview(0, chargeBoxId, null, null, null,
                    null, null, null, null, null, null, null, null);
        }

        return ChargeBoxOverview.toOverviewFromDetails(
                chargePointService.getDetails(results.getFirst().getChargeBoxPk()));
    }
}