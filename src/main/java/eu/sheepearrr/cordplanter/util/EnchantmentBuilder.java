package eu.sheepearrr.cordplanter.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanter;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentBuilder {
    public static List<EquipmentSlotGroup> parseSlots(JsonElement element) {
        if (element.isJsonArray()) {
            List<EquipmentSlotGroup> groups = new ArrayList<>();
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    EquipmentSlotGroup group = EquipmentSlotGroup.getByName(el.getAsString().toLowerCase());
                    if (group != null) {
                        groups.add(group);
                    }
                }
            }
            return groups;
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return List.of(EquipmentSlotGroup.getByName(element.getAsString().toLowerCase()));
        }
        return List.of();
    }

    public static RegistryKeySet<ItemType> parseItemSet(JsonElement element) {
        if (element.isJsonArray()) {
            List<TypedKey<ItemType>> idkfemidk = new ArrayList<>();
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    idkfemidk.add(TypedKey.create(RegistryKey.ITEM, el.getAsString()));
                }
            }
            return RegistrySet.keySet(RegistryKey.ITEM, idkfemidk);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            if (element.getAsString().startsWith("#")) {
                return RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getTag(TagKey.create(RegistryKey.ITEM, element.getAsString().substring(1)));
            }
            return RegistrySet.keySet(RegistryKey.ITEM, TypedKey.create(RegistryKey.ITEM, element.getAsString()));
        }
        return null;
    }

    public static RegistryKeySet<Enchantment> parseExclusiveSet(JsonElement element) {
        if (element.isJsonArray()) {
            List<TypedKey<Enchantment>> idkfemidk = new ArrayList<>();
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    idkfemidk.add(TypedKey.create(RegistryKey.ENCHANTMENT, el.getAsString()));
                }
            }
            return RegistrySet.keySet(RegistryKey.ENCHANTMENT, idkfemidk);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            if (element.getAsString().startsWith("#")) {
                return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getTag(TagKey.create(RegistryKey.ENCHANTMENT, Key.key(element.getAsString().substring(1))));
            }
            return RegistrySet.keySet(RegistryKey.ENCHANTMENT, TypedKey.create(RegistryKey.ENCHANTMENT, element.getAsString()));
        }
        return null;
    }

    public static EnchantmentRegistryEntry.EnchantmentCost parseCost(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("base") || !(obj.get("base").isJsonPrimitive() && obj.get("base").getAsJsonPrimitive().isNumber())) {
                CordPlanter.LOGGER.warn(String.format("Incorrectly defined enchantment cost: \"{}\"", element));
                return EnchantmentRegistryEntry.EnchantmentCost.of(1, 0);
            }
            if (!obj.has("additional") || !(obj.get("additional").isJsonPrimitive() && obj.get("additional").getAsJsonPrimitive().isNumber())) {
                CordPlanter.LOGGER.warn(String.format("Incorrectly defined enchantment cost: \"{}\"", element));
                return EnchantmentRegistryEntry.EnchantmentCost.of(1, 0);
            }
            return EnchantmentRegistryEntry.EnchantmentCost.of(obj.get("base").getAsInt(), obj.get("additional").getAsInt());
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return EnchantmentRegistryEntry.EnchantmentCost.of(element.getAsInt(), 0);
        }
        CordPlanter.LOGGER.warn(String.format("Incorrectly defined enchantment cost: \"{}\"", element));
        return EnchantmentRegistryEntry.EnchantmentCost.of(1, 0);
    }
}
