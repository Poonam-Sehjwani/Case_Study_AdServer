
package com.retail.streaming.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

//retry 3 times (1s apart), then push the bad message to <topic>.DLT
@Configuration
public class KafkaConsumerConfig {

    // retry 3 times (1s apart), then push the bad message to <topic>.DLT
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<?, ?> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLT",
                                        record.partition()
                                )
                );

        // 3 retries, with a 1-second delay between attempts.
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
