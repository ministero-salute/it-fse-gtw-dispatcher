/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing the current status of an ingestion transaction from broker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO containing the current status of an ingestion transaction")
public class GetIngestionStatusResponseDTO {

    @JsonProperty("workflowInstanceId")
    @Schema(description = "Workflow instance identifier", example = "2.16.840.1.113883.2.9.2.120.4.4.b0f3ffcf25ce2aafc7dc901e2febc51f43837f4ca0fe3b6d1b02194e9047b6db.df3ea8b89f^^^^urn:ihe:iti:xdw:2013:workflowInstanceId")
    private String workflowInstanceId;

    @JsonProperty("type")
    @Schema(description = "Operation type", example = "CREATE", allowableValues = {"CREATE", "REPLACE", "UPDATE", "DELETE"})
    private String type;

    @JsonProperty("status")
    @Schema(description = "Transaction status", example = "SUCCESS", allowableValues = {"SUCCESS", "BLOCKING_ERROR", "NON_BLOCKING_ERROR", "ASYNC_RETRY"})
    private String status;

    @JsonProperty("insertionDate")
    @Schema(description = "Transaction insertion date", example = "2025-12-09T08:47:30Z")
    private Date insertionDate;

    @JsonProperty("rde")
    @Schema(description = "RDE code", example = "120")
    private String rde;
}
