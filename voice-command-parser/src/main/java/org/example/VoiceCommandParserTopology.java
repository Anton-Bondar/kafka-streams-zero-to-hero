package org.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.example.model.ParsedVoiceCommand;
import org.example.model.VoiceCommand;
import org.example.serdes.JsonSerde;
import org.example.service.SpeechToTextService;
import org.example.service.TranslateService;

public class VoiceCommandParserTopology {

    public static final String VOICE_COMMANDS_TOPIC = "voice-commands";
    public static final String RECOGNIZED_COMMANDS_TOPIC = "recognized-commands";

    private final SpeechToTextService speechToTextService;
    private final TranslateService translateService;

    public VoiceCommandParserTopology(SpeechToTextService speechToTextService, TranslateService translateService) {
        this.speechToTextService = speechToTextService;
        this.translateService = translateService;
    }

    public Topology createTopology() {
        var streamsBuilder = new StreamsBuilder();

        var voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);
        var parsedVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);
        var streamsMap = streamsBuilder.stream(VOICE_COMMANDS_TOPIC,
                Consumed.with(Serdes.String(), voiceCommandJsonSerde))
            .mapValues((key, value) -> speechToTextService.speechToText(value))
            .split(Named.as("language-"))
            .branch((key, value) -> value.getLanguage().startsWith("en"), Branched.as("english"))
            .defaultBranch(Branched.as("non-english"));
        streamsMap.get("language-non-english")
            .mapValues((key, value) -> translateService.translate(value))
            .merge(streamsMap.get("language-english"))
            .to(RECOGNIZED_COMMANDS_TOPIC, Produced.with(Serdes.String(), parsedVoiceCommandJsonSerde));

        return streamsBuilder.build();
    }

}
