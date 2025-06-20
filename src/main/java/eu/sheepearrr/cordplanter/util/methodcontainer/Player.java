package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.util.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

public record Player(org.bukkit.entity.Player player, MethodContext context) implements BasicMethodContainer {

    @Override
    public Function<JsonArray, Boolean> getExpression(JsonObject obj) {
        return switch (obj.get("method").getAsString()) {
            case "is_op" -> this::isOp;
            case "send_message" -> this::sendMessage;
            case "set_op" -> this::setOp;
            default -> BasicMethodContainer.super.getExpression(obj);
        };
    }

    @Override
    public Function<JsonArray, Object> getReturningExpression(JsonObject obj) {
        return switch (obj.get("method").getAsString()) {
            case "name" -> this::name;
            default -> BasicMethodContainer.super.getReturningExpression(obj);
        };
    }

    public boolean isOp(JsonArray args) {
        return this.player.isOp();
    }

    public boolean sendMessage(JsonArray args) {
        this.player.sendMessage(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false));
        return true;
    }

    public boolean setOp(JsonArray args) {
        boolean opStatus = this.player.isOp();
        this.player.setOp(args.get(0).getAsBoolean());
        return opStatus;
    }

    public String name(JsonArray args) {
        return this.player.getName();
    }
}
