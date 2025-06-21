package eu.sheepearrr.cordplanter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.sheepearrr.cordplanter.util.EnchantmentBuilder;
import eu.sheepearrr.cordplanter.util.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import eu.sheepearrr.cordplanter.util.WorkspaceProperties;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.inventory.ItemType;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.C;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

public class CordPlanterBootstrap implements PluginBootstrap {
    public static CordPlanterBootstrap INSTANCE;
    public static final String currentFormat = "1.0.0-0.SNAPSHOT";
    public JsonObject data;
    public Map<String, WorkspaceProperties> enabledWorkspaces = new HashMap<>();
    public Map<String, WorkspaceProperties> disabledWorkspaces = new HashMap<>();
    public Map<String, JsonObject> enchants = new HashMap<>();
    public Map<String, JsonObject> commands = new HashMap<>();
    public Map<String, Map<String, JsonObject>> tags = new HashMap<>();
    public Map<String, Boolean> settings = new HashMap<>();
    public Map<String, Object> internalVariables = new HashMap<>();

    @Override
    public void bootstrap(BootstrapContext bootstrapContext) {
        try {
            Gson gson = new Gson();
            INSTANCE = this;
            File workshopsDirectory = new File(bootstrapContext.getDataDirectory().toString() + "/workspaces");
            File dataDir = new File(bootstrapContext.getDataDirectory().toString());
            File dataFile = new File(dataDir.getPath() + "/data.json");
            if (!dataDir.exists()) {
                dataDir.mkdir();
            }
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                data = new JsonObject();
            } else {
                data = gson.fromJson(new FileReader(dataFile), JsonObject.class);
            }
            if (!data.has("disabled")) {
                data.add("disabled", new JsonArray());
            }
            if (!data.has("settings")) {
                data.add("settings", new JsonObject());
            }
            if (!data.has("disable_format_warning")) {
                data.addProperty("disable_format_warning", false);
            }
            if (data.get("settings").getAsJsonObject().isEmpty()) {
                URL defaultSettingsUrl = new URL("https://raw.githubusercontent.com/Sheepearrrrrrrrrr/Data/refs/heads/main/cordplanter/default_settings/" + currentFormat + ".json");
                HttpURLConnection conn = (HttpURLConnection) defaultSettingsUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoOutput(true);
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                this.data.add("settings", gson.fromJson(rd, JsonObject.class));
                rd.close();
                conn.disconnect();
            }
            for (Entry<String, JsonElement> entry : this.data.getAsJsonObject("settings").entrySet()) {
                this.settings.put(entry.getKey(), entry.getValue().getAsBoolean());
            }
            if (!this.settings.get("require_current_format") && !this.data.get("disable_format_warning").getAsBoolean()) {
                CordPlanter.LOGGER.warn("\n===========================================================================================================================================================\n\n!!! WARNING !!!\n\nCordPlanter is configured to not require workspaces to use the current format. This could lead to the incorrect reading of definitions or missing features.\nFor the integrity of the plugin, I would enable this option.\nTo disable this warning, change \"disable_format_warning\" to true in the data.json config file.\n    - Sheepearrr, owner of CordPlanter\n\n===========================================================================================================================================================");
            }
            if (!workshopsDirectory.exists()) {
                workshopsDirectory.mkdir();
            }
            if (workshopsDirectory.listFiles() != null) {
                for (File f : Objects.requireNonNull(workshopsDirectory.listFiles())) {
                    if (!f.getName().endsWith(".json") || !f.exists()) {
                        continue;
                    }
                    JsonObject jsObj = gson.fromJson(new FileReader(f), JsonObject.class);
                    if (data.get("disabled").getAsJsonArray().contains(jsObj.get("id"))) {
                        disabledWorkspaces.put(jsObj.get("id").getAsString(), new WorkspaceProperties(jsObj.get("compile_version").getAsInt(), jsObj.get("format").getAsString(), jsObj.get("display_name").getAsString(), jsObj.get("display_version").getAsString()));
                        continue;
                    }
                    if ((!enabledWorkspaces.containsKey(jsObj.get("id").getAsString()) || enabledWorkspaces.get(jsObj.get("id").getAsString()).compileVersion < jsObj.get("compile_version").getAsInt()) && (!this.settings.get("require_current_format") || jsObj.get("format").getAsString().equals(currentFormat))) {
                        enabledWorkspaces.put(jsObj.get("id").getAsString(), new WorkspaceProperties(jsObj.get("compile_version").getAsInt(), jsObj.get("format").getAsString(), jsObj.get("display_name").getAsString(), jsObj.get("display_version").getAsString()));
                        for (JsonElement element : jsObj.get("workspace").getAsJsonArray()) {
                            if (element.isJsonObject()) {
                                JsonObject elementObject = element.getAsJsonObject();
                                if (elementObject.get("type").getAsString().equals("base")) {
                                    switch (elementObject.get("base").getAsString()) {
                                        case "enchantment" -> {
                                            String id = jsObj.get("id").getAsString() + ":" + elementObject.get("id").getAsString();
                                            if (elementObject.get("id").getAsString().contains(":")) {
                                                id = elementObject.get("id").getAsString();
                                            }
                                            enchants.put(id, elementObject.get("values").getAsJsonObject());
                                        }
                                        case "command" -> commands.put(elementObject.get("name").getAsString(), elementObject);
                                        case "tag" -> {

                                        }
                                        default -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
            bootstrapContext.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commandContext -> {
                commandContext.registrar().register(
                        Commands.literal("workspace")
                                .requires(stack -> stack.getSender().isOp())
                                .then(Commands.literal("list").executes(stack -> {
                                    Component comp = Component.empty().append(Component.text("ℹ").color(TextBuilder.presetColors.get("dark_green"))).append(Component.text(" Workspaces (" + (enabledWorkspaces.size() + disabledWorkspaces.size()) + "):\n"));
                                    boolean hasShit = false;
                                    if (!enabledWorkspaces.isEmpty()) {
                                        comp = comp.append(Component.text("Enabled\n    ").color(TextBuilder.presetColors.get("green")));
                                        hasShit = true;
                                        boolean i = false;
                                        int j = 0;
                                        int k = 0;
                                        for (Map.Entry<String, WorkspaceProperties> entry : enabledWorkspaces.entrySet()) {
                                            if (i && k < enabledWorkspaces.size()) comp = comp.append(Component.text(", "));
                                            if (j >= 4 && k < enabledWorkspaces.size()) {
                                                comp = comp.append(Component.text("\n    "));
                                                j = 0;
                                            }
                                            comp = comp.append(Component.text(entry.getValue().displayName));
                                            i = true;
                                            j++;
                                            k++;
                                        }
                                    }
                                    if (!disabledWorkspaces.isEmpty()) {
                                        if (hasShit) {
                                            comp = comp.append(Component.text("\n"));
                                        }
                                        comp = comp.append(Component.text("Disabled\n    ").color(TextBuilder.presetColors.get("red")));
                                        boolean i = false;
                                        int j = 0;
                                        int k = 0;
                                        for (Map.Entry<String, WorkspaceProperties> entry : disabledWorkspaces.entrySet()) {
                                            if (i && k < disabledWorkspaces.size()) comp = comp.append(Component.text(", "));
                                            if (j >= 4 && k < disabledWorkspaces.size()) {
                                                comp = comp.append(Component.text("\n    "));
                                                j = 0;
                                            }
                                            comp = comp.append(Component.text(entry.getValue().displayName));
                                            i = true;
                                            j++;
                                            k++;
                                        }
                                    }
                                    stack.getSource().getSender().sendMessage(comp);
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("disable").then(Commands.argument("name", StringArgumentType.string()).suggests((context, builder) -> {
                                    for (String entry : enabledWorkspaces.keySet()) {
                                        builder.suggest(entry);
                                    }
                                    return builder.buildFuture();
                                }).executes(stack -> {
                                    String name = stack.getArgument("name", String.class);
                                    if (enabledWorkspaces.containsKey(name)) {
                                        disabledWorkspaces.put(name, enabledWorkspaces.get(name));
                                        enabledWorkspaces.remove(name);
                                        stack.getSource().getSender().sendMessage(
                                                Component.empty()
                                                        .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                        .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                        .append(Component.text("Disabled workspace ").color(TextBuilder.presetColors.get("gray")))
                                                        .append(Component.text(name).color(TextBuilder.presetColors.get("red")))
                                                        .append(Component.text(".").color(TextBuilder.presetColors.get("gray")))
                                                        .append(Component.text("\n (You do need to restart the server for it to apply. DO NOT RELOAD.)").color(TextBuilder.presetColors.get("dark_gray")))
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    stack.getSource().getSender().sendMessage(
                                            Component.empty()
                                                    .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                    .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                    .append(Component.text("Couldn't find enabled workspace to disable.").color(TextBuilder.presetColors.get("gray")))
                                    );
                                    return -1;
                                })))
                                .then(Commands.literal("enable").then(Commands.argument("name", StringArgumentType.string()).suggests((context, builder) -> {
                                    for (String entry : disabledWorkspaces.keySet()) {
                                        builder.suggest(entry);
                                    }
                                    return builder.buildFuture();
                                }).executes(stack -> {
                                    String name = stack.getArgument("name", String.class);
                                    if (disabledWorkspaces.containsKey(name)) {
                                        enabledWorkspaces.put(name, disabledWorkspaces.get(name));
                                        disabledWorkspaces.remove(name);
                                        stack.getSource().getSender().sendMessage(
                                                Component.empty()
                                                        .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                        .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                        .append(Component.text("Enabled workspace ").color(TextBuilder.presetColors.get("gray")))
                                                        .append(Component.text(name).color(TextBuilder.presetColors.get("green")))
                                                        .append(Component.text(".").color(TextBuilder.presetColors.get("gray")))
                                                        .append(Component.text("\n (You do need to restart the server for it to apply. DO NOT RELOAD.)").color(TextBuilder.presetColors.get("dark_gray")))
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    stack.getSource().getSender().sendMessage(
                                            Component.empty()
                                                    .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                    .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                    .append(Component.text("Couldn't find disabled workspace to enable.").color(TextBuilder.presetColors.get("gray")))
                                    );
                                    return -1;
                                })))
                                .then(Commands.literal("settings").then(Commands.argument("setting", StringArgumentType.string()).suggests((context, builder) -> {
                                    for (String setting : settings.keySet()) {
                                        builder.suggest(setting);
                                    }
                                    return builder.buildFuture();
                                }).then(Commands.literal("get").executes(stack -> {
                                    String setting = stack.getArgument("setting", String.class);
                                    if (settings.containsKey(setting)) {
                                        stack.getSource().getSender().sendMessage(
                                                Component.empty()
                                                        .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                        .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                        .append(Component.text(setting).color(TextBuilder.presetColors.get("red")))
                                                        .append(Component.text(" is ").color(TextBuilder.presetColors.get("gray")))
                                                        .append(Component.text(settings.get(setting) ? "enabled" : "disabled").color(TextBuilder.presetColors.get(settings.get(setting) ? "green" : "red")))
                                                        .append(Component.text(".").color(TextBuilder.presetColors.get("gray")))
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    return -1;
                                })).then(Commands.literal("set").then(Commands.argument("state", BoolArgumentType.bool()).executes(stack -> {
                                    String setting = stack.getArgument("setting", String.class);
                                    Boolean state = stack.getArgument("state", Boolean.class);
                                    if (settings.containsKey(setting)) {
                                        if (settings.get(setting) == state) {
                                            stack.getSource().getSender().sendMessage(
                                                    Component.empty()
                                                            .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                            .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                            .append(Component.text(setting).color(TextBuilder.presetColors.get(state ? "green" : "red")))
                                                            .append(Component.text(" is already in that state.").color(TextBuilder.presetColors.get("gray")))
                                            );
                                        } else {
                                            settings.put(setting, state);
                                            stack.getSource().getSender().sendMessage(
                                                    Component.empty()
                                                            .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                            .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                            .append(Component.text(setting).color(TextBuilder.presetColors.get("red")))
                                                            .append(Component.text(" is now ").color(TextBuilder.presetColors.get("gray")))
                                                            .append(Component.text(state ? "enabled" : "disabled").color(TextBuilder.presetColors.get(state ? "green" : "red")))
                                                            .append(Component.text(".").color(TextBuilder.presetColors.get("gray")))
                                            );
                                        }
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    return -1;
                                })))))
                                .build()
                );
                commandContext.registrar().register(Commands.literal("testingtesting").executes(stack -> {
                    for (Map.Entry<String, JsonObject> entry : CordPlanterBootstrap.INSTANCE.commands.entrySet()) {
                        Map<String, Object> props = Map.ofEntries(
                                Map.entry("executer", stack.getSource().getExecutor())
                        );
                        MethodContext context = new MethodContext(entry.getValue().getAsJsonArray("requires").asList(), props);
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
                if (this.settings.get("allow_custom_commands")) {
                    for (Entry<String, JsonObject> entry : commands.entrySet()) {
                        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(entry.getKey());
                        if (entry.getValue().has("requires")) {
                            node.requires(stack -> {
                                Map<String, Object> props = new HashMap<>(Map.ofEntries(
                                        Map.entry("sender", stack.getSender()),
                                        Map.entry("executer", stack.getExecutor()),
                                        Map.entry("location", stack.getLocation())
                                ));
                                MethodContext context = new MethodContext(entry.getValue().getAsJsonArray("requires").asList(), props);
                                return context.requires(stack);
                            });
                        }
                        if (entry.getValue().has("executes")) {
                            node.executes(stack -> {
                                Map<String, Object> props = new HashMap<>(Map.ofEntries(
                                        Map.entry("sender", stack.getSource().getSender()),
                                        Map.entry("executer", stack.getSource().getExecutor())
                                ));
                                MethodContext context = new MethodContext(entry.getValue().getAsJsonArray("executes").asList(), props);
                                return context.executes(stack);
                            });
                        }
                        commandContext.registrar().register(node.build());
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (this.settings.get("allow_custom_enchantments")) {
            bootstrapContext.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
                for (Map.Entry<String, JsonObject> entry : enchants.entrySet()) {
                    event.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, entry.getKey()), builder -> {
                        builder.description(TextBuilder.getComponentFromJsonElement(entry.getValue().get("description"), null, false));
                        builder.maximumCost(EnchantmentBuilder.parseCost(entry.getValue().get("max_cost")));
                        builder.minimumCost(EnchantmentBuilder.parseCost(entry.getValue().get("min_cost")));
                        builder.supportedItems(EnchantmentBuilder.parseItemSet(entry.getValue().get("supported")));
                        builder.weight(Math.clamp(entry.getValue().get("weight").getAsInt(), 0, 1024));
                        builder.maxLevel(Math.clamp(entry.getValue().get("max_level").getAsInt(), 0, 255));
                        builder.anvilCost(Math.clamp(entry.getValue().get("anvil_cost").getAsInt(), 0, Integer.MAX_VALUE));
                        builder.activeSlots(EnchantmentBuilder.parseSlots(entry.getValue().get("slots")));
                        if (entry.getValue().has("primary")) {
                            builder.primaryItems(EnchantmentBuilder.parseItemSet(entry.getValue().get("primary")));
                        }
                        if (entry.getValue().has("exclusive")) {
                            builder.exclusiveWith(EnchantmentBuilder.parseExclusiveSet(entry.getValue().get("exclusive")));
                        }
                        /*
                         * OPTIONAL:
                         * - PRIMARY ITEMS
                         * - EXCLUSIVITY
                         */
                    });
                }
            }));
        }
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new CordPlanter();
    }
}
