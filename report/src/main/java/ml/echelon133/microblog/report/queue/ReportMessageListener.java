package ml.echelon133.microblog.report.queue;

import ml.echelon133.microblog.report.repository.ReportRepository;
import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.report.Report;
import ml.echelon133.microblog.shared.report.ReportCreationDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Listener of report messages published in Redis.
 * Each received {@link ReportCreationDto} message is transformed and then saved in the database as a {@link Report} object.
 */
public class ReportMessageListener implements MessageListener {

    private static final Logger LOGGER = LogManager.getLogger(ReportMessageListener.class);

    private final ReportRepository reportRepository;

    @Autowired
    public ReportMessageListener(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topicName = new String(message.getChannel());

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(message.getBody()));
            var report = (ReportCreationDto)ois.readObject();
            if (topicName.equals(QueueTopic.REPORT.getTopic())) {
                LOGGER.info(String.format(
                        "Creating a report of post '%s', on behalf of user '%s' for '%s' reason",
                        report.getReportedPost(), report.getReportingUser(), report.getReason()
                ));
                reportRepository.save(new Report(
                        report.getReason(), report.getContext(), report.getReportedPost(), report.getReportingUser()
                ));
            } else {
                LOGGER.warn("Received unexpected topic name: " + topicName);
            }
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("Failed to deserialize a message from topic " + topicName);
            e.printStackTrace();
        }
    }
}
