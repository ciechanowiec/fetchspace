package eu.ciechanowiec.fetchspace.command;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.telegram.TGUpdateBasic;
import eu.ciechanowiec.sling.telegram.api.TGCommand;
import eu.ciechanowiec.sling.telegram.api.TGCommands;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesRegistrar;
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

@Component(
        service = {WithMappedTGCommand.class, StartCommandAction.class},
        immediate = true
)
@Slf4j
@ServiceDescription("Action for '/start' command")
public class StartCommandAction implements WithMappedTGCommand {

    private final TGCommands tgCommands;
    private final TGUpdatesRegistrar tgUpdatesRegistrar;
    private final FullResourceAccess fullResourceAccess;

    @Activate
    public StartCommandAction(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            TGCommands tgCommands,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            TGUpdatesRegistrar tgUpdatesRegistrar,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            FullResourceAccess fullResourceAccess
    ) {
        this.tgCommands = tgCommands;
        this.tgUpdatesRegistrar = tgUpdatesRegistrar;
        this.fullResourceAccess = fullResourceAccess;
    }

    @Override
    public TGCommand mappedTGCommand() {
        return tgCommands.of("/start", true);
    }

    @SneakyThrows
    @Override
    public void receive(TGUpdate tgUpdate) {
        log.trace("Received {}", tgUpdate);
        SendMessage sendMessage = new SendMessage(
                tgUpdate.tgChatID().asString(),
                "This bot instantly generates a shareable link for any file you upload. Just send a file "
                   + "as an attachment, and receive a public internet link to easily share it with anyone 🌐"
        );
        sendMessage.setReplyToMessageId(tgUpdate.tgMessage().tgMessageID().asInt());
        Message sentMessage = tgUpdate.tgBot().tgIOGate().execute(sendMessage);
        Update update = new Update();
        update.setMessage(sentMessage);
        tgUpdatesRegistrar.register(new TGUpdateBasic(update, tgUpdate.tgBot(), fullResourceAccess));
    }
}
