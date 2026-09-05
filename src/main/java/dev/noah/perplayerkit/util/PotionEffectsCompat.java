package dev.noah.perplayerkit.util;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/** Reads real brewing effects, including duration and multi-effect potions, on old and new Bukkit APIs. */
public final class PotionEffectsCompat {
    private static final Method BASE_TYPE = method(PotionMeta.class, "getBasePotionType");
    private static final Method EFFECTS = method(PotionType.class, "getPotionEffects");
    private PotionEffectsCompat() {}
    private static Method method(Class<?> type, String name) {
        try { return type.getMethod(name); }
        catch (NoSuchMethodException ignored) { return null; }
    }
    @SuppressWarnings("unchecked")
    public static Collection<PotionEffect> baseEffects(PotionMeta meta) {
        if (BASE_TYPE == null || EFFECTS == null) return Legacy.effects(meta);
        try {
            Object type = BASE_TYPE.invoke(meta);
            return type == null ? List.of() : (Collection<PotionEffect>) EFFECTS.invoke(type);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not inspect base potion effects", error);
        }
    }
    /** Isolates API types that may be removed on newer servers. */
    private static final class Legacy {
        static Collection<PotionEffect> effects(PotionMeta meta) {
            org.bukkit.potion.PotionData data = meta.getBasePotionData();
            return data == null ? List.of() : org.bukkit.potion.Potion.getBrewer().getEffects(data.getType(), data.isUpgraded(), data.isExtended());
        }
    }
}
