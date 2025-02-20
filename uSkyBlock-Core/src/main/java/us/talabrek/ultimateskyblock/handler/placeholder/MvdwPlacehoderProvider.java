package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class MvdwPlacehoderProvider implements Provider<MVdWPlaceholderAPI> {

    private final PlaceholderAPI.PlaceholderReplacer replacer;

    @Inject
    public MvdwPlacehoderProvider(PlaceholderAPI.PlaceholderReplacer replacer) {
        this.replacer = replacer;
    }

    @Override
    public MVdWPlaceholderAPI get() {
        return new MVdWPlaceholderAPI(replacer);
    }
}
