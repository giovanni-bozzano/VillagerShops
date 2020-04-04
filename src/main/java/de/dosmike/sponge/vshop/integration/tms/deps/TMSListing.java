package de.dosmike.sponge.vshop.integration.tms.deps;

import de.dosmike.sponge.toomuchstock.service.TransactionPreview;
import de.dosmike.sponge.vshop.integration.tms.Listing;

import java.math.BigDecimal;

public class TMSListing extends Listing {

    TransactionPreview preview;
    public TMSListing(TMSQuery query, boolean purchase) {
        super();
        if (purchase)
            preview = TMSQueryBuilder.pcs.getPurchaseInformation(query.getItem(), query.getAmount(), query.getBasePrice(), query.getCurrency(), query.getShopId(), query.getPlayerId());
        else
            preview = TMSQueryBuilder.pcs.getSellingInformation(query.getItem(), query.getAmount(), query.getBasePrice(), query.getCurrency(), query.getShopId(), query.getPlayerId());
    }

    @Override
    public int getTradeableAmount() {
        return preview.getAffordableAmount();
    }

    @Override
    public BigDecimal getPrice(int forAmount) {
        return preview.getCumulativeValueFor(forAmount);
    }

    @Override
    public void confirm(int amount) {
        preview.confirm(amount);
    }
}
