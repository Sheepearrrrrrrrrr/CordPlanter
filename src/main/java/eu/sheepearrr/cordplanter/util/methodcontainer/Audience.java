package eu.sheepearrr.cordplanter.util.methodcontainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.sheepearrr.cordplanter.util.MethodContext;
import eu.sheepearrr.cordplanter.util.TextBuilder;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    @Override
    public Object returnThis(JsonArray args) {
        return this.audience;
    }
}
