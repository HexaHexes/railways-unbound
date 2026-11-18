package org.bitwisemadness.railwaysunbound.railways;

import net.neoforged.fml.ModList;
import org.bitwisemadness.railwaysunbound.RailwaysUnbound;

/**
 * Presence gate and failure damper for Railways Untold. Contains no Railways
 * Untold types: when it is absent, the JVM never loads
 * {@link RailwaysUntoldAccess}, so no missing-class error can fire.
 */
public final class RailwaysUntoldBridge {

    private RailwaysUntoldBridge() {
    }

    /** True when Railways Untold is loaded. */
    public static boolean isPresent() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("railwaysuntold");
    }

    /**
     * Registers every Railways Untold hook this addon owns: the biome
     * avoidance provider, the dimension generator claim and the chunk-load
     * dimension extension. A no-op without Railways Untold; a registration
     * failure degrades to inert, never to a crash.
     */
    public static void registerHooks() {
        if (!isPresent()) {
            RailwaysUnbound.LOGGER.info("Railways Untold absent: every option stays inert");
            return;
        }
        try {
            RailwaysUntoldAccess.registerHooks();
        } catch (Throwable failure) {
            RailwaysUnbound.LOGGER.error("Railways Untold is present but hook "
                    + "registration failed; tuning stays inert and the game continues",
                    failure);
        }
    }
}
