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
package dev.noah.perplayerkit.storage.exceptions;

/** A kit read or write the backend could not complete. The kit data API is unchecked. */
public class KitStorageException extends RuntimeException {

    public enum Operation {
        SAVE("save"), READ("read"), EXISTS("existence check"), DELETE("delete"), LIST_IDS("ID listing");

        private final String label;

        Operation(String label) {
            this.label = label;
        }
    }

    private final Operation operation;
    private final String kitID;

    public KitStorageException(Operation operation, String kitID, Throwable cause) {
        super("Kit " + operation.label + " failed" + (kitID == null ? "" : " for " + kitID), cause);
        this.operation = operation;
        this.kitID = kitID;
    }

    public Operation operation() {
        return operation;
    }

    /** Null for operations that span every kit, such as listing IDs. */
    public String kitID() {
        return kitID;
    }
}
