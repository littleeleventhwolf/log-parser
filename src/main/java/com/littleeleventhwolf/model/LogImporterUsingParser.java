package com.littleeleventhwolf.model;

import com.littleeleventhwolf.domain.LogData;
import com.littleeleventhwolf.domain.ops.LogDataCollector;
import com.littleeleventhwolf.parser.Log4jParser;
import com.littleeleventhwolf.parser.LogParser;
import com.littleeleventhwolf.parser.ParsingContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class LogImporterUsingParser implements LogImporter {
    private LogParser parser;

    public LogImporterUsingParser(LogParser parser) {
        this.parser = parser;
    }

    @Override
    public void init(Properties properties) {
        parser.init(properties);
    }

    @Override
    public void initParsingContext(ParsingContext parsingContext) {
        parser.initParsingContext(parsingContext);
    }

    @Override
    public void importLogs(InputStream in, LogDataCollector dataCollector, ParsingContext parsingContext) {
        String line;
        LogData logData;

        BufferedReader logReader = new BufferedReader(new InputStreamReader(in));
        while (true) {
            try {
                line = logReader.readLine();
                if (line == null) {
                    break;
                }
                if (parser instanceof Log4jParser) {
                    synchronized (parsingContext) {
                        logData = parser.parse(line, parsingContext);
                    }
                } else {
                    logData = parser.parse(line, parsingContext);
                }
                if (logData != null) {
                    dataCollector.add(logData);
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        parseBuffer(dataCollector, parsingContext);
    }

    private void parseBuffer(LogDataCollector dataCollector, ParsingContext parsingContext) {
        if (parser instanceof Log4jParser) {
            Log4jParser log4jParser = (Log4jParser) parser;
            LogData logData = log4jParser.parseBuffer(parsingContext);
            if (logData != null) {
                synchronized (parsingContext) {
                    dataCollector.add(logData);
                }
            }
        }
    }
}
