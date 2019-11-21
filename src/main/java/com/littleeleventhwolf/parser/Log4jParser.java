package com.littleeleventhwolf.parser;

import com.google.common.base.Strings;
import com.littleeleventhwolf.domain.LogData;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j
public class Log4jParser implements LogParser {
    public static final String PROPERTY_PATTERN = "pattern";
    public static final String PROPERTY_DATE_FORMAT = "dateFormat";

    private final List<String> keywords = new ArrayList<>();
    private final List<String> matchingKeywords = new ArrayList<>();

    protected static final String TIMESTAMP = "TIMESTAMP";
    protected static final String THREAD = "THREAD";
    protected static final String LEVEL = "LEVEL";
    protected static final String CLASS = "CLASS";
    protected static final String FILE = "FILE";
    protected static final String LINE = "LINE";
    protected static final String MESSAGE = "MESSAGE";
    protected static final String PROPERTY_LOG_EVENT_PROPERTOES = "Log4jParser.logEventProperties";

    private Pattern exceptionPattern;
    private static final String EXCEPTION_PATTERN = "^\\s+at.*";
    private final String[] emptyException = {""};

    private static final String VALID_DATEFORMAT_CHARS = "GyMwWDdFEaHkKhmsSzZ";
    private static final String VALID_DATEFORMAT_CHAR_PATTERN = "[" + VALID_DATEFORMAT_CHARS + "]";
    private static final String MULTIPLE_SPACES_REGEXP = "[ ]+";
    private static final String PATTERN_WILDCARD = "*";
    private static final String REGEXP_DEFAULT_WILDCARD = ".*?";
    private static final String REGEXP_GREEDY_WILDCARD = ".*";
    private static final String DEFAULT_GROUP = "(" + REGEXP_DEFAULT_WILDCARD + ")";
    private static final String GREEDY_GROUP = "(" + REGEXP_GREEDY_WILDCARD + ")";
    private static final String IN_SPACE_GROUP = "(\\s*?\\S*\\s*?)";
    private final String newLine = System.getProperty("line.separator");

    private String regexp;
    private Pattern regexpPattern;

