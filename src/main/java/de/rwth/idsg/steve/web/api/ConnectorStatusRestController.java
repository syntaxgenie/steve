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

import de.rwth.idsg.steve.service.ChargePointService;
import de.rwth.idsg.steve.web.api.ApiControllerAdvice.ApiErrorResponse;
import de.rwth.idsg.steve.web.dto.ConnectorStatusForm;
import de.rwth.idsg.steve.web.dto.ConnectorStatusOverview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author SteVe Community Team
 * @since 09.06.2026
 */
@Tag(
        name = "connector-status-controller",
        description = """
        Operations related to querying connector status.
        A connector represents a physical charging socket on a charge box.
        The jsonAndDisconnected field indicates whether a WebSocket station
        is currently connected to the OCPP server — use this for online/offline detection.
        """
)
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/connectorStatus", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ConnectorStatusRestController {

    private final ChargePointService chargePointService;

    @Operation(description = """
        Returns the latest connector status for all connectors, with optional filters.
        Each entry represents the most recent StatusNotification received from a connector.
        The jsonAndDisconnected field is true when the charge box is a WebSocket station
        that is currently not connected to the OCPP server.
        Use chargeBoxId to filter by a specific station, and status to filter by connector state.
        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))})}
    )
    @GetMapping
    public List<ConnectorStatusOverview> get(@ParameterObject ConnectorStatusForm params) {
        log.debug("Read connector status request for query: {}", params);

        var response = ConnectorStatusOverview.fromList(
                chargePointService.getChargePointConnectorStatus(params));

        log.debug("Read connector status response count: {}", response.size());
        return response;
    }
}