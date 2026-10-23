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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;

import java.util.Arrays;
import java.util.stream.Collectors;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Combines non-blocking diagnostics within the existing response warning limit. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WarningUtility {

    public static String merge(String... warnings) {

        String merged = Arrays.stream(warnings)
                .filter(warning -> !StringUtility.isNullOrEmpty(warning))
                .distinct()
                .collect(Collectors.joining(", "));

        if (merged.isEmpty()) {
            return null;
        }
        if (merged.length() > Constants.App.MAX_SIZE_WARNING) {
            return merged.substring(0, Constants.App.MAX_SIZE_WARNING - 3) + "...";
        }
        return merged;
    }
}
