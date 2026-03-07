package dev.noah.perplayerkit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownManagerTest {

    @Test
    void keyIsNotOnCooldownBeforeSet() {
        CooldownManager cooldownManager = new CooldownManager(1);

        assertFalse(cooldownManager.isOnCooldown("alpha"));
    }

    @Test
    void keyIsOnCooldownImmediatelyAfterSet() {
        CooldownManager cooldownManager = new CooldownManager(1);

        cooldownManager.setCooldown("alpha");

        assertTrue(cooldownManager.isOnCooldown("alpha"));
    }

    @Test
    void keyExpiresAfterCooldownWindow() throws InterruptedException {
        CooldownManager cooldownManager = new CooldownManager(1);

        cooldownManager.setCooldown("alpha");
        Thread.sleep(1200);

        assertFalse(cooldownManager.isOnCooldown("alpha"));
    }
}
