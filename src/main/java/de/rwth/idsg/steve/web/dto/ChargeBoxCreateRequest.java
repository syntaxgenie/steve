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

import com.neovisionaries.i18n.CountryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import ocpp.cs._2015._10.RegistrationStatus;

import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/chargeBoxes.
 * <p>
 * chargeBoxId is the only required field. All other fields are optional metadata.
 * <p>
 * registrationStatus defaults to ACCEPTED and
 * insertConnectorStatusAfterTransactionMsg defaults to true — these are
 * the sensible defaults for a station registered via API and are not exposed
 * to the caller.
 * <p>
 * country must be an ISO 3166-1 alpha-2 code (e.g. "DE", "US") — it maps
 * to CountryCode in the Address form.
 * <p>
 * latitude and longitude are passed as strings and parsed to BigDecimal by
 * the mapper. The Address form validates the range (-90..90 and -180..180).
 */
public record ChargeBoxCreateRequest(

        @NotBlank(message = "ChargeBox ID is required")
        @Schema(
                description = "The OCPP charge box identifier. Must match the last segment of the station's WebSocket URL. " +
                              "No whitespace or special characters (=, /, (, ), <, >) allowed.",
                example = "CP-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String chargeBoxId,

        @Schema(description = "Human-readable description", example = "Parking garage level 2, bay 14")
        String description,

        @Schema(description = "Operator or admin note", example = "Installed 2024-01")
        String note,

        @Schema(description = "Street name", example = "Templergraben")
        String street,

        @Schema(description = "House number", example = "55")
        String houseNumber,

        @Schema(description = "ZIP / postal code", example = "52062")
        String zipCode,

        @Schema(description = "City", example = "Aachen")
        String city,

        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "DE")
        String country,

        @Schema(description = "Latitude (-90 to 90)", example = "50.7753")
        String latitude,

        @Schema(description = "Longitude (-180 to 180)", example = "6.0839")
        String longitude
) {
    public static ChargePointFormForCreate toCreateForm(ChargeBoxCreateRequest req) {
        ChargePointFormForCreate form = new ChargePointFormForCreate();
        form.setChargeBoxId(req.chargeBoxId());
        form.setDescription(req.description());
        form.setNote(req.note());
        form.setRegistrationStatus(RegistrationStatus.ACCEPTED);
        form.setInsertConnectorStatusAfterTransactionMsg(true);
        form.setAddress(buildAddress(req));
        return form;
    }

    private static Address buildAddress(ChargeBoxCreateRequest req) {
        if (req.street() == null && req.houseNumber() == null && req.zipCode() == null
            && req.city() == null && req.country() == null
            && req.latitude() == null && req.longitude() == null) {
            return new Address();
        }

        Address a = new Address();
        a.setStreet(req.street());
        a.setHouseNumber(req.houseNumber());
        a.setZipCode(req.zipCode());
        a.setCity(req.city());
        a.setCountry(parseCountryCode(req.country()));
        a.setLatitude(parseBigDecimal(req.latitude()));
        a.setLongitude(parseBigDecimal(req.longitude()));
        return a;
    }

    private static CountryCode parseCountryCode(String alpha2) {
        if (alpha2 == null) {
            return null;
        }
        CountryCode code = CountryCode.getByCode(alpha2);
        if (code == null) {
            throw new de.rwth.idsg.steve.SteveException.BadRequest("Unknown country code: " + alpha2);
        }
        return code;
    }

    private static BigDecimal parseBigDecimal(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new de.rwth.idsg.steve.SteveException.BadRequest("Invalid decimal value: " + value);
        }
    }
}