package dk.lockfuglsang.minecraft.util;

import org.bukkit.block.data.BlockData;

// This class should ideally be located in package us.talabrek.ultimateskyblock.challenge. However, it is not possible
// to move there as it is required by ItemStackUtil in this module. This is a limitation of the current design.
// The parsing logic should eventually be moved to the uSkyBlock-Core module.
public record BlockRequirement(BlockData type, int amount) {
}
