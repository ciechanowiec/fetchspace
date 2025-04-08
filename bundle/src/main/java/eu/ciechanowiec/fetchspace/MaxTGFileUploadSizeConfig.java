package eu.ciechanowiec.fetchspace;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface MaxTGFileUploadSizeConfig {

    long DEFAULT_MAX_SIZE_BYTES = 2_147_483_648L; // 2 GB

    @AttributeDefinition(
        name = "TG File Max Upload Size (bytes)",
        description = "Maximum size of a file that can be uploaded to Telegram. Default is 2147483648 (2 GB).",
        type = AttributeType.LONG,
        defaultValue = "2147483648"
    )
    @SuppressWarnings("squid:S100")
    long tg$_$file_max$_$upload$_$size_bytes() default DEFAULT_MAX_SIZE_BYTES;
}
