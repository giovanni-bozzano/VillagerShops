package de.dosmike.sponge.vshop.integration.tms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Listing {

    protected List<BigDecimal> priceStack;
    protected Listing() {}
    protected Listing(PriceQuery query, boolean purchase) {
        BigDecimal balance = query.account.getBalance(query.currency);
        int maxAffordable = query.item.getType().getMaxStackQuantity();
        if (purchase) {
            BigDecimal affordableBD = balance.divide(query.basePrice, BigDecimal.ROUND_FLOOR);
            maxAffordable = affordableBD.compareTo(BigDecimal.valueOf(maxAffordable)) >= 0 ? maxAffordable : affordableBD.intValue();
        }  // accounts do not expose a capacity although the support it
        priceStack = new ArrayList<>(maxAffordable);
        for (int i = 1; i <= maxAffordable; i++) {
            priceStack.add(query.basePrice.multiply(BigDecimal.valueOf(i)));
        }
    }

    /** return trade-able amount constrained by trade and account balance limit.
     * this does not take inventories into account.<br>
     * Keep in mind that this information does not update!
     * You need to request another instance for that.
     */
    public int getTradeableAmount() {
        return priceStack.size();
    }
    /**
     * return the price for the specified amount of items<br>
     * Keep in mind that this information does not update!
     * You need to request another instance for that.
     */
    public BigDecimal getPrice(int forAmount) {
        if (forAmount < 0)
            throw new IllegalArgumentException("Can't return price for negative quantities");
        if (forAmount == 0)
            return BigDecimal.ZERO;
        if (forAmount > priceStack.size())
            throw new IllegalArgumentException("This amount is not tradeable by the player");
        return priceStack.get(forAmount-1);
    }
    /**
     * confirm a transaction over the specified amount of items.
     * might change prices if TooMuchStock is installed.
     */
    public void confirm(int amount) {
        /**/
    }

}
