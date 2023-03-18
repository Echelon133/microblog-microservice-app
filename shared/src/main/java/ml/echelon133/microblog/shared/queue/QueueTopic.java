package ml.echelon133.microblog.shared.queue;

import org.springframework.data.redis.listener.ChannelTopic;

public class QueueTopic {

    public final static ChannelTopic CREATE_FOLLOW_TOPIC = new ChannelTopic("create-follows");
    public final static ChannelTopic REMOVE_FOLLOW_TOPIC = new ChannelTopic("remove-follows");
}
