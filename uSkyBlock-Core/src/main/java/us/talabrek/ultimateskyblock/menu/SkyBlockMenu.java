package us.talabrek.ultimateskyblock.menu;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.handler.SchematicHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.message.Placeholder;
import us.talabrek.ultimateskyblock.player.IslandPerk;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.UltimateHolder;
import us.talabrek.ultimateskyblock.player.UltimateHolder.MenuType;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ComponentLineSplitter;
import us.talabrek.ultimateskyblock.util.GuiItemUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dk.lockfuglsang.minecraft.po.I18nUtil.fromLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static dk.lockfuglsang.minecraft.util.FormatUtil.stripFormatting;
import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static us.talabrek.ultimateskyblock.challenge.ChallengeLogic.CHALLENGE_PAGE_SIZE;
import static us.talabrek.ultimateskyblock.challenge.ChallengeLogic.COLS_PER_ROW;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Msg.MENU;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.LogUtil.log;

// TODO: Move all the texts to resource-files (translatable).

/**
 * The UI menu of uSkyBlock (using the inventory UI).
 */
@Singleton
public class SkyBlockMenu {
    private final Pattern PERM_VALUE_PATTERN = Pattern.compile("(\\[(?<perm>(?<not>[!])?[^\\]]+)\\])?(?<value>.*)");
    private final Pattern CHALLENGE_PAGE_HEADER = Pattern.compile(trLegacy("Challenge Menu") + ".*\\((?<p>[0-9]+)/(?<max>[0-9]+)\\)");

    private final uSkyBlock plugin;
    private final ChallengeLogic challengeLogic;
    private final PluginConfig config;
    private final PerkLogic perkLogic;
    private final SchematicHandler schematicHandler;
    private final LimitLogic limitLogic;
    private final ConfirmHandler confirmHandler;
    private final Scheduler scheduler;

    private final ItemStack sign = new ItemStack(Material.OAK_SIGN, 1);
    private final ItemStack biome = new ItemStack(Material.JUNGLE_SAPLING, 1);
    private final ItemStack lock = new ItemStack(Material.IRON_BARS, 1);
    private final ItemStack warpset = new ItemStack(Material.END_PORTAL_FRAME, 1);
    private final ItemStack warptoggle = new ItemStack(Material.LEVER, 1);
    private final ItemStack invite = new ItemStack(Material.CARROT_ON_A_STICK, 1);
    private final ItemStack kick = new ItemStack(Material.LEATHER_BOOTS, 1);
    private final List<PartyPermissionMenuItem> permissionMenuItems = Arrays.asList(
        new PartyPermissionMenuItem(biome, "canChangeBiome", trLegacy("Change Biome"),
            trLegacy("change the island's biome.")),
        new PartyPermissionMenuItem(lock, "canToggleLock", trLegacy("Toggle Island Lock"),
            trLegacy("toggle the island's lock.")),
        new PartyPermissionMenuItem(warpset, "canChangeWarp", trLegacy("Set Island Warp"),
            trLegacy("set the island's warp."),
            trLegacy("set the island's warp,<newline>which allows non-group<newline>members to teleport to<newline>the island.")),
        new PartyPermissionMenuItem(warptoggle, "canToggleWarp", trLegacy("Toggle Island Warp"),
            trLegacy("toggle the island's warp."),
            trLegacy("toggle the island's warp,<newline>allowing members to turn it<newline>on or off at any time, but<newline>not set the location.")),
        new PartyPermissionMenuItem(invite, "canInviteOthers", trLegacy("Invite Players"),
            trLegacy("invite others to the island."),
            trLegacy("invite<newline>other players to the island if<newline>there is enough room for more<newline>members")),
        new PartyPermissionMenuItem(kick, "canKickOthers", trLegacy("Kick Players"),
            trLegacy("kick others from the island."),
            trLegacy("kick<newline>other players from the island,<newline>but they are unable to kick<newline>the island leader."))
    );

    @Inject
    public SkyBlockMenu(
        @NotNull uSkyBlock plugin,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull PluginConfig config,
        @NotNull PerkLogic perkLogic,
        @NotNull SchematicHandler schematicHandler,
        @NotNull LimitLogic limitLogic,
        @NotNull ConfirmHandler confirmHandler,
        @NotNull Scheduler scheduler
    ) {
        this.plugin = plugin;
        this.challengeLogic = challengeLogic;
        this.config = config;
        this.perkLogic = perkLogic;
        this.schematicHandler = schematicHandler;
        this.limitLogic = limitLogic;
        this.confirmHandler = confirmHandler;
        this.scheduler = scheduler;
    }

