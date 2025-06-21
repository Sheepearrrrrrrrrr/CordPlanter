package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import eu.sheepearrr.cordplanter.util.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.function.Function;

public class CommandSender extends Audience {
    public final org.bukkit.command.CommandSender sender;

    public CommandSender(org.bukkit.command.CommandSender sender, MethodContext mContext) {
        super(sender, mContext);
        this.sender = sender;
    }

    @Override
    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                case "is_op" -> this::isOp;
                case "set_op" -> this::setOp;
                case "get_name" -> this::name;
                default -> super.getExpression(obj);
            };
        }
        return this::returnThis;
    }

    @Override
    public Object returnThis(JsonArray args) {
        return this.sender;
    }

    public boolean setOp(JsonArray args) {
        boolean opStatus = this.sender.isOp();
        if (CordPlanterBootstrap.INSTANCE.settings.get("allow_giving_operator_status")) {
            this.sender.setOp(args.get(0).getAsBoolean());
        }
        return opStatus;
    }

    public boolean isOp(JsonArray args) {
        return this.sender.isOp();
    }

    public String name(JsonArray args) {
        return this.sender.getName();
    }
}
