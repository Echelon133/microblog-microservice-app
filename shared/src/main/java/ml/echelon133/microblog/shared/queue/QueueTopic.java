package ml.echelon133.microblog.shared.queue;

import org.springframework.data.redis.listener.ChannelTopic;

public class QueueTopic {

    public final static ChannelTopic FOLLOW = new ChannelTopic("follow");
    public final static ChannelTopic UNFOLLOW = new ChannelTopic("unfollow");

    public final static ChannelTopic NOTIFICATION = new ChannelTopic("notification");

    public final static ChannelTopic REPORT = new ChannelTopic("report");
}
