package com.littleeleventhwolf.parser;

import com.littleeleventhwolf.domain.LogData;

import java.util.Properties;

public interface LogParser {
    void init(Properties properties);

    void initParsingContext(ParsingContext parsingContext);

    LogData parse(String line, ParsingContext parsingContext);

    LogData parseBuffer(ParsingContext parsingContext);
}
