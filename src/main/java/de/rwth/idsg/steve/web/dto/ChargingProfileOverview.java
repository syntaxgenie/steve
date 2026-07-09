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
import de.rwth.idsg.steve.repository.dto.ChargingProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only view of a charging profile, returned by the REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChargingProfileOverview(

        @Schema(description = "Database primary key of the charging profile")
        Integer chargingProfilePk,

        @Schema(description = "Human-readable description")
        String description,

        @Schema(description = "OCPP stack level")
        Integer stackLevel,

        @Schema(description = "Charging profile purpose: TX_PROFILE, TX_DEFAULT_PROFILE, CHARGE_POINT_MAX_PROFILE")
        String chargingProfilePurpose,

        @Schema(description = "Charging profile kind: ABSOLUTE, RELATIVE, RECURRING")
        String chargingProfileKind,

        @Schema(description = "Recurrency kind: DAILY, WEEKLY (only for RECURRING kind)")
        String recurrencyKind,

        @Schema(description = "Charging rate unit: W or A")
        String chargingRateUnit,

        @Schema(description = "Minimum charging rate")
        BigDecimal minChargingRate,

        @Schema(description = "Schedule periods")
        List<SchedulePeriodDTO> schedulePeriods
) {
    public record SchedulePeriodDTO(
            Integer startPeriodInSeconds,
            BigDecimal powerLimit,
            Integer numberPhases
    ) {}

    public static ChargingProfileOverview from(final ChargingProfile.Overview overview) {
        return new ChargingProfileOverview(
                overview.getChargingProfilePk(),
                overview.getDescription(),
                overview.getStackLevel(),
                overview.getProfilePurpose(),
                overview.getProfileKind(),
                overview.getRecurrencyKind(),
                null,   // chargingRateUnit not on Overview
                null,   // minChargingRate not on Overview
                null    // schedulePeriods not on Overview
        );
    }

    public static List<ChargingProfileOverview> fromList(final List<ChargingProfile.Overview> list) {
        return list.stream().map(ChargingProfileOverview::from).toList();
    }
}