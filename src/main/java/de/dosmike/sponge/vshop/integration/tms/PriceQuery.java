package de.dosmike.sponge.vshop.integration.tms;

import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;

import java.math.BigDecimal;
import java.util.UUID;

public class PriceQuery {

    protected ItemStackSnapshot item;
    protected int amount;
    protected BigDecimal basePrice;
    protected Currency currency;
    protected UUID shopId;
    protected UUID playerId;
    protected UniqueAccount account;

    protected PriceQuery(PriceQueryBuilder builder) {
        item = builder.item;
        amount = builder.amount;
        basePrice = builder.basePrice;
        currency = builder.currency;
        shopId = builder.shopId;
        playerId = builder.playerId;
        account = VillagerShops.getEconomy().getOrCreateAccount(builder.playerId).orElseThrow(()->new RuntimeException("Could not fetch account for player "+builder.playerId.toString()));
    }

    public ItemStackSnapshot getItem() { return item; }
    public int getAmount() { return amount; }
    public BigDecimal getBasePrice() { return basePrice; }
    public Currency getCurrency() { return currency; }
    public UUID getShopId() { return shopId; }
    public UUID getPlayerId() { return playerId; }
    public UniqueAccount getAccount() { return account; }

    /** Create a Snapshot of the current price for the specified amount.
     * @return how much this item currently costs for this player (in this shop) */
    public BigDecimal getDisplayPriceBuying() {
        return basePrice.multiply(BigDecimal.valueOf(amount));
    }
    /** Create a Snapshot of the current price for the specified amount.
     * @return how much this player currently gets for this item (in this shop) */
    public BigDecimal getDisplayPriceSelling() {
        return basePrice.multiply(BigDecimal.valueOf(amount));
    }
    /**
     * Calculates that maximum affordable amount of items (ignoring inventories) and
     * creates a Snapshot of prices per amount for the player.
     * Also contains a #confirm function that confirms the transaction over a certain
     * amount of items to TooMuchStock, adjusting prices accordingly.
     * @return the price listing for the player, seeking to purchase this item
     */
    public Listing getTradeListingBuying() {
        return new Listing(this, true);
    }
    /**
     * Calculates that maximum affordable amount of items (ignoring inventories) and
     * creates a Snapshot of prices per amount for the player.
     * Also contains a #confirm function that confirms the transaction over a certain
     * amount of items to TooMuchStock, adjusting prices accordingly.
     * @return the price listing for the player, seeking to sell this item
     */
    public Listing getTradeListingSelling() {
        return new Listing(this, false);
    }

}
