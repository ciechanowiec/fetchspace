package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import eu.ciechanowiec.sling.telegram.api.TGAsset;
import eu.ciechanowiec.sling.telegram.api.TGFile;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;

class TGFileSize {

    private final TGUpdate tgUpdate;

    TGFileSize(TGUpdate tgUpdate) {
        this.tgUpdate = tgUpdate;
    }

    boolean isTooBig() {
        return tgUpdate.tgMessage()
                .tgDocument()
                .map(TGAsset.class::cast)
                .or(() -> tgUpdate.tgMessage().tgVideo())
                .or(() -> tgUpdate.tgMessage().tgAudio())
                .or(() -> tgUpdate.tgMessage().tgVoice())
                .map(TGAsset::tgFile)
                .map(TGFile::size)
                .map(size -> size.biggerThan(maxDocumentSize()))
                .orElse(false);
    }

    @SuppressWarnings("MagicNumber")
    DataSize maxDocumentSize() {
        return new DataSize(20, DataUnit.MEGABYTES);
    }
}
