package org.bitwisemadness.railwaysunbound;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.bitwisemadness.railwaysunbound.railways.RailwaysUntoldBridge;
import org.slf4j.Logger;

/**
 * Railways Unbound entry point.
 *
 * <p>Every option tunes Railways Untold's procedural track worldgen: the
 * dimension gate vetoes its login bootstrap, the biome filter feeds its
 * avoidance API, and the generator claim opens whitelisted dimensions. All
 * hooks register once at construction and read the live server config on
 * every query, so an edit applies on reload without a restart.</p>
 */
@Mod(RailwaysUnbound.MOD_ID)
public final class RailwaysUnbound {

    public static final String MOD_ID = "railways_unbound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RailwaysUnbound(IEventBus modBus, ModContainer container) {
        RailwaysUnboundConfigs.register(container);
        modBus.addListener(RailwaysUnbound::onConfigLoading);
        modBus.addListener(RailwaysUnbound::onConfigReloading);
        RailwaysUntoldBridge.registerHooks();
        LOGGER.info("Railways Unbound initialized (Railways Untold: {})",
                RailwaysUntoldBridge.isPresent());
    }

    /** Surfaces degenerate-but-valid configs loudly at first load. */
    private static void onConfigLoading(ModConfigEvent.Loading event) {
        warnFor(event);
    }

    /** Same warning path on {@code /reload}, without restarting the server. */
    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        warnFor(event);
    }

    private static void warnFor(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(MOD_ID)) {
            RailwaysUnboundConfigs.warnOnDegenerateCombos();
        }
    }
}
