package eu.ciechanowiec.fetchspace;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.telegram.TGUpdateBasic;
import eu.ciechanowiec.sling.telegram.api.TGUpdate;
import eu.ciechanowiec.sling.telegram.api.TGUpdatesRegistrar;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
class WarnAboutBigTGFile {

    private final TGUpdatesRegistrar tgUpdatesRegistrar;
    private final ResourceAccess resourceAccess;

    WarnAboutBigTGFile(TGUpdatesRegistrar tgUpdatesRegistrar, ResourceAccess resourceAccess) {
        this.tgUpdatesRegistrar = tgUpdatesRegistrar;
        this.resourceAccess = resourceAccess;
    }

    @SneakyThrows
    void warnIfBig(TGUpdate tgUpdate, MaxTGFileUploadSize maxTGFileUploadSize) {
        Optional.of(tgUpdate)
            .filter(maxTGFileUploadSize::isRiskySize)
            .filter(tgUpdateWithFile -> !maxTGFileUploadSize.isTooBig(tgUpdateWithFile))
            .ifPresent(
                SneakyConsumer.sneaky(
                    tgUpdateToWarnAbout -> {
                        log.trace("Warning about too big file from {}", tgUpdateToWarnAbout);
                        String messageText = messageText(tgUpdateToWarnAbout);
                        SendMessage sendMessage = new SendMessage(tgUpdate.tgChatID().asString(), messageText);
                        sendMessage.setParseMode(ParseMode.HTML);
                        sendMessage.setReplyToMessageId(tgUpdate.tgMessage().tgMessageID().asInt());
                        Message sentMessage = tgUpdate.tgBot().tgIOGate().execute(sendMessage);
                        log.debug("Sent {}", sentMessage);
                        Update update = new Update();
                        update.setMessage(sentMessage);
                        tgUpdatesRegistrar.register(new TGUpdateBasic(update, tgUpdate.tgBot(), resourceAccess));
                    }
                )
            );
    }

    private String messageText(TGUpdate tgUpdate) {
        String template
            = "🐘 The file you've sent is quite large%s. We're processing it now - just give us a moment!";
        return Optional.of(tgUpdate)
            .map(SizeOfTGFileFromTGUpdate::new)
            .flatMap(SizeOfTGFileFromTGUpdate::get)
            .map(DataSize::megabytes)
            .map(Double::intValue)
            .map(" (%d MB)"::formatted)
            .map(template::formatted)
            .orElse(template.formatted(StringUtils.EMPTY));
    }
}
