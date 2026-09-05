package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.kits.*;
import dev.noah.perplayerkit.commands.features.*;
import dev.noah.perplayerkit.commands.share.*;
import dev.noah.perplayerkit.commands.shortcuts.*;
import dev.noah.perplayerkit.util.*;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.List;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class LocationCommandRoutesTest {
    static Stream<Arguments> commands() {
        Plugin plugin = mock(Plugin.class, RETURNS_DEEP_STUBS);
        return Stream.of(
            Arguments.of(new MainMenuCommand(plugin), "menu", "kit"),
            Arguments.of(new PublicKitCommand(plugin), "public-kits", "publickit"),
            Arguments.of(new EnderchestCommand(), "enderchests", "ec"),
            Arguments.of(new SwapKitCommand(), "kits", "swapkit"),
            Arguments.of(new DeleteKitCommand(), "kits", "deletekit"),
            Arguments.of(new DeleteKitCommand(true), "enderchests", "deleteec"),
            Arguments.of(new ShortKitCommand(), "kits", "k1"),
            Arguments.of(new ShortECCommand(), "enderchests", "ec1"),
            Arguments.of(new HealCommand(), "heal", "heal"),
            Arguments.of(new RepairCommand(), "repair", "repair"),
            Arguments.of(new CopyKitCommand(), "sharing", "copykit"),
            Arguments.of(new ShareKitCommand(), "sharing", "sharekit"),
            Arguments.of(new ShareECKitCommand(), "sharing", "shareec"),
            Arguments.of(new ShareAcceptCommand(), "sharing", "shareaccept"),
            Arguments.of(new TransferKitsCommand(), "sharing", "transferkits")
        );
    }
    @ParameterizedTest @MethodSource("commands")
    void everyCommandChecksItsFeatureBeforeDoingWork(CommandExecutor command, String feature, String label) {
        Plugin plugin = mock(Plugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("locations." + feature + ".mode", "deny");
        config.set("locations." + feature + ".entries", List.of("pvp"));
        when(plugin.getConfig()).thenReturn(config); new LocationAccess(plugin);
        Player player = mock(Player.class); World world = mock(World.class);
        when(world.getName()).thenReturn("pvp"); when(player.getWorld()).thenReturn(world);
        try (var lang = mockStatic(Lang.class); var sounds = mockStatic(SoundManager.class)) {
            Lang messages = mock(Lang.class); lang.when(Lang::get).thenReturn(messages);
            command.onCommand(player, null, label, new String[]{"1"});
            verify(messages).sendNoPrefix(player, "error.disabled-at-location");
            verify(player, never()).getInventory(); verify(player, never()).getEnderChest();
        }
    }
}
