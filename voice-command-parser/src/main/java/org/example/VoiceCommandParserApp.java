package org.example;

import org.apache.kafka.streams.KafkaStreams;
import org.example.config.StreamsConfiguration;
import org.example.service.MockSttClient;
import org.example.service.MockTranslateClient;

public class VoiceCommandParserApp {

    public static void main(String[] args) {
        var voiceCommandParserTopology = new VoiceCommandParserTopology(new MockSttClient(), new MockTranslateClient());
        var streamConfiguration = new StreamsConfiguration();
        var kafkaStreams = new KafkaStreams(voiceCommandParserTopology.createTopology(), streamConfiguration.createConfig());

        kafkaStreams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }

}
