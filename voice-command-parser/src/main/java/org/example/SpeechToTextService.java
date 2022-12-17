package org.example;

import org.example.model.ParsedVoiceCommand;
import org.example.model.VoiceCommand;

public interface SpeechToTextService {

    ParsedVoiceCommand speechToText(VoiceCommand voiceCommand);

}
