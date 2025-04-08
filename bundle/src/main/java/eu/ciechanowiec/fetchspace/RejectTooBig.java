package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.telegram.TGUpdateBasic;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesRegistrar;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
class RejectTooBig {

    private final TGUpdatesRegistrar tgUpdatesRegistrar;
    private final ResourceAccess resourceAccess;

    RejectTooBig(TGUpdatesRegistrar tgUpdatesRegistrar, ResourceAccess resourceAccess) {
        this.tgUpdatesRegistrar = tgUpdatesRegistrar;
        this.resourceAccess = resourceAccess;
    }

    @SneakyThrows
    void reject(TGUpdate tgUpdate, MaxTGFileUploadSize maxTGFileUploadSize) {
        log.trace("Rejecting too big file from {}", tgUpdate);
        String messageText = "🚧 The file you've sent is too big. Maximum size is %dMB. Try again with a smaller file."
            .formatted((int) maxTGFileUploadSize.value().megabytes());
        SendMessage sendMessage = new SendMessage(tgUpdate.tgChatID().asString(), messageText);
        sendMessage.setParseMode(ParseMode.HTML);
        sendMessage.setReplyToMessageId(tgUpdate.tgMessage().tgMessageID().asInt());
        Message sentMessage = tgUpdate.tgBot().tgIOGate().execute(sendMessage);
        log.debug("Sent {}", sentMessage);
        Update update = new Update();
        update.setMessage(sentMessage);
        tgUpdatesRegistrar.register(new TGUpdateBasic(update, tgUpdate.tgBot(), resourceAccess));
    }
}
