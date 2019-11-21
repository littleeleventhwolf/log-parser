package com.littleeleventhwolf.model;

import com.littleeleventhwolf.domain.ops.LogDataCollector;
import com.littleeleventhwolf.parser.ParsingContext;

import java.io.InputStream;
import java.util.Properties;

public interface LogImporter {
    void init(Properties properties);

    void initParsingContext(ParsingContext parsingContext);

    void importLogs(InputStream in, LogDataCollector dataCollector, ParsingContext parsingContext);
}
