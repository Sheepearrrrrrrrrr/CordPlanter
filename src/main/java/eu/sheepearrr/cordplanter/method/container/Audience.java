package eu.sheepearrr.cordplanter.method.container;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import eu.sheepearrr.cordplanter.method.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

public class Audience implements BasicMethodContainer {
    public MethodContext context;
    public final net.kyori.adventure.audience.Audience audience;


    public Audience(net.kyori.adventure.audience.Audience audience, MethodContext context) {
        this.audience = audience;
        this.context = context;
    }

    @Override
    public Function<JsonArray, Object> getExpression(JsonObject obj) {
        if (obj.has("method")) {
            return switch (obj.get("method").getAsString()) {
                case "send_message" -> this::sendMessage;
                case "show_bossbar" -> this::showBossBar;
                case "hide_bossbar" -> this::hideBossBar;
                case "modify_bossbar" -> this::modifyBossBar;
                case "show_title" -> this::showTitle;
                case "send_actionbar" -> this::sendActionBar;
                case "send_title_part" -> this::sendTitlePart;
                case "clear_title" -> this::clearTitle;
                case "reset_title" -> this::resetTitle;
                case "clear_resource_packs" -> this::clearResourcePacks;
                case "send_resource_packs", "send_resource_pack" -> this::sendResourcePacks;
                case "remove_resource_packs", "remove_resource_pack" -> this::removeResourcePacks;
                case "open_book" -> this::openBook;
                case "play_sound" -> this::playSound;
                case "stop_sound " -> this::stopSound;
                case "send_player_list_footer" -> this::sendPlayerListFooter;
                case "send_player_list_header" -> this::sendPlayerListHeader;
                case "send_player_list_header_and_footer" -> this::sendPlayerListHeaderAndFooter;
                default -> BasicMethodContainer.super.getExpression(obj);
            };
        }
        return this::returnThis;
    }

    private BossBar bossbarFromJsonObject(JsonObject obj) {
        final Component name = TextBuilder.getComponentFromJsonElement(obj.get("name"), this.context, false);
        final float progress = Math.clamp(((float) this.context.getValue(obj.get("progress"))) / 100.0F, 0.0F, 1.0F);
        final BossBar.Color color = BossBar.Color.NAMES.value((String) this.context.getValue(obj.get("color")));
        final BossBar.Overlay overlay = BossBar.Overlay.NAMES.value((String) this.context.getValue(obj.get("overlay")));
        assert color != null && overlay != null;
        if (obj.has("flags")) {
            Set<BossBar.Flag> flags = new HashSet<>();
            for (JsonElement flag : obj.get("flags").getAsJsonArray()) {
                flags.add(BossBar.Flag.NAMES.value((String) this.context.getValue(flag)));
            }
            return BossBar.bossBar(name, progress, color, overlay, flags);
        }
        return BossBar.bossBar(name, progress, color, overlay);
    }

    public boolean showBossBar(JsonArray args) {
        final BossBar bar = bossbarFromJsonObject((JsonObject) this.context.getValue(args.get(0)));
        this.audience.showBossBar(bar);
        if (args.size() > 1) {
            if (args.get(1) instanceof JsonObject obj && obj.get("base").getAsBoolean()) {
                if (obj.get("type").getAsString().equals("internal")) {
                    this.context.setInternalVariableTo(obj.get("name").getAsString(), bar);
                    return true;
                }
                this.context.setVariableTo(obj.get("name").getAsString(), bar);
                return true;
            }
            this.context.setVariableTo(args.get(1).getAsString(), bar);
        }
        return true;
    }

