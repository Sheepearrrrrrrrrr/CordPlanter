package eu.sheepearrr.cordplanter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
    private static final Map<String, ArgumentType<?>> argumentTypes = Map.ofEntries(
            Map.entry("string", StringArgumentType.string()),
            Map.entry("word", StringArgumentType.word()),
            Map.entry("greedy_string", StringArgumentType.greedyString()),
            Map.entry("integer", IntegerArgumentType.integer()),
            Map.entry("float", FloatArgumentType.floatArg()),
            Map.entry("double", DoubleArgumentType.doubleArg()),
            Map.entry("uuid", ArgumentTypes.uuid()),
            Map.entry("world", ArgumentTypes.world()),
            Map.entry("entity", ArgumentTypes.entity()),
            Map.entry("entities", ArgumentTypes.entities()),
            Map.entry("time", ArgumentTypes.time()),
            Map.entry("player", ArgumentTypes.player()),
            Map.entry("players", ArgumentTypes.players()),
            Map.entry("player_profiles", ArgumentTypes.playerProfiles())
    );

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
            this.refreshCurrentFormatSettings(false);
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
                                    Component comp = Component.empty().append(Component.text("ℹ").color(TextBuilder.presetColors.get("dark_green"))).append(Component.text(" Workspaces (" + (enabledWorkspaces.size() + disabledWorkspaces.size()) + "):"));
                                    Map<String, ArrayList<Entry<String, WorkspaceProperties>>> versions = new HashMap<>();
                                    for (Entry<String, WorkspaceProperties> entry : enabledWorkspaces.entrySet()) {
                                        String format = entry.getValue().format;
                                        if (versions.containsKey(format)) {
                                            ArrayList<Entry<String, WorkspaceProperties>> idk = versions.get(format);
                                            idk.add(entry);
                                            versions.put(format, idk);
                                            continue;
                                        }
                                        versions.put(format, new ArrayList<>(List.of(entry)));
                                    }
                                    for (Entry<String, WorkspaceProperties> entry : disabledWorkspaces.entrySet()) {
                                        String format = entry.getValue().format;
                                        if (versions.containsKey(format)) {
                                            ArrayList<Entry<String, WorkspaceProperties>> idk = versions.get(format);
                                            idk.add(entry);
                                            versions.put(format, idk);
                                            continue;
                                        }
                                        versions.put(format, new ArrayList<>(List.of(entry)));
                                    }
                                    for (Entry<String, ArrayList<Entry<String, WorkspaceProperties>>> format : versions.entrySet()) {
                                        comp = comp.append(Component.text("\n").append(Component.text(format.getKey()).color(TextBuilder.presetColors.get(format.getKey().equals(currentFormat) ? "green" : (this.settings.get("require_current_format") ? "dark_red" : "red")))).append(Component.text(":\n    ")));
                                        boolean i = false;
                                        int j = 0;
                                        Component prevComp = Component.empty();
                                        for (Entry<String, WorkspaceProperties> entry : format.getValue()) {
                                            if (i) {
                                                prevComp = prevComp.append(Component.text(", "));
                                            }
                                            if (j >= 3) {
                                                prevComp = prevComp.append(Component.text("\n    "));
                                                j = 0;
                                            }
                                            prevComp = prevComp.append(Component.text(entry.getValue().displayName).color(TextBuilder.presetColors.get(disabledWorkspaces.containsKey(entry.getKey()) ? "red" : "green")));
                                            i = true;
                                            j++;
                                        }
                                        comp = comp.append(prevComp);
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
                                                Component.text()
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
                                            Component.text()
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
                                                Component.text()
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
                                            Component.text()
                                                    .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                    .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                    .append(Component.text("Couldn't find disabled workspace to enable.").color(TextBuilder.presetColors.get("gray")))
                                    );
                                    return -1;
                                })))
                                .then(Commands.literal("settings").then(Commands.literal("get").then(Commands.argument("setting", StringArgumentType.string()).suggests((context, builder) -> {
                                            for (String setting : settings.keySet()) {
                                                builder.suggest(setting);
                                            }
                                            return builder.buildFuture();
                                    }).executes(stack -> {
                                    String setting = stack.getArgument("setting", String.class);
                                    if (settings.containsKey(setting)) {
                                        stack.getSource().getSender().sendMessage(
                                                Component.text()
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
                                }))).then(Commands.literal("set").then(Commands.argument("setting", StringArgumentType.string()).suggests((context, builder) -> {
                                            for (String setting : settings.keySet()) {
                                                builder.suggest(setting);
                                            }
                                            return builder.buildFuture();
                                }).then(Commands.argument("state", BoolArgumentType.bool()).executes(stack -> {
                                    String setting = stack.getArgument("setting", String.class);
                                    Boolean state = stack.getArgument("state", Boolean.class);
                                    if (settings.containsKey(setting)) {
                                        if (settings.get(setting) == state) {
                                            stack.getSource().getSender().sendMessage(
                                                    Component.text()
                                                            .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                            .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                            .append(Component.text(setting).color(TextBuilder.presetColors.get(state ? "green" : "red")))
                                                            .append(Component.text(" is already in that state.").color(TextBuilder.presetColors.get("gray")))
                                            );
                                        } else {
                                            settings.put(setting, state);
                                            stack.getSource().getSender().sendMessage(
                                                    Component.text()
                                                            .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                            .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                            .append(Component.text(setting).color(TextBuilder.presetColors.get("red")))
                                                            .append(Component.text(" is now ").color(TextBuilder.presetColors.get("gray")))
                                                            .append(Component.text(state ? "enabled" : "disabled").color(TextBuilder.presetColors.get(state ? "green" : "red")))
                                                            .append(Component.text(".").color(TextBuilder.presetColors.get("gray")))
                                            );
                                            if (setting.equals("allow_granting_operator_status") && state) {
                                                stack.getSource().getSender().sendMessage(
                                                        Component.text()
                                                                .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                                .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                                .append(Component.text("!!! WARNING !!! Enabling this option grants workspaces the power to GRANT OPERATOR STATUS to any Player, etc. This can be used as a force-op exploit, even through harmless looking stuff like text replacement. Only enable this option if you really need it.").color(TextBuilder.presetColors.get("gray")))
                                                                .append(Component.text("\nClick here to reset this setting.").hoverEvent(HoverEvent.showText(Component.text("RESET SETTING").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))).color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE).clickEvent(ClickEvent.runCommand("workspace settings reset allow_granting_operator_status")))
                                                );
                                                CordPlanter.LOGGER.warn("\n===========================================================================================================================================================\n\n!!! CRITICAL WARNING !!!\n\nCordPlanter was configured to grant workspaces the power to GRANT OPERATOR STATUS to any Player, etc. This can be used as a force-op exploit, even through harmless looking stuff like text replacement.\nOnly enable this option if you really need it.\nPlease run \"workspace settings reset allow_granting_operator_status\" to reset this option.\n\n===========================================================================================================================================================");
                                            }
                                        }
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    return -1;
                                })))).then(Commands.literal("reset").executes(command -> {
                                    try {
                                        refreshCurrentFormatSettings(true);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    command.getSource().getSender().sendMessage(
                                            Component.text()
                                                    .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                    .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                    .append(Component.text("Successfully reset the plugin settings.").color(TextBuilder.presetColors.get("gray")))
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }).then(Commands.argument("setting", StringArgumentType.string()).suggests((context, builder) -> {
                                            for (String setting : settings.keySet()) {
                                                builder.suggest(setting);
                                            }
                                            return builder.buildFuture();
                                }).executes(command -> {
                                    String setting = command.getArgument("setting", String.class);
                                    if (this.settings.containsKey(setting)) {
                                        try {
                                            this.settings.put(setting, urlStuff().get(setting).getAsBoolean());
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        command.getSource().getSender().sendMessage(
                                                Component.text()
                                                        .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                        .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                        .append(Component.text("Successfully reset the ").color(TextBuilder.presetColors.get("gray")))
                                                        .append(Component.text(setting).color(TextBuilder.presetColors.get("red")))
                                                        .append(Component.text(" setting.").color(TextBuilder.presetColors.get("gray")))
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    command.getSource().getSender().sendMessage(
                                            Component.text()
                                                    .append(Component.text("CordPlanter").color(TextBuilder.presetColors.get("red")).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                                                    .append(Component.text(" | ").color(TextBuilder.presetColors.get("dark_gray")))
                                                    .append(Component.text("Couldn't find the ").color(TextBuilder.presetColors.get("gray")))
                                                    .append(Component.text(setting).color(TextBuilder.presetColors.get("red")))
                                                    .append(Component.text(" setting.").color(TextBuilder.presetColors.get("gray")))
                                    );
                                    return -1;
                                }))))
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
                        if (entry.getValue().has("then")) {
                            for (JsonElement then : entry.getValue().get("then").getAsJsonArray()) {
                                node.then(then(then.getAsJsonObject()));
                            }
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

    private CommandNode<CommandSourceStack> then(JsonObject obj) {
        var node = obj.get("type").getAsString().equals("argument") ? Commands.argument(obj.get("name").getAsString(), argumentTypes.get(obj.get("argument_type").getAsString())) : Commands.literal(obj.get("name").getAsString());
        if (obj.has("requires")) {
            node.requires(stack -> {
                Map<String, Object> props = new HashMap<>(Map.ofEntries(
                        Map.entry("sender", stack.getSender()),
                        Map.entry("executer", stack.getExecutor()),
                        Map.entry("location", stack.getLocation())
                ));
                MethodContext context = new MethodContext(obj.getAsJsonArray("requires").asList(), props);
                return context.requires(stack);
            });
        }
        if (obj.has("executes")) {
            node.executes(stack -> {
                Map<String, Object> props = new HashMap<>(Map.ofEntries(
                        Map.entry("sender", stack.getSource().getSender()),
                        Map.entry("executer", stack.getSource().getExecutor())
                ));
                MethodContext context = new MethodContext(obj.getAsJsonArray("executes").asList(), props);
                return context.executes(stack);
            });
        }
        if (obj.has("then")) {
            for (JsonElement then : obj.get("then").getAsJsonArray()) {
                node.then(then(then.getAsJsonObject()));
            }
        }
        return node.build();
    }

    private void refreshCurrentFormatSettings(boolean forceReload) throws Exception {
        if (data.get("settings").getAsJsonObject().isEmpty() || forceReload) {
            this.data.add("settings", this.urlStuff());
        }
        for (Entry<String, JsonElement> entry : this.data.getAsJsonObject("settings").entrySet()) {
            this.settings.put(entry.getKey(), entry.getValue().getAsBoolean());
        }
        if (!this.settings.get("require_current_format") && !this.data.get("disable_format_warning").getAsBoolean()) {
            CordPlanter.LOGGER.warn("\n===========================================================================================================================================================\n\n!!! WARNING !!!\n\nCordPlanter is configured to not require workspaces to use the current format. This could lead to the incorrect reading of definitions or missing features.\nFor the integrity of the plugin, I would enable this option.\nTo disable this warning, change \"disable_format_warning\" to true in the data.json config file.\n    - Sheepearrr, owner of CordPlanter\n\n===========================================================================================================================================================");
        }
    }

    private JsonObject urlStuff() throws Exception {
        URL defaultSettingsUrl = new URL("https://raw.githubusercontent.com/Sheepearrrrrrrrrr/Data/refs/heads/main/cordplanter/default_settings/" + currentFormat + ".json");
        HttpURLConnection conn = (HttpURLConnection) defaultSettingsUrl.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        JsonObject toReturn = new Gson().fromJson(rd, JsonObject.class);
        rd.close();
        conn.disconnect();
        return toReturn;
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new CordPlanter();
    }
}
