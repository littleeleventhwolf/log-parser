package com.littleeleventhwolf.parser;

import lombok.Data;

import java.util.HashMap;

@Data
public class ParsingContext {
    private StringBuilder unmatchedLog;
    private HashMap<String, Object> customContextProperties;

    public ParsingContext() {
        unmatchedLog = new StringBuilder();
        customContextProperties = new HashMap<>();
    }
}
