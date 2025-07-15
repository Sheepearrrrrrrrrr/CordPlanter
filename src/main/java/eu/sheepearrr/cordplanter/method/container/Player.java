package eu.sheepearrr.cordplanter.method.container;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.method.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;

import java.util.function.Function;

public class Player extends Entity {
    public final org.bukkit.entity.Player player;

    public Player(org.bukkit.entity.Player player, MethodContext context) {
        super(player, context);
        this.player = player;
    }

    @Override
    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                case "perform_command" -> this::performCommand;
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

    public boolean performCommand(JsonArray args) {
        return this.player.performCommand(TextBuilder.textReplacement(new JsonObject(), (String) this.context.getValue(args.get(0)), this.context));
    }
}
