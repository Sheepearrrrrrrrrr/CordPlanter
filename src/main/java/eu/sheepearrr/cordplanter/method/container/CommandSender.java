package eu.sheepearrr.cordplanter.method.container;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import eu.sheepearrr.cordplanter.method.MethodContext;

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
        if (CordPlanterBootstrap.INSTANCE.settings.get("allow_granting_operator_status")) {
            this.sender.setOp((boolean) this.context.getValue(args.get(0)));
        } else {
            CordPlanter.LOGGER.warn("\n===========================================================================================================================================================\n\n!!! WARNING !!!\n\nA CordPlanter workspace tried to grant/take away OPERATOR STATUS to/from a player, but failed due to the restrictions configured in the plugin settings.\nPlease for your own safety look through every workspace you have applied, even in text, as text replacements in the right context can trigger this.\n    - Sheepearrr, owner of CordPlanter\n\n===========================================================================================================================================================\nTranslations: https://github.com/Sheepearrrrrrrrrr/Data/tree/main/cordplanter/translations/unauthorized_operator_status_warning");
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
