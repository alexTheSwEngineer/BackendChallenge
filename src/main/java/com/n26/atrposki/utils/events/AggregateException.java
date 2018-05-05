package com.n26.atrposki.utils.events;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 * shamelessly stolen from a previous chalenge (searhink) that in term shamelessly stole it form
 * https://github.com/BoltsFramework/Bolts-Android/blob/master/bolts-tasks/src/main/java/bolts/AggregateException.java
 * An exception class that is an aggregate of multiple throwables.
 *
 */
public class AggregateException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "There were multiple errors.";

    private List<Throwable> innerThrowables;

    public AggregateException(String detailMessage, Throwable[] innerThrowables) {
        this(detailMessage, Arrays.asList(innerThrowables));
    }

    public AggregateException(String detailMessage, List<? extends Throwable> innerThrowables) {
        super(detailMessage,
                innerThrowables != null && innerThrowables.size() > 0 ? innerThrowables.get(0) : null);
        this.innerThrowables = Collections.unmodifiableList(innerThrowables);
    }

    public AggregateException(List<? extends Throwable> innerThrowables) {
        this(DEFAULT_MESSAGE, innerThrowables);
    }

    public List<Throwable> getInnerThrowables() {
        return innerThrowables;
    }

    @Override
    public void printStackTrace(PrintStream err) {
        super.printStackTrace(err);

        int currentIndex = -1;
        for (Throwable throwable : innerThrowables) {
            err.append("\n");
            err.append("  Inner throwable #");
            err.append(Integer.toString(++currentIndex));
            err.append(": ");
            throwable.printStackTrace(err);
            err.append("\n");
        }
    }

    @Override
    public void printStackTrace(PrintWriter err) {
        super.printStackTrace(err);

        int currentIndex = -1;
        for (Throwable throwable : innerThrowables) {
            err.append("\n");
            err.append("  Inner throwable #");
            err.append(Integer.toString(++currentIndex));
            err.append(": ");
            throwable.printStackTrace(err);
            err.append("\n");
        }
    }
}