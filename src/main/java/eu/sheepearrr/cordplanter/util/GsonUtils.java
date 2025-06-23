package eu.sheepearrr.cordplanter.util;

import com.google.gson.*;

public class GsonUtils {
    public static Object getValue(JsonElement element) {
        if (element instanceof JsonObject object) {
            return object;
        }
        if (element instanceof JsonArray array) {
            return array;
        }
        if (element instanceof JsonNull jsonNull) {
            return jsonNull;
        }
        if (element instanceof JsonPrimitive primitive) {
            if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else {
                return primitive.getAsBoolean();
            }
        }
        return element;
    }
}
