package ml.echelon133.microblog.shared.queue;

import org.springframework.data.redis.listener.ChannelTopic;

/**
 * All {@link ChannelTopic}s used during communication between services.
 */
public class QueueTopic {

    /**
     * Topic containing information about users being followed.
     */
    public final static ChannelTopic FOLLOW = new ChannelTopic("follow");

    /**
     * Topic containing information about users being unfollowed.
     */
    public final static ChannelTopic UNFOLLOW = new ChannelTopic("unfollow");

    /**
     * Topic containing information about notifications.
     */
    public final static ChannelTopic NOTIFICATION = new ChannelTopic("notification");

    /**
     * Topic containing information about posts being reported.
     */
    public final static ChannelTopic REPORT = new ChannelTopic("report");

    /**
     * Topic containing information about what post needs to be deleted after a report
     * has been accepted.
     */
    public final static ChannelTopic REPORT_ACTION = new ChannelTopic("report-action");
}
