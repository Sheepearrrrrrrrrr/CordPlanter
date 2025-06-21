package eu.sheepearrr.cordplanter;

import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class CordPlanterListener implements Listener {
    public CordPlanterListener() {
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && CordPlanterBootstrap.INSTANCE.settings.get("allow_granting_operator_status")) {
            CordPlanter.LOGGER.warn("\n===========================================================================================================================================================\n\n!!! CRITICAL WARNING !!!\n\nCordPlanter was configured to grant workspaces the power to GRANT OPERATOR STATUS to any Player, etc. This can be used as a force-op exploit, even through harmless looking stuff like text replacement.\nOnly enable this option if you really need it.\nPlease run \"workspace settings reset allow_granting_operator_status\" to reset this option.\n\n===========================================================================================================================================================");
            player.sendMessage(Component.text("!!! CRITICAL WARNING !!!\n\nPlease check console for further instructions and the reason why this message got sent to you.").color(TextBuilder.presetColors.get("red")));
        }
    }
}
