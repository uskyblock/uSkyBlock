package us.talabrek.ultimateskyblock.hook.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.IslandLevel;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

public class UsbExpansion extends PlaceholderExpansion {

    private final uSkyBlock usb;

    public UsbExpansion(uSkyBlock usb) {
        this.usb = usb;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params_) {
        IslandLogic logic = usb.getIslandLogic();
        String[] params = params_.split("_");

        if(params[0].equalsIgnoreCase("istop")) {
            // check if top10 even exists
            if(logic.getRanks(0, 1).size() == 0) {
                return "unknown";
            }
            int place;
            try {
                place = Integer.parseInt(params[1]);
            }catch (NumberFormatException e) {
                return "error";
            }
            if(logic.getRanks(0, place).size() < place -1) {
                return "place does not exist";
            }
            IslandLevel isLevel = logic.getRanks(0, place).get(place - 1);
            switch (params[2]) {
                case "leadername": return isLevel.getLeaderName();
                case "coordinates": return isLevel.getIslandName();
                case "level": return String.valueOf(isLevel.getScore());
            }
            return null;
        }

        IslandInfo isInfo = logic.getIslandInfo(new PlayerInfo(player.getName(), player.getUniqueId(), usb));
        if(isInfo == null) return "error";
        if(params[0].equalsIgnoreCase("self")) {
            switch (params[1]) {
                case "leadername": return isInfo.getLeader();
                case "coordinates": return isInfo.getName();
                case "level": return String.valueOf(isInfo.getLevel());
                case "biome": return isInfo.getBiome();
                case "hoppers": return String.valueOf(isInfo.getHopperCount());
                case "max-animals": return String.valueOf(isInfo.getMaxAnimals());
                case "max-monsters": return String.valueOf(isInfo.getMaxMonsters());
                case "max-golems": return String.valueOf(isInfo.getMaxGolems());
                case "max-villagers": return String.valueOf(isInfo.getMaxVillagers());
                case "max-partysize": return String.valueOf(isInfo.getMaxPartySize());
                case "partysize": return String.valueOf(isInfo.getPartySize());
                case "online": return String.valueOf(isInfo.getOnlineMembers().size());
                case "score-multiplier": return String.valueOf(isInfo.getScoreMultiplier());
                case "score-offset": return String.valueOf(isInfo.getScoreOffset());
            }
        }

        return null; // Placeholder is unknown by the Expansion
    }

    @Override
    public @NotNull String getIdentifier() {
        return "uskyblock";
    }

    @Override
    public @NotNull String getAuthor() {
        return "treppenhaus";
    }

    @Override
    public @NotNull String getVersion() {
        return "0";
    }
}
