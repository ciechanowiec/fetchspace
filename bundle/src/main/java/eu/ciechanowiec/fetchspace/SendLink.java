package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.api.DownloadLink;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.telegram.TGUpdateBasic;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesReceiver;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesRegistrar;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.stream.Stream;

@Component(
        service = {SendLink.class, TGUpdatesReceiver.class},
        immediate = true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Slf4j
@ToString
@Designate(
        ocd = SendLinkConfig.class
)
@ServiceDescription("Sends to a TG user a link to the document attached to the passed update")
public class SendLink implements TGUpdatesReceiver {

    private final DownloadLink downloadLink;
    private final TGUpdatesRegistrar tgUpdatesRegistrar;
    private final ResourceAccess resourceAccess;
    private SendLinkConfig config;

    @Activate
    public SendLink(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            DownloadLink downloadLink,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            TGUpdatesRegistrar tgUpdatesRegistrar,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            ResourceAccess resourceAccess,
            SendLinkConfig config
    ) {
        this.downloadLink = downloadLink;
        this.tgUpdatesRegistrar = tgUpdatesRegistrar;
        this.resourceAccess = resourceAccess;
        this.config = config;
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(SendLinkConfig config) {
        log.info("Reconfiguring {}", this);
        this.config = config;
        log.info("Reconfigured {}", this);
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("LambdaBodyLength")
    public void receive(TGUpdate tgUpdate) {
        log.trace("Sending link for {}", tgUpdate);
        String textMessage = new TextWithLink(tgUpdate, downloadLink).get();
        SendMessage messageForRequester = new SendMessage(tgUpdate.tgChatID().asString(), textMessage);
        messageForRequester.setParseMode(ParseMode.HTML);
        messageForRequester.setReplyToMessageId(tgUpdate.tgMessage().tgMessageID().asInt());
        Message sentMessageToRequester = tgUpdate.tgBot().tgIOGate().execute(messageForRequester);
        log.debug("Sent {}", sentMessageToRequester);
        Update update = new Update();
        update.setMessage(sentMessageToRequester);
        tgUpdatesRegistrar.register(new TGUpdateBasic(update, tgUpdate.tgBot(), resourceAccess));
        Conditional.onTrueExecute(
                textMessage.contains("Here is the download link"),
                () -> Stream.of(config.chats$_$with$_$moderators_ids()).forEach(
                        SneakyConsumer.sneaky(
                                moderatorChatID -> {
                                    SendMessage messageForModerator = new SendMessage(moderatorChatID, textMessage);
                                    messageForModerator.setParseMode(ParseMode.HTML);
                                    Message sentMessageToModerator = tgUpdate.tgBot()
                                                                             .tgIOGate()
                                                                             .execute(messageForModerator);
                                    log.debug("Sent {}", sentMessageToModerator);
                                    Update updateForModerator = new Update();
                                    updateForModerator.setMessage(sentMessageToModerator);
                                    tgUpdatesRegistrar.register(
                                            new TGUpdateBasic(updateForModerator, tgUpdate.tgBot(), resourceAccess)
                                    );
                                }
                        )
                )
        );
    }
}
