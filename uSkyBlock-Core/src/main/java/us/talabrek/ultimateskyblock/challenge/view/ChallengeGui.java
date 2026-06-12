package us.talabrek.ultimateskyblock.challenge.view;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.gui.InventoryButton;
import us.talabrek.ultimateskyblock.gui.InventoryGui;
import us.talabrek.ultimateskyblock.gui.PaginationEntry;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

public class ChallengeGui extends InventoryGui {
    public static final int CHALLENGE_SLOT_COUNT = 9;
    public static final int INVENTORY_SIZE = 54;
    public static final int PAGINATION_ROW_START = 45;

    private final ChallengePageView pageView;
    private final IntConsumer pageClickHandler;
    private final BiConsumer<Player, ChallengeSlotView> challengeClickHandler;

    public ChallengeGui(
        @NotNull ChallengePageView pageView,
        @NotNull IntConsumer pageClickHandler,
        @NotNull BiConsumer<Player, ChallengeSlotView> challengeClickHandler
    ) {
        super(createInventory(pageView));
        this.pageView = Objects.requireNonNull(pageView, "pageView");
        this.pageClickHandler = Objects.requireNonNull(pageClickHandler, "pageClickHandler");
        this.challengeClickHandler = Objects.requireNonNull(challengeClickHandler, "challengeClickHandler");
        configureButtons();
    }

    private static Inventory createInventory(ChallengePageView pageView) {
        String title = trLegacy("Challenge Menu (<page>/<total>)",
            number("page", pageView.pageNumber()),
            number("total", pageView.totalPages()));
        return Bukkit.createInventory(null, INVENTORY_SIZE, title);
    }

    private void configureButtons() {
        for (ChallengeRankRowView rowView : pageView.rows()) {
            for (ChallengeSlotView slotView : rowView.slots()) {
                addButton(slotView.slotIndex(), createChallengeButton(slotView));
            }
        }
        for (PaginationEntry paginationEntry : pageView.pagination().entries()) {
            addButton(PAGINATION_ROW_START + paginationEntry.slotIndex(), createPaginationButton(paginationEntry));
        }
    }

    private InventoryButton createChallengeButton(ChallengeSlotView slotView) {
        return new InventoryButton()
            .creator(player -> createChallengeIcon(slotView))
            .consumer((player, event) -> {
                if (slotView.clickable()) {
                    challengeClickHandler.accept(player, slotView);
                }
            });
    }

    private ItemStack createChallengeIcon(ChallengeSlotView slotView) {
        ItemStack item = slotView.icon().create();
        ItemStackUtil.setComponentDisplayName(item, slotView.title());
        ItemStackUtil.setComponentLore(item, slotView.lore());
        if (slotView.completed()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.LOYALTY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private InventoryButton createPaginationButton(PaginationEntry entry) {
        return new InventoryButton()
            .creator(player -> createPaginationIcon(entry))
            .consumer((player, event) -> pageClickHandler.accept(entry.pageNumber()));
    }

    private ItemStack createPaginationIcon(PaginationEntry entry) {
        Material material = entry.current() ? Material.WRITABLE_BOOK : Material.BOOK;
        ItemStack item = new ItemStack(material);
        Component title = switch (entry.kind()) {
            case FIRST -> tr("First Page", MUTED);
            case LAST -> tr("Last Page", MUTED);
            case PAGE -> entry.current()
                ? tr("Current page", MUTED)
                : tr("Page <page>", MUTED, number("page", entry.pageNumber()));
        };
        ItemStackUtil.setComponentDisplayName(item, title);
        item.setAmount(Math.min(entry.pageNumber(), 64));
        return item;
    }
}