    public Inventory displayPartyPlayerGUI(final Player inventoryViewer, final PlayerProfile partyMember) {
        Preconditions.checkNotNull(partyMember, "Player profile must not be null");
        UUID partyMemberId = Preconditions.checkNotNull(partyMember.getUniqueId(), "Player UUID must not be null");
        List<Component> lores = new ArrayList<>();
        String emptyTitle = miniToLegacy("<player> [<menu>]", unparsed("player", ""), component("menu", tr("Permissions")));
        String name = resolvePartyMemberName(partyMember, partyMemberId);
        int maxNameLength = Math.max(0, 32 - emptyTitle.length());
        String title = miniToLegacy("<blue><player> [<menu>]",
            unparsed("player", name.substring(0, Math.min(maxNameLength, name.length()))),
            component("menu", tr("Permissions")));
        Inventory menu = Bukkit.createInventory(new UltimateHolder(inventoryViewer, title, MenuType.DEFAULT), 9, title);
        final ItemStack pHead = new ItemStack(Material.PLAYER_HEAD, 1);
        final SkullMeta meta3 = requireNonNull((SkullMeta) requireNonNull(pHead.getItemMeta()));
        addLore(lores, tr("Click here to return to<newline>your island group's info.", MUTED));
        ItemStackUtil.setComponentDisplayName(sign, tr("Player Permissions", PRIMARY));
        ItemStackUtil.setComponentLore(sign, lores);
        menu.addItem(sign);
        lores.clear();
        meta3.setOwnerProfile(partyMember);
        pHead.setItemMeta(meta3);
        ItemStackUtil.setComponentDisplayName(pHead, tr("<player>'s permissions", unparsed("player", name, PRIMARY)));
        addLore(lores, tr("Hover over an icon to view<newline>a permission. Change the<newline>permission by clicking it.", MUTED));
        ItemStackUtil.setComponentLore(pHead, lores);
        menu.addItem(pHead);
        lores.clear();
        IslandInfo islandInfo = plugin.getIslandInfo(inventoryViewer);
        boolean isLeader = islandInfo.isLeader(inventoryViewer);
        for (PartyPermissionMenuItem menuItem : permissionMenuItems) {
            ItemStack itemStack = menuItem.getIcon();
            if (islandInfo.hasPerm(partyMemberId, menuItem.getPerm())) {
                ItemStackUtil.setComponentDisplayName(itemStack, fromLegacy("\u00a7a" + menuItem.getTitle()));
                lores.add(tr("This player <success>can</success>", MUTED));
                addLore(lores, fromLegacy(menuItem.getDescription()));
                if (isLeader) {
                    addLore(lores, tr("Click here to remove this permission."));
                }
            } else {
                ItemStackUtil.setComponentDisplayName(itemStack, fromLegacy("\u00a7c" + menuItem.getTitle()));
                lores.add(tr("This player <error>cannot</error>", MUTED));
                addLore(lores, fromLegacy(menuItem.getDescription()));
                if (isLeader) {
                    addLore(lores, tr("Click here to grant this permission."));
                }
            }
            ItemStackUtil.setComponentLore(itemStack, lores);
            menu.addItem(itemStack);
            lores.clear();
        }
        return menu;
    }

    private void addLore(List<Component> lores, Component multiLine) {
        List<Component> lines = ComponentLineSplitter.splitLines(multiLine);
        lores.addAll(lines);
    }

    private void addTranslatedComponentLore(List<Component> lores, String translation) {
        for (String line : translation.split("\n")) {
            lores.add(fromLegacy(line));
        }
    }

    public Inventory displayPartyGUI(final Player player) {
        List<Component> lores = new ArrayList<>();
        String title = trLegacy("Island Group Members", MENU);
        Inventory menu = Bukkit.createInventory(new UltimateHolder(player, title, MenuType.DEFAULT), 18, title);
        IslandInfo islandInfo = plugin.getIslandInfo(player);
        final Set<UUID> memberList = islandInfo.getMemberUUIDs();
        ItemStackUtil.setComponentDisplayName(sign, tr("Island Group Members", SECONDARY));
        // I18N: <current> and <max> are localized number tags. Tag arguments use DecimalFormat patterns; keep both tag names unchanged.
        lores.add(tr("Group members: <current>/<max>",
            MUTED,
            number("current", islandInfo.getPartySize(), SECONDARY),
            number("max", islandInfo.getMaxPartySize(), PRIMARY)));
        if (islandInfo.getPartySize() < islandInfo.getMaxPartySize()) {
            addLore(lores, tr("More players can be invited to this island.", SECONDARY));
        } else {
            addLore(lores, tr("This island is full.", ERROR));
        }
        addLore(lores, tr("Hover over a player's icon to<newline>view their permissions. The<newline>leader can change permissions<newline>by clicking a player's icon.", MUTED));
        ItemStackUtil.setComponentLore(sign, lores);
        menu.addItem(sign.clone());
        lores.clear();
        for (UUID memberId : memberList) {
            ItemStack headItem = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta3 = requireNonNull((SkullMeta) requireNonNull(headItem.getItemMeta()));
            meta3.setOwnerProfile(Bukkit.createPlayerProfile(memberId));
            headItem.setItemMeta(meta3);
            ItemStackUtil.setComponentDisplayName(headItem, tr("<player>'s permissions", unparsed("player", String.valueOf(memberId), PRIMARY)));
            boolean isLeader = islandInfo.isLeader(memberId);
            if (isLeader) {
                addLore(lores, tr("Leader", SECONDARY.decorate(BOLD)));
            } else {
                addLore(lores, tr("Member", PRIMARY.decorate(BOLD)));
            }
            for (PartyPermissionMenuItem perm : permissionMenuItems) {
                if (isLeader || islandInfo.hasPerm(memberId, perm.getPerm())) {
                    // I18N: A UI string indicating a player has a particular permission
                    lores.add(tr("<success>Can</success> <permission>", MUTED, unparsed("permission", perm.getShortDescription())));
                } else {
                    // I18N: A UI string indicating a player does not have a particular permission
                    lores.add(tr("<error>Cannot</error> <permission>", MUTED, unparsed("permission", perm.getShortDescription())));
                }
            }
            if (islandInfo.isLeader(player.getName())) {
                addLore(lores, tr("Click to change this player's permissions.", PRIMARY));
            }
            ItemStackUtil.setComponentLore(headItem, lores);
            menu.addItem(headItem);
            lores.clear();
        }
        return menu;
    }

