/*
 * Copyright 2022-2026 Noah Ross
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
package dev.noah.perplayerkit.kitdata;

import dev.noah.perplayerkit.kitdata.KitDataSnapshot.PublicKitEntry;
import dev.noah.perplayerkit.util.IDUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * Reader/writer for the .ppk kit data exchange format.
 * <p>
 * The file is a small custom binary container: a magic number and format
 * version, an informational header (plugin version, creation time), up to
 * three sections (kit room, public kits, player kits) and a trailing end
 * marker plus CRC32 checksum of everything before it. Item data is carried
 * as the same Base64 blob strings the storage backends store, copied
 * verbatim.
 * <p>
 * The reader treats every file as untrusted: all counts and lengths are
 * bounds-checked before any allocation, IDs and pages are validated against
 * strict patterns, duplicate sections/entries are rejected, and a truncated
 * or checksum-mismatching file fails as a whole. Item blobs are only
 * validated structurally here; decoding them into item stacks happens in
 * {@link KitDataService} where the whole import still fails closed.
 */
public final class KitDataFile {

    public static final String FILE_EXTENSION = ".ppk";

    static final int MAGIC = 0x50504B44; // "PPKD"
    static final int FORMAT_VERSION = 1;
    static final int END_MAGIC = 0x504B454E; // "PKEN"

    static final byte SECTION_KIT_ROOM = 1;
    static final byte SECTION_PUBLIC_KITS = 2;
    static final byte SECTION_PLAYER_KITS = 3;

    /** Format limit, independent of the configured number of visible pages. */
    public static final int KIT_ROOM_PAGES = dev.noah.perplayerkit.KitRoomDataManager.MAX_PAGE_COUNT;
    static final int MAX_PUBLIC_KITS = 5_000;
    static final int MAX_PLAYER_KITS = 1_000_000;
    static final int MAX_BLOB_BYTES = 8 * 1024 * 1024;
    static final int MAX_NAME_LENGTH = 256;

    private static final Pattern PUBLIC_KIT_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern ICON_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,64}");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    private KitDataFile() {
    }

    /** Contents of a parsed file plus its informational header fields. */
    public record ReadResult(KitDataSnapshot snapshot, String pluginVersion, long createdAtMillis) {
    }

    /**
     * Validates and normalizes a user-supplied export/import file name:
     * letters, digits, dots, dashes and underscores only (no path separators,
     * so the file can never escape the plugin folder), appending the
     * {@value #FILE_EXTENSION} extension if missing. Returns null if the name
     * is not acceptable.
     */
    public static String sanitizeFileName(String input) {
        if (input == null) {
            return null;
        }
        String name = input.toLowerCase(Locale.ROOT).endsWith(FILE_EXTENSION)
                ? input.substring(0, input.length() - FILE_EXTENSION.length())
                : input;
        if (name.isEmpty() || !FILE_NAME_PATTERN.matcher(name).matches() || name.contains("..")) {
            return null;
        }
        return name + FILE_EXTENSION;
    }

    public static void write(OutputStream stream, KitDataSnapshot snapshot,
                             String pluginVersion, long createdAtMillis)
            throws IOException, InvalidKitDataException {
        validateSnapshot(snapshot);

        CRC32 crc = new CRC32();
        DataOutputStream out = new DataOutputStream(new CheckedOutputStream(new BufferedOutputStream(stream), crc));

        out.writeInt(MAGIC);
        out.writeInt(FORMAT_VERSION);
        out.writeUTF(pluginVersion == null ? "unknown" : pluginVersion);
        out.writeLong(createdAtMillis);

        int sections = (snapshot.getKitRoomPages().isEmpty() ? 0 : 1)
                + (snapshot.getPublicKits().isEmpty() ? 0 : 1)
                + (snapshot.getPlayerKits().isEmpty() ? 0 : 1);
        out.writeInt(sections);

        if (!snapshot.getKitRoomPages().isEmpty()) {
            out.writeByte(SECTION_KIT_ROOM);
            out.writeInt(snapshot.getKitRoomPages().size());
            for (var entry : snapshot.getKitRoomPages().entrySet()) {
                out.writeByte(entry.getKey());
                writeBlob(out, entry.getValue());
            }
        }

        if (!snapshot.getPublicKits().isEmpty()) {
            out.writeByte(SECTION_PUBLIC_KITS);
            out.writeInt(snapshot.getPublicKits().size());
            for (PublicKitEntry kit : snapshot.getPublicKits().values()) {
                out.writeUTF(kit.id());
                out.writeUTF(kit.name() == null ? kit.id() : kit.name());
                out.writeUTF(kit.icon());
                out.writeBoolean(kit.data() != null);
                if (kit.data() != null) {
                    writeBlob(out, kit.data());
                }
            }
        }

        if (!snapshot.getPlayerKits().isEmpty()) {
            out.writeByte(SECTION_PLAYER_KITS);
            out.writeInt(snapshot.getPlayerKits().size());
            for (var entry : snapshot.getPlayerKits().entrySet()) {
                out.writeUTF(entry.getKey());
                writeBlob(out, entry.getValue());
            }
        }

        out.writeInt(END_MAGIC);
        out.flush();
        long checksum = crc.getValue();
        // The checksum itself is not covered by the CRC; the reader captures
        // its running value before reading these final 8 bytes.
        out.writeLong(checksum);
        out.flush();
    }

    public static ReadResult read(InputStream stream) throws IOException, InvalidKitDataException {
        CRC32 crc = new CRC32();
        DataInputStream in = new DataInputStream(new CheckedInputStream(new BufferedInputStream(stream), crc));

        try {
            if (in.readInt() != MAGIC) {
                throw new InvalidKitDataException("not a PerPlayerKit kit data (.ppk) file");
            }
            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new InvalidKitDataException("unsupported format version " + version
                        + " (this plugin supports version " + FORMAT_VERSION
                        + "; the file may come from a newer plugin release)");
            }
            String pluginVersion = in.readUTF();
            if (pluginVersion.length() > 128) {
                throw new InvalidKitDataException("plugin version field is too long");
            }
            long createdAt = in.readLong();

            int sectionCount = in.readInt();
            if (sectionCount < 0 || sectionCount > 3) {
                throw new InvalidKitDataException("invalid section count: " + sectionCount);
            }

            KitDataSnapshot snapshot = new KitDataSnapshot();
            Set<Byte> seenSections = new HashSet<>();
            for (int i = 0; i < sectionCount; i++) {
                byte type = in.readByte();
                if (!seenSections.add(type)) {
                    throw new InvalidKitDataException("duplicate section: " + type);
                }
                switch (type) {
                    case SECTION_KIT_ROOM -> readKitRoomSection(in, snapshot);
                    case SECTION_PUBLIC_KITS -> readPublicKitsSection(in, snapshot);
                    case SECTION_PLAYER_KITS -> readPlayerKitsSection(in, snapshot);
                    default -> throw new InvalidKitDataException("unknown section type: " + type);
                }
            }

            if (in.readInt() != END_MAGIC) {
                throw new InvalidKitDataException("end marker missing, file is corrupted");
            }
            long computed = crc.getValue();
            long stored = in.readLong();
            if (computed != stored) {
                throw new InvalidKitDataException("checksum mismatch, file is corrupted");
            }
            if (in.read() != -1) {
                throw new InvalidKitDataException("unexpected trailing data after end of file");
            }
            return new ReadResult(snapshot, pluginVersion, createdAt);
        } catch (EOFException e) {
            throw new InvalidKitDataException("file is truncated or corrupted", e);
        } catch (UTFDataFormatException e) {
            throw new InvalidKitDataException("malformed text field, file is corrupted", e);
        }
    }

    private static void readKitRoomSection(DataInputStream in, KitDataSnapshot snapshot)
            throws IOException, InvalidKitDataException {
        int count = in.readInt();
        if (count < 1 || count > KIT_ROOM_PAGES) {
            throw new InvalidKitDataException("invalid kit room page count: " + count);
        }
        for (int i = 0; i < count; i++) {
            int page = in.readByte();
            if (page < 0 || page >= KIT_ROOM_PAGES) {
                throw new InvalidKitDataException("invalid kit room page number: " + page);
            }
            if (snapshot.getKitRoomPages().containsKey(page)) {
                throw new InvalidKitDataException("duplicate kit room page: " + page);
            }
            snapshot.getKitRoomPages().put(page, readBlob(in, "kit room page " + page));
        }
    }

    private static void readPublicKitsSection(DataInputStream in, KitDataSnapshot snapshot)
            throws IOException, InvalidKitDataException {
        int count = in.readInt();
        if (count < 1 || count > MAX_PUBLIC_KITS) {
            throw new InvalidKitDataException("invalid public kit count: " + count);
        }
        for (int i = 0; i < count; i++) {
            String id = in.readUTF();
            validatePublicKitId(id);
            String name = in.readUTF();
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
                throw new InvalidKitDataException("invalid name for public kit " + id);
            }
            String icon = in.readUTF();
            if (!ICON_PATTERN.matcher(icon).matches()) {
                throw new InvalidKitDataException("invalid icon for public kit " + id);
            }
            String data = in.readBoolean() ? readBlob(in, "public kit " + id) : null;
            if (snapshot.getPublicKits().put(id, new PublicKitEntry(id, name, icon, data)) != null) {
                throw new InvalidKitDataException("duplicate public kit: " + id);
            }
        }
    }

    private static void readPlayerKitsSection(DataInputStream in, KitDataSnapshot snapshot)
            throws IOException, InvalidKitDataException {
        int count = in.readInt();
        if (count < 1 || count > MAX_PLAYER_KITS) {
            throw new InvalidKitDataException("invalid player kit count: " + count);
        }
        for (int i = 0; i < count; i++) {
            String id = in.readUTF();
            if (!IDUtil.isPlayerDataId(id)) {
                throw new InvalidKitDataException("invalid player kit ID: " + abbreviate(id));
            }
            if (snapshot.getPlayerKits().put(id, readBlob(in, "player kit " + id)) != null) {
                throw new InvalidKitDataException("duplicate player kit: " + id);
            }
        }
    }

    private static void writeBlob(DataOutputStream out, String data) throws IOException, InvalidKitDataException {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_BLOB_BYTES) {
            throw new InvalidKitDataException("kit data blob has invalid size: " + bytes.length + " bytes");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readBlob(DataInputStream in, String what) throws IOException, InvalidKitDataException {
        int length = in.readInt();
        if (length < 1 || length > MAX_BLOB_BYTES) {
            throw new InvalidKitDataException("invalid data size for " + what + ": " + length + " bytes");
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void validateSnapshot(KitDataSnapshot snapshot) throws InvalidKitDataException {
        if (snapshot.isEmpty()) {
            throw new InvalidKitDataException("nothing to write");
        }
        for (int page : snapshot.getKitRoomPages().keySet()) {
            if (page < 0 || page >= KIT_ROOM_PAGES) {
                throw new InvalidKitDataException("invalid kit room page number: " + page);
            }
        }
        if (snapshot.getPublicKits().size() > MAX_PUBLIC_KITS) {
            throw new InvalidKitDataException("too many public kits: " + snapshot.getPublicKits().size());
        }
        for (PublicKitEntry kit : snapshot.getPublicKits().values()) {
            validatePublicKitId(kit.id());
            if (kit.icon() == null || !ICON_PATTERN.matcher(kit.icon()).matches()) {
                throw new InvalidKitDataException("invalid icon for public kit " + kit.id());
            }
        }
        if (snapshot.getPlayerKits().size() > MAX_PLAYER_KITS) {
            throw new InvalidKitDataException("too many player kits: " + snapshot.getPlayerKits().size());
        }
        for (String id : snapshot.getPlayerKits().keySet()) {
            if (!IDUtil.isPlayerDataId(id)) {
                throw new InvalidKitDataException("invalid player kit ID: " + abbreviate(id));
            }
        }
    }

    private static void validatePublicKitId(String id) throws InvalidKitDataException {
        if (id == null || !PUBLIC_KIT_ID_PATTERN.matcher(id).matches()) {
            throw new InvalidKitDataException("invalid public kit ID: " + abbreviate(id));
        }
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() <= 48 ? value : value.substring(0, 48) + "…";
    }
}
