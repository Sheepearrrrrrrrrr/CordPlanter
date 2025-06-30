package eu.sheepearrr.cordplanter.method;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import eu.sheepearrr.cordplanter.util.GsonUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Function;

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
    
    public Object removeVariable(String name) {
        return this.props.remove(name);
    }
    
    public Object removeInternalVariable(String name) {
        return CordPlanterBootstrap.INSTANCE.internalVariables.remove(name);
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
                case "condition" -> condition(obj);
                case "method" -> getExpression(obj).apply(obj.has("arguments") && obj.get("arguments").isJsonArray() ? obj.getAsJsonArray("arguments") : new JsonArray());
            }
            currentLine++;
        }
        return true;
    }

    public Object getValue(JsonElement value) {
        if (value instanceof JsonPrimitive primitive) {
            if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.getAsNumber().doubleValue() % 1 == 0) {
                return primitive.getAsInt();
            }
            return primitive.getAsDouble();
        } else if (value instanceof JsonObject object) {
            if (object.has("type") && object.get("type").getAsString().equals("method")) {
                return this.getExpression(object).apply(object.has("arguments") ? object.get("arguments").getAsJsonArray() : new JsonArray());
            }
            return object;
        } else if (value instanceof JsonArray array) {
            List<Object> newList = new ArrayList<>();
            for (JsonElement object : array) {
                newList.add(getValue(object));
            }
            return newList;
        }
        return null;
    }

    private void schedule(JsonObject obj) {
        Bukkit.getScheduler().runTaskLater(CordPlanter.INSTANCE, () -> {
            for (JsonElement element : obj.get("commands").getAsJsonArray()) {
                JsonObject commandObj = element.getAsJsonObject();
                switch (commandObj.get("type").getAsString()) {
                    case "method" -> getExpression(commandObj).apply(commandObj.has("arguments") && commandObj.get("arguments").isJsonArray() ? commandObj.getAsJsonArray("arguments") : new JsonArray());
                    case "schedule" -> schedule(commandObj);
                    case "condition" -> condition(commandObj);
                }
            }
        }, obj.get("delay").getAsLong());
    }

    private void condition(JsonObject object) {
        if ((boolean) this.getExpression(object.get("condition").getAsJsonObject()).apply(object.get("condition").getAsJsonObject().get("arguments").getAsJsonArray())) {
            for (JsonElement element : object.get("then").getAsJsonArray()) {
                JsonObject commandObj = element.getAsJsonObject();
                switch (commandObj.get("type").getAsString()) {
                    case "method" -> getExpression(commandObj).apply(commandObj.has("arguments") && commandObj.get("arguments").isJsonArray() ? commandObj.getAsJsonArray("arguments") : new JsonArray());
                    case "schedule" -> schedule(commandObj);
                    case "condition" -> condition(commandObj);
                }
            }
        } else if (object.has("else")) {
            for (JsonElement element : object.get("else").getAsJsonArray()) {
                JsonObject commandObj = element.getAsJsonObject();
                switch (commandObj.get("type").getAsString()) {
                    case "method" -> getExpression(commandObj).apply(commandObj.has("arguments") && commandObj.get("arguments").isJsonArray() ? commandObj.getAsJsonArray("arguments") : new JsonArray());
                    case "schedule" -> schedule(commandObj);
                    case "condition" -> condition(commandObj);
                }
            }
        }
    }

    public int executes(CommandContext<CommandSourceStack> context) {
        currentLine = 0;
        for (JsonElement element : commands) {
            JsonObject obj = element.getAsJsonObject();
            switch (obj.get("type").getAsString()) {
                case "return" -> {
                    return (int) getExpression(obj.get("value").getAsJsonObject()).apply(obj.get("value").getAsJsonObject().getAsJsonArray("arguments"));
                }
                case "method" -> getExpression(obj).apply(obj.has("arguments") && obj.get("arguments").isJsonArray() ? obj.getAsJsonArray("arguments") : new JsonArray());
                case "schedule" -> schedule(obj);
                case "condition" -> condition(obj);
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
                case "method" -> getExpression(obj).apply(obj.has("arguments") && obj.get("arguments").isJsonArray() ? obj.getAsJsonArray("arguments") : new JsonArray());
                case "schedule" -> schedule(obj);
                case "condition" -> condition(obj);
            }
            currentLine++;
        }
    }

    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("from") && props.get(obj.get("from").getAsString()) != null) {
            return switch (props.get(obj.get("from").getAsString())) {
                case Player player -> new eu.sheepearrr.cordplanter.method.container.Player(player, this).getExpression(obj);
                case CommandSender sender -> new eu.sheepearrr.cordplanter.method.container.CommandSender(sender, this).getExpression(obj);
                case CommandSourceStack stack -> new eu.sheepearrr.cordplanter.method.container.CommandSourceStack(stack, this).getExpression(obj);
                default -> null;
            };
        }
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                case "get_variable" -> (args -> this.getVariable(args.get(0).getAsString()));
                case "get_internal_variable" -> (args -> this.getInternalVariable(args.get(0).getAsString()));
                case "set_variable" -> (args -> args.get(1) instanceof JsonObject object && object.has("is_expression") && object.get("is_expression").getAsBoolean()
                        ? this.setVariableTo(args.get(0).getAsString(), this.getExpression(object).apply(object.get("arguments").getAsJsonArray()))
                        : this.setVariableTo(args.get(0).getAsString(), GsonUtils.getValue(args.get(1))));
                case "set_internal_variable" -> (args -> args.get(1) instanceof JsonObject object && object.has("is_expression") && object.get("is_expression").getAsBoolean()
                        ? this.setInternalVariableTo(args.get(0).getAsString(), this.getExpression(object).apply(object.get("arguments").getAsJsonArray()))
                        : this.setInternalVariableTo(args.get(0).getAsString(), GsonUtils.getValue(args.get(1))));
                case "get_argument" -> (args -> this.getArgument(args.get(0).getAsString()));
                case "load_line" -> (args -> {
                    int line = (int) this.getValue(args.get(0));
                    if (this.commands.get(line).getAsJsonObject().has("method") && !this.commands.get(line).getAsJsonObject().get("method").getAsString().equals("load_line")) {
                        this.getExpression(this.commands.get(line).getAsJsonObject());
                        return true;
                    }
                    return false;
                });
                case "random_uuid" -> (args -> UUID.randomUUID());
                case "is_equal" -> (args -> {
                    Object first = getValue(args.get(0));
                    Object second = getValue(args.get(1));
                    return first.equals(second);
                });
                case "ternary_condition" -> (args -> {
                    JsonObject object = args.get(0).getAsJsonObject();
                    if ((boolean) this.getExpression(object.get("condition").getAsJsonObject()).apply(object.get("condition").getAsJsonObject().get("arguments").getAsJsonArray())) {
                        return this.getValue(object.get("then"));
                    } else {
                        return this.getValue(args.get(1));
                    }
                });
                default -> null;
            };
        }
        return null;
    }
}