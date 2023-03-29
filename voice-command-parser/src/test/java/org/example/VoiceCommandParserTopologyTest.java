package org.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.example.model.ParsedVoiceCommand;
import org.example.model.VoiceCommand;
import org.example.serdes.JsonSerde;
import org.example.service.SpeechToTextService;
import org.example.service.TranslateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static org.example.VoiceCommandParserTopology.RECOGNIZED_COMMANDS_TOPIC;
import static org.example.VoiceCommandParserTopology.VOICE_COMMANDS_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VoiceCommandParserTopologyTest {

    public static final String VOICE_COMMAND_TEXT = "call john";
    @Mock
    private SpeechToTextService speechToTextService;
    @Mock
    private TranslateService translateService;
    private TopologyTestDriver topologyDriver;
    private TestInputTopic<String, VoiceCommand> voiceCommandInputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> recognizedCommandsOutputTopic;
    @InjectMocks
    private VoiceCommandParserTopology voiceCommandParserTopology;

    @BeforeEach
    void setUp() {
        var topology = voiceCommandParserTopology.createTopology();
        var props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

        topologyDriver = new TopologyTestDriver(topology);
        var voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);
        var parsedVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);
        voiceCommandInputTopic = topologyDriver.createInputTopic(VOICE_COMMANDS_TOPIC, Serdes.String().serializer(),
            voiceCommandJsonSerde.serializer());

        recognizedCommandsOutputTopic = topologyDriver.createOutputTopic(RECOGNIZED_COMMANDS_TOPIC, Serdes.String().deserializer(),
            parsedVoiceCommandJsonSerde.deserializer());
    }

    @Test
    @DisplayName("Given an English voice command, When processed correctly Then I receive a ParsedVoiceCommand in the recognnized-commands topic.")
    void testScenario1() {
        // Preconditions (Given)
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
            .id(UUID.randomUUID().toString())
            .audio(randomBytes)
            .audioCodec("FLAC")
            .language("en-US")
            .build();

        ParsedVoiceCommand inputParsedVoiceCommand = ParsedVoiceCommand.builder()
            .id(voiceCommand.getId())
            .text(VOICE_COMMAND_TEXT)
            .language("en-US")
            .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(inputParsedVoiceCommand);

        // Actions (When)
        voiceCommandInputTopic.pipeInput(voiceCommand);

        // Verifications (Then)
        var parsedVoiceCommand = recognizedCommandsOutputTopic.readValue();

        assertEquals(voiceCommand.getId(), parsedVoiceCommand.getId());
        assertEquals(VOICE_COMMAND_TEXT, parsedVoiceCommand.getText());
    }

    @Test
    @DisplayName("Given a non-English voice command, When processed correctly Then I receive a ParsedVoiceCommand in the recognnized-commands topic.")
    void testScenario2() {
        // Preconditions (Given)
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
            .id(UUID.randomUUID().toString())
            .audio(randomBytes)
            .audioCodec("FLAC")
            .language("es-AR")
            .build();

        var inputParsedVoiceCommand = ParsedVoiceCommand.builder()
            .id(voiceCommand.getId())
            .text("llamar a juan")
            .language("es-AR")
            .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(inputParsedVoiceCommand);

        var translatedVoiceCommand = ParsedVoiceCommand.builder()
            .id(voiceCommand.getId())
            .text("call juan")
            .language("en-US")
            .build();
        given(translateService.translate(inputParsedVoiceCommand)).willReturn(translatedVoiceCommand);

        // Actions (When)
        voiceCommandInputTopic.pipeInput(voiceCommand);

        // Verifications (Then)
        var parsedVoiceCommand = recognizedCommandsOutputTopic.readValue();

        assertEquals(voiceCommand.getId(), parsedVoiceCommand.getId());
        assertEquals("call juan", parsedVoiceCommand.getText());
    }
}