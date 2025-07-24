package dev.feintha.runtimeadvancements;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.ListBuilder;
import dev.feintha.runtimeadvancements.event.AdvancementLoading;
import dev.feintha.runtimeadvancements.event.AdvancementRemoval;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancement.*;
import net.minecraft.command.argument.*;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.command.AdvancementCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class RuntimeAdvancements implements ModInitializer {
    public static MinecraftServer INSTANCE;
    public static ServerAdvancementLoader getAdvancementLoader() {return INSTANCE.getAdvancementLoader();}
    public static AdvancementManager getAdvancementManager() {
        return getAdvancementLoader().getManager();
    }
    static final Field ADVANCEMENTS_FIELD;

    static {
        try {
            ADVANCEMENTS_FIELD = ServerAdvancementLoader.class.getDeclaredField("advancements");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Identifier, AdvancementEntry> getAdvancements() {
        try {
            ADVANCEMENTS_FIELD.setAccessible(true);
            //noinspection unchecked
            var a = (Map<Identifier, AdvancementEntry>) ADVANCEMENTS_FIELD.get(getAdvancementLoader());
            var m = new HashMap<>(a);
            ADVANCEMENTS_FIELD.set(getAdvancementLoader(), m);
            return m;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static void register(AdvancementEntry entry) {
        getAdvancements().put(entry.id(), entry);
        getAdvancementManager().addAll(List.of(entry));
        for(PlacedAdvancement placedAdvancement : getAdvancementManager().getRoots()) {
            if (placedAdvancement.getAdvancementEntry().value().display().isPresent()) {
                AdvancementPositioner.arrangeForTree(placedAdvancement);
            }
        }
        AdvancementUpdateS2CPacket packet = new AdvancementUpdateS2CPacket(false, List.of(entry), Set.of(), Map.of(), false);
        for (ServerPlayerEntity serverPlayerEntity : INSTANCE.getPlayerManager().getPlayerList()) {
            serverPlayerEntity.networkHandler.sendPacket(packet);
        }
    }

    public static void grantAdvancements(ServerPlayerEntity entity, boolean showToast, AdvancementEntry ... entries) {
        AdvancementUpdateS2CPacket packet = new AdvancementUpdateS2CPacket(false, List.of(entries), Set.of(), Map.of(), showToast);
        if (entity == null) {
            for (ServerPlayerEntity serverPlayerEntity : INSTANCE.getPlayerManager().getPlayerList()) {
                serverPlayerEntity.networkHandler.sendPacket(packet);
            }
            return;
        }
        entity.networkHandler.sendPacket(packet);
    }
    public static void revokeAdvancements(ServerPlayerEntity entity, boolean showToast, AdvancementEntry ... entries) {
        AdvancementUpdateS2CPacket packet = new AdvancementUpdateS2CPacket(false, List.of(), Arrays.stream(entries).map(AdvancementEntry::id).collect(Collectors.toSet()), Map.of(), showToast);
        if (entity == null) {
            for (ServerPlayerEntity serverPlayerEntity : INSTANCE.getPlayerManager().getPlayerList()) {
                serverPlayerEntity.networkHandler.sendPacket(packet);
            }
            return;
        }
        entity.networkHandler.sendPacket(packet);
    }
    public static class Events {

        public static final Event<AdvancementLoading> LOADING = EventFactory.createArrayBacked(AdvancementLoading.class, listeners ->(builder -> List.of(listeners).forEach(a->a.addStaticAdvancements(builder))));
        public static final Event<AdvancementRemoval> REMOVE = EventFactory.createArrayBacked(AdvancementRemoval.class, listeners ->(list -> List.of(listeners).forEach(a->a.removeStaticAdvancements(list))));

    }
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer ->INSTANCE = minecraftServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(minecraftServer -> INSTANCE = null);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("create_advancement").then(
                CommandManager.argument("id", IdentifierArgumentType.identifier())
                .then(CommandManager.argument("parent", IdentifierArgumentType.identifier()).suggests((context, builder) -> {
                    for (Identifier identifier : getAdvancements().keySet()) {
                        builder.suggest(identifier.toString());
                    }
                    return builder.buildFuture();
                }).then(CommandManager.argument("title", TextArgumentType.text(registryAccess))
                .then(CommandManager.argument("description", TextArgumentType.text(registryAccess))
                .then(CommandManager.argument("stack", ItemStackArgumentType.itemStack(registryAccess))
                .executes(context -> {
                    var name = IdentifierArgumentType.getIdentifier(context, "id");
                    var parent = IdentifierArgumentType.getIdentifier(context, "parent");
                    if (parent.getPath().equalsIgnoreCase("null") || parent.getPath().equalsIgnoreCase("nil")) {
                        parent = null;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Called /test_command."), false);
                    var title = TextArgumentType.getTextArgument(context, "title");
                    var description = TextArgumentType.getTextArgument(context, "description");
                    var stack = ItemStackArgumentType.getItemStackArgument(context, "stack").createStack(1,false);
                    var advancement = new AdvancementEntry(name, new Advancement(Optional.ofNullable(parent), Optional.of(new AdvancementDisplay(stack, title, description, Optional.empty(), AdvancementFrame.GOAL, false, false, false)), AdvancementRewards.NONE, Map.of(), AdvancementRequirements.EMPTY, false));
                    register(advancement);
                    return 1;
                })))))));
        });

    }
}
