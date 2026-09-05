package dev.noah.perplayerkit;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.*;
import org.bukkit.plugin.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionBundlesTest {
    @Test void nonOpAdminCanEditRoomWithoutChangingLegacyNotificationAudience() throws Exception {
        Server server = mock(Server.class);
        PluginManager manager = new SimplePluginManager(server, new SimpleCommandMap(server));
        when(server.getPluginManager()).thenReturn(manager);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             var input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            for (Permission permission : new PluginDescriptionFile(input).getPermissions()) manager.addPermission(permission);
            PermissibleBase player = new PermissibleBase(mock(ServerOperator.class));
            assertFalse(player.hasPermission("perplayerkit.kitnotify"));
            player.addAttachment(plugin, "perplayerkit.admin", true);
            assertTrue(player.hasPermission("perplayerkit.editkitroom"));
            assertTrue(player.hasPermission("perplayerkit.staff"));
            assertFalse(player.hasPermission("perplayerkit.menu"));
            player.addAttachment(plugin, "perplayerkit.use", true);
            assertTrue(player.hasPermission("perplayerkit.menu"));
            assertFalse(player.hasPermission("perplayerkit.kitnotify"));
            player.addAttachment(plugin, "perplayerkit.kitnotify", true);
            assertTrue(player.hasPermission("perplayerkit.kitnotify"));
            player.addAttachment(plugin, "perplayerkit.kitnotify", false);
            assertFalse(player.hasPermission("perplayerkit.kitnotify"));
            ServerOperator op = mock(ServerOperator.class);
            when(op.isOp()).thenReturn(true);
            PermissibleBase operator = new PermissibleBase(op);
            assertTrue(operator.hasPermission("perplayerkit.kitnotify"));
            assertTrue(player.hasPermission("perplayerkit.deleteenderchest"));
            player.addAttachment(plugin, "perplayerkit.enderchest", false);
            assertFalse(player.hasPermission("perplayerkit.enderchest"));
        }
    }
}
