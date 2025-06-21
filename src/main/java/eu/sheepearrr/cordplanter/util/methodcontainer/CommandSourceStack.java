package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.util.MethodContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommandSourceStack implements BasicMethodContainer {
    public io.papermc.paper.command.brigadier.CommandSourceStack stack;
    public MethodContext context;

    @Override
    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                case "with_location" -> this::returnThis;
                default -> BasicMethodContainer.super.getExpression(obj);
            };
        }
        return this::returnThis;
    }

    public boolean withExecuter(JsonArray args) {
        /* TODO */
        return true;
    }

    public boolean withLocation(JsonArray args) {
        /* TODO: Redo this because this is quite frankly useless and very badly written (like everything else I made but that's besides the point). IT DOESN'T EVEN RETURN ANYTHING BUT A BOOLEAN?!  */
        if (args.get(0).isJsonObject()) {
            Location loc = this.stack.getLocation();
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

    @Override
    public Object returnThis(JsonArray args) {
        return this.stack;
    }
}