    public boolean modifyBossBar(JsonArray args) {
        if (args.get(1) instanceof JsonArray modifications) {
            BossBar bar;
            if (args.get(0) instanceof JsonObject obj) {
                String name = obj.get("name").getAsString();
                if (obj.get("type").getAsString().equals("internal")) {
                    bar = (BossBar) this.context.getInternalVariable(name);
                } else {
                    bar = (BossBar) this.context.getVariable(name);
                }
            } else {
                bar = (BossBar) this.context.getVariable(args.get(0).getAsString());
            }
            this.audience.hideBossBar(bar);
            for (JsonElement element : modifications) {
                JsonObject obj = element.getAsJsonObject();
                switch (obj.get("type").getAsString()) {
                    case "set_progress" -> bar.progress(Math.clamp(obj.get("progress").getAsFloat() / 100.0F, 0.0F, 1.0F));
                    case "add_progress" -> bar.progress(Math.clamp(bar.progress() + obj.get("amount").getAsFloat() / 100.0F, 0.0F, 1.0F));
                    case "change_color" -> bar.color(Objects.requireNonNull(BossBar.Color.NAMES.value(obj.get("color").getAsString())));
                    case "change_overlay" -> bar.overlay(Objects.requireNonNull(BossBar.Overlay.NAMES.value(obj.get("overlay").getAsString())));
                    case "change_name" -> bar.name(TextBuilder.getComponentFromJsonElement(obj.get("name"), this.context, false));
                    case "append_to_name" -> bar.name(bar.name().append(TextBuilder.getComponentFromJsonElement(obj.get("to_append"), this.context, false)));
                    case "add_flag" -> {
                        if (obj.get("flags").isJsonArray()) {
                            Set<BossBar.Flag> flags = new HashSet<>();
                            for (JsonElement flag : obj.get("flags").getAsJsonArray()) {
                                flags.add(BossBar.Flag.NAMES.value(flag.getAsString()));
                            }
                            bar.addFlags(flags);
                        } else {
                            bar.addFlag(Objects.requireNonNull(BossBar.Flag.NAMES.value(obj.get("flags").getAsString())));
                        }
                    }
                }
            }
            this.audience.showBossBar(bar);
            if (args.size() > 2) {
                if (args.get(2) instanceof JsonObject obj && obj.get("base").getAsBoolean()) {
                    if (obj.get("type").getAsString().equals("internal")) {
                        this.context.setInternalVariableTo(obj.get("name").getAsString(), bar);
                        return true;
                    }
                    this.context.setVariableTo(obj.get("name").getAsString(), bar);
                    return true;
                }
                this.context.setVariableTo(args.get(2).getAsString(), bar);
            }
            return true;
        }
        return false;
    }

    public boolean hideBossBar(JsonArray args) {
        BossBar bar;
        boolean should_remove_value = args.size() > 1 && args.get(1).isJsonPrimitive() && args.get(1).getAsJsonPrimitive().isBoolean() && args.get(1).getAsBoolean();
        if (args.get(0) instanceof JsonObject obj) {
            String name = (String) this.context.getValue(obj.get("name"));
            if (obj.get("type").getAsString().equals("internal")) {
                bar = (BossBar) this.context.getInternalVariable(name);
                if (should_remove_value) {
                    this.context.removeInternalVariable(name);
                }
            } else {
                bar = (BossBar) this.context.getVariable(name);
                if (should_remove_value) {
                    this.context.removeVariable(name);
                }
            }
        } else {
            bar = (BossBar) this.context.getVariable(args.get(0).getAsString());
            if (should_remove_value) {
                this.context.removeVariable((String) this.context.getValue(args.get(0)));
            }
        }
        this.audience.hideBossBar(bar);
        return true;
    }

