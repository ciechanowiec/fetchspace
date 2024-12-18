package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.telegram.api.TGAssets;
import eu.ciechanowiec.sling.telegram.api.TGPhoto;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;

class UnaryTGPhoto {

    private final TGUpdate tgUpdate;

    UnaryTGPhoto(TGUpdate tgUpdate) {
        this.tgUpdate = tgUpdate;
    }

    @SuppressWarnings({"unchecked", "squid:S1905", "squid:S1612", "PMD.LambdaCanBeMethodReference"})
    Optional<TGPhoto> get() {
        TGAssets<TGPhoto> tgPhotos = tgUpdate.tgMessage().tgPhotos();
        boolean isUnaryPhoto = tgPhotos.all().size() == NumberUtils.INTEGER_ONE;
        return (Optional<TGPhoto>) Conditional.conditional(isUnaryPhoto)
                .onTrue(() -> tgPhotos.all().stream().findFirst())
                .onFalse(() -> Optional.empty())
                .get(Optional.class);
    }
}
