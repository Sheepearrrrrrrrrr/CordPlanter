package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import eu.sheepearrr.cordplanter.util.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

public class Player extends CommandSender {
    public final org.bukkit.entity.Player player;

    public Player(org.bukkit.entity.Player player, MethodContext context) {
        super(player, context);
        this.player = player;
    }

    @Override
    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                default -> super.getExpression(obj);
            };
        }
        return this::returnThis;
    }

    @Override
    public Object returnThis(JsonArray args) {
        return this.player;
    }

    @Override
    public String name(JsonArray args) {
        return this.player.getName();
    }
}
