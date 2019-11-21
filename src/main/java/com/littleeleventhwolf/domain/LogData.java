package com.littleeleventhwolf.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.log4j.Level;

@Data
@ToString
@EqualsAndHashCode
public class LogData implements Serializable {
    private LocalDateTime timestamp;
    private Level level;
    private String clazz;
    private String file;
    private String line;
    private String thread;
    private String message;
}
