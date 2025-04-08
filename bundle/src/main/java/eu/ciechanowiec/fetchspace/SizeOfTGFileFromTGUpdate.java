package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.telegram.api.TGAsset;
import eu.ciechanowiec.sling.telegram.api.TGFile;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class SizeOfTGFileFromTGUpdate {

    private final TGUpdate tgUpdate;

    SizeOfTGFileFromTGUpdate(TGUpdate tgUpdate) {
        this.tgUpdate = tgUpdate;
    }

    Optional<DataSize> get() {
        Optional<DataSize> dataSize = tgUpdate.tgMessage()
            .tgDocument()
            .map(TGAsset.class::cast)
            .or(() -> tgUpdate.tgMessage().tgVideo())
            .or(() -> tgUpdate.tgMessage().tgAudio())
            .or(() -> tgUpdate.tgMessage().tgVoice())
            .map(TGAsset::tgFile)
            .map(TGFile::size);
        log.trace("Size of file from {} is {}", tgUpdate, dataSize);
        return dataSize;
    }
}