    private String timestampFormat = "yyyy/MM/dd HH:mm:ss.SSS";
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timestampFormat);
    private String timestampPattern;

    private String logFormat;

    public Log4jParser() {
        keywords.add(TIMESTAMP);
        keywords.add(THREAD);
        keywords.add(LEVEL);
        keywords.add(CLASS);
        keywords.add(FILE);
        keywords.add(LINE);
        keywords.add(MESSAGE);
        exceptionPattern = Pattern.compile(EXCEPTION_PATTERN);
    }

    private String convertTimeStamp() {
        String result = timestampFormat.replaceAll(VALID_DATEFORMAT_CHAR_PATTERN + "+", "\\\\S+");
        return result.replaceAll(Pattern.quote("."), "\\\\.");
    }

    private String singleReplace(String inputString, String oldString, String newString) {
        String result = inputString;
        int propLength = oldString.length();
        int startPos = result.indexOf(oldString);
        if (startPos == -1) {
            return result;
        }
        if (startPos == 0) {
            result = result.substring(propLength);
            result = newString + result;
        } else {
            result = result.substring(0, startPos) + newString + result.substring(startPos + propLength);
        }
        return result;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private String replaceMetaChars(String input) {
        String result = input.replaceAll("\\\\", "\\\\\\");

        result = result.replaceAll(Pattern.quote("]"), "\\\\]");
        result = result.replaceAll(Pattern.quote("["), "\\\\[");
        result = result.replaceAll(Pattern.quote("^"), "\\\\^");
        result = result.replaceAll(Pattern.quote("$"), "\\\\$");
        result = result.replaceAll(Pattern.quote("."), "\\\\.");
        result = result.replaceAll(Pattern.quote("|"), "\\\\|");
        result = result.replaceAll(Pattern.quote("?"), "\\\\?");
        result = result.replaceAll(Pattern.quote("+"), "\\\\+");
        result = result.replaceAll(Pattern.quote("("), "\\\\(");
        result = result.replaceAll(Pattern.quote(")"), "\\\\)");
        result = result.replaceAll(Pattern.quote("-"), "\\\\-");
        result = result.replaceAll(Pattern.quote("{"), "\\\\{");
        result = result.replaceAll(Pattern.quote("}"), "\\\\}");
        result = result.replaceAll(Pattern.quote("#"), "\\\\#");
        return result;
    }

    private void initializePatterns() {
        List<String> buildingKeywords = new ArrayList<>();
        String newPattern = logFormat;
        for (String keyword : keywords) {
            int index = newPattern.indexOf(keyword);
            if (index > -1) {
                buildingKeywords.add(keyword);
                newPattern = singleReplace(newPattern, keyword, Integer.toString(buildingKeywords.size() - 1));
            }
        }

        String buildingInt = "";
        for (int i = 0; i < newPattern.length(); i++) {
            String thisValue = String.valueOf(newPattern.substring(i, i+1));
            if (isInteger(thisValue)) {
                buildingInt = buildingInt + thisValue;
            } else {
                if (isInteger(buildingInt)) {
                    matchingKeywords.add(buildingKeywords.get(Integer.parseInt(buildingInt)));
                }
                // reset
                buildingInt = "";
            }
        }
        // add remaining buildingInt
        if (isInteger(buildingInt)) {
            matchingKeywords.add(buildingKeywords.get(Integer.parseInt(buildingInt)));
        }

        newPattern = replaceMetaChars(newPattern);
        newPattern = newPattern.replaceAll(MULTIPLE_SPACES_REGEXP, MULTIPLE_SPACES_REGEXP);
        newPattern = newPattern.replaceAll(Pattern.quote(PATTERN_WILDCARD), REGEXP_DEFAULT_WILDCARD);

        for (int i = 0; i < buildingKeywords.size(); i++) {
            String keyword = buildingKeywords.get(i);
            if (i == (buildingKeywords.size() - 1)) {
                newPattern = singleReplace(newPattern, String.valueOf(i), GREEDY_GROUP);
            } else if (TIMESTAMP.equals(keyword)) {
                newPattern = singleReplace(newPattern, String.valueOf(i), "(" + timestampPattern.replaceAll("'", "") + ")");
            } else if (LEVEL.equals(keyword)) {
                newPattern = singleReplace(newPattern, String.valueOf(i), IN_SPACE_GROUP);
            } else {
                newPattern = singleReplace(newPattern, String.valueOf(i), DEFAULT_GROUP);
            }
        }

        regexp = newPattern;
    }

    protected void createPattern() {
        regexpPattern = Pattern.compile(regexp);
    }

    @Override
    public void init(Properties properties) {
        logFormat = properties.getProperty(PROPERTY_PATTERN);
        timestampFormat = properties.getProperty(PROPERTY_DATE_FORMAT);
        if (!Strings.isNullOrEmpty(timestampFormat)) {
            timestampPattern = convertTimeStamp();
        }
        initializePatterns();
        createPattern();
    }

    @Override
    public void initParsingContext(ParsingContext parsingContext) {
        parsingContext.getCustomContextProperties().put(PROPERTY_LOG_EVENT_PROPERTOES, new HashMap<String, Object>());
    }

    private int getExceptionLine(ParsingContext ctx) {
        String[] additionalLines = ctx.getUnmatchedLog().toString().split("\n");
        for (int i = 0; i < additionalLines.length; i++) {
            Matcher exceptionMatcher = exceptionPattern.matcher(additionalLines[i]);
            if (exceptionMatcher.matches()) {
                return i;
            }
        }
        return -1;
    }

    private String[] buildException(int exceptionLine, ParsingContext ctx) {
        if (exceptionLine == -1) {
            return emptyException;
        }
        String[] additionalLines = ctx.getUnmatchedLog().toString().split("\n");
        String[] exception = new String[additionalLines.length - exceptionLine];
        System.arraycopy(additionalLines, exceptionLine, exception, 0, exception.length);
        return exception;
    }

    private String buildMessage(String firstMessageLine, int exceptionLine, ParsingContext ctx) {
        if (ctx.getUnmatchedLog().length() == 0) {
            return firstMessageLine;
        }
        StringBuffer message = new StringBuffer();
        if (!Strings.isNullOrEmpty(firstMessageLine)) {
            message.append(firstMessageLine);
        }
        String[] additionalLines = ctx.getUnmatchedLog().toString().split("\n");
        int linesToProcess = (exceptionLine == -1 ? additionalLines.length : exceptionLine);
        for (int i = 0; i < linesToProcess; i++) {
            message.append(newLine);
            message.append(additionalLines[i]);
        }
        return message.toString();
    }

    private LoggingEvent convertToEvent(Map<String, Object> fieldMap, String[] exception) {
        if (fieldMap == null) {
            return null;
        }
        if (exception == null) {
            exception = emptyException;
        }
        Logger logger = null;
        long timestamp = 0L;
        String level = null;
        String thread = null;
        String message = null;
        String ndc = null;
        String clazz = null;
        String method = null;
        String file = null;
        String line = null;
        Hashtable properties = new Hashtable();

        /* ************* variable assignment ************* */
        logger = Logger.getLogger("log4j");
        if (fieldMap.containsKey(TIMESTAMP)) {
            String dateString = (String) fieldMap.remove(TIMESTAMP);
            timestamp = LocalDateTime.parse(dateString, dateTimeFormatter).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (timestamp == 0L) {
            timestamp = System.currentTimeMillis();
        }
        message = (String) fieldMap.remove(MESSAGE);
        if (message == null) {
            message = StringUtils.EMPTY;
        }
        level = StringUtils.trim((String) fieldMap.remove(LEVEL));
        Level levelObj;
        if (level == null) {
            levelObj = Level.DEBUG;
        } else {
            level = level.trim();
            levelObj = Level.toLevel(level.trim());
        }
        thread = StringUtils.trim((String) fieldMap.remove(THREAD));
        if (thread == null) {
            thread = StringUtils.EMPTY;
        }
        clazz = StringUtils.trim((String) fieldMap.remove(CLASS));
        file = (String) fieldMap.remove(FILE);
        line = (String) fieldMap.remove(LINE);
        method = StringUtils.EMPTY;
        ndc = StringUtils.EMPTY;
        // the remaining is properties
        properties.putAll(fieldMap);
        LocationInfo info = new LocationInfo(file, clazz, method, line);
        return new LoggingEvent(null, logger, timestamp, levelObj, message, thread, new ThrowableInformation(exception), ndc, info, properties);
    }

    private LoggingEvent buildEvent(ParsingContext ctx) {
        HashMap<String, Object> logEventParsingPropertiesMap = (HashMap<String, Object>) ctx.getCustomContextProperties().get(PROPERTY_LOG_EVENT_PROPERTOES);
        if (logEventParsingPropertiesMap.size() == 0) {
            ctx.getUnmatchedLog().setLength(0);
            return null;
        }
        int exceptionLine = getExceptionLine(ctx);
        String[] exception = buildException(exceptionLine, ctx);
        String[] additionalLines = ctx.getUnmatchedLog().toString().split("\n");
        if (additionalLines.length > 0 && exception.length > 0) {
            logEventParsingPropertiesMap.put(MESSAGE, buildMessage((String) logEventParsingPropertiesMap.get(MESSAGE), exceptionLine, ctx));
        }
        LoggingEvent event = convertToEvent(logEventParsingPropertiesMap, exception);
        logEventParsingPropertiesMap.clear();
        ctx.getUnmatchedLog().setLength(0);
        return event;
    }

    private Level parseLevel(String s) {
        if (s.equalsIgnoreCase("INFO")) {
            return Level.INFO;
        } else if (s.equalsIgnoreCase("ERROR")) {
            return Level.ERROR;
        } else if (s.equalsIgnoreCase("FATAL")) {
            return Level.FATAL;
        } else if (s.equalsIgnoreCase("WARN")) {
            return Level.WARN;
        } else if (s.equalsIgnoreCase("DEBUG")) {
            return Level.DEBUG;
        } else if (s.equalsIgnoreCase("TRACE")) {
            return Level.TRACE;
        }
        return null;
    }

    private LogData translateLog4j(LoggingEvent event) {
        LogData ld = new LogData();
        ld.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault()));
        StringBuilder sb = new StringBuilder();
        sb.append(event.getMessage());
        if (event.getThrowableInformation() != null) {
            String[] throwableStrRep = event.getThrowableInformation().getThrowableStrRep();
            for (String str : throwableStrRep) {
                sb.append("\n");
                sb.append(str);
            }
        }
        ld.setMessage(sb.toString().trim());

        ld.setLevel(parseLevel(event.getLevel().toString()));
        ld.setClazz(event.getLocationInformation().getClassName());
        ld.setFile(event.getLocationInformation().getFileName());
        ld.setLine(event.getLocationInformation().getLineNumber());
        ld.setThread(event.getThreadName());
        return ld;
    }

    private Map<String, Object> processEvent(MatchResult result) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 1; i < result.groupCount() + 1; i++) {
            String key = matchingKeywords.get(i - 1);
            String value = result.group(i);
            map.put(key, value);
        }
        return map;
    }

    @Override
    public LogData parse(String line, ParsingContext parsingContext) {
        LogData logData = null;
        if (line.trim().equals("")) {
            parsingContext.getUnmatchedLog().append('\n');
            parsingContext.getUnmatchedLog().append(line);
            return null;
        }
        Matcher eventMatcher = regexpPattern.matcher(line);
        Matcher exceptionMatcher = exceptionPattern.matcher(line);
        HashMap<String, Object> logEventParsingProperties = (HashMap<String, Object>) parsingContext.getCustomContextProperties().get(PROPERTY_LOG_EVENT_PROPERTOES);
        if (eventMatcher.matches()) {
            LoggingEvent event = buildEvent(parsingContext);
            if (event != null) {
                logData = translateLog4j(event);
            }
            processEvent(eventMatcher.toMatchResult()).entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> logEventParsingProperties.put(entry.getKey(), entry.getValue()));
        } else if (exceptionMatcher.matches()) {
            if (parsingContext.getUnmatchedLog().length() > 0) {
                parsingContext.getUnmatchedLog().append("\n");
            }
            parsingContext.getUnmatchedLog().append(line);
        } else {
            if (parsingContext.getUnmatchedLog().length() > 0) {
                parsingContext.getUnmatchedLog().append("\n");
            }
            parsingContext.getUnmatchedLog().append(line.trim());
        }

        return logData;
    }

    @Override
    public LogData parseBuffer(ParsingContext parsingContext) {
        LogData logData = null;
        LoggingEvent event = buildEvent(parsingContext);
        if (event != null) {
            logData = translateLog4j(event);
        }
        return logData;
    }
}
