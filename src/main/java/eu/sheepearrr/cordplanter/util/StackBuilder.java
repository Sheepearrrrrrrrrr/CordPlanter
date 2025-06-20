package eu.sheepearrr.cordplanter.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class StackBuilder {
    public static ItemStack getStackFromJson(JsonObject object) {
        String id = "stick";
        int amount = 1;
        if (object.has("id") && object.get("id").isJsonPrimitive() && object.get("id").getAsJsonPrimitive().isString()) {
            id = object.get("id").getAsString();
        }
        if (object.has("amount") && object.get("amount").isJsonPrimitive() && object.get("amount").getAsJsonPrimitive().isNumber()) {
            amount = object.get("amount").getAsInt();
        }
        ItemStack stack = ItemStack.of(Material.valueOf(id), amount);
        if (object.has("components") && object.get("components").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("components").entrySet()) {
                switch (entry.getKey()) {
                    case "max_stack_size" -> stack.setData(DataComponentTypes.MAX_STACK_SIZE, parseIntegerProperty(entry.getValue()));
                    case "attribute_modifiers" -> { /* TODO */
                        ItemAttributeModifiers.Builder attrs = ItemAttributeModifiers.itemAttributes();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            if (!element.isJsonObject()) {
                                componentParseError(element);
                                continue;
                            }
                            JsonObject obj = element.getAsJsonObject();
                            Attribute attr = switch (obj.get("type").getAsString()) {
                                case "max_health" -> Attribute.MAX_HEALTH;
                                case "attack_damage" -> Attribute.ATTACK_DAMAGE;
                                case "attack_speed" -> Attribute.ATTACK_SPEED;
                                case "attack_knockback" -> Attribute.ATTACK_KNOCKBACK;
                                case "armor_toughness" -> Attribute.ARMOR_TOUGHNESS;
                                case "block_breaking_speed" -> Attribute.BLOCK_BREAK_SPEED;
                                case "block_interaction_range" -> Attribute.BLOCK_INTERACTION_RANGE;
                                case "entity_interaction_range" -> Attribute.ENTITY_INTERACTION_RANGE;
                                case "burning_time" -> Attribute.BURNING_TIME;
                                case "explosion_knockback_resistance" -> Attribute.EXPLOSION_KNOCKBACK_RESISTANCE;
                                case "flying_speed" -> Attribute.FLYING_SPEED;
                                case "fall_damage_multiplier" -> Attribute.FALL_DAMAGE_MULTIPLIER;
                                case "follow_range" -> Attribute.FOLLOW_RANGE;
                                case "gravity" -> Attribute.GRAVITY;
                                case "knockback_resistance" -> Attribute.KNOCKBACK_RESISTANCE;
                                case "jump_strength" -> Attribute.JUMP_STRENGTH;
                                case "luck" -> Attribute.LUCK;
                                case "max_absorption" -> Attribute.MAX_ABSORPTION;
                                case "mining_efficiency" -> Attribute.MINING_EFFICIENCY;
                                case "movement_efficiency" -> Attribute.MOVEMENT_EFFICIENCY;
                                case "oxygen_bonus" -> Attribute.OXYGEN_BONUS;
                                case "movement_speed" -> Attribute.MOVEMENT_SPEED;
                                case "safe_fall_distance" -> Attribute.SAFE_FALL_DISTANCE;
                                case "scale" -> Attribute.SCALE;
                                case "sneaking_speed" -> Attribute.SNEAKING_SPEED;
                                case "spawn_reinforcements" -> Attribute.SPAWN_REINFORCEMENTS;
                                case "step_height" -> Attribute.STEP_HEIGHT;
                                case "submerged_mining_speed" -> Attribute.SUBMERGED_MINING_SPEED;
                                case "sweep_damage_ratio" -> Attribute.SWEEPING_DAMAGE_RATIO;
                                case "tempt_range" -> Attribute.TEMPT_RANGE;
                                case "water_movement_efficiency" -> Attribute.WATER_MOVEMENT_EFFICIENCY;
                                default -> Attribute.ARMOR;
                            };
                            NamespacedKey key = parseNamespacedKeyProperty(obj.get("key"));
                            double value = obj.get("value").getAsDouble();
                            AttributeModifier.Operation oper = parseAttributeOperation(obj.get("operation").getAsString());
                            if (obj.has("slot") && obj.get("slot").isJsonPrimitive() && obj.get("slot").getAsJsonPrimitive().isString()) {
                                EquipmentSlotGroup slotGroup = EquipmentSlotGroup.ANY;
                                if (EquipmentSlotGroup.getByName(obj.get("slot").getAsString().toLowerCase()) != null) {
                                    slotGroup = EquipmentSlotGroup.getByName(obj.get("slot").getAsString().toLowerCase());
                                }
                                attrs.addModifier(attr, new AttributeModifier(key, value, oper), slotGroup);
                            } else {
                                attrs.addModifier(attr, new AttributeModifier(key, value, oper));
                            }
                            /*
                            * TYPES:
                            * - ARMOR
                            * - MAX HEALTH
                            * - ATTACK DAMAGE
                            * - ATTACK SPEED
                            * - ATTACK KNOCKBACK
                            * - ARMOR TOUGHNESS
                            * - BLOCK BREAKING SPEED
                            * - BLOCK INTERACTION RANGE
                            * - ENTITY INTERACTION RANGE
                            * - BURNING TIME
                            * - EXPLOSION KNOCKBACK RESISTANCE
                            * - FLYING SPEED
                            * - FALL DAMAGE MULTIPLIER
                            * - FOLLOW RANGE
                            * - GRAVITY
                            * - KNOCKBACK RESISTANCE
                            * - JUMP STRENGTH
                            * - LUCK
                            * - MAX ABSORPTION
                            * - MINING EFFICIENCY
                            * - MOVEMENT EFFICIENCY
                            * - OXYGEN BONUS
                            * - MOVEMENT SPEED
                            * - SAFE FALL DISTANCE
                            * - SCALE
                            * - SNEAKING SPEED
                            * - SPAWN REINFORCEMENTS
                            * - STEP HEIGHT
                            * - SUBMERGED MINING SPEED
                            * - SWEEPING DAMAGE RATIO
                            * - TEMPT RANGE
                            * - WATER MOVEMENT EFFICIENCY
                            */
                        }
                        stack.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, attrs.build());
                    }
                    default -> CordPlanter.LOGGER.warn(String.format("Unrecognized component: \"{}\". Skipping.", entry.getKey()));
                }
            }
        }
        return stack;
    }

    private static Integer parseIntegerProperty(JsonElement element) {
        return element.getAsInt();
    }

    private static NamespacedKey parseNamespacedKeyProperty(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return NamespacedKey.fromString(element.getAsString());
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            return new NamespacedKey(obj.get("namespace").getAsString(), obj.get("value").getAsString());
        }
        componentParseError(element);
        return NamespacedKey.minecraft("stick");
    }

    private static AttributeModifier.Operation parseAttributeOperation(String name) {
        if (name.equals("scalar")) {
            return AttributeModifier.Operation.ADD_SCALAR;
        } else if (name.equals("multiply_scalar_1")) {
            return AttributeModifier.Operation.MULTIPLY_SCALAR_1;
        }
        return AttributeModifier.Operation.ADD_NUMBER;
    }

    private static void componentParseError(JsonElement obj) {
        CordPlanter.LOGGER.warn("==================\nError while parsing: " + new Gson().toJson(obj) + ".\n==================");
    }
}
