package eu.sheepearrr.cordplanter.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class MethodContext {
    public Map<String, Object> props = new HashMap<>();
    public final List<JsonElement> commands;
    private int currentLine;

    public MethodContext(List<JsonElement> commands, Map<String, Object> props) {
        this.commands = commands;
        this.props = props;
    }

    public boolean setVariableTo(String name, Object value) {
        this.props.put(name.startsWith("{<") ? name.substring(2) : name, value);
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

    public CommandContext<CommandSourceStack> getCommandContext() {
        return (CommandContext<CommandSourceStack>) this.props.get("{<context");
    }

    public Object getArgument(String name) {
        return this.getCommandContext().getArgument(name, CordPlanterBootstrap.argumentTypeOutputs.get(((Map<String, String>) this.props.get("{<arguments")).get(name)));
    }

    public boolean requires(CommandSourceStack stack) {
        currentLine = 0;
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "return" -> {
                    return (boolean) getExpression(obj.get("value").getAsJsonObject()).apply(obj.get("value").getAsJsonObject().getAsJsonArray("arguments"));
                }
                case "method" -> getExpression(obj).apply(obj.getAsJsonArray("arguments"));
            }
            currentLine++;
        }
        return true;
    }

    private void schedule(JsonObject obj) {
        Bukkit.getScheduler().runTaskLater(CordPlanter.INSTANCE, () -> {
            for (JsonElement element : obj.get("commands").getAsJsonArray()) {
                JsonObject commandObj = element.getAsJsonObject();
                switch (commandObj.get("type").getAsString()) {
                    case "method" -> getExpression(commandObj).apply(commandObj.getAsJsonArray("arguments"));
                    case "schedule" -> schedule(commandObj);
                }
            }
        }, obj.get("delay").getAsLong());
    }

    public int executes(CommandContext<CommandSourceStack> context) {
        currentLine = 0;
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "return" -> {
                    return (int) getExpression(obj.get("value").getAsJsonObject()).apply(obj.get("value").getAsJsonObject().getAsJsonArray("arguments"));
                }
                case "method" -> getExpression(obj).apply(obj.getAsJsonArray("arguments"));
                case "schedule" -> schedule(obj);
            }
            currentLine++;
        }
        return Command.SINGLE_SUCCESS;
    }

    public void resourceCallback() {
        currentLine = 0;
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "method" -> getExpression(obj).apply(obj.getAsJsonArray("arguments"));
                case "schedule" -> schedule(obj);
            }
            currentLine++;
        }
    }

    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("from") && props.get(obj.get("from").getAsString()) != null) {
            return switch (props.get(obj.get("from").getAsString())) {
                case Player player -> new eu.sheepearrr.cordplanter.util.methodcontainer.Player(player, this).getExpression(obj);
                case CommandSender sender -> new eu.sheepearrr.cordplanter.util.methodcontainer.CommandSender(sender, this).getExpression(obj);
                default -> null;
            };
        }
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                case "get_variable" -> (args -> this.getVariable(args.get(0).getAsString()));
                case "get_internal_variable" -> (args -> this.getInternalVariable(args.get(0).getAsString()));
                case "set_variable" -> (args -> this.setVariableTo(args.get(0).getAsString(), args.get(1)));
                case "set_internal_variable" -> (args -> this.setInternalVariableTo(args.get(0).getAsString(), args.get(1)));
                case "get_argument" -> (args -> this.getArgument(args.get(0).getAsString()));
                case "load_line" -> (args -> {
                    int line = args.get(0).getAsInt();
                    if (line != this.currentLine && this.commands.get(line).getAsJsonObject().has("method") && !this.commands.get(line).getAsJsonObject().get("method").getAsString().equals("load_line")) {
                        this.getExpression(this.commands.get(line).getAsJsonObject());
                        return true;
                    }
                    return false;
                });
                case "random_uuid" -> (args -> UUID.randomUUID());
                default -> null;
            };
        }
        return null;
    }
}