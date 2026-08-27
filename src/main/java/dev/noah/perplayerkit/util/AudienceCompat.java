/*
 * Copyright 2026 Noah Ross
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
package dev.noah.perplayerkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;

/**
 * Sends components through the server's own Adventure API when it has one.
 *
 * <p>Paper implements Adventure natively - {@code CommandSender} is an
 * {@code Audience} - while this plugin ships its own copy so it still runs on
 * Spigot. Those are two different {@code Component} classes, and on Paper 26+
 * the server moved to Adventure 5 while {@code adventure-platform-bukkit}
 * targets 4.x: it stops recognising the server, hands back an audience that
 * throws nothing and delivers nothing, and every message to a player silently
 * disappears.
 *
 * <p>So talk to the server directly instead. Components are handed over as
 * JSON, the one representation both Adventure versions agree on, which keeps
 * click and hover events intact - a legacy string would lose the clickable
 * autosetup prompt and the share request buttons. On Spigot there is no native
 * audience and callers fall back to {@code BukkitAudiences} as before.
 */
public final class AudienceCompat {

    /**
     * The server's Adventure package, assembled at runtime.
     *
     * <p>Do not turn this into a plain "net.kyori..." literal. The shade plugin
     * relocates our Adventure to dev.noah.perplayerkit.libs.kyori and rewrites
     * matching string constants along with it, so a literal here would silently
     * start naming our own relocated classes instead of the server's, the
     * lookups below would find nothing, and every message would quietly fall
     * back to the delivery that does not work on Paper 26+. Splitting it keeps
     * javac from folding it into one constant for shade to match.
     */
    private static final String SERVER_ADVENTURE = String.join(".", "net", "kyori", "adventure");
    private static final String SERVER_COMPONENT = SERVER_ADVENTURE + ".text.Component";
    private static final String SERVER_GSON = SERVER_ADVENTURE + ".text.serializer.gson.GsonComponentSerializer";

    /**
     * The JSON we hand over, in a dialect every server can read.
     *
     * <p>Minecraft 1.21.5 renamed the event keys - {@code clickEvent} became
     * {@code click_event} - and our Adventure emits the new spelling. An older
     * server reads that JSON quite happily, ignores the key it does not know,
     * and shows the message with the click and hover silently missing. The
     * compatibility options emit both spellings, so the text stays interactive
     * whichever end is reading it.
     */
    private static final GsonComponentSerializer BRIDGE_JSON = GsonComponentSerializer.builder()
            .options(JSONOptions.compatibility())
            .build();

    /** {@code CommandSender#sendMessage(Component)}, or null off Paper. */
    private static final Method SEND_MESSAGE = findSendMessage();

    /** True when the server's Component class is the very one we compiled against. */
    private static final boolean SAME_COMPONENT_CLASS =
            SEND_MESSAGE != null && SEND_MESSAGE.getParameterTypes()[0] == Component.class;

    /** The server's own GsonComponentSerializer, when a conversion is needed. */
    private static final Object SERVER_SERIALIZER =
            SAME_COMPONENT_CLASS ? null : findServerSerializer();
    private static final Method SERVER_DESERIALIZE =
            SERVER_SERIALIZER == null ? null : findDeserialize(SERVER_SERIALIZER);

    private AudienceCompat() {
    }

    /** Whether the running server can accept components directly. */
    public static boolean isAvailable() {
        return SEND_MESSAGE != null && (SAME_COMPONENT_CLASS || SERVER_DESERIALIZE != null);
    }

    /**
     * @return false if this server has no native audience, or the handover
     * failed - the caller should then fall back to its own delivery
     */
    public static boolean send(CommandSender sender, Component message) {
        if (!isAvailable()) {
            return false;
        }

        try {
            Object payload = SAME_COMPONENT_CLASS
                    ? message
                    : SERVER_DESERIALIZE.invoke(SERVER_SERIALIZER, BRIDGE_JSON.serialize(message));
            SEND_MESSAGE.invoke(sender, payload);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Silent on purpose: the caller falls back to the delivery this
            // plugin used before, so a failure here costs nothing and is not
            // worth a line in every server's log.
            return false;
        }
    }

    private static Method findSendMessage() {
        for (Method method : CommandSender.class.getMethods()) {
            if (!method.getName().equals("sendMessage") || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].getName().equals(SERVER_COMPONENT)) {
                return method;
            }
        }
        return null;
    }

    private static Object findServerSerializer() {
        if (SEND_MESSAGE == null) {
            return null;
        }
        try {
            ClassLoader serverLoader = SEND_MESSAGE.getParameterTypes()[0].getClassLoader();
            Class<?> serializer = Class.forName(SERVER_GSON, true, serverLoader);
            return serializer.getMethod("gson").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Erasure makes the parameter String on some versions and Object on others. */
    private static Method findDeserialize(Object serializer) {
        for (Method method : serializer.getClass().getMethods()) {
            if (!method.getName().equals("deserialize") || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isAssignableFrom(String.class)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
