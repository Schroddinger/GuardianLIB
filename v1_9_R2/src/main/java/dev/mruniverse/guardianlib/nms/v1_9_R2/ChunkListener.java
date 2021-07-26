package dev.mruniverse.guardianlib.nms.v1_9_R2;

import dev.mruniverse.guardianlib.core.GuardianLIB;
import dev.mruniverse.guardianlib.core.events.GuardianChunkUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkListener implements Listener {
    private final GuardianLIB guardianLIB;

    public ChunkListener(GuardianLIB guardianLIB) {
        this.guardianLIB = guardianLIB;
    }

    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {

        GuardianChunkUnloadEvent unloadEvent = new GuardianChunkUnloadEvent(event.getChunk(),event.getWorld());
        guardianLIB.getServer().getPluginManager().callEvent(unloadEvent);
        if(unloadEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }
}
