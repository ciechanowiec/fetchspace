package eu.ciechanowiec.fetchspace.command;

import eu.ciechanowiec.sling.telegram.api.TGCommand;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesReceiver;

public interface WithMappedTGCommand extends TGUpdatesReceiver {

    TGCommand mappedTGCommand();
}
