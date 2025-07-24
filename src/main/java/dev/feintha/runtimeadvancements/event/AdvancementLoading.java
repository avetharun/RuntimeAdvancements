package dev.feintha.runtimeadvancements.event;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.ListBuilder;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public interface AdvancementLoading {
    public static class WrappedBuilder {
        final ImmutableMap.Builder<Identifier, AdvancementEntry> builder;
        @Deprecated Map<Identifier, AdvancementEntry> getEntries() {
            try {
                ENTRIES.setAccessible(true);
                //noinspection unchecked
                return (Map<Identifier, AdvancementEntry>) ENTRIES.get(builder);
            } catch (IllegalAccessException e) { throw new RuntimeException(e); }
        }
        public WrappedBuilder(ImmutableMap.Builder<Identifier, AdvancementEntry> builder) {
            this.builder = builder;
        }
        public WrappedBuilder addAll(AdvancementEntry...entries) {
            for (AdvancementEntry entry : entries) {
                this.add(entry);
            }
            return this;
        }
        public WrappedBuilder add(AdvancementEntry entry) {
            builder.put(entry.id(), entry);
            return this;
        }
        public WrappedBuilder add(Identifier id, AdvancementEntry entry) {
            builder.put(id, entry);
            return this;
        }
        private static final Field ENTRIES;

        static {
            try {
                ENTRIES = ImmutableMap.Builder.class.getDeclaredField("entries");
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        public boolean contains(Identifier key) {
            return getEntries().containsKey(key);
        }

        public boolean contains(AdvancementEntry entry) {
            return getEntries().containsValue(entry);
        }

    }
    void addStaticAdvancements(WrappedBuilder builder);
}
