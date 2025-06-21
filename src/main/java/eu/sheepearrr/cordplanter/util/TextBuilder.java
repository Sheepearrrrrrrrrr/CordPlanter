package eu.sheepearrr.cordplanter.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TextBuilder {
    public static final Map<String, TextColor> presetColors = Map.ofEntries(
            Map.entry("black", TextColor.color(0, 0, 0)),
            Map.entry("dark_blue", TextColor.color(0, 0, 170)),
            Map.entry("dark_green", TextColor.color(0, 170, 0)),
            Map.entry("dark_aqua", TextColor.color(0, 170, 170)),
            Map.entry("dark_red", TextColor.color(170, 0, 0)),
            Map.entry("dark_purple", TextColor.color(170, 0, 170)),
            Map.entry("gold", TextColor.color(255, 170, 0)),
            Map.entry("gray", TextColor.color(170, 170, 170)),
            Map.entry("dark_gray", TextColor.color(85, 85, 85)),
            Map.entry("blue", TextColor.color(85, 85, 255)),
            Map.entry("green", TextColor.color(85, 255, 85)),
            Map.entry("aqua", TextColor.color(85, 255, 255)),
            Map.entry("red", TextColor.color(255, 85, 85)),
            Map.entry("light_purple", TextColor.color(255, 85, 255)),
            Map.entry("yellow", TextColor.color(255, 255, 85)),
            Map.entry("white", TextColor.color(255, 255, 255))
    );

    public static Component jsonObjComp(JsonObject jsonObj, @Nullable MethodContext mContext) {
        Component comp;
        if (jsonObj.has("text")) {
            String text = jsonObj.get("text").getAsString();
            if (text.contains("$#") && mContext != null && CordPlanterBootstrap.INSTANCE.settings.get("allow_text_replacement")) {
                for (int index = text.indexOf("$#"); index >= 0; index = text.indexOf("$#", index + 1)) {
                    StringBuilder toReplace = new StringBuilder();
                    int end = text.length();
                    for (int i = index + 2; i < text.length(); i++) {
                        if ((String.valueOf(text.charAt(i)) + text.charAt(i + 1)).equals("#$")) {
                            end = i;
                            break;
                        }
                        toReplace.append(text.charAt(i));
                    }
                    List<String> idkfem = new ArrayList<>();
                    String idkidk = toReplace.toString();
                    StringBuilder prevThings = new StringBuilder();
                    boolean isParsingArgs = false;
                    JsonArray args = null;
                    for (int i = 0; i < idkidk.length(); i++) {
                        if (idkidk.charAt(i) == '[') {
                            isParsingArgs = true;
                        }
                        if (idkidk.charAt(i) == '&') {
                            idkfem.add(prevThings.toString());
                            prevThings = new StringBuilder();
                            continue;
                        }
                        prevThings.append(idkidk.charAt(i));
                        if (idkidk.charAt(i) == ']' && isParsingArgs) {
                            args = new Gson().fromJson(prevThings.toString(), JsonArray.class);
                            break;
                        }
                        if (i >= idkidk.length() - 1) {
                            idkfem.add(prevThings.toString());
                        }
                    }
                    JsonObject fakeAssThingy = new JsonObject();
                    if (idkfem.size() > 1) {
                        fakeAssThingy.addProperty("from", idkfem.get(0));
                        fakeAssThingy.addProperty("method", idkfem.get(1));
                    } else if (!idkfem.isEmpty()) {
                        String lalala = idkfem.getFirst();
                        if (lalala.endsWith("@")) {
                            fakeAssThingy.addProperty("from", lalala);
                        } else {
                            fakeAssThingy.addProperty("method", lalala);
                        }
                    } else {
                        parseError(jsonObj);
                        return Component.empty();
                    }
                    text = text.substring(0, index) + mContext.getExpression(fakeAssThingy, null).apply(args != null ? args : new JsonArray()).toString() + (end + 2 < text.length() ? text.substring(end + 2) : "") ;
                }
            }
            comp = Component.text(text);
        } else if (jsonObj.has("translate")) {
            comp = Component.translatable(jsonObj.get("translate").getAsString());
        } else if (jsonObj.has("keybind")) {
            comp = Component.keybind(jsonObj.get("keybind").getAsString());
        } else if (jsonObj.has("score")) {
            JsonObject obj = jsonObj.get("score").getAsJsonObject();
            comp = Component.score(obj.get("holder").getAsString(), obj.get("objective").getAsString());
        } else if (jsonObj.has("selector")) {
            if (jsonObj.get("selector").isJsonObject()) {
                comp = Component.selector(jsonObj.get("selector").getAsJsonObject().get("pattern").getAsString(), getComponentFromJsonElement(jsonObj.get("selector").getAsJsonObject().get("separator"), mContext, false));
            } else if (jsonObj.get("selector").isJsonPrimitive() && jsonObj.get("selector").getAsJsonPrimitive().isString()) {
                comp = Component.selector(jsonObj.get("selector").getAsString());
            } else {
                parseError(jsonObj);
                return Component.empty();
            }
        } else if (jsonObj.has("nbt")) {
            JsonObject obj = jsonObj.get("nbt").getAsJsonObject();
            String path = obj.get("path").getAsString();
            switch (obj.get("type").getAsString()) {
                case "entity" -> comp = Component.entityNBT(path, obj.get("selector").getAsString());
                case "block" -> {
                    String pos;
                    boolean hasInterpret = false;
                    boolean interpret = false;
                    Component separator = Component.empty();
                    boolean hasSeparator = false;
                    if (obj.get("pos").isJsonPrimitive() && obj.get("pos").getAsJsonPrimitive().isString()) {
                        pos = obj.get("pos").getAsString();
                    } else if (obj.get("pos").isJsonObject()) {
                        JsonObject posObj = obj.getAsJsonObject("pos");
                        pos = (String.valueOf(posObj.get("x").getAsInt()) + posObj.get("y").getAsInt()) + posObj.get("z").getAsInt();
                    } else {
                        parseError(jsonObj);
                        return Component.empty();
                    }
                    if (obj.has("interpret")) {
                        interpret = obj.get("interpret").getAsBoolean();
                        hasInterpret = true;
                    }
                    if (obj.has("separator")) {
                        separator = getComponentFromJsonElement(obj.get("separator"), mContext, false);
                        hasSeparator = true;
                    }
                    if (hasSeparator) {
                        comp = Component.blockNBT(path, interpret, separator, BlockNBTComponent.Pos.fromString(pos));
                    } else if (hasInterpret) {
                        comp = Component.blockNBT(path, interpret, BlockNBTComponent.Pos.fromString(pos));
                    } else {
                        comp = Component.blockNBT(path, BlockNBTComponent.Pos.fromString(pos));
                    }
                }
                case "storage" -> {
                    Key storage;
                    boolean hasInterpret = false;
                    boolean interpret = false;
                    Component separator = Component.empty();
                    boolean hasSeparator = false;
                    if (obj.get("key").isJsonObject()) {
                        JsonObject key = obj.get("key").getAsJsonObject();
                        storage = Key.key(key.get("namespace").getAsString(), key.get("value").getAsString());
                    } else if (obj.get("key").isJsonPrimitive() && obj.get("key").getAsJsonPrimitive().isString()) {
                        storage = Key.key(obj.get("key").getAsString());
                    } else {
                        parseError(jsonObj);
                        return Component.empty();
                    }
                    if (obj.has("interpret")) {
                        interpret = obj.get("interpret").getAsBoolean();
                        hasInterpret = true;
                    }
                    if (obj.has("separator")) {
                        separator = getComponentFromJsonElement(obj.get("separator"), mContext, false);
                        hasSeparator = true;
                    }
                    if (hasSeparator) {
                        comp = Component.storageNBT(path, interpret, separator, storage);
                    } else if (hasInterpret) {
                        comp = Component.storageNBT(path, interpret, storage);
                    } else {
                        comp = Component.storageNBT(path, storage);
                    }
                }
                default -> {
                    parseError(jsonObj);
                    return Component.empty();
                }
            }
            /*
            * TYPES:
            * - ENTITY
            *   - NBT PATH
            *   - SELECTOR
            * - BLOCK
            *   - NBT PATH
            *   - POS
            *   - INTERPRET
            *   - SEPARATOR
            * - STORAGE
            *   - NBT PATH
            *   - STORAGE KEY
            *   - INTERPRET
            *   - SEPARATOR
            */
        } else {
            parseError(jsonObj);
            return Component.empty();
        }
        if (jsonObj.has("color")) {
            JsonElement colorEl = jsonObj.get("color");
            int r;
            int g;
            int b;
            if (colorEl instanceof JsonObject colorObj) {
                if (colorObj.has("type")) {
                    if (colorObj.get("type").getAsString().equalsIgnoreCase("rgb")) {
                        r = colorObj.get("r").getAsInt();
                        g = colorObj.get("g").getAsInt();
                        b = colorObj.get("b").getAsInt();
                    } else if (colorObj.get("type").getAsString().equalsIgnoreCase("hsv")) {
                        float hue = ((float) colorObj.get("h").getAsInt()) / 360.0F;
                        float saturation = ((float) colorObj.get("s").getAsInt()) / 100.0F;
                        float value = ((float) colorObj.get("v").getAsInt()) / 100.0F;
                        Color hsbColor = Color.getHSBColor(hue, saturation, value);
                        r = hsbColor.getRed();
                        g = hsbColor.getGreen();
                        b = hsbColor.getBlue();
                    } else {
                        parseError(jsonObj);
                        return Component.empty();
                    }
                } else {
                    parseError(jsonObj);
                    return Component.empty();
                }

            } else if (colorEl.isJsonPrimitive() && colorEl.getAsJsonPrimitive().isString()) {
                String colorStr = colorEl.getAsString();
                if (colorStr.startsWith("#") && colorStr.length() >= 7) {
                    String hexCode = colorStr.substring(1, 6);
                    r = Integer.parseInt(hexCode.substring(0, 2), 16);
                    g = Integer.parseInt(hexCode.substring(2, 4), 16);
                    b = Integer.parseInt(hexCode.substring(4), 16);
                } else if (presetColors.containsKey(colorStr)) {
                    TextColor color = presetColors.get(colorStr);
                    r = color.red();
                    g = color.green();
                    b = color.blue();
                } else {
                    parseError(jsonObj);
                    return Component.empty();
                }
            } else {
                parseError(jsonObj);
                return Component.empty();
            }
            comp = comp.color(TextColor.color(r, g, b));
        }
        if (jsonObj.has("bold")) {
            comp = comp.decoration(TextDecoration.BOLD, jsonObj.get("bold").getAsBoolean());
        }
        if (jsonObj.has("italic")) {
            comp = comp.decoration(TextDecoration.ITALIC, jsonObj.get("italic").getAsBoolean());
        }
        if (jsonObj.has("obfuscated")) {
            comp = comp.decoration(TextDecoration.OBFUSCATED, jsonObj.get("obfuscated").getAsBoolean());
        }
        if (jsonObj.has("underlined")) {
            comp = comp.decoration(TextDecoration.UNDERLINED, jsonObj.get("underlined").getAsBoolean());
        }
        if (jsonObj.has("strikethrough")) {
            comp = comp.decoration(TextDecoration.STRIKETHROUGH, jsonObj.get("strikethrough").getAsBoolean());
        }
        if (jsonObj.has("font")) {
            comp = comp.font(Key.key(jsonObj.get("font").getAsString()));
        }
        if (jsonObj.has("extra")) {
            for (JsonElement i : jsonObj.get("extra").getAsJsonArray()) {
                comp = comp.append(getComponentFromJsonElement(i, mContext, false));
            }
        }
        if (jsonObj.has("hover_event")) {
            JsonObject hoverObject = jsonObj.getAsJsonObject("hover_event");
            switch (hoverObject.get("type").getAsString()) {
                case "show_entity" -> {
                    UUID uuid = UUID.fromString(hoverObject.get("uuid").getAsString());
                    Key type = Key.key(hoverObject.get("entity_type").getAsString());
                    Component name = Component.empty();
                    boolean hasName = false;
                    if (hoverObject.has("name")) {
                        name = getComponentFromJsonElement(hoverObject.get("name"), mContext, false);
                        hasName = true;
                    }
                    if (hasName) {
                        comp = comp.hoverEvent(HoverEvent.showEntity(type, uuid, name));
                    } else {
                        comp = comp.hoverEvent(HoverEvent.showEntity(type, uuid));
                    }
                }
                case "show_text" -> {
                    comp = comp.hoverEvent(HoverEvent.showText(getComponentFromJsonElement(hoverObject.get("text"), mContext, false)));
                }
                case "show_item" -> {
                    /* TODO WHEN ITEM PARSING IS DONE */
                }
                default -> {
                    parseError(jsonObj);
                    return Component.empty();
                }
            }
        }
        if (jsonObj.has("click_event")) {
            JsonObject clickEventObject = jsonObj.getAsJsonObject("click_event");
            comp = switch (clickEventObject.get("type").getAsString()) {
                case "change_page" -> {
                    String page;
                    if (clickEventObject.get("page").getAsJsonPrimitive().isString()) {
                        page = clickEventObject.get("page").getAsString();
                    } else if (clickEventObject.get("page").getAsJsonPrimitive().isNumber()) {
                        page = String.valueOf(clickEventObject.get("page").getAsInt());
                    } else {
                        parseError(jsonObj);
                        yield Component.empty();
                    }
                    yield comp.clickEvent(ClickEvent.changePage(page));
                }
                case "copy_to_clipboard" -> comp.clickEvent(ClickEvent.copyToClipboard(clickEventObject.get("text").getAsString()));
                case "open_file" -> comp.clickEvent(ClickEvent.openFile(clickEventObject.get("file").getAsString()));
                case "open_url" -> comp.clickEvent(ClickEvent.openUrl(clickEventObject.get("url").getAsString()));
                case "run_command" -> comp.clickEvent(ClickEvent.runCommand(clickEventObject.get("command").getAsString()));
                case "suggest_command" -> comp.clickEvent(ClickEvent.suggestCommand(clickEventObject.get("command").getAsString()));

                default -> {
                    parseError(jsonObj);
                    yield Component.empty();
                }
            };
            /*
             * TYPES:
             * X CALLBACK
             * - CHANGE PAGE
             * - COPY TO CLIPBOARD
             * - OPEN FILE
             * - OPEN URL
             * - RUN COMMAND
             * - SUGGEST COMMAND
             */
        }
        return comp;
    }

    public static Component jsonArrComp(JsonArray jsonArr, MethodContext mContext) {
        Component comp = getComponentFromJsonElement(jsonArr.get(0), mContext, true);
        boolean i = false;
        for (JsonElement j : jsonArr) {
            if (!i) {
                i = true;
                continue;
            }
            comp = comp.append(getComponentFromJsonElement(j, mContext, true));
        }
        return comp;
    }

    public static Component getComponentFromJsonElement(JsonElement element, MethodContext mContext, boolean isNested) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return Component.text(element.getAsString());
        } else if (element.isJsonObject()) {
            return jsonObjComp(element.getAsJsonObject(), mContext);
        } else if (element.isJsonArray() && !isNested) {
            return jsonArrComp(element.getAsJsonArray(), mContext);
        } else {
            parseError(element);
            return Component.empty();
        }
    }

    private static void parseError(JsonElement jsonEl) {
        CordPlanter.LOGGER.warn("==================\nError while parsing: " + new Gson().toJson(jsonEl) + ".\nSkipping.\n==================");
    }
}