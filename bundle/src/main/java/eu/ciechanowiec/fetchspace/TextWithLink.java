package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.sling.rocket.asset.api.DownloadLink;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.telegram.api.TGAsset;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;

class TextWithLink {

    private final MemoizingSupplier<String> textSupplier;

    TextWithLink(TGUpdate tgUpdate, DownloadLink downloadLink) {
        textSupplier = new MemoizingSupplier<>(
                () -> tgUpdate.tgMessage()
                        .tgDocument()
                        .map(TGAsset.class::cast)
                        .or(() -> tgUpdate.tgMessage().tgVideo())
                        .or(() -> tgUpdate.tgMessage().tgAudio())
                        .or(() -> tgUpdate.tgMessage().tgVoice())
                        .or(() -> new UnaryTGPhoto(tgUpdate).get())
                        .flatMap(TGAsset::asset)
                        .map(downloadLink::generate)
                        .map(link -> "📥 Here is the download link:%n%n<a href=\"%s\">%s</a>".formatted(link, link))
                        .orElse("Unable to generate the download link. Try again via sending a file as an attachment 📎")
        );
    }

    String get() {
        return textSupplier.get();
    }
}
