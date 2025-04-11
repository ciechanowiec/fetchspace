package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import java.util.concurrent.atomic.AtomicReference;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Component(
    service = MaxTGFileUploadSize.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = MaxTGFileUploadSizeConfig.class)
@Slf4j
@ToString
public class MaxTGFileUploadSize {

    private final AtomicReference<DataSize> value;

    @Activate
    public MaxTGFileUploadSize(MaxTGFileUploadSizeConfig config) {
        this.value = new AtomicReference<>(
            new DataSize(config.tg$_$file_max$_$upload$_$size_bytes(), DataUnit.BYTES)
        );
        log.debug("Initialized {}", this);
    }

    @Modified
    void modified(MaxTGFileUploadSizeConfig config) {
        value.set(new DataSize(config.tg$_$file_max$_$upload$_$size_bytes(), DataUnit.BYTES));
    }

    boolean isTooBig(TGUpdate tgUpdate) {
        return new SizeOfTGFileFromTGUpdate(tgUpdate).get()
            .map(size -> size.biggerThan(value()))
            .orElse(false);
    }

    @SuppressWarnings("MagicNumber")
    boolean isRiskySize(TGUpdate tgUpdate) {
        return new SizeOfTGFileFromTGUpdate(tgUpdate).get()
            .map(size -> size.biggerThan(new DataSize(200, DataUnit.MEGABYTES)))
            .orElse(false);
    }

    DataSize value() {
        return value.get();
    }
}
