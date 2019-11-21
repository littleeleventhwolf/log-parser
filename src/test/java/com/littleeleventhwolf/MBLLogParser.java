package com.littleeleventhwolf;

import com.littleeleventhwolf.domain.LogData;
import com.littleeleventhwolf.domain.ops.ProxyLogDataCollector;
import com.littleeleventhwolf.model.LogImporterUsingParser;
import com.littleeleventhwolf.parser.Log4jParser;
import com.littleeleventhwolf.parser.ParsingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

public class MBLLogParser {
    @Test
    public void parseLog() throws FileNotFoundException {
        // Define log4j properties
        Properties props = new Properties();
        props.put("pattern", "TIMESTAMP THREAD [LEVEL] CLASS (FILE:LINE) MESSAGE");
        props.put("dateFormat", "yyyy/MM/dd HH:mm:ss.SSS");
        Log4jParser logParser = new Log4jParser();
        LogImporterUsingParser importerUsingParser = new LogImporterUsingParser(logParser);
        importerUsingParser.init(props);

        // create parsing context
        ParsingContext context = new ParsingContext();
        importerUsingParser.initParsingContext(context);

        // create input stream from file
        InputStream in = new SequenceInputStream(
                new FileInputStream("/Users/wangxinglong/MyTask/mbl/build-20190930-dx.log"),
                new FileInputStream("/Users/wangxinglong/MyTask/mbl/build-20190930-yf.log"));

        // Create log collector, it capture all parsed log events
        ProxyLogDataCollector dataCollector = new ProxyLogDataCollector();

        // parse log file
        importerUsingParser.importLogs(in, dataCollector, context);

        Arrays.stream(dataCollector.getLogData())
                .filter(line -> line.getMessage().contains("RUN SQL:"))
                .sorted(Comparator.comparing(LogData::getTimestamp))
                .limit(10)
                .map(line -> Pair.of(line.getTimestamp(), line.getMessage()))
                .forEach(System.out::println);
    }
}
