package de.dosmike.sponge.vshop.integration.tms;

import com.google.inject.internal.cglib.proxy.$UndeclaredThrowableException;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.integration.tms.deps.TMSQueryBuilder;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

public class PriceQueryBuilder {

    public PriceQueryBuilder(){}

    protected ItemStackSnapshot item = null;
    protected int amount = 0;
    protected BigDecimal basePrice = null;
    protected Currency currency = null;
    protected UUID shopId = null;
    protected UUID playerId = null;

    public PriceQueryBuilder setItem(ItemStackSnapshot item) {
        this.item = item;
        return this;
    }
    public PriceQueryBuilder setItem(ItemStack item) {
        this.item = item.createSnapshot();
        return this;
    }
    public PriceQueryBuilder setPricingInformation(int quantity, BigDecimal basePrice, Currency currency) {
        this.amount = quantity;
        this.basePrice = basePrice;
        this.currency = currency;
        return this;
    }
    public PriceQueryBuilder bindShop(UUID shopId) {
        this.shopId = shopId;
        return this;
    }
    public PriceQueryBuilder bindPlayer(UUID playerId) {
        this.playerId = playerId;
        return this;
    }
    public PriceQueryBuilder bindPlayer(Player player) {
        this.playerId = player.getUniqueId();
        return this;
    }
    public PriceQuery build() {
        if (item == null)
            throw new IllegalStateException("Item has to be set");
        if (amount < 1)
            throw new IllegalStateException("Amount has to be positive non-zero integer");
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO)<=0)
            throw new IllegalStateException("BasePrice hast to be positive non-zero BigDecimal");
        if (currency == null)
            throw new IllegalStateException("Currency has to be set");
        return new PriceQuery(this);
    }

    private static Supplier<PriceQueryBuilder> builder=null;
    public static PriceQueryBuilder get() {
        if (builder != null) return builder.get();
        if (Sponge.getPluginManager().getPlugin("toomuchstock").isPresent()) {
            try {
                builder = TMSQueryBuilder::new;
            } catch (Throwable t) {
                VillagerShops.w("Could not load TooMuchStock-Integration!");
                t.printStackTrace();
                builder = PriceQueryBuilder::new;
            }
        } else {
            builder = PriceQueryBuilder::new;
        }
        return builder.get();
    }

}
