package com.littleeleventhwolf.domain.ops;

import com.littleeleventhwolf.domain.LogData;

import java.util.Collections;
import java.util.LinkedList;

public class ProxyLogDataCollector implements LogDataCollector {
    private final LinkedList<LogData> list;

    public ProxyLogDataCollector() {
        list = new LinkedList<>();
    }

    @Override
    public void add(LogData... logDatas) {
        Collections.addAll(list, logDatas);
    }

    @Override
    public LogData[] getLogData() {
        LogData[] datas = new LogData[list.size()];
        datas = list.toArray(datas);
        return datas;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public int clear() {
        int size = list.size();
        list.clear();
        return size;
    }
}
