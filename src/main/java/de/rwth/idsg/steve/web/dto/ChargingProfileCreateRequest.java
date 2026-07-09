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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ocpp.cp._2015._10.ChargingProfileKindType;
import ocpp.cp._2015._10.ChargingProfilePurposeType;
import ocpp.cp._2015._10.ChargingRateUnitType;
import ocpp.cp._2015._10.RecurrencyKindType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for POST /api/v1/chargingProfiles.
 * Maps to ChargingProfileForm for persistence via ChargingProfileRepository.
 * Used by the platform to create TxProfile charging profiles for Smart Charge
 * and Green Mode scheduling.
 */
public record ChargingProfileCreateRequest(

        @Schema(description = "Human-readable description", example = "Smart Charge - 7.2kW limit")
        String description,

        @Schema(description = "Note", example = "Created by Oversight platform")
        String note,

        @NotNull(message = "Stack Level is required")
        @PositiveOrZero(message = "Stack Level must be 0 or positive")
        @Schema(description = "OCPP stack level. Use 0 for TxProfile.", example = "0")
        Integer stackLevel,

        @NotNull(message = "Charging Profile Purpose is required")
        @Schema(description = "TX_PROFILE, TX_DEFAULT_PROFILE, or CHARGE_POINT_MAX_PROFILE",
                example = "TX_PROFILE")
        ChargingProfilePurposeType chargingProfilePurpose,

        @NotNull(message = "Charging Profile Kind is required")
        @Schema(description = "ABSOLUTE, RELATIVE, or RECURRING", example = "RELATIVE")
        ChargingProfileKindType chargingProfileKind,

        @Schema(description = "DAILY or WEEKLY — only for RECURRING kind")
        RecurrencyKindType recurrencyKind,

        @NotNull(message = "Charging Rate Unit is required")
        @Schema(description = "W (watts) or A (amperes)", example = "A")
        ChargingRateUnitType chargingRateUnit,

        @Schema(description = "Minimum charging rate", example = "6.0")
        BigDecimal minChargingRate,

        @NotEmpty(message = "At least one schedule period is required")
        @Schema(description = "List of schedule periods defining power limits over time")
        List<@Valid SchedulePeriodRequest> schedulePeriods
) {
    public record SchedulePeriodRequest(

            @NotNull(message = "Start period is required")
            @Min(value = 0, message = "Start period must be 0 or positive")
            @Schema(description = "Seconds from schedule start", example = "0")
            Integer startPeriodInSeconds,

            @NotNull(message = "Power limit is required")
            @DecimalMin(value = "0.0", message = "Power limit must be 0 or positive")
            @Digits(integer = 6, fraction = 1,
                    message = "Power limit must have at most 6 digits and 1 decimal place")
            @Schema(description = "Power limit in W or A (matches chargingRateUnit)", example = "16.0")
            BigDecimal powerLimit,

            @Schema(description = "Number of phases (1-3)", example = "1")
            Integer numberPhases
    ) {}

    /**
     * Maps this request to the internal ChargingProfileForm used by
     * ChargingProfileRepository.add().
     */
    public ChargingProfileForm toForm() {
        ChargingProfileForm form = new ChargingProfileForm();
        form.setDescription(description);
        form.setNote(note);
        form.setStackLevel(stackLevel);
        form.setChargingProfilePurpose(chargingProfilePurpose);
        form.setChargingProfileKind(chargingProfileKind);
        form.setRecurrencyKind(recurrencyKind);
        form.setChargingRateUnit(chargingRateUnit);
        form.setMinChargingRate(minChargingRate);

        if (schedulePeriods != null) {
            List<ChargingProfileForm.SchedulePeriod> periods = schedulePeriods.stream()
                    .map(p -> {
                        ChargingProfileForm.SchedulePeriod sp = new ChargingProfileForm.SchedulePeriod();
                        sp.setStartPeriodInSeconds(p.startPeriodInSeconds());
                        sp.setPowerLimit(p.powerLimit());
                        sp.setNumberPhases(p.numberPhases());
                        return sp;
                    })
                    .toList();
            form.setSchedulePeriods(periods);
        }

        return form;
    }
}