    public boolean sendMessage(JsonArray args) {
        this.audience.sendMessage(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false));
        return true;
    }

    private Duration parseDuration(JsonElement element) {
        if (element instanceof JsonObject obj) {
            long time = (long) this.context.getValue(obj.get("time"));
            return switch (obj.get("unit").getAsString()) {
                case "days" -> Duration.ofDays(time);
                case "hours" -> Duration.ofHours(time);
                case "minutes" -> Duration.ofMinutes(time);
                case "milliseconds" -> Duration.ofMillis(time);
                case "nanoseconds" -> Duration.ofNanos(time);
                default -> Duration.ofSeconds(time);
            };
        }
        return Duration.ofSeconds(element.getAsLong());
    }

    public boolean showTitle(JsonArray args) {
        final Component title = TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false);
        final Component subtitle = TextBuilder.getComponentFromJsonElement(args.get(1), this.context, false);
        if (args.size() > 2){
            JsonObject times = args.get(2).getAsJsonObject();
            this.audience.showTitle(Title.title(title, subtitle, Title.Times.times(parseDuration(times.get("fade_in")), parseDuration(times.get("stay")), parseDuration(times.get("fade_out")))));
        }
        this.audience.showTitle(Title.title(title, subtitle));
        return true;
    }

    public boolean sendTitlePart(JsonArray args) {
        switch (args.get(0).getAsString()) {
            case "title" -> this.audience.sendTitlePart(TitlePart.TITLE, TextBuilder.getComponentFromJsonElement(args.get(1), this.context, false));
            case "subtitle" -> this.audience.sendTitlePart(TitlePart.SUBTITLE, TextBuilder.getComponentFromJsonElement(args.get(1), this.context, false));
            case "times" -> {
                final JsonObject times = args.get(1).getAsJsonObject();
                this.audience.sendTitlePart(TitlePart.TIMES, Title.Times.times(parseDuration(times.get("fade_in")), parseDuration(times.get("stay")), parseDuration(times.get("fade_out"))));
            }
        }
        return true;
    }

    public boolean sendActionBar(JsonArray args) {
        this.audience.sendActionBar(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false));
        return true;
    }

    public boolean clearTitle(JsonArray args) {
        this.audience.clearTitle();
        return true;
    }

    public boolean resetTitle(JsonArray args) {
        this.audience.resetTitle();
        return true;
    }

    public boolean clearResourcePacks(JsonArray args) {
        if (CordPlanterBootstrap.INSTANCE.settings.get("allow_resource_pack_application")) {
            this.audience.clearResourcePacks();
            return true;
        }
        return false;
    }

    public boolean sendResourcePacks(JsonArray args) {
        if (CordPlanterBootstrap.INSTANCE.settings.get("allow_resource_pack_application")) {
            ResourcePackRequest.Builder builder = ResourcePackRequest.resourcePackRequest();
            if (args.get(0) instanceof JsonArray array) {
                List<ResourcePackInfo> packInfos = new ArrayList<>();
                for (JsonElement request : array) {
                    JsonObject requestObject = request.getAsJsonObject();
                    packInfos.add(ResourcePackInfo.resourcePackInfo(UUID.fromString(requestObject.get("uuid").getAsString()), URI.create(requestObject.get("uri").getAsString()), requestObject.get("hash").getAsString()));
                }
                builder.packs(packInfos);
            } else if (args.get(0) instanceof JsonObject obj) {
                builder.packs(List.of(ResourcePackInfo.resourcePackInfo(UUID.fromString(obj.get("uuid").getAsString()), URI.create(obj.get("uri").getAsString()), obj.get("hash").getAsString())));
            } else {
                return false;
            }
            if (args.size() > 1 && args.get(1) instanceof JsonObject optionsObject) {
                if (optionsObject.has("prompt")) {
                    builder.prompt(TextBuilder.getComponentFromJsonElement(optionsObject.get("prompt"), this.context, false));
                }
                if (optionsObject.has("required")) {
                    builder.required((boolean) this.context.getValue(optionsObject.get("required")));
                }
                if (optionsObject.has("replace")) {
                    builder.replace((boolean) this.context.getValue(optionsObject.get("replace")));
                }
                if (optionsObject.has("callback")) {
                    builder.callback(ResourcePackCallback.onTerminal((uuid, aud) -> {
                        Map<String, Object> props = Map.ofEntries(
                                Map.entry("uuid", uuid),
                                Map.entry("audience", aud)
                        );
                        MethodContext context = new MethodContext(optionsObject.get("on_success").getAsJsonArray().asList(), props, null);
                        context.resourceCallback();
                    }, (uuid, aud) -> {
                        Map<String, Object> props = Map.ofEntries(
                                Map.entry("uuid", uuid),
                                Map.entry("audience", aud)
                        );
                        MethodContext context = new MethodContext(optionsObject.get("on_failure").getAsJsonArray().asList(), props, null);
                        context.resourceCallback();
                    }));
                }
            }
            this.audience.sendResourcePacks(builder.asResourcePackRequest());
            return true;
        }
        return false;
    }

    public boolean removeResourcePacks(JsonArray args) {
        if (CordPlanterBootstrap.INSTANCE.settings.get("allow_resource_pack_application")) {
            if (args.get(0) instanceof JsonArray uuids) {
                List<UUID> packUuids = new ArrayList<>();
                for (JsonElement uuid : uuids) {
                    packUuids.add(UUID.fromString(uuid.getAsString()));
                }
                this.audience.removeResourcePacks(packUuids);
            } else if (args.get(0).isJsonPrimitive() && args.get(0).getAsJsonPrimitive().isString()) {
                this.audience.removeResourcePacks(List.of(UUID.fromString(args.get(0).getAsString())));
            }
        }
        return false;
    }

    public boolean openBook(JsonArray args) {
        if (
                args.size() >= 2 &&
                args.get(0) instanceof JsonObject properties &&
                args.get(1) instanceof JsonArray pages &&
                properties.has("author") &&
                properties.has("title")
        ) {
            Book.Builder builder = Book.builder();
            builder.author(TextBuilder.getComponentFromJsonElement(properties.get("author"), this.context, false));
            builder.title(TextBuilder.getComponentFromJsonElement(properties.get("title"), this.context, false));
            for (JsonElement page : pages) {
                builder.addPage(TextBuilder.getComponentFromJsonElement(page, this.context, false));
            }
            this.audience.openBook(builder.build());
            return true;
        }
        return false;
    }

    public boolean playSound(JsonArray args) {
        if (args.get(0) instanceof JsonObject properties && properties.has("type")) {
            Sound.Builder builder = Sound.sound();
            Key type;
            if (properties.get("type") instanceof JsonObject key) {
                type = Key.key((String) this.context.getValue(key.get("namespace")), (String) this.context.getValue(key.get("value")));
            } else if (properties.get("type").isJsonPrimitive() && properties.get("type").getAsJsonPrimitive().isString()) {
                type = Key.key((String) this.context.getValue(properties.get("type")));
            } else {
                return false;
            }
            builder.type(type);
            if (properties.has("pitch") && properties.get("pitch").isJsonPrimitive() && properties.get("pitch").getAsJsonPrimitive().isNumber()) {
                builder.pitch(Math.clamp((float) this.context.getValue(properties.get("pitch")), -1.0F, 1.0F));
            }
            if (properties.has("volume") && properties.get("volume").isJsonPrimitive() && properties.get("volume").getAsJsonPrimitive().isNumber()) {
                builder.volume(Math.clamp((long) this.context.getValue(properties.get("volume")), 0, Integer.MAX_VALUE));
            }
            if (properties.has("seed") && properties.get("seed").isJsonPrimitive() && properties.get("seed").getAsJsonPrimitive().isNumber()) {
                builder.seed(properties.get("seed").getAsLong());
            }
            if (properties.has("source") && properties.get("source").isJsonPrimitive() && properties.get("source").getAsJsonPrimitive().isString()) {
                builder.source(Sound.Source.NAMES.valueOr((String) this.context.getValue(properties.get("source")), Sound.Source.MASTER));
            }
            Sound sound = builder.build();
            if (args.size() > 1) {
                double x;
                double y;
                double z;
                if (args.get(1) instanceof JsonObject coordinateObject) {
                    x = coordinateObject.get("x").getAsDouble();
                    y = coordinateObject.get("y").getAsDouble();
                    z = coordinateObject.get("z").getAsDouble();
                } else if (args.get(1).isJsonPrimitive() && args.get(1).getAsJsonPrimitive().isString()) {
                    List<Double> coords = new ArrayList<>();
                    String coordsText = args.get(1).getAsString();
                    StringBuilder prevStuff = new StringBuilder();
                    for (int i = 0; i < coordsText.length(); i++) {
                        if (coordsText.charAt(i) == ' ' || i == coordsText.length() - 1) {
                            coords.add(Double.valueOf(prevStuff.toString()));
                            prevStuff = new StringBuilder();
                            continue;
                        }
                        prevStuff.append(coordsText.charAt(i));
                    }
                    x = coords.getFirst();
                    y = coords.get(1);
                    z = coords.getLast();
                } else {
                    return false;
                }
                this.audience.playSound(sound, x, y, z);
                return true;
            }
            this.audience.playSound(sound);
            return true;
        }
        return false;
    }

    public boolean stopSound(JsonArray args) {
        if (args.get(0) instanceof JsonObject properties) {
            if (properties.has("type") && properties.get("type").isJsonPrimitive() && properties.get("type").getAsJsonPrimitive().isString()) {
                switch (properties.get("type").getAsString()) {
                    case "by_sound_definition" -> {
                        Sound.Builder builder = Sound.sound();
                        Key type;
                        if (properties.get("type") instanceof JsonObject key) {
                            type = Key.key((String) this.context.getValue(key.get("namespace")), (String) this.context.getValue(key.get("value")));
                        } else if (properties.get("type").isJsonPrimitive() && properties.get("type").getAsJsonPrimitive().isString()) {
                            type = Key.key((String) this.context.getValue(properties.get("type")));
                        } else {
                            return false;
                        }
                        builder.type(type);
                        if (properties.has("pitch") && properties.get("pitch").isJsonPrimitive() && properties.get("pitch").getAsJsonPrimitive().isNumber()) {
                            builder.pitch(Math.clamp((float) this.context.getValue(properties.get("pitch")), -1.0F, 1.0F));
                        }
                        if (properties.has("volume") && properties.get("volume").isJsonPrimitive() && properties.get("volume").getAsJsonPrimitive().isNumber()) {
                            builder.volume(Math.clamp((long) this.context.getValue(properties.get("volume")), 0, Integer.MAX_VALUE));
                        }
                        if (properties.has("seed") && properties.get("seed").isJsonPrimitive() && properties.get("seed").getAsJsonPrimitive().isNumber()) {
                            builder.seed(properties.get("seed").getAsLong());
                        }
                        if (properties.has("source") && properties.get("source").isJsonPrimitive() && properties.get("source").getAsJsonPrimitive().isString()) {
                            builder.source(Sound.Source.NAMES.valueOr((String) this.context.getValue(properties.get("source")), Sound.Source.MASTER));
                        }
                        Sound sound = builder.build();
                        this.audience.stopSound(sound);
                        return true;
                    }
                    case "all" -> this.audience.stopSound(SoundStop.all());
                    case "by_sound" -> {
                        Key sound;
                        if (properties.get("type") instanceof JsonObject key) {
                            sound = Key.key((String) this.context.getValue(key.get("namespace")), (String) this.context.getValue(key.get("value")));
                        } else if (properties.get("type").isJsonPrimitive() && properties.get("type").getAsJsonPrimitive().isString()) {
                            sound = Key.key((String) this.context.getValue(properties.get("type")));
                        } else {
                            return false;
                        }
                        this.audience.stopSound(SoundStop.named(sound));
                    }
                    case "by_source" -> this.audience.stopSound(SoundStop.source(Sound.Source.NAMES.valueOr(properties.get("source").getAsString(), Sound.Source.MASTER)));
                    case "by_sound_and_source" -> {
                        Key sound;
                        if (properties.get("type") instanceof JsonObject key) {
                            sound = Key.key((String) this.context.getValue(key.get("namespace")), (String) this.context.getValue(key.get("value")));
                        } else if (properties.get("type").isJsonPrimitive() && properties.get("type").getAsJsonPrimitive().isString()) {
                            sound = Key.key((String) this.context.getValue(properties.get("type")));
                        } else {
                            return false;
                        }
                        this.audience.stopSound(SoundStop.namedOnSource(sound, Sound.Source.NAMES.valueOr((String) this.context.getValue(properties.get("source")), Sound.Source.MASTER)));
                    }
                }
            }
        }
        return false;
    }

    public boolean sendPlayerListFooter(JsonArray args) {
        if (!args.isEmpty()) {
            this.audience.sendPlayerListFooter(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false));
        }
        return false;
    }

    public boolean sendPlayerListHeader(JsonArray args) {
        if (!args.isEmpty()) {
            this.audience.sendPlayerListHeader(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false));
        }
        return false;
    }

    public boolean sendPlayerListHeaderAndFooter(JsonArray args) {
        if (args.size() > 1) {
            this.audience.sendPlayerListHeaderAndFooter(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false), TextBuilder.getComponentFromJsonElement(args.get(1), this.context, false));
        }
        return false;
    }

    @Override
    public Object returnThis(JsonArray args) {
        return this.audience;
    }
}
