package com.facebook.presto.cli;

import io.airlift.command.Command;
import io.airlift.command.Option;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.net.URI;

import static jline.internal.Configuration.getUserHome;

@Command(name = "console", description = "Interactive console")
public class Console
        implements Runnable
{
    @Option(name = "-s", title = "server")
    public URI server = URI.create("http://localhost:8080/v1/presto/query");

    @Option(name = "--debug", title = "debug")
    public boolean debug = false;

    @Override
    public void run()
    {
        try (LineReader reader = new LineReader(getHistory())) {
            while (true) {
                String line = reader.readLine("presto> ");
                if (line == null) {
                    return;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    if (!process(line)) {
                        return;
                    }
                }
                catch (Exception e) {
                    System.out.println("error running command: " + e.getMessage());
                    if (debug) {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (IOException e) {
            System.err.println("readline error: " + e.getMessage());
        }
    }

    private boolean process(String line)
    {
        if (line.endsWith(";")) {
            line = line.substring(0, line.length() - 1);
        }

        if (line.equalsIgnoreCase("quit")) {
            return false;
        }

        line = "sql:" + line;

        Execute query = new Execute();
        query.server = server;
        query.query = line;
        query.run();

        return true;
    }

    private static MemoryHistory getHistory()
    {
        MemoryHistory history;
        File historyFile = new File(getUserHome(), ".presto_history");
        try {
            history = new FileHistory(historyFile);
        }
        catch (IOException e) {
            System.err.printf("WARNING: Failed to load history file (%s): %s. " +
                    "History will not be available during this session.%n",
                    historyFile, e.getMessage());
            history = new MemoryHistory();
        }
        history.setAutoTrim(true);
        return history;
    }

    private static class LineReader
            extends ConsoleReader
            implements Closeable
    {
        private LineReader(History history)
                throws IOException
        {
            super();
            setExpandEvents(false);
            setHistory(history);
        }

        @Override
        public String readLine(String prompt, Character mask)
                throws IOException
        {
            String line = super.readLine(prompt, mask);
            if (getHistory() instanceof Flushable) {
                ((Flushable) getHistory()).flush();
            }
            return line;
        }

        @Override
        public void close()
        {
            shutdown();
        }
    }
}