    public Inventory displayLogGUI(final Player player) {
        List<Component> lores = new ArrayList<>();
        String title = trLegacy("Island Log", MENU);
        Inventory menu = Bukkit.createInventory(new UltimateHolder(player, title, MenuType.DEFAULT), 9, title);
        addLore(lores, tr("Click here to return to<newline>the main island screen.", PRIMARY));
        ItemStackUtil.setComponentDisplayName(sign, tr("Island Log", PRIMARY));
        ItemStackUtil.setComponentLore(sign, lores);
        menu.addItem(sign);
        lores.clear();
        ItemStack menuItem = new ItemStack(Material.WRITABLE_BOOK, 1);
        lores.addAll(plugin.getIslandInfo(player).getLog().stream().map(I18nUtil::fromLegacy).toList());
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Island Log", PRIMARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.setItem(8, menuItem);
        lores.clear();
        return menu;
    }

    private void addExtraMenus(Player player, Inventory menu) {
        ConfigurationSection extras = config.getYamlConfig().getConfigurationSection("options.extra-menus");
        if (extras == null) {
            return;
        }
        for (String sIndex : extras.getKeys(false)) {
            ConfigurationSection menuSection = extras.getConfigurationSection(sIndex);
            if (menuSection == null) {
                continue;
            }
            try {
                int index = Integer.parseInt(sIndex, 10);
                String title = menuSection.getString("title", "\u00a9Unknown");
                String icon = menuSection.getString("displayItem", "CHEST");
                List<Component> lores = new ArrayList<>();
                for (String l : menuSection.getStringList("lore")) {
                    Matcher matcher = PERM_VALUE_PATTERN.matcher(l);
                    if (matcher.matches()) {
                        String perm = matcher.group("perm");
                        String lore = matcher.group("value");
                        boolean not = matcher.group("not") != null;
                        if (perm != null) {
                            boolean hasPerm = player.hasPermission(perm);
                            if ((hasPerm && !not) || (!hasPerm && not)) {
                                lores.add(fromLegacy(lore));
                            }
                        } else {
                            lores.add(fromLegacy(lore));
                        }
                    }
                }
                // Only SIMPLE icons supported...
                ItemStack item = GuiItemUtil.createGuiDisplayItem(icon, title);
                ItemStackUtil.setComponentLore(item, lores);
                menu.setItem(index, item);
            } catch (Exception e) {
                log(Level.INFO, "Unable to add extra-menu " + sIndex + ": " + e);
            }
        }
    }

    private boolean isExtraMenuAction(Player player, ItemStack currentItem) {
        ConfigurationSection extras = config.getYamlConfig().getConfigurationSection("options.extra-menus");
        if (extras == null || currentItem == null || currentItem.getItemMeta() == null) {
            return false;
        }
        Material itemType = currentItem.getType();
        String itemTitle = requireNonNull(currentItem.getItemMeta()).getDisplayName();
        for (String sIndex : extras.getKeys(false)) {
            ConfigurationSection menuSection = extras.getConfigurationSection(sIndex);
            if (menuSection == null) {
                continue;
            }
            try {
                String title = menuSection.getString("title", "\u00a9Unknown");
                String icon = menuSection.getString("displayItem", "CHEST");
                Material material = Material.matchMaterial(icon);
                if (title.equals(itemTitle) && material == itemType) {
                    for (String command : menuSection.getStringList("commands")) {
                        Matcher matcher = PERM_VALUE_PATTERN.matcher(command);
                        if (matcher.matches()) {
                            String perm = matcher.group("perm");
                            String cmd = matcher.group("value");
                            boolean not = matcher.group("not") != null;
                            if (perm != null) {
                                boolean hasPerm = player.hasPermission(perm);
                                if ((hasPerm && !not) || (!hasPerm && not)) {
                                    plugin.execCommand(player, cmd, false);
                                }
                            } else {
                                plugin.execCommand(player, cmd, false);
                            }
                        } else {
                            log(Level.INFO, "Malformed menu " + title + ", invalid command : " + command);
                        }
                    }
                    return true;
                }
            } catch (Exception e) {
                log(Level.INFO, "Unable to execute commands for extra-menu " + sIndex + ": " + e);
            }
        }
        return false;
    }

    public Inventory displayChallengeGUI(final Player player, int page, String playerName) {
        int total = challengeLogic.getTotalPages();
        String title = miniToLegacy("<blue><title> (<page>/<total>)",
            legacyArg("title", trLegacy("Challenge Menu")),
            number("page", page),
            number("total", total));
        Inventory menu = Bukkit.createInventory(new UltimateHolder(player, title, MenuType.DEFAULT), CHALLENGE_PAGE_SIZE + COLS_PER_ROW, title);
        final PlayerInfo pi = playerName == null ? plugin.getPlayerInfo(player) : plugin.getPlayerInfo(playerName);
        challengeLogic.populateChallengeRank(menu, pi, page);
        int[] pages = new int[9];
        pages[0] = 1;
        pages[8] = total;
        int startOffset = 2;
        if (page > 5) {
            startOffset = (int) ((Math.round(page / 2d)) - 1);
            if (startOffset > total - 7) {
                startOffset = total - 7;
            }
        }
        for (int i = 0; i < 7; i++) {
            pages[i + 1] = startOffset + i;
        }
        for (int i = 0; i < pages.length; i++) {
            int p = pages[i];
            if (p >= 1 && p <= total) {
                ItemStack pageItem;
                if (p == page) {
                    pageItem = GuiItemUtil.createGuiDisplayItem(Material.WRITABLE_BOOK, trLegacy("Current page", MUTED));
                } else {
                    // I18N: <page> is a localized number tag. Tag arguments use DecimalFormat patterns; keep tag name "page".
                    pageItem = GuiItemUtil.createGuiDisplayItem(Material.BOOK, trLegacy("Page <page>", MUTED, number("page", p)));
                }
                if (i == 0) {
                    ItemStackUtil.Builder pageItemBuilder = ItemStackUtil.builder(pageItem)
                        .displayName(trLegacy("First Page", MUTED));
                    if (playerName != null && !playerName.trim().isEmpty()) {
                        pageItemBuilder.lore(playerName);
                    }
                    pageItem = pageItemBuilder.build();
                } else if (i == 8) {
                    pageItem = ItemStackUtil.builder(pageItem).displayName(trLegacy("Last Page", MUTED)).build();
                }
                pageItem.setAmount(p);
                menu.setItem(i + CHALLENGE_PAGE_SIZE, pageItem);
            }
        }
        return menu;
    }

