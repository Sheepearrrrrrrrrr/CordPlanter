package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;

import java.util.function.Function;

public interface BasicMethodContainer {
    default Function<JsonArray, Boolean> getExpression(JsonObject obj) {
        CordPlanter.LOGGER.warn("Couldn't find returning expression: \"" + obj.get("method").getAsString() + "\"");
        return null;
    }

    default Function<JsonArray, Object> getReturningExpression(JsonObject obj) {
        CordPlanter.LOGGER.warn("Couldn't find returning expression: \"" + obj.get("method").getAsString() + "\"");
        return null;
    }
}
