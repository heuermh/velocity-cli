/*
 * The authors of this file license it to you under the
 * Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.heuermh.velocity.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.Properties;

import com.google.common.base.Splitter;

import com.google.common.collect.Maps;

import org.apache.velocity.VelocityContext;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;

import org.dishevelled.commandline.argument.AbstractArgument;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.StringArgument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Command line interface to Apache Velocity.
 */
public final class VelocityCommandLine implements Runnable {
    /** Input template file. */
    private final File templateFile;

    /** Output file. */
    private final File outputFile;

    /** Charset. */
    private final Charset charset;

    /** Velocity context. */
    private final VelocityContext velocityContext;

    /** Velocity engine. */
    private final VelocityEngine velocityEngine;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(VelocityCommandLine.class);

    /** Usage string. */
    private static final String USAGE = "velocity -c foo=bar,baz=qux | -j context.json -r /resource/path -t template.wm [-o output.txt] [-e euc-jp] [--verbose]";


    /**
     * Create a new command line interface to Apache Velocity.
     *
     * @param context context (can be null if jsonFile is not null)
     * @param jsonFile (can be null if context is not null)
     * @param resourcePath resource path, if any
     * @param templateFile input template file, must not be null
     * @param outputFile output file, if any
     * @param charset charset, must not be null
     * @throws FileNotFoundException
     */
    public VelocityCommandLine(final String context,
                               final File jsonFile,
                               final File resourcePath,
                               final File templateFile,
                               final File outputFile,
                               final Charset charset) throws FileNotFoundException, IllegalArgumentException
    {
        checkNotNull(templateFile);
        checkNotNull(charset);
        this.templateFile = templateFile;
        this.outputFile = outputFile;
        this.charset = charset;

        if (context != null) {
            Map<String, Object> map = Maps.newHashMap(Splitter.on(",").withKeyValueSeparator("=").split(context));
            logger.info("Using {} as context", map);
            velocityContext = new VelocityContext(map);
        }
        else if (jsonFile != null) {

            FileInputStream is = new FileInputStream(jsonFile);
            JSONTokener tokener = new JSONTokener(is);
            JSONObject object = new JSONObject(tokener);

            velocityContext = new VelocityContext();
            for(String key : object.keySet())
            {
                velocityContext.put(key, object.get(key));
            }
        }
        else {
            throw(new IllegalArgumentException("context or jsonFile must not be null"));
        }

        final Properties config = new Properties();
        logger.info("Using {} as input and default encoding", charset);
        config.setProperty(Velocity.ENCODING_DEFAULT, charset.toString());
        config.setProperty(Velocity.INPUT_ENCODING, charset.toString());
        if (resourcePath != null) {
            logger.info("Using {} as resource path", resourcePath);
            config.setProperty(Velocity.RESOURCE_LOADER, "classpath,file");
            config.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, resourcePath.toString());
        }

        velocityEngine = new VelocityEngine();
        velocityEngine.init(config);
    }


    @Override
    public void run() {
        try (Writer writer = writer(outputFile, charset)) {
            logger.info("Merging template {} to {}", templateFile, outputFile == null ? "stdout" : outputFile);
            velocityEngine.mergeTemplate(templateFile.getName(), charset.name(), velocityContext, writer);
        }
        catch (IOException e) {
            logger.error("Could not merge template {}, caught IOException", templateFile, e);
            System.exit(1);
        }
    }


    /**
     * Create a new writer with the specified output file or stdout and charset file encoding.
     *
     * @param outputFile output file, if any
     * @param charset charset file encoding
     * @return a new writer with the specified output file and charset
     * @throws FileNotFoundException if output file specified but not found
     */
    private static Writer writer(final File outputFile, final Charset charset) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter((outputFile == null) ? System.out : new FileOutputStream(outputFile), charset.newEncoder()));
    }

    /**
     * Charset argument.
     */
    private static class CharsetArgument extends AbstractArgument<Charset> {
        CharsetArgument(final String shortName, final String longName, final String description, final boolean required) {
            super(shortName, longName, description, required);
        }

        @Override
        protected Charset convert(final String s) throws Exception {
            return Charset.forName(s);
        }
    }

    /**
     * Main.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help = new Switch("h", "help", "display help message");
        Switch verbose = new Switch("v", "verbose", "display verbose log messages");

        // required arguments
        FileArgument templateFile = new FileArgument("t", "template", "template file", true);

        // optional arguments
        StringArgument context = new StringArgument("c", "context", "context as comma-separated key value pairs", false);
        FileArgument jsonFile = new FileArgument("j", "jsonFile", "context as json file", false);
        FileArgument resourcePath = new FileArgument("r", "resource", "resource path", false);
        FileArgument outputFile = new FileArgument("o", "output", "output file, default stdout", false);
        CharsetArgument charset = new CharsetArgument("e", "encoding", "encoding, default UTF-8", false);

        ArgumentList arguments = new ArgumentList(about, help, context, jsonFile, resourcePath, templateFile, outputFile, charset, verbose);
        CommandLine commandLine = new CommandLine(args);
        try
        {
            CommandLineParser.parse(commandLine, arguments);
            if (about.wasFound()) {
                About.about(System.out);
                System.exit(0);
            }
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(-2);
            }
            if (verbose.wasFound()) {
                Properties systemProperties = System.getProperties();
                systemProperties.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
            }

            new VelocityCommandLine(context.getValue(),
                                    jsonFile.getValue(),
                                    resourcePath.getValue(),
                                    templateFile.getValue(),
                                    outputFile.getValue(),
                                    charset.getValue(StandardCharsets.UTF_8)).run();
        }
        catch (CommandLineParseException e) {
            if (about.wasFound()) {
                About.about(System.out);
                System.exit(0);
            }
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(0);
            }
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        catch (IllegalArgumentException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        catch (FileNotFoundException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
    }
}
