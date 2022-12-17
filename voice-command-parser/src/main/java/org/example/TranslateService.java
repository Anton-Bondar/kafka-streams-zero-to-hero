package org.example;

import org.example.model.ParsedVoiceCommand;

public interface TranslateService {

    ParsedVoiceCommand translate(ParsedVoiceCommand original);
}
