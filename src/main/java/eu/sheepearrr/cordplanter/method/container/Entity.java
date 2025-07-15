package eu.sheepearrr.cordplanter.method.container;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.method.MethodContext;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Entity extends CommandSender {
    public org.bukkit.entity.Entity entity;
    public MethodContext context;

    @Override
    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                default -> super.getExpression(obj);
            };
        }
        return this::returnThis;
    }

    public boolean saveInData(JsonArray args) {
        String path = args.get(0).getAsString();
        List<String> pathToStuff = new ArrayList<>();
        StringBuilder prev = new StringBuilder();
        var value = context.getValue(args.get(1));
        if (path.contains("%")) {
            for (int i = 0; i < path.length(); i++) {
                if (path.charAt(i) == '%') {
                    pathToStuff.add(prev.toString());
                    prev = new StringBuilder();
                    continue;
                }
                prev.append(path.charAt(i));
            }
        } else {
            pathToStuff = List.of(path);
        }
        int i = 0;
        PersistentDataContainer prevContainer = entity.getPersistentDataContainer();
        for (String cont : pathToStuff) {
            if (i + 1 >= pathToStuff.size()) {
                /* TODO: FINISH THIS */
                switch (value) {
                    case Integer in -> prevContainer.set(new NamespacedKey(context.namespace == null ? CordPlanter.INSTANCE.getName() : context.namespace, cont), PersistentDataType.INTEGER, in);
                    case String st -> prevContainer.set(new NamespacedKey(context.namespace == null ? CordPlanter.INSTANCE.getName() : context.namespace, cont), PersistentDataType.STRING, st);
                    default -> {}
                }
                continue;
            }
            prevContainer = prevContainer.get(new NamespacedKey(context.namespace == null ? CordPlanter.INSTANCE.getName() : context.namespace, cont), PersistentDataType.TAG_CONTAINER);
            i++;
        }
        return true;
    }

    public Entity(org.bukkit.entity.Entity entity, MethodContext context) {
        super(entity, context);
        this.entity = entity;
        this.context = context;
    }

    @Override
    public Object returnThis(JsonArray args) {
        return this.entity;
    }
}
