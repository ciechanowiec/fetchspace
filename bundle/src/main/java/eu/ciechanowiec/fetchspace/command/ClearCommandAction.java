package eu.ciechanowiec.fetchspace.command;

import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DeletableResource;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.telegram.TGUpdateBasic;
import eu.ciechanowiec.sling.telegram.api.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;

@Component(
        service = {WithMappedTGCommand.class, ClearCommandAction.class},
        immediate = true
)
@Slf4j
@ServiceDescription("Action for '/clear' command")
public class ClearCommandAction implements WithMappedTGCommand {

    private final TGCommands tgCommands;
    private final TGChats tgChats;
    private final TGUpdatesRegistrar tgUpdatesRegistrar;
    private final FullResourceAccess fullResourceAccess;

    @Activate
    public ClearCommandAction(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            TGCommands tgCommands,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            TGChats tgChats,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            TGUpdatesRegistrar tgUpdatesRegistrar,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            FullResourceAccess fullResourceAccess
    ) {
        this.tgCommands = tgCommands;
        this.tgChats = tgChats;
        this.tgUpdatesRegistrar = tgUpdatesRegistrar;
        this.fullResourceAccess = fullResourceAccess;
    }

    @Override
    public TGCommand mappedTGCommand() {
        return tgCommands.of("/clear", true);
    }

    @SneakyThrows
    @Override
    public void receive(TGUpdate tgUpdate) {
        log.trace("Received {}", tgUpdate);
        JCRPath chatJCRPath = tgChats.getOrCreate(tgUpdate, tgUpdate.tgBot()).jcrPath();
        log.debug("Deleting all assets in {}", chatJCRPath);
        new AssetsRepository(fullResourceAccess).find(chatJCRPath)
                .stream()
                .map(asset -> new DeletableResource(asset, fullResourceAccess))
                .map(DeletableResource::delete)
                .flatMap(Optional::stream)
                .forEach(deleted -> log.trace("Deleted {}", deleted));
        SendMessage sendMessage = new SendMessage(
                tgUpdate.tgChatID().asString(),
                "All files that you uploaded to this bot were deleted "
                   + "and will not be available for downloading anymore 🗑️"
        );
        sendMessage.setReplyToMessageId(tgUpdate.tgMessage().tgMessageID().asInt());
        Message sentMessage = tgUpdate.tgBot().tgIOGate().execute(sendMessage);
        Update update = new Update();
        update.setMessage(sentMessage);
        tgUpdatesRegistrar.register(new TGUpdateBasic(update, tgUpdate.tgBot(), fullResourceAccess));
    }
}
