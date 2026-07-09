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
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import io.swagger.v3.oas.annotations.media.Schema;
import ocpp.cs._2015._10.RegistrationStatus;
import jooq.steve.db.tables.records.ChargeBoxRecord;
import jooq.steve.db.tables.records.AddressRecord;

import java.math.BigDecimal;

/**
 * Request body for PUT /api/v1/chargeBoxes/{chargeBoxPk}.
 * <p>
 * This is a partial update: a null field means "keep the current value in the DB".
 * Only non-null fields are applied on top of the existing record.
 * <p>
 * chargeBoxId is intentionally absent — it is the WebSocket identity of the station
 * and cannot be changed without reconfiguring the station firmware.
 */
public record ChargeBoxUpdateRequest(

        @Schema(description = "Human-readable description. Null = keep existing value.")
        String description,

        @Schema(description = "Operator or admin note. Null = keep existing value.")
        String note,

        @Schema(description = "Street name. Null = keep existing value.")
        String street,

        @Schema(description = "House number. Null = keep existing value.")
        String houseNumber,

        @Schema(description = "ZIP / postal code. Null = keep existing value.")
        String zipCode,

        @Schema(description = "City. Null = keep existing value.")
        String city,

        @Schema(description = "ISO 3166-1 alpha-2 country code. Null = keep existing value.", example = "DE")
        String country,

        @Schema(description = "Latitude (-90 to 90). Null = keep existing value.", example = "50.7753")
        String latitude,

        @Schema(description = "Longitude (-180 to 180). Null = keep existing value.", example = "6.0839")
        String longitude
) {
    public static ChargePointFormForUpdate toUpdateForm(int chargeBoxPk,
                                                        ChargePoint.Details existing,
                                                        ChargeBoxUpdateRequest req) {
        ChargeBoxRecord cb = existing.getChargeBox();
        AddressRecord addr = existing.getAddress(); // nullable

        ChargePointFormForUpdate form = new ChargePointFormForUpdate();

        form.setChargeBoxPk(chargeBoxPk);
        form.setChargeBoxId(cb.getChargeBoxId());

        // Required fields — preserve existing values so the service layer stays valid
        form.setRegistrationStatus(RegistrationStatus.fromValue(cb.getRegistrationStatus()));
        form.setInsertConnectorStatusAfterTransactionMsg(
                cb.getInsertConnectorStatusAfterTransactionMsg() != null
                        ? cb.getInsertConnectorStatusAfterTransactionMsg()
                        : true
        );
        form.setSecurityProfile(
                de.rwth.idsg.steve.ocpp.OcppSecurityProfile.fromValueNoException(
                        cb.getSecurityProfile() != null ? cb.getSecurityProfile().toString() : "0"
                )
        );

        // Optional scalar fields — use request value if non-null, else keep existing
        form.setDescription(req.description() != null ? req.description() : cb.getDescription());
        form.setNote(req.note() != null ? req.note() : cb.getNote());

        // Address — merge request fields on top of existing AddressRecord
        form.setAddress(mergeAddress(addr, req));

        return form;
    }

    private static Address mergeAddress(AddressRecord existing, ChargeBoxUpdateRequest req) {
        boolean requestHasAny = req.street() != null || req.houseNumber() != null
                                || req.zipCode() != null || req.city() != null || req.country() != null
                                || req.latitude() != null || req.longitude() != null;

        if (existing == null && !requestHasAny) {
            return null;
        }

        Address a = new Address();

        if (existing != null) {
            a.setAddressPk(existing.getAddressPk());
        }

        a.setStreet(req.street() != null ? req.street() : (existing != null ? existing.getStreet() : null));
        a.setHouseNumber(req.houseNumber() != null ? req.houseNumber() : (existing != null ? existing.getHouseNumber() : null));
        a.setZipCode(req.zipCode() != null ? req.zipCode() : (existing != null ? existing.getZipCode() : null));
        a.setCity(req.city() != null ? req.city() : (existing != null ? existing.getCity() : null));

        String countryStr = req.country() != null
                ? req.country()
                : (existing != null ? existing.getCountry() : null);
        a.setCountry(parseCountryCode(countryStr));

        String latStr = req.latitude() != null
                ? req.latitude()
                : (existing != null && existing.getLatitude() != null ? existing.getLatitude().toPlainString() : null);
        a.setLatitude(parseBigDecimal(latStr));

        String lonStr = req.longitude() != null
                ? req.longitude()
                : (existing != null && existing.getLongitude() != null ? existing.getLongitude().toPlainString() : null);
        a.setLongitude(parseBigDecimal(lonStr));

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