package org.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.example.model.ParsedVoiceCommand;
import org.example.model.VoiceCommand;
import org.example.serdes.JsonSerde;

public class VoiceCommandParserTopology {

    public static final String VOICE_COMMANDS_TOPIC = "voice-commands";
    public static final String RECOGNIZED_COMMANDS_TOPIC = "recognized-commands";

    private final SpeechToTextService speechToTextService;

    public VoiceCommandParserTopology(SpeechToTextService speechToTextService) {
        this.speechToTextService = speechToTextService;
    }

    public Topology createTopology() {
        var streamsBuilder = new StreamsBuilder();

        JsonSerde<VoiceCommand> voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);
        JsonSerde<ParsedVoiceCommand> parsedVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);
        streamsBuilder.stream(VOICE_COMMANDS_TOPIC, Consumed.with(Serdes.String(), voiceCommandJsonSerde))
            .mapValues((key, value) -> speechToTextService.speechToText(value))
            .to(RECOGNIZED_COMMANDS_TOPIC, Produced.with(Serdes.String(), parsedVoiceCommandJsonSerde));

        return streamsBuilder.build();
    }

}