    public Inventory displayIslandGUI(final Player player) {
        if (plugin.hasIsland(player)) {
            return createMainMenu(player);
        } else {
            return createInitMenu(player);
        }
    }

    private Inventory createInitMenu(Player player) {
        List<String> schemeNames = schematicHandler.getSchemeNames();
        int menuSize = (int) Math.ceil(getMaxSchemeIndex(schemeNames) / 9d) * 9;
        String title = trLegacy("Island Create Menu", MENU);
        Inventory menu = Bukkit.createInventory(new UltimateHolder(player, title, MenuType.DEFAULT), menuSize, title);
        List<Component> lores = new ArrayList<>();
        ItemStack menuItem = new ItemStack(Material.OAK_SAPLING, 1);
        addLore(lores, tr("Start your skyblock journey<newline>by starting your own island.<newline>Complete challenges to earn<newline>items and skybucks to help<newline>expand your skyblock. You can<newline>invite others to join in<newline>building your island empire!<newline><primary>Click here to start!</primary>"));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Start an Island", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        if (config.getYamlConfig().getBoolean("island-schemes-enabled", true) && schemeNames.size() > 1) {
            int index = 1;
            for (String schemeName : schemeNames) {
                IslandPerk islandPerk = perkLogic.getIslandPerk(schemeName);
                boolean enabled = config.getYamlConfig().getBoolean("island-schemes." + islandPerk.getSchemeName() + ".enabled", true);
                if (!enabled) {
                    continue; // Skip
                }
                index = Math.max(config.getYamlConfig().getInt("island-schemes." + islandPerk.getSchemeName() + ".index", index), 1);
                menuItem = islandPerk.getDisplayItem();
                ItemMeta meta = requireNonNull(requireNonNull(menuItem.getItemMeta()));
                List<String> oldLore = meta.getLore();
                if (oldLore == null) {
                    oldLore = List.of();
                }
                lores = new ArrayList<>(oldLore.stream().map(I18nUtil::fromLegacy).toList());
                if (player.hasPermission(islandPerk.getPermission())) {
                    addLore(lores, tr("Click to create!", SECONDARY));
                } else {
                    addLore(lores, tr("No access!<newline><muted>(<permission>)</muted>",
                        ERROR,
                        unparsed("permission", islandPerk.getPermission())));
                }
                ItemStackUtil.setComponentLore(menuItem, lores);
                menu.setItem(index++, menuItem);
            }
        }

        lores.clear();
        menuItem = new ItemStack(Material.SHORT_GRASS, 1);
        addLore(lores, tr("Teleport to the spawn area."));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Return to Spawn", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.setItem(menuSize - 2, menuItem);

