package org.example.service;

import org.example.model.ParsedVoiceCommand;

public interface TranslateService {

    ParsedVoiceCommand translate(ParsedVoiceCommand original);
}
