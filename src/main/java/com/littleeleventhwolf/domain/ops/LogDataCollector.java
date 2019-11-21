package com.littleeleventhwolf.domain.ops;

import com.littleeleventhwolf.domain.LogData;

public interface LogDataCollector {
    void add(LogData... logDatas);

    LogData[] getLogData();

    int getCount();

    int clear();
}