        lores.clear();
        menuItem = new ItemStack(Material.PLAYER_HEAD, 1);
        addLore(lores, tr("Want to join another player's<newline>island instead of starting<newline>your own? If another player<newline>invites you to their island<newline>you can click here or use<newline><cmd>/is accept</cmd> to join them.<newline><primary>Click here to accept an invite!</primary><newline>(You must be invited first)"));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Join an Island", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.setItem(menuSize - 1, menuItem);
        return menu;
    }

    private int getMaxSchemeIndex(List<String> schemeNames) {
        int index = 1;
        for (String schemeName : schemeNames) {
            int nextIndex = config.getYamlConfig().getInt("island-schemes." + schemeName + ".index", index);
            if (nextIndex > index) {
                index = nextIndex;
            } else {
                index++;
            }
        }
        return index + 3;
    }

    private Inventory createMainMenu(Player player) {
        String title = trLegacy("Island Menu", MENU);
        Inventory menu = Bukkit.createInventory(new UltimateHolder(player, title, MenuType.DEFAULT), 18, title);
        List<Component> lores = new ArrayList<>();
        ItemStack menuItem = new ItemStack(Material.OAK_DOOR, 1);
        addLore(lores, tr("Return to your island's home<newline>point. You can change your home<newline>point to any location on your<newline>island using <cmd>/is sethome</cmd>.<newline><primary>Click here to return home.</primary>"));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Return Home", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        IslandInfo islandInfo = plugin.getIslandInfo(player);

        menuItem = new ItemStack(Material.DIAMOND_ORE, 1);
        addLore(lores, tr("View a list of <primary>challenges</primary> that<newline>you can complete on your island<newline>to earn skybucks, items, perks,<newline>and titles."));
        if (challengeLogic.isEnabled()) {
            addLore(lores, tr("Click here to view challenges.", PRIMARY));
        } else {
            addLore(lores, tr("Challenges disabled.", ERROR));
        }
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Challenges", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);

        lores.clear();
        menuItem = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Island Level", SECONDARY));
        List<Component> islandLevelLore = new ArrayList<>();
        // I18N: <level:'#,##0'> is a localized number tag. It uses a DecimalFormat pattern; keep tag name "level".
        islandLevelLore.add(tr("Current level: <level:'#,##0'>", MUTED,
            number("level", islandInfo.getLevel(), SECONDARY)));
        islandLevelLore.addAll(limitLogic.getSummaryComponents(islandInfo));
        addTranslatedComponentLore(
            islandLevelLore,
            trLegacy("Gain island levels by expanding<newline>your skyblock and completing<newline>certain challenges. Rarer blocks<newline>will add more to your level.<newline><primary>Click here to refresh.</primary><newline>(must be on your island)")
        );
        ItemStackUtil.setComponentLore(menuItem, islandLevelLore);
        menu.addItem(menuItem);
        lores.clear();

        menuItem = new ItemStack(Material.PLAYER_HEAD, 1);
        // I18N: <current> and <max> are localized number tags. Tag arguments use DecimalFormat patterns; keep both tag names unchanged.
        lores.add(tr("Members: <current>/<max>",
            MUTED,
            number("current", islandInfo.getPartySize(), SECONDARY),
            number("max", islandInfo.getMaxPartySize(), PRIMARY)));
        addLore(lores, tr("View the members of your island<newline>group and their permissions. If<newline>you are the island leader, you<newline>can change member permissions.<newline><primary>Click here to view or change.</primary>"));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Island Group", PRIMARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        menuItem = new ItemStack(Material.JUNGLE_SAPLING, 1);
        lores.add(tr("Current biome: <biome>", MUTED, Placeholder.legacy("biome", islandInfo.getBiomeName(), SECONDARY)));
        addLore(lores, tr("The island biome affects things<newline>like grass color and spawning<newline>of both animals and monsters."));
        if (islandInfo.hasPerm(player, "canChangeBiome")) {
            addLore(lores, tr("Click here to change biomes.", PRIMARY));
        } else {
            addLore(lores, tr("You can't change the biome.", ERROR));
        }
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Change Island Biome", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        menuItem = new ItemStack(Material.IRON_BARS, 1);
        if (plugin.getIslandInfo(player).isLocked()) {
            addLore(lores, tr("Lock status: <secondary>Active</secondary><newline>Your island is currently <error>locked</error>.<newline>Players outside your group<newline>cannot enter your island.", MUTED));
            if (islandInfo.hasPerm(player, "canToggleLock") && player.hasPermission("usb.island.lock")) {
                addLore(lores, tr("Click here to unlock your island.", PRIMARY));
            } else {
                addLore(lores, tr("You can't change the lock.", ERROR));
            }
        } else {
            addLore(lores, tr("Lock status: Inactive<newline>Your island is currently <secondary>unlocked</secondary>.<newline>All players can enter your island,<newline>but only you and your group<newline>members may build there.", MUTED));
            if (islandInfo.hasPerm(player, "canToggleLock") && player.hasPermission("usb.island.lock")) {
                addLore(lores, tr("Click here to lock your island.", PRIMARY));
            } else {
                addLore(lores, tr("You can't change the lock.", ERROR));
            }
        }
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Island Lock", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        if (plugin.getIslandInfo(player).hasWarp()) {
            menuItem = new ItemStack(Material.END_PORTAL_FRAME, 1);
            addLore(lores, tr("Warp status: <secondary>Active</secondary><newline>Other players may warp to your<newline>island at any time to the point<newline>you set with <cmd>/is setwarp</cmd>.", MUTED));
            if (islandInfo.hasPerm(player, "canToggleWarp") && player.hasPermission("usb.island.togglewarp")) {
                addLore(lores, tr("Click here to deactivate.", PRIMARY));
            } else {
                addLore(lores, tr("You can't change the warp.", ERROR));
            }
        } else {
            menuItem = new ItemStack(Material.END_STONE, 1);
            addLore(lores, tr("Warp status: Inactive<newline>Other players can't warp to your<newline>island. Set a warp point with<newline><cmd>/is setwarp</cmd> before activating.", MUTED));
            if (islandInfo.hasPerm(player, "canToggleWarp") && player.hasPermission("usb.island.togglewarp")) {
                addLore(lores, tr("Click here to activate.", PRIMARY));
            } else {
                addLore(lores, tr("You can't change the warp.", ERROR));
            }
        }
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Island Warp", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        menuItem = new ItemStack(Material.SHORT_GRASS, 1);
        addLore(lores, tr("Teleport to the spawn area."));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Return to Spawn", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        menuItem = new ItemStack(Material.WRITABLE_BOOK, 1);
        addLore(lores, tr("View a log of events from<newline>your island such as member,<newline>biome, and warp changes.<newline><primary>Click to view the log.</primary>"));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Island Log", SECONDARY));
        menu.addItem(menuItem);
        menu.setItem(8, menuItem); // Last item, first line
        lores.clear();

        menuItem = new ItemStack(Material.RED_BED, 1); // red bed
        addLore(lores, tr("When you teleport to your<newline>island you will be taken to<newline>this location.<newline><primary>Click here to change.</primary>"));
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Change Home Location", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.setItem(9, menuItem); // First item, 2nd line
        lores.clear();

        menuItem = new ItemStack(Material.HOPPER, 1);
        addLore(lores, tr("When your warp is activated,<newline>other players will be taken to<newline>this point when they teleport<newline>to your island."));
        if (islandInfo.hasPerm(player, "canChangeWarp") && player.hasPermission("usb.island.setwarp")) {
            addLore(lores, tr("Click here to change.", PRIMARY));
        } else {
            addLore(lores, tr("You can't change the warp.", ERROR));
        }
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Change Warp Location", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.setItem(15, menuItem);
        lores.clear();
        if (islandInfo.isLeader(player)) {
            if (config.getYamlConfig().getBoolean("island-schemes-enabled", true)) {
                menuItem = new ItemStack(Material.PODZOL, 1);
                addLore(lores, tr("Restarts your island.<newline><error>Warning! This will remove your items and island!</error>"));
                ItemStackUtil.setComponentDisplayName(menuItem, tr("Restart Island", ERROR));
                ItemStackUtil.setComponentLore(menuItem, lores);
                menu.setItem(17, menuItem);
                lores.clear();
            }
        } else {
            menuItem = new ItemStack(Material.IRON_DOOR, 1);
            addLore(lores, tr("Leaves your island.<newline><error>Warning! This will remove all your items!</error>"));
            addLore(lores, tr("Click to leave", ERROR));
            ItemStackUtil.setComponentDisplayName(menuItem, tr("Leave Island", ERROR));
            ItemStackUtil.setComponentLore(menuItem, lores);
            menu.setItem(17, menuItem);
            lores.clear();
            Duration durationLeft = confirmHandler.durationLeft(player, "/is leave");
            if (durationLeft.isPositive()) {
                updateLeaveMenuItemTimer(player, menu, menuItem);
            }
        }
        addExtraMenus(player, menu);
        return menu;
    }

    public void onClick(InventoryClickEvent event) {
        ItemStack currentItem = event != null ? event.getCurrentItem() : null;
        if (event == null || currentItem == null) {
            return; // Bail out, nothing we can do anyway
        }
        Player p = (Player) event.getWhoClicked();
        ItemMeta meta = requireNonNull(currentItem.getItemMeta());
        SkullMeta skull = meta instanceof SkullMeta ? (SkullMeta) meta : null;
        if (!(event.getInventory().getHolder() instanceof UltimateHolder))
            return;
        String inventoryName = stripFormatting(((UltimateHolder) event.getInventory().getHolder()).getTitle());
        int slotIndex = event.getSlot();
        int menuSize = event.getInventory().getSize();
        if (inventoryName.equalsIgnoreCase(stripFormatting(trLegacy("Island Group Members")))) {
            onClickPartyMenu(event, currentItem, p, meta, skull, slotIndex);
        } else if (inventoryName.contains(stripFormatting(trLegacy("Permissions")))) {
            onClickPermissionMenu(event, currentItem, p, inventoryName, slotIndex);
        } else if (inventoryName.contains(stripFormatting(trLegacy("Challenge Menu")))) {
            onClickChallengeMenu(event, currentItem, p, inventoryName);
        } else if (inventoryName.equalsIgnoreCase(stripFormatting(trLegacy("Island Log")))) {
            onClickLogMenu(event, p, slotIndex);
        } else if (inventoryName.equalsIgnoreCase(stripFormatting(trLegacy("Island Menu")))) {
            onClickMainMenu(event, currentItem, p, slotIndex);
        } else if (inventoryName.equalsIgnoreCase(stripFormatting(trLegacy("Island Create Menu")))) {
            onClickCreateMenu(event, p, meta, slotIndex, menuSize);
        } else if (inventoryName.equalsIgnoreCase(stripFormatting(trLegacy("Island Restart Menu")))) {
            onClickRestartMenu(event, p, meta, slotIndex, currentItem);
        }
    }

    private void onClickRestartMenu(final InventoryClickEvent event, final Player p, ItemMeta meta, int slotIndex, ItemStack currentItem) {
        event.setCancelled(true);
        if (slotIndex == 0) {
            p.openInventory(createMainMenu(p));
        } else if (currentItem != null && meta != null && meta.hasDisplayName()) {
            String schemeName = stripFormatting(meta.getDisplayName());
            IslandPerk islandPerk = perkLogic.getIslandPerk(schemeName);
            if (perkLogic.getSchemes(p).contains(schemeName) && p.hasPermission(islandPerk.getPermission())) {
                if (confirmHandler.durationLeft(p, "/is restart").isPositive()) {
                    p.performCommand("island restart " + schemeName);
                } else {
                    p.performCommand("island restart " + schemeName);
                    updateRestartMenuTimer(p, event.getInventory());
                }
            }
        }
    }

    private void updateRestartMenuTimer(final Player p, final Inventory inventory) {
        final BukkitTask[] hackySharing = new BukkitTask[1];
        hackySharing[0] = scheduler.sync(() -> {
            if (inventory.getViewers().contains(p)) {
                updateRestartMenu(inventory, p, schematicHandler.getSchemeNames());
            }
            if (!confirmHandler.durationLeft(p, "/is restart").isPositive() || !inventory.getViewers().contains(p)) {
                if (hackySharing.length > 0 && hackySharing[0] != null) {
                    hackySharing[0].cancel();
                }
            }
        }, Duration.ZERO, Duration.ofSeconds(1));
    }

    private void onClickCreateMenu(InventoryClickEvent event, Player p, ItemMeta meta, int slotIndex, int menuSize) {
        event.setCancelled(true);
        if (slotIndex == 0) {
            p.performCommand("island create");
        } else if (slotIndex == menuSize - 2) {
            p.performCommand("island spawn");
        } else if (slotIndex == menuSize - 1) {
            p.performCommand("island accept");
        } else if (meta != null && meta.hasDisplayName()) {
            String schemeName = stripFormatting(meta.getDisplayName());
            if (perkLogic.getSchemes(p).contains(schemeName)) {
                p.performCommand("island create " + schemeName);
            } else {
                sendErrorTr(p, "You do not have access to that island schematic.");
            }
        }
    }

    private void onClickMainMenu(InventoryClickEvent event, ItemStack currentItem, Player player, int slotIndex) {
        event.setCancelled(true);
        if (slotIndex < 0 || slotIndex > 35) {
            return;
        }
        PlayerInfo playerInfo = plugin.getPlayerInfo(player);
        IslandInfo islandInfo = plugin.getIslandInfo(playerInfo);
        if (currentItem.getType() == Material.JUNGLE_SAPLING) {
            player.performCommand("island biome");
        } else if (currentItem.getType() == Material.PLAYER_HEAD) {
            player.performCommand("island party");
        } else if (currentItem.getType() == Material.RED_BED) {
            player.performCommand("island sethome");
            player.performCommand("island");
        } else if (currentItem.getType() == Material.SHORT_GRASS) {
            player.performCommand("island spawn");
        } else if (currentItem.getType() == Material.HOPPER) {
            player.performCommand("island setwarp");
            player.performCommand("island");
        } else if (currentItem.getType() == Material.WRITABLE_BOOK) {
            player.performCommand("island log");
        } else if (currentItem.getType() == Material.OAK_DOOR) {
            player.performCommand("island home");
        } else if (currentItem.getType() == Material.EXPERIENCE_BOTTLE) {
            player.performCommand("island level");
        } else if (currentItem.getType() == Material.DIAMOND_ORE) {
            player.performCommand("c");
        } else if (currentItem.getType() == Material.END_STONE || currentItem.getType() == Material.END_PORTAL_FRAME) {
            player.performCommand("island togglewarp");
            player.performCommand("island");
        } else if (currentItem.getType() == Material.IRON_BARS && islandInfo.isLocked()) {
            player.performCommand("island unlock");
            player.performCommand("island");
        } else if (currentItem.getType() == Material.IRON_BARS && !islandInfo.isLocked()) {
            player.performCommand("island lock");
            player.performCommand("island");
        } else if (slotIndex == 17) {
            if (islandInfo.isLeader(player) && config.getYamlConfig().getBoolean("island-schemes-enabled", true)) {
                player.openInventory(createRestartGUI(player));
            } else {
                if (confirmHandler.durationLeft(player, "/is leave").isPositive()) {
                    player.performCommand("island leave");
                } else {
                    player.performCommand("island leave");
                    updateLeaveMenuItemTimer(player, event.getInventory(), currentItem);
                }
            }
        } else {
            if (!isExtraMenuAction(player, currentItem)) {
                player.performCommand("island");
            }
        }
    }

    private void updateLeaveMenuItemTimer(final Player player, final Inventory inventory, final ItemStack currentItem) {
        BukkitTask[] hackySharing = new BukkitTask[1];
        hackySharing[0] = scheduler.sync(() -> {
            Duration durationLeft = confirmHandler.durationLeft(player, "/is leave");
            if (inventory.getViewers().contains(player)) {
                updateLeaveMenuItem(inventory, currentItem, durationLeft);
            }
            if (!durationLeft.isPositive() || !inventory.getViewers().contains(player)) {
                if (hackySharing.length > 0 && hackySharing[0] != null) {
                    hackySharing[0].cancel();
                }
            }
        }, Duration.ZERO, Duration.ofSeconds(1));
    }

    private void updateLeaveMenuItem(Inventory inventory, ItemStack currentItem, Duration durationLeft) {
        if (currentItem == null || currentItem.getItemMeta() == null || currentItem.getItemMeta().getLore() == null) {
            return;
        }
        ItemMeta meta = requireNonNull(currentItem.getItemMeta());
        List<String> lore = meta.getLore();
        if (!durationLeft.isNegative()) {
            lore.set(lore.size() - 1, trLegacy("Click within <duration> to leave!",
                ERROR,
                unparsed("duration", TimeUtil.durationAsString(durationLeft), PRIMARY)));
        } else {
            lore.set(lore.size() - 1, trLegacy("Click to leave", ERROR));
        }
        meta.setLore(lore);
        currentItem.setItemMeta(meta);
        inventory.setItem(17, currentItem);
    }

    public Inventory createRestartGUI(Player player) {
        List<String> schemeNames = schematicHandler.getSchemeNames();
        int menuSize = (int) Math.ceil(getMaxSchemeIndex(schemeNames) / 9d) * 9;
        String title = trLegacy("Island Restart Menu", MENU);
        Inventory menu = Bukkit.createInventory(new UltimateHolder(player, title, MenuType.DEFAULT), menuSize, title);
        List<Component> lores = new ArrayList<>();
        ItemStack menuItem = new ItemStack(Material.OAK_SIGN, 1);
        ItemStackUtil.setComponentDisplayName(menuItem, tr("Return to the main menu", SECONDARY));
        ItemStackUtil.setComponentLore(menuItem, lores);
        menu.addItem(menuItem);
        lores.clear();

        updateRestartMenu(menu, player, schemeNames);
        if (confirmHandler.durationLeft(player, "/is restart").isPositive()) {
            updateRestartMenuTimer(player, menu);
        }
        return menu;
    }

    private void updateRestartMenu(Inventory menu, Player player, List<String> schemeNames) {
        ItemStack menuItem;
        ItemMeta meta;
        List<Component> lores;
        int index = 1;
        for (String schemeName : schemeNames) {
            IslandPerk islandPerk = perkLogic.getIslandPerk(schemeName);
            boolean enabled = config.getYamlConfig().getBoolean("island-schemes." + islandPerk.getSchemeName() + ".enabled", true);
            if (!enabled) {
                continue; // Skip
            }
            index = config.getYamlConfig().getInt("island-schemes." + islandPerk.getSchemeName() + ".index", index);
            menuItem = islandPerk.getDisplayItem();
            meta = requireNonNull(menuItem.getItemMeta());
            List<String> oldLore = meta.getLore();
            if (oldLore == null) {
                oldLore = List.of();
            }
            lores = new ArrayList<>(oldLore.stream().map(I18nUtil::fromLegacy).toList());
            if (player.hasPermission(islandPerk.getPermission())) {
                Duration durationLeft = confirmHandler.durationLeft(player, "/is restart");
                if (durationLeft.isPositive()) {
                    addLore(lores, tr("Click within <duration> to restart!",
                        ERROR,
                        unparsed("duration", TimeUtil.durationAsString(durationLeft), PRIMARY)));
                } else {
                    addLore(lores, tr("Click to restart!", SECONDARY));
                }
            } else {
                addLore(lores, tr("No access!<newline><muted>(<permission>)</muted>",
                    ERROR,
                    unparsed("permission", islandPerk.getPermission())));
            }
            ItemStackUtil.setComponentLore(menuItem, lores);
            menu.setItem(index++, menuItem);
        }
    }

    private void onClickLogMenu(InventoryClickEvent event, Player p, int slotIndex) {
        event.setCancelled(true);
        if (slotIndex < 0 || slotIndex > 35) {
            return;
        }
        p.performCommand("island");
    }

    private void onClickChallengeMenu(InventoryClickEvent event, ItemStack currentItem, Player p, String inventoryName) {
        int slotIndex = event.getRawSlot();
        event.setCancelled(true);
        Matcher m = CHALLENGE_PAGE_HEADER.matcher(inventoryName);
        int page = 1;
        int max = challengeLogic.getTotalPages();
        if (m.find()) {
            page = Integer.parseInt(m.group("p"));
            max = Integer.parseInt(m.group("max"));
        }
        ItemStack item = event.getInventory().getItem(event.getInventory().getSize() - 9);
        String playerName = item != null && item.hasItemMeta() && requireNonNull(item.getItemMeta()).getLore() != null
            && !item.getItemMeta().getLore().isEmpty()
            ? item.getItemMeta().getLore().getFirst()
            : null;
        if (playerName != null && playerName.trim().isEmpty()) {
            playerName = null;
        }
        // Last row is pagination
        if (slotIndex >= CHALLENGE_PAGE_SIZE && slotIndex < CHALLENGE_PAGE_SIZE + COLS_PER_ROW
            && currentItem != null && currentItem.getType() != Material.AIR) {
            // Pagination
            p.openInventory(displayChallengeGUI(p, currentItem.getAmount(), playerName));
            return;
        }
        // If in action bar or anywhere else, just bail out
        if (slotIndex < 0 || slotIndex > CHALLENGE_PAGE_SIZE || isAirOrLocked(currentItem)) {
            return;
        }
        if ((slotIndex % 9) > 0) { // 0,9... are the rank-headers...
            if (currentItem.getItemMeta() != null) {
                String challenge = requireNonNull(currentItem.getItemMeta()).getDisplayName();
                String challengeName = stripFormatting(challenge);
                p.performCommand("c c " + challengeName);
            }
            p.openInventory(displayChallengeGUI(p, page, playerName));
        } else {
            if (slotIndex < (CHALLENGE_PAGE_SIZE / 2)) { // Upper half
                if (page > 1) {
                    p.openInventory(displayChallengeGUI(p, page - 1, playerName));
                } else {
                    p.performCommand("island");
                }
            } else if (page < max) {
                p.openInventory(displayChallengeGUI(p, page + 1, playerName));
            } else {
                p.performCommand("island");
            }
        }
    }

    private boolean isAirOrLocked(ItemStack currentItem) {
        return currentItem != null && currentItem.getType() == Material.AIR ||
            currentItem != null && currentItem.getItemMeta() != null && currentItem.getItemMeta().getDisplayName().equals(trLegacy("Locked Challenge", ERROR));
    }

    private void onClickPermissionMenu(InventoryClickEvent event, ItemStack currentItem, Player p, String inventoryName, int slotIndex) {
        event.setCancelled(true);
        if (slotIndex < 0 || slotIndex > 35) {
            return;
        }
        IslandInfo islandInfo = plugin.getIslandInfo(p);
        if (!plugin.getIslandInfo(p).isLeader(p)) {
            p.openInventory(displayPartyGUI(p));
        }
        String[] playerPerm = inventoryName.split(" ");
        String name = playerPerm[0];
        UUID uuid = plugin.getPlayerDB().getUUIDFromName(name);
        PlayerProfile profile = uuid != null ? Bukkit.createPlayerProfile(uuid, name) : null;
        ItemStack skullItem = event.getInventory().getItem(1);
        if (skullItem != null && skullItem.getType().equals(Material.PLAYER_HEAD)) {
            ItemMeta meta = requireNonNull(skullItem.getItemMeta());
            if (meta instanceof SkullMeta) {
                profile = ((SkullMeta) meta).getOwnerProfile();
            }
        }
        if (profile == null || profile.getUniqueId() == null) {
            p.openInventory(displayPartyGUI(p));
            return;
        }
        for (PartyPermissionMenuItem item : permissionMenuItems) {
            if (currentItem.getType() == item.getIcon().getType()) {
                islandInfo.togglePerm(profile.getUniqueId(), item.getPerm());
                p.openInventory(displayPartyPlayerGUI(p, profile));
                return;
            }
        }
        if (currentItem.getType() == Material.OAK_SIGN) {
            p.openInventory(displayPartyGUI(p));
        } else {
            p.openInventory(displayPartyPlayerGUI(p, profile));
        }
    }

    private void onClickPartyMenu(InventoryClickEvent event, ItemStack currentItem, Player p, ItemMeta meta, SkullMeta skull, int slotIndex) {
        event.setCancelled(true);
        if (slotIndex < 0 || slotIndex > 35) {
            return;
        }
        if (meta == null || currentItem.getType() == Material.OAK_SIGN) {
            p.performCommand("island");
        } else if (skull != null && skull.hasOwner() && plugin.getIslandInfo(p).isLeader(p)) {
            p.openInventory(displayPartyPlayerGUI(p, skull.getOwnerProfile()));
        }
    }

    public List<PartyPermissionMenuItem> getPermissionMenuItems() {
        return permissionMenuItems;
    }

    private String resolvePartyMemberName(@NotNull PlayerProfile profile, @NotNull UUID memberId) {
        String profileName = profile.getName();
        if (profileName != null && !profileName.trim().isEmpty()) {
            return profileName;
        }
        String resolvedName = plugin.getPlayerDB().getName(memberId);
        if (resolvedName != null && !resolvedName.trim().isEmpty()) {
            return resolvedName;
        }
        return memberId.toString();
    }
}
