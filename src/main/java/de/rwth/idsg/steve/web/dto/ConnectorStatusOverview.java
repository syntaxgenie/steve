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
package de.rwth.idsg.steve.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.rwth.idsg.steve.repository.dto.ConnectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Read-only view of a connector's current status, returned by the REST API.
 * jsonAndDisconnected is true when the charge box is a WebSocket/JSON station
 * that is currently not connected to the OCPP server. This is the primary
 * field for determining online/offline state on the platform side.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConnectorStatusOverview(

        @Schema(description = "Database primary key of the charge box")
        int chargeBoxPk,

        @Schema(description = "The OCPP charge box identifier")
        String chargeBoxId,

        @Schema(description = "Connector ID within the charge box")
        int connectorId,

        @Schema(description = "Last status reported by the station, e.g. Available, Charging, Faulted")
        String status,

        @Schema(description = "Last error code reported by the station")
        String errorCode,

        @Schema(description = "Timestamp of the last status notification, human-readable")
        String timestamp,

        @Schema(description = "OCPP protocol version, e.g. OCPP1.6J")
        String ocppProtocol,

        @Schema(description = """
        True when the charge box is a WebSocket/JSON station that is currently
        disconnected from the OCPP server. Use this field to determine online/offline state.
        Null for SOAP stations (always considered connected if registered).
        """)
        Boolean jsonAndDisconnected
) {
    public static ConnectorStatusOverview from(final ConnectorStatus cs) {
        return new ConnectorStatusOverview(
                cs.getChargeBoxPk(),
                cs.getChargeBoxId(),
                cs.getConnectorId(),
                cs.getStatus(),
                cs.getErrorCode(),
                cs.getTimeStamp(),
                cs.getOcppProtocol() != null ? cs.getOcppProtocol().getCompositeValue() : null,
                cs.isJsonAndDisconnected()
        );
    }

    public static List<ConnectorStatusOverview> fromList(final List<ConnectorStatus> list) {
        return list.stream()
                .map(ConnectorStatusOverview::from)
                .toList();
    }
}