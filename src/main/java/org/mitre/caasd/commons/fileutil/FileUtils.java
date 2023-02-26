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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toCollection;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.Files;
import com.google.common.io.Resources;

public class FileUtils {

    private FileUtils() {
    }

    /**
     * Create a BufferedReader for a given file. This method (1) provides a "normal" BufferedReader
     * for uncompressed .txt files OR (2) provides a BufferedReader that automatically decompressed
     * gzip files.
     *
     * @param file A text file or a gzip compressed text file
     *
     * @return A BufferedReader that can provide the lines of the input file
     * @throws Exception Any Exceptions thrown attempting to read the file
     */
    public static BufferedReader createReaderFor(File file) throws IOException {

        if (isGZipFile(file)) {
            return createReaderForGZipFile(file);
        } else {
            return new BufferedReader(new FileReader(file));
        }
    }

    //as per the .gz file format all .gz file begin with these two bytes
    private static final byte GZ_MAGIC_BYTE_0 = (byte) 0x1f;
    private static final byte GZ_MAGIC_BYTE_1 = (byte) 0x8b;
    //SOME references show this byte as well
    private static final byte GZ_MAGIC_BYTE_2 = (byte) 0x08;

    /**
     * Examine the bytes of the the provided File, return true if the file begins with a "magic
     * bytes 0x1f and 0x8b" that are specified in the .gz file format
     *
     * @param file A File
     *
     * @return True if the provided file begins with a identifier provided by the .gz file format.
     * @throws java.lang.Exception (Any Exceptions thrown while inspecting the provided file to see
     *                             if it contains the "magic bytes" of the .gz format)
     */
    public static boolean isGZipFile(File file) throws IOException {

        //so we'll read the first two byte and check them against those
        byte[] byteBuffer = new byte[3];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(byteBuffer);
        }

        return byteBuffer[0] == GZ_MAGIC_BYTE_0
            && byteBuffer[1] == GZ_MAGIC_BYTE_1;
        //we aren't checking the 3rd byte -- even though we grab it
    }

    /**
     * Create a PrintWriter that will stream content to a compressed .gz file
     *
     * @param targetGzFile A target file whose name ends with ".gz"
     *
     * @return A PrintWriter that will need to be closed once the writing is done
     * @throws IOException
     */
    public static PrintWriter buildGzWriter(File targetGzFile) throws IOException {
        checkNotNull(targetGzFile);
        checkArgument(targetGzFile.getName().endsWith(".gz"));

        /*
         * These 3 lines produce an "Unreleased Resource Finding in FAA code scans. THAT FINDING
         * IS A FALSE POSITIVE because: (1) Closing the returned PrintWriter will automatically
         * close the GZIPOutputStream AND the FileOutputStream. (2) The whole purpose of this code
         * is to leave the resource open. If the file was repeatedly opened and closed overall
         * system performance would crater because the underlying File system would get thrashed.
         * (3) We are using gz compression to compress a large volume of data. Consequently, leaving
         * the stream open is required for performance AND correctness (compression does not work
         * well when the compression stream is broken up into small pieces)
         */
        FileOutputStream fos = new FileOutputStream(targetGzFile); //pipe data to a file...
        GZIPOutputStream gos = new GZIPOutputStream(fos); //gz compress the data...
        return new PrintWriter(gos); //enable calls like "write(String)"
    }


    /**
     * Collect all the lines in a text file OR a GZIP compressed text file.
     *
     * @param f sourceFile
     *
     * @return
     */
    public static ArrayList<String> fileLines(File f) {
        FileLineIterator fli = new FileLineIterator(f);
        return stream(fli).collect(toCollection(ArrayList::new));
    }

    /** Equivalent to {@code fileLines(File f)}. */
    public static ArrayList<String> gzFileLines(File f) {
        return fileLines(f);
    }

    /** Equivalent to {@code new FileLineIterator(f)}. */
    public static Iterator<String> fileLineIter(File f) {
        return new FileLineIterator(f);
    }

    /** Equivalent to {@code new FileLineIterator(f)}. */
    public static Iterator<String> gzFileLineIter(File f) {
        return new FileLineIterator(f);
    }

    public static BufferedReader createReaderForGZipFile(File file) throws FileNotFoundException, IOException {
        InputStream fileStream = new FileInputStream(file);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        return new BufferedReader(decoder);
    }

    /**
     * Append a String to an (perhaps) existing file. This method is inefficient if used repeatedly
     * to make small writes. Each invocation opens a new FileOutputStream and PrintWriter so it is
     * best to aggregate all "writeMe" strings and write them all in one call to this method.
     *
     * @param fileName The name of the file you wish to append text to. This fileName can include a
     *                 path. However, if a path is include all directories must exist otherwise an
     *                 error will be thrown.
     * @param writeMe  The text you want to have added to the file.
     *
     * @throws Exception Throws multiple IO related exceptions
     */
    public static void appendToFile(String fileName, String writeMe) throws Exception {
        checkNotNull(fileName);
        checkNotNull(writeMe);
        appendToFile(new File(fileName), writeMe);
    }

    /**
     * Append a String to an (perhaps) existing file. This method is inefficient if used repeatedly
     * to make small writes. Each invocation opens a new FileOutputStream and PrintWriter so it is
     * best to aggregate all "writeMe" strings and write them all in one call to this method.
     *
     * @param file    The file you wish to append text to.
     * @param writeMe The text you want to have added to the file.
     *
     * @throws Exception Throws multiple IO related exceptions
     */
    public static void appendToFile(File file, String writeMe) throws Exception {
        checkNotNull(file);
        checkNotNull(writeMe);
        writeToFile(file, writeMe, true);
    }

    /**
     * Write a String to a newly created file. This method is inefficient if used repeatedly to make
     * small writes. Each invocation opens a new FileOutputStream and PrintWriter so it is best to
     * aggregate all "writeMe" strings and write them all in one call to this method.
     *
     * @param file    The file you wish to append text to.
     * @param writeMe The text you want to have added to the file.
     *
     * @throws IOException Throws IOException as well as FileNotFoundException
     */
    public static void writeToNewFile(File file, String writeMe) throws IOException {
        checkNotNull(file);
        checkNotNull(writeMe);
        writeToFile(file, writeMe, false);
    }

    /**
     * Write a String to a file. This method is inefficient if used repeatedly to make small writes.
     * Each invocation opens a new FileOutputStream and PrintWriter so it is best to aggregate all
     * "writeMe" strings and write them all in one call to this method.
     *
     * @param file    The file you wish to append text to.
     * @param writeMe The text you want to have added to the file.
     * @param append  True will append to a file if that file already exists, False will overwrite
     *                any preexisting file.
     *
     * @throws IOException Throws IOException as well as FileNotFoundException
     */
    private static void writeToFile(File file, String writeMe, boolean append) throws IOException {
        //NOTE:  This method is private so that we expose the clearest possible external API.
        FileOutputStream fos = new FileOutputStream(file, append);
        PrintWriter dout = new PrintWriter(fos);

        dout.write(writeMe);

        dout.close();
        fos.close();
    }

    /**
     * Write a String to a newly created file. This method is inefficient if used repeatedly to make
     * small writes. Each invocation opens a new FileOutputStream and PrintWriter so it is best to
     * aggregate all "writeMe" strings and write them all in one call to this method.
     *
     * @param fileName The name of the file you wish to append text to. This fileName can include a
     *                 path. However, if a path is include all directories must exist otherwise an
     *                 error will be thrown.
     * @param writeMe  The text you want to have added to the file.
     *
     * @throws IOException Throws IOException as well as FileNotFoundException
     */
    public static void writeToNewFile(String fileName, String writeMe) throws IOException {
        checkNotNull(fileName);
        checkNotNull(writeMe);
        writeToNewFile(new File(fileName), writeMe);
    }

    /**
     * Write a String to a newly created file that is gz compressed.
     *
     * @param fileName The name of the file you wish to write text to. This fileName can include a
     *                 path. However, if a path is include all directories must exist otherwise an
     *                 error will be thrown. Additionally, the file name must end in ".gz" or
     *                 ".gzip"
     * @param writeMe  The text you want to have added to the file.
     *
     * @throws IOException
     */
    public static void writeToNewGzFile(String fileName, String writeMe) throws IOException {
        checkNotNull(fileName);
        checkNotNull(writeMe);
        writeToNewGzFile(new File(fileName), writeMe);
    }

    /**
     * Write a String to a newly created file that is gz compressed.
     *
     * @param aGzFile The target file. The name of this file must end in ".gz" or ".gzip". The
     *                content of this file will be overwritten.
     * @param writeMe The text you want to have added to the file.
     *
     * @throws IOException
     */
    public static void writeToNewGzFile(File aGzFile, String writeMe) throws IOException {
        checkArgument(
            "gz".equals(Files.getFileExtension(aGzFile.getName()))
                || "gzip".equals(Files.getFileExtension(aGzFile.getName())),
            "The output file " + aGzFile + " does not have the extension .gz or .gzip"
        );
        FileOutputStream fos = new FileOutputStream(aGzFile);
        GZIPOutputStream gos = new GZIPOutputStream(fos);
        PrintWriter dout = new PrintWriter(gos);

        dout.write(writeMe);

        dout.close();
        gos.finish();
        gos.close();
        fos.close();
    }

    public static void serialize(Serializable ser, File targetFile) {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(targetFile))) {

            oos.writeObject(ser);

            oos.close();
        } catch (Exception ex) {
            throw demote(ex);
        }
    }

    /**
     * Serialize an object to the file "fileName".
     *
     * @param ser      A Serializable object
     * @param fileName The name of the file this Object will be serialized to
     */
    public static void serialize(Serializable ser, String fileName) {
        serialize(ser, new File(fileName));
    }

    /**
     * Create an Object from a file.
     *
     * @param f A file that represents a serialized object.
     *
     * @return The deserialized Object found within the provided File
     * @throws RuntimeException All ClassNotFoundExceptions and IOExceptions are rethrown as
     *                          RuntimeExceptions
     */
    public static Object deserialize(File f) {
        checkNotNull(f, "Cannot deserialize a null file");
        checkArgument(f.exists(), "Cannot deserialize: " + f.getAbsolutePath() + " because the file does not exists");

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            ois.close();
            return obj;
        } catch (ClassNotFoundException | IOException ex) {
            throw demote(ex);
        }
    }

    /**
     * Deserialize an Object from an InputStream. This stream-based deserialization method (as
     * opposed to the File-based deserialization method) is frequently used in conjunction with the
     * idiom: "getClass().getResourceAsStream(fileName)". This idiom for accessing resources is more
     * reliable than using "deserialize(new File(classLoader.getResource(filename).getFile()))"
     * because getResourceAsStream(fileName) will work if the calling code is packaged into a jar
     * whereas classLoader.getResource(fileName) will fail if the calling code is packaged into a
     * jar.
     *
     * @param inputStream An InputStream (frequently created using the idiom:
     *                    "getClass().getResourceAsStream(fileName)"
     *
     * @return The deserialized Object found within the provided Stream
     * @throws RuntimeException All ClassNotFoundExceptions and IOExceptions are rethrown as
     *                          RuntimeExceptions
     */
    public static Object deserialize(InputStream inputStream) {
        try {
            ObjectInputStream ois = new ObjectInputStream(inputStream);

            Object obj = ois.readObject();
            ois.close();
            inputStream.close();
            return obj;
        } catch (ClassNotFoundException | IOException ex) {
            throw demote(ex);
        }
    }

    /**
     * Create and load a new Properties object from a file.
     * <p>
     * This method also ensures that if a property is set in an input File then it is specified
     * EXACTLY once (comments are ignored). The protection exists to ensure that subtle errors are
     * not caused by accidentally loading a Property File that contains two different values for a
     * single property.
     *
     * @param propertiesFile The raw text-based properties file
     *
     * @return The Properties object
     * @throws IOException When a problem occurs while trying to open/read the propertiesFile
     */
    public static Properties getProperties(File propertiesFile) throws IOException {

        /*
         * this custom sub-class ensures the process of extracting Properties from a flat file NEVER
         * sets the same property twice.
         */
        class PropertiesThatWontPutTwice extends Properties {

            @Override
            public synchronized Object put(Object key, Object value) {
                Object prior = super.put(key, value);
                if (prior != null) {
                    throw new IllegalArgumentException("The property: " + key + " was already set");
                }
                return prior;
            }
        }

        PropertiesThatWontPutTwice props = new PropertiesThatWontPutTwice();

        try (InputStream input = new FileInputStream(propertiesFile)) {
            props.load(input);
        }

        Properties output = new Properties();
        props.entrySet().forEach(
            entry -> output.put(entry.getKey(), entry.getValue())
        );
        return output;
    }

    public static void makeDirIfMissing(String dir) {
        makeDirIfMissing(new File(dir));
    }

    public static void makeDirIfMissing(File dir) {
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * Recursively delete a directory.
     *
     * @param directory directory to delete
     *
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {

        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);

        if (!directory.delete()) {
            throw new IOException("Unable to delete directory " + directory + ".");
        }
    }

    /**
     * Clean a directory without deleting it.
     *
     * @param directory directory to clean
     *
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {

        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Delete a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * </p>
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted. (java.io.File methods
     * returns a boolean)</li>
     * </ul>
     *
     * @param file file or directory to delete.
     *
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist: " + file);
            }
            if (!file.delete()) {
                throw new IOException("Unable to delete file: " + file);
            }
        }
    }

    /**
     * Attempt to load a file resource. If the resource is found return it as a File, otherwise
     * return it as empty Optional.
     * <p>
     * Note: this method of accessing resources (by looking for a raw File) will fail (or rather
     * return empty Optionals) if the provided clazz parameter is packaged into a deployed jar. In
     * this case the desired resource is actually buried somewhere in the deployed jar and not
     * available as a raw File.
     *
     * @param clazz    Use this class's ClassLoader to locate a resource
     * @param filename The name of the resource being searched for
     *
     * @return The File (wrapped in an Optional), or an empty Optional
     */
    public static Optional<File> getResourceAsFile(Class clazz, String filename) {
        // this will look in src/main/resources
        //  (question, is it always src/main/resources?  What about src/test/resources
        ClassLoader classLoader = clazz.getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());
        if (file.exists()) {
            return Optional.of(file);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the {@code File} corresponding to to {@code resourceName} if the resource is found
     * using the {@linkplain Thread#getContextClassLoader() context class loader}. In simple
     * environments, the context class loader will find resources from the class path. In
     * environments where different threads can have different class loaders, for example app
     * servers, the context class loader will typically have been set to an appropriate loader for
     * the current thread.
     * <p>
     * <p>
     * In the unusual case where the context class loader is null, the class loader that loaded this
     * class ({@code Resources}) will be used instead.
     *
     * @param filename the resource name, relative to the classpath root. Do not include the '/'
     *                 prefix.
     *
     * @return the File named {@code fileName} relative to the classpath root
     * @throws IllegalArgumentException if the resource is not found
     */
    public static File getResourceFile(String filename) {
        //this method was copied from the CAASD test project
        URL url = Resources.getResource(filename);
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw demote("Classpath resource " + url + " cannot be resolved.", e);
        }
    }
}
