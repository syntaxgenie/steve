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
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import io.swagger.v3.oas.annotations.media.Schema;
import jooq.steve.db.tables.records.ChargeBoxRecord;
import jooq.steve.db.tables.records.AddressRecord;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only view of a charge box returned by the REST API.
 * The list endpoint (GET /api/v1/chargeBoxes) populates only the fields
 * available from ChargePoint.Overview (chargeBoxPk, chargeBoxId, description,
 * ocppProtocol, lastHeartbeatTimestamp). Address and location fields are null
 * in list responses — use GET /api/v1/chargeBoxes/{chargeBoxPk} for the full record.
 * The single-record endpoints (GET /{pk}, POST, PUT, DELETE) populate all fields
 * from ChargePoint.Details, which includes the joined AddressRecord.
 * Address fields are null if no address was registered for the charge box.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChargeBoxOverview(

        @Schema(description = "Database primary key of the charge box")
        int chargeBoxPk,

        @Schema(description = "The OCPP charge box identifier — matches the last segment of the station's WebSocket URL")
        String chargeBoxId,

        @Schema(description = "Human-readable description")
        String description,

        @Schema(description = "OCPP protocol version last seen, e.g. OCPP1.6J")
        String ocppProtocol,

        @Schema(description = "Last heartbeat timestamp, human-readable (e.g. '5 minutes ago')")
        String lastHeartbeatTimestamp,

        @Schema(description = "Operator or admin note")
        String note,

        @Schema(description = "Street name")
        String street,

        @Schema(description = "House number")
        String houseNumber,

        @Schema(description = "ZIP / postal code")
        String zipCode,

        @Schema(description = "City")
        String city,

        @Schema(description = "Country (ISO 3166-1 alpha-2)", example = "DE")
        String country,

        @Schema(description = "Latitude of the physical location")
        String latitude,

        @Schema(description = "Longitude of the physical location")
        String longitude
) {
    public static ChargeBoxOverview from(ChargePoint.Overview cp) {
        return new ChargeBoxOverview(
                cp.getChargeBoxPk(),
                cp.getChargeBoxId(),
                cp.getDescription(),
                cp.getOcppProtocol(),
                cp.getLastHeartbeatTimestamp(),
                null,   // note — not in Overview
                null,   // street
                null,   // houseNumber
                null,   // zipCode
                null,   // city
                null,   // country
                null,   // latitude
                null    // longitude
        );
    }

    public static List<ChargeBoxOverview> toOverviewList(List<ChargePoint.Overview> list) {
        return list.stream()
                .map(ChargeBoxOverview::from)
                .collect(Collectors.toList());
    }

    public static ChargeBoxOverview toOverviewFromDetails(ChargePoint.Details details) {
        ChargeBoxRecord cb   = details.getChargeBox();
        AddressRecord addr = details.getAddress(); // nullable

        return new ChargeBoxOverview(
                cb.getChargeBoxPk(),
                cb.getChargeBoxId(),
                cb.getDescription(),
                cb.getOcppProtocol(),
                null, // lastHeartbeatTimestamp (humanized) is not on Details; null is acceptable here
                cb.getNote(),
                addr != null ? addr.getStreet()      : null,
                addr != null ? addr.getHouseNumber() : null,
                addr != null ? addr.getZipCode()     : null,
                addr != null ? addr.getCity()        : null,
                addr != null ? addr.getCountry()     : null, // stored as alpha-2 string in AddressRecord
                addr != null && addr.getLatitude()  != null ? addr.getLatitude().toPlainString()  : null,
                addr != null && addr.getLongitude() != null ? addr.getLongitude().toPlainString() : null
        );
    }
}