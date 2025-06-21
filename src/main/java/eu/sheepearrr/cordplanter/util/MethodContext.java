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
    @Nullable
    private CommandSourceStack lastStack;

    public MethodContext(List<JsonElement> commands, Map<String, Object> props) {
        this.commands = commands;
        this.props = props;
    }

    public void setVariableTo(String name, Object value) {
        this.props.put(name, value);
    }

    public Object getVariable(String name) {
        return this.props.get(name);
    }

    public void setInternalVariableTo(String name, Object value) {
        CordPlanterBootstrap.INSTANCE.internalVariables.put(name, value);
    }

    public Object getInternalVariable(String name) {
        return CordPlanterBootstrap.INSTANCE.internalVariables.get(name);
    }

    public boolean requires(CommandSourceStack stack) {
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "return" -> {
                    return (boolean) getExpression(obj.get("value").getAsJsonObject(), stack).apply(obj.get("value").getAsJsonObject().getAsJsonArray("args"));
                }
                case "method" -> {
                    getExpression(obj, stack).apply(obj.getAsJsonArray("args"));
                }
                default -> {}
            }
        }
        return true;
    }

    public int executes(CommandContext<CommandSourceStack> stack) {
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "return" -> {
                    return (int) getExpression(obj.get("value").getAsJsonObject(), stack.getSource()).apply(obj.get("value").getAsJsonObject().getAsJsonArray("args"));
                }
                case "method" -> getExpression(obj, stack.getSource()).apply(obj.getAsJsonArray("args"));
                default -> {}
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    public Function<JsonArray, Object> getExpression(JsonObject obj, CommandSourceStack stack) {
        if (stack != null) {
            this.lastStack = stack;
        }
        if (props.get(obj.get("from").getAsString()) != null) {
            return switch (props.get(obj.get("from").getAsString())) {
                case Player player -> new eu.sheepearrr.cordplanter.util.methodcontainer.Player(player, this).getExpression(obj);
                case CommandSender sender -> new eu.sheepearrr.cordplanter.util.methodcontainer.CommandSender(sender, this).getExpression(obj);
                default -> null;
            };
        }
        if (obj.has("method")) {
            return switch (obj.get("method").toString()) {
                case "with_executer" -> this::withExecuter;
                case "with_location" -> this::withLocation;
                default -> null;
            };
        }
        return null;
    }

    public boolean withExecuter(JsonArray args) {
        /* TODO */
        return true;
    }

    public boolean withLocation(JsonArray args) {
        if (args.get(0).isJsonObject() && this.lastStack != null) {
            Location loc = this.lastStack.getLocation();
            JsonObject obj = args.get(0).getAsJsonObject();
            if (obj.has("world")) {
                loc.setWorld(Bukkit.getWorld(obj.get("world").getAsString()));
            }
            if (obj.has("pos")) {
                if (obj.get("pos").isJsonObject()) {
                    JsonObject posObj = obj.getAsJsonObject("pos");
                    if (posObj.has("x")) {
                        loc.setX(posObj.get("x").getAsDouble());
                    }
                    if (posObj.has("y")) {
                        loc.setX(posObj.get("y").getAsDouble());
                    }
                    if (posObj.has("z")) {
                        loc.setX(posObj.get("z").getAsDouble());
                    }
                } else if (obj.get("pos").isJsonPrimitive() && obj.get("pos").getAsJsonPrimitive().isString()) {
                    String str = obj.get("pos").getAsString();
                    List<Double> positions = new ArrayList<>();
                    StringBuilder prevPos = new StringBuilder();
                    for (int i = 0; i < str.length(); i++) {
                        if (str.charAt(i) == ' ') {
                            positions.add(Double.valueOf(prevPos.toString()));
                            continue;
                        }
                        if (str.charAt(i) == '#') {
                            positions.add(
                                    switch (positions.size()) {
                                        case 0 -> loc.getX();
                                        case 1 -> loc.getY();
                                        case 2 -> loc.getZ();
                                        default -> 0.0;
                                    }
                            );
                        }
                        prevPos.append(str.charAt(i));
                    }
                }
            }
            if (obj.has("rot")) {
                if (obj.get("rot").isJsonObject()) {
                    JsonObject rotObj = obj.getAsJsonObject("rot");
                    if (rotObj.has("yaw")) {
                        loc.setYaw(rotObj.get("yaw").getAsFloat());
                    }
                    if (rotObj.has("pitch")) {
                        loc.setPitch(rotObj.get("pitch").getAsFloat());
                    }
                } else if (obj.get("rot").isJsonPrimitive() && obj.get("rot").getAsJsonPrimitive().isString()) {
                    String str = obj.get("rot").getAsString();
                    List<Float> rots = new ArrayList<>();
                    StringBuilder prevChar = new StringBuilder();
                    for (int i = 0; i < str.length(); i++) {
                        if (str.charAt(i) == ' ') {
                            rots.add(Float.valueOf(prevChar.toString()));
                            continue;
                        }
                        if (str.charAt(i) == '#') {
                            rots.add(
                                    switch (rots.size()) {
                                        case 0 -> loc.getYaw();
                                        case 1 -> loc.getPitch();
                                        default -> 0.0F;
                                    }
                            );
                        }
                        prevChar.append(str.charAt(i));
                    }
                }
            }
            return true;
        }
        return false;
    }
}