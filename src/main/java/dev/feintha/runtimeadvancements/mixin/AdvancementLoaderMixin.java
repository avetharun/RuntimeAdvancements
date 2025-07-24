package dev.feintha.runtimeadvancements.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.feintha.runtimeadvancements.RuntimeAdvancements;
import dev.feintha.runtimeadvancements.event.AdvancementLoading;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Mixin(ServerAdvancementLoader.class)
public class AdvancementLoaderMixin {
    @Shadow private Map<Identifier, AdvancementEntry> advancements;
    @Inject(method="apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at= @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap$Builder;buildOrThrow()Lcom/google/common/collect/ImmutableMap;", shift = At.Shift.BEFORE))
    void applyBuildMixin(Map<Identifier, Advancement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci, @Local ImmutableMap.Builder<Identifier, AdvancementEntry> builder) {
        RuntimeAdvancements.Events.LOADING.invoker().addStaticAdvancements(new AdvancementLoading.WrappedBuilder(builder));
    }
    @Inject(method="apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at= @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap$Builder;buildOrThrow()Lcom/google/common/collect/ImmutableMap;", shift = At.Shift.AFTER))
    void applyMixin(Map<Identifier, Advancement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci){
        this.advancements = new HashMap<>(this.advancements);
        var elementsToRemove = new HashSet<Identifier>();
        RuntimeAdvancements.Events.REMOVE.invoker().removeStaticAdvancements(elementsToRemove);
        for (Identifier identifier : elementsToRemove) {
            this.advancements.remove(identifier);
        }
    }
}
