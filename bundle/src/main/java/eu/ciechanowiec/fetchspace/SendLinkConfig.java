package eu.ciechanowiec.fetchspace;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface SendLinkConfig {

    @AttributeDefinition(
        name = "IDs of Chats with Moderators",
        description = "All successful send link messages will be additionally sent to chats with moderators "
            + "specified in this configuration",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String[] chats$_$with$_$moderators_ids() default {};
}
