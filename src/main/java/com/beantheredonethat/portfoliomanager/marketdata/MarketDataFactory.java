package com.beantheredonethat.portfoliomanager.marketdata;

import com.beantheredonethat.portfoliomanager.exception.MarketDataException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MarketDataFactory {

    private final Map<AssetType, MarketDataService> services = new EnumMap<>(AssetType.class);

    public MarketDataFactory(List<MarketDataService> implementations) {
        for (MarketDataService svc : implementations) {
            Set<AssetType> supported = svc.supportedAssetTypes();
            if (supported != null) {
                for (AssetType t : supported) {
                    services.put(t, svc);
                }
            }
        }
    }

    public MarketDataService getService(AssetType type) {
        MarketDataService svc = services.get(type);
        if (svc == null) {
            throw new MarketDataException("No market data service registered for asset type: " + type);
        }
        return svc;
    }
}

