package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.util.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.function.Function;

public record CommandSender(org.bukkit.command.CommandSender sender, MethodContext context) implements BasicMethodContainer {
    @Override
    public Function<JsonArray, Boolean> getExpression(JsonObject obj) {
        return switch (obj.get("method").getAsString()) {
            case "send_message" -> this::sendMessage;
            case "is_op" -> this::isOp;
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

    public boolean sendMessage(JsonArray args) {
        this.sender.sendMessage(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false));
        return true;
    }

    public boolean setOp(JsonArray args) {
        boolean opStatus = this.sender.isOp();
        this.sender.setOp(args.get(0).getAsBoolean());
        return opStatus;
    }

    public boolean isOp(JsonArray args) {
        return this.sender.isOp();
    }

    public String name(JsonArray args) {
        return this.sender.getName();
    }
}
