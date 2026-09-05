package dev.noah.perplayerkit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class KitRoomPagesTest {
    @Test void existingConfigsKeepFivePages() {
        assertEquals(5, KitRoomDataManager.configuredPages(new YamlConfiguration()));
    }
    @ParameterizedTest @ValueSource(ints={1,5,6,10,99})
    void acceptsSupportedPageCounts(int count) {
        var config = new YamlConfiguration(); config.set("kitroom.pages", count);
        assertEquals(count, KitRoomDataManager.configuredPages(config));
    }
    @ParameterizedTest @ValueSource(strings={"0", "100", "-1", "five", "1.5", "true"})
    void rejectsInvalidPageCounts(String value) throws Exception {
        var config = new YamlConfiguration(); config.loadFromString("kitroom:\n  pages: " + value + "\n");
        assertThrows(IllegalArgumentException.class, () -> KitRoomDataManager.configuredPages(config));
    }
}
