package eu.sheepearrr.cordplanter.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class MethodContext {
    public Map<String, Object> props = new HashMap<>();
    public final List<JsonElement> commands;

    public MethodContext(List<JsonElement> commands, Map<String, Object> props) {
        this.commands = commands;
        this.props = props;
    }

    public boolean setVariableTo(String name, Object value) {
        this.props.put(name, value);
        return true;
    }

    public Object getVariable(String name) {
        return this.props.get(name);
    }

    public boolean setInternalVariableTo(String name, Object value) {
        CordPlanterBootstrap.INSTANCE.internalVariables.put(name, value);
        return true;
    }

    public Object getInternalVariable(String name) {
        return CordPlanterBootstrap.INSTANCE.internalVariables.get(name);
    }

    public boolean requires(CommandSourceStack stack) {
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "return" -> {
                    return (boolean) getExpression(obj.get("value").getAsJsonObject()).apply(obj.get("value").getAsJsonObject().getAsJsonArray("args"));
                }
                case "method" -> {
                    getExpression(obj).apply(obj.getAsJsonArray("args"));
                }
                default -> {}
            }
        }
        return true;
    }

    public int executes(CommandContext<CommandSourceStack> context) {
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "return" -> {
                    return (int) getExpression(obj.get("value").getAsJsonObject()).apply(obj.get("value").getAsJsonObject().getAsJsonArray("args"));
                }
                case "method" -> getExpression(obj).apply(obj.getAsJsonArray("args"));
                default -> {}
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (props.get(obj.get("from").getAsString()) != null) {
            return switch (props.get(obj.get("from").getAsString())) {
                case Player player -> new eu.sheepearrr.cordplanter.util.methodcontainer.Player(player, this).getExpression(obj);
                case CommandSender sender -> new eu.sheepearrr.cordplanter.util.methodcontainer.CommandSender(sender, this).getExpression(obj);
                default -> null;
            };
        }
        if (obj.has("method")) {
            return switch (obj.get("method").toString()) {
                case "get_variable" -> (args -> this.getVariable(args.get(0).getAsString()));
                case "get_internal_variable" -> (args -> this.getInternalVariable(args.get(0).getAsString()));
                case "set_variable" -> (args -> this.setVariableTo(args.get(0).getAsString(), args.get(1)));
                case "set_internal_variable" -> (args -> this.setInternalVariableTo(args.get(0).getAsString(), args.get(1)));
                default -> null;
            };
        }
        return null;
    }
}