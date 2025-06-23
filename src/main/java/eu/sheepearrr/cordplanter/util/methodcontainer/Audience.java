package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import eu.sheepearrr.cordplanter.util.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
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
                default -> BasicMethodContainer.super.getExpression(obj);
            };
        }
        return this::returnThis;
    }

    private BossBar bossbarFromJsonObject(JsonObject obj) {
        final Component name = TextBuilder.getComponentFromJsonElement(obj.get("name"), this.context, false);
        final float progress = Math.clamp(obj.get("progress").getAsFloat() / 100.0F, 0.0F, 1.0F);
        final BossBar.Color color = BossBar.Color.NAMES.value(obj.get("color").getAsString());
        final BossBar.Overlay overlay = BossBar.Overlay.NAMES.value(obj.get("overlay").getAsString());
        assert color != null && overlay != null;
        if (obj.has("flags")) {
            Set<BossBar.Flag> flags = new HashSet<>();
            for (JsonElement flag : obj.get("flags").getAsJsonArray()) {
                flags.add(BossBar.Flag.NAMES.value(flag.getAsString()));
            }
            return BossBar.bossBar(name, progress, color, overlay, flags);
        }
        return BossBar.bossBar(name, progress, color, overlay);
    }

    public boolean showBossBar(JsonArray args) {
        BossBar bar = bossbarFromJsonObject(args.get(0).getAsJsonObject());
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
        return true;
    }

    public boolean sendMessage(JsonArray args) {
        this.audience.sendMessage(TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false));
        return true;
    }

    private Duration parseDuration(JsonElement element) {
        if (element instanceof JsonObject obj) {
            long time = obj.get("time").getAsLong();
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
        Component title = TextBuilder.getComponentFromJsonElement(args.get(0), this.context, false);
        Component subtitle = TextBuilder.getComponentFromJsonElement(args.get(1), this.context, false);
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
                JsonObject times = args.get(1).getAsJsonObject();
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
                    builder.required(optionsObject.get("required").getAsBoolean());
                }
                if (optionsObject.has("replace")) {
                    builder.replace(optionsObject.get("replace").getAsBoolean());
                }
                if (optionsObject.has("callback")) {
                    builder.callback(ResourcePackCallback.onTerminal((uuid, aud) -> {
                        Map<String, Object> props = Map.ofEntries(
                                Map.entry("uuid", uuid),
                                Map.entry("audience", aud)
                        );
                        MethodContext context = new MethodContext(optionsObject.get("on_success").getAsJsonArray().asList(), props);
                        context.resourceCallback();
                    }, (uuid, aud) -> {
                        Map<String, Object> props = Map.ofEntries(
                                Map.entry("uuid", uuid),
                                Map.entry("audience", aud)
                        );
                        MethodContext context = new MethodContext(optionsObject.get("on_failure").getAsJsonArray().asList(), props);
                        context.resourceCallback();
                    }));
                }
            }
            this.audience.sendResourcePacks(builder.asResourcePackRequest());
            return true;
        }
        return false;
    }

    @Override
    public Object returnThis(JsonArray args) {
        return this.audience;
    }
}
