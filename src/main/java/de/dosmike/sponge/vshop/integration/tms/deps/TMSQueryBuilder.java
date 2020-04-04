package de.dosmike.sponge.vshop.integration.tms.deps;

import de.dosmike.sponge.toomuchstock.service.PriceCalculationService;
import de.dosmike.sponge.vshop.integration.tms.PriceQuery;
import de.dosmike.sponge.vshop.integration.tms.PriceQueryBuilder;
import org.spongepowered.api.Sponge;

import java.math.BigDecimal;

public class TMSQueryBuilder extends PriceQueryBuilder {

    static PriceCalculationService pcs = null;

    public TMSQueryBuilder() {
        if (pcs == null)
            pcs = Sponge.getServiceManager().provide(PriceCalculationService.class).get();
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
        if (playerId == null)
            throw new IllegalStateException("Listings require PlayerID");
        return new TMSQuery(this);
    }

}
