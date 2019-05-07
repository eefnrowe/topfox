package com.topfox.spring;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.FormattedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// com.topfox.spring.SqlLogger
public class SqlLogger extends FormattedLogger {
    protected Logger logger= LoggerFactory.getLogger(getClass());


    protected PrintStream getStream() {
        return System.out;
    }

    @Override
    public void logException(Exception e) {
        e.printStackTrace(getStream());
    }

    @Override
    public void logText(String text) {
        if (text.length() > 10) {
            // 去掉多个换行
            Pattern p = Pattern.compile("(\r?\n(\\s*\r?\n)+)");
            Matcher m = p.matcher(text);
            text = m.replaceAll("\r\n");

            if (logger.isDebugEnabled()) {
                getStream().println(text);
            }
        }
    }

    @Override
    public boolean isCategoryEnabled(Category category) {
        // no restrictions on logger side
        return true;
    }
}

