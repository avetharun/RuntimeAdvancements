package dev.feintha.runtimeadvancements.event;

import com.mojang.serialization.ListBuilder;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public interface AdvancementRemoval {
    void removeStaticAdvancements(HashSet<Identifier> builder);
}
