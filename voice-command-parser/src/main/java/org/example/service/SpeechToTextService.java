package org.example.service;

import org.example.model.ParsedVoiceCommand;
import org.example.model.VoiceCommand;

public interface SpeechToTextService {

    ParsedVoiceCommand speechToText(VoiceCommand voiceCommand);

}
