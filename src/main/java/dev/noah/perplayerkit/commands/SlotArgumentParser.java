/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.commands;

import com.google.common.primitives.Ints;
import org.jetbrains.annotations.Nullable;

public final class SlotArgumentParser {
    private SlotArgumentParser() {
    }

    public static @Nullable Integer parseSlot(String slotArgument) {
        return Ints.tryParse(slotArgument);
    }

    public static @Nullable Integer parseSlotInRange(String slotArgument, int min, int max) {
        Integer slot = parseSlot(slotArgument);
        if (slot == null || slot < min || slot > max) {
            return null;
        }
        return slot;
    }
}
