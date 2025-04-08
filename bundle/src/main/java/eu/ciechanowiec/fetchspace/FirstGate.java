package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.fetchspace.command.WithMappedTGCommand;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.telegram.api.TGCommand;
import eu.ciechanowiec.sling.telegram.api.TGRootUpdatesReceiver;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesRegistrar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.propertytypes.ServiceDescription;

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
    private final MaxTGFileUploadSize maxTgFileUploadSize;

    @Activate
    public FirstGate(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        TGUpdatesRegistrar tgUpdatesRegistrar,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        SendLink sendLink,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        ResourceAccess resourceAccess,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        MaxTGFileUploadSize maxTgFileUploadSize
    ) {
        this.tgUpdatesRegistrar = tgUpdatesRegistrar;
        this.sendLink = sendLink;
        this.resourceAccess = resourceAccess;
        this.withMappedTGCommands = new ArrayList<>();
        this.maxTgFileUploadSize = maxTgFileUploadSize;
    }

    @SneakyThrows
    @Override
    public void receive(TGUpdate tgUpdate) {
        log.debug("Received {}", tgUpdate);
        Optional.of(tgUpdate)
            .filter(consideredTGUpdate -> !maxTgFileUploadSize.isTooBig(consideredTGUpdate))
            .map(
                tgUpdateToProcess -> {
                    WarnAboutBigTGFile warnAboutBigTGFile = new WarnAboutBigTGFile(tgUpdatesRegistrar, resourceAccess);
                    warnAboutBigTGFile.warnIfBig(tgUpdate, maxTgFileUploadSize);
                    return tgUpdateToProcess;
                }
            ).map(tgUpdatesRegistrar::register)
            .ifPresentOrElse(
                registeredUpdate -> mapToTGCommand(registeredUpdate).ifPresentOrElse(
                    withMappedTGCommand -> withMappedTGCommand.receive(registeredUpdate),
                    () -> sendLink.receive(registeredUpdate)
                ),
                () -> {
                    RejectTooBig rejectTooBig = new RejectTooBig(tgUpdatesRegistrar, resourceAccess);
                    rejectTooBig.reject(tgUpdate, maxTgFileUploadSize);
                }
            );
    }

    private Optional<WithMappedTGCommand> mapToTGCommand(TGUpdate tgUpdate) {
        TGCommand actualTGCommand = tgUpdate.tgMessage().tgCommand();
        return withMappedTGCommands.stream()
            .filter(withMappedTGCommand -> withMappedTGCommand.mappedTGCommand().equals(actualTGCommand))
            .findFirst();
    }
}
