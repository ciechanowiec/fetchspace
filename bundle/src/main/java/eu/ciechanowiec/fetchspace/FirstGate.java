package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.fetchspace.command.WithMappedTGCommand;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.telegram.api.TGCommand;
import eu.ciechanowiec.sling.telegram.api.TGRootUpdatesReceiver;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesRegistrar;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.ArrayList;
import java.util.Collection;

@Component(
        service = {FirstGate.class, TGRootUpdatesReceiver.class},
        immediate = true
)
@Slf4j
@ServiceDescription("First gate for incoming TG updates")
public class FirstGate implements TGRootUpdatesReceiver {

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private final Collection<WithMappedTGCommand> withMappedTGCommands;
    private final TGUpdatesRegistrar tgUpdatesRegistrar;
    private final SendLink sendLink;
    private final ResourceAccess resourceAccess;

    @Activate
    public FirstGate(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            TGUpdatesRegistrar tgUpdatesRegistrar,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            SendLink sendLink,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            ResourceAccess resourceAccess
    ) {
        this.tgUpdatesRegistrar = tgUpdatesRegistrar;
        this.sendLink = sendLink;
        this.resourceAccess = resourceAccess;
        this.withMappedTGCommands = new ArrayList<>();
    }

    @SneakyThrows
    @Override
    public void receive(TGUpdate tgUpdate) {
        log.debug("Received {}", tgUpdate);
        TGFileSize tgFileSize = new TGFileSize(tgUpdate);
        TGUpdate registeredUpdate = tgUpdatesRegistrar.register(tgUpdate);
        TGCommand actualTGCommand = tgUpdate.tgMessage().tgCommand();
        withMappedTGCommands.stream()
                .filter(withMappedTGCommand -> withMappedTGCommand.mappedTGCommand().equals(actualTGCommand))
                .findFirst()
                .ifPresentOrElse(
                        withMappedTGCommand -> withMappedTGCommand.receive(registeredUpdate),
                        () -> dispatch(registeredUpdate, tgFileSize)
                );
    }

    private void dispatch(TGUpdate tgUpdate, TGFileSize tgFileSize) {
        if (tgFileSize.isTooBig()) {
            new RejectTooBig(tgUpdatesRegistrar, resourceAccess).reject(tgUpdate, tgFileSize.maxDocumentSize());
        } else {
            sendLink.receive(tgUpdate);
        }
    }
}
