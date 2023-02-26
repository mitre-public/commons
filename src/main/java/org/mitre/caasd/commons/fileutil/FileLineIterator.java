/*
 *    Copyright 2022 The MITRE Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.mitre.caasd.commons.fileutil;

import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.fileutil.FileUtils.createReaderFor;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A FileLineIterator returns one line from a text file or a GZIP-ed text file.
 */
public class FileLineIterator implements Iterator<String>, AutoCloseable {

    private BufferedReader reader;

    private String nextLine;

    /**
     * Create a FileLineIterator backed by a text file OR a text file that was compressed with
     * GZIP.
     *
     * @param fileName myData.txt or myData.txt.gz
     */
    public FileLineIterator(String fileName) {
        this(new File(fileName));
    }

    /**
     * Create a FileLineIterator backed by a text file OR a text file that was compressed with
     * GZIP.
     *
     * @param file myData.txt or myData.txt.gz
     */
    public FileLineIterator(File file) {
        try {
            this.reader = createReaderFor(file); //supports .gz files and regular files
            updateNext();
        } catch (IOException ioe) {
            throw demote(ioe);
        }
    }

    public FileLineIterator(Reader reader) {
        this(new BufferedReader(reader));
    }

    public FileLineIterator(BufferedReader reader) {
        this.reader = requireNonNull(reader);
        updateNext();
    }

    @Override
    public boolean hasNext() {
        return nextLine != null;
    }

    /**
     * @return The next line from the text file.
     */
    @Override
    public String next() {

        if (nextLine == null) {
            throw new NoSuchElementException();
        }

        String returnMe = nextLine;

        updateNext(); //overwrites nextLine;

        return returnMe;
    }

    /**
     * Read the next line from the text file, save that value in the "nextLine" field.
     */
    private void updateNext() {
        retrieveNextLine();
        closeReaderIfDone();
    }

    private void retrieveNextLine() {
        try {
            this.nextLine = reader.readLine();
        } catch (IOException ioe) {
            throw demote("Could not load the next line of the input file", ioe);
        }
    }

    private void closeReaderIfDone() {
        try {
            if (!hasNext()) {
                close();
            }
        } catch (IOException ioe) {
            throw demote("Could not close the BufferedReader", ioe);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("A LineIterator cannot manipulate a text file.");
    }

    @Override
    public void close() throws IOException {
        /*
         * ensure "hasNext()" always returns false after close() is called. We want consistent
         * behavior if this FileLineIterator is closed before it reaches the end of the file.
         */
        this.nextLine = null;

        this.reader.close();
    }
}
