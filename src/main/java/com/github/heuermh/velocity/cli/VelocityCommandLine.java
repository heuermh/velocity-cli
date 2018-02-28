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

import static java.util.Objects.requireNonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.JdkLogChute;
import org.apache.velocity.tools.generic.EscapeTool;
import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.StringArgument;

/**
 * Command line interface to Apache Velocity.
 */
public final class VelocityCommandLine implements Runnable {

    /** Input resource path. */
    private final File resourcePath;    
    
    /** Input template file. */
    private final File templateFile;

    /** Output file. */
    private final File outputFile;

    /** Encoding. */
    private final Charset charset;

    /** Velocity context. */
    private final VelocityContext velocityContext;

    /** Velocity engine. */
    private final VelocityEngine velocityEngine;

    /** Usage string. */
    private static final String USAGE = "java VelocityCommandLine -c foo=bar "
            + "-r /my/path/to/templates -t template.vm [-o output.txt] [-e encoding] "
            + "[-x escapetool] [-p file.properties] [-P Properties] [-l logLevel]";

    private static final Logger LOG = Logger.getLogger(VelocityCommandLine.class.getName());

    /**
     * Create a new command line interface to Apache Velocity.
     *
     * @param context context, must not be null
     * @param resourcePath file resource path, must not be null
     * @param templateFile input template file, must not be null
     * @param outputFile output file
     * @param charset charset, must not be null
     * @param escapetool escapetool
     * @param properties
     * @param propertiesName propertiesName, must not be null
     */
    public VelocityCommandLine(final @Nonnull String context, final @Nonnull File resourcePath,
            final @Nonnull File templateFile, final File outputFile, 
            final @Nonnull Charset charset, final String escapetool,
            final Properties properties, final @Nonnull String propertiesName) {
        requireNonNull(context);
        requireNonNull(resourcePath);
        requireNonNull(templateFile);
        requireNonNull(charset);
        requireNonNull(propertiesName);

        this.resourcePath = resourcePath;
        this.templateFile = templateFile;
        this.outputFile = outputFile;
        this.charset = charset;

        final Pattern comma = literalPattern(",");
        final Pattern equals = literalPattern("=");
        
        final HashMap<String, String> contextMap = comma.splitAsStream(context)
                .collect(HashMap<String, String>::new,
                        (m, e) -> {
                            String[] s = equals.split(e);
                            m.put(s[0], s[1]);
                        },
                        (m, n) -> m.putAll(n));

        velocityContext = new VelocityContext(refineContext(contextMap));
        if (escapetool != null) {
            LOG.config((() -> "setting EscapeTool to " + escapetool));
            velocityContext.put(escapetool, new EscapeTool());
        }
        
        if (properties != null) {
            LOG.config(() -> "setting Properties to " + propertiesName);
            LOG.fine(() -> "properties: " + properties);
            velocityContext.put(propertiesName, properties);
        }

        final Properties config = new Properties();
        config.setProperty(Velocity.RESOURCE_LOADER, "classpath,file");
        config.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, this.resourcePath.toString());
        config.setProperty(Velocity.ENCODING_DEFAULT, charset.toString());
        config.setProperty(Velocity.INPUT_ENCODING, charset.toString());
        config.setProperty(Velocity.OUTPUT_ENCODING, charset.toString());
        
        velocityEngine = new VelocityEngine();
        Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, JdkLogChute.class);
        
        velocityEngine.init(config);
    }

    /** Convert {@code {foo.bar=a,foo.baz=b}} to {@code {foo.{bar=a,baz=b}}}. */
    static Map<String, Object> refineContext(Map<String, String> context) {
        Map<String, Object> result = new HashMap<>();

        context.entrySet().forEach(refineEntryInto(result));
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private static Consumer<Entry<String, String>> refineEntryInto(Map<String, Object> result) {
        Pattern dot = literalPattern(".");
        return e -> {
            String key = e.getKey();
            String value = e.getValue();

            Map<String, Object> m = result;
            String[] parts = dot.split(key);
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                Object o = m.computeIfAbsent(part, __ -> new HashMap<>());
                if (o instanceof Map) {
                    m = (Map<String, Object>) o;
                } else {
                    throw new IllegalStateException("unexpected " + o);
                }
            }
            m.put(parts[parts.length - 1], value);
        };
    }

    private static Pattern literalPattern(String s) {
        return Pattern.compile(s, Pattern.LITERAL);
    }

    @Override
    public void run() {
        try (Writer writer = writer(outputFile, charset)) {
            velocityEngine.mergeTemplate(templateFile.getName(), charset.name(), velocityContext, writer);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error when merging templeate:", e);
            throw new RuntimeException(e);
        }
    }

    private static Writer writer(File outputFile, Charset charset) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(
                (outputFile == null)
                        ? System.out
                        : new FileOutputStream(outputFile), charset.newEncoder()));
    }

    /**
     * Main.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help = new Switch("h", "help", "display help message");
        StringArgument context = new StringArgument("c", "context", "context as comma-separated key value pairs", true);
        FileArgument resourcePath = new FileArgument("r", "resourcePath", "Resource Path", true);
        FileArgument templateFile = new FileArgument("t", "template", "template file", true);
        FileArgument outputFile = new FileArgument("o", "output", "output file, default stdout", false);
        StringArgument encoding = new StringArgument("e", "encoding", "encoding, default utf-8", false);
        StringArgument escapeTool = new StringArgument("x", "escapetool", "add escapetool into context", false);
        FileArgument propertiesFile = new FileArgument("p", "propertiesFile", "add properties into context, default null or System.getProperties, if -P given", false);
        StringArgument propertiesName = new StringArgument("P", "propertiesName", "name for properties, default Properties", false);
        StringArgument logLevel = new StringArgument("l", "logLevel", "log level, default SEVERE", false);

        ArgumentList arguments = new ArgumentList(about, help, context, resourcePath, templateFile, outputFile, encoding, escapeTool, propertiesFile, propertiesName, logLevel);
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
            if (logLevel.wasFound()) {
                try {
                    final Level level = Level.parse(
                            logLevel.getValue(Level.SEVERE.toString()));
                    LOG.setLevel(level);
                    final Logger rootLogger = LogManager.getLogManager().getLogger("");
                    for (Handler h : rootLogger.getHandlers()) {
                        LOG.config(() -> "Setting log level for handler " 
                                + h.getClass() + " to " + level);
                        h.setLevel(level);
                    }
                    LOG.config(() -> "Set log level to " + LOG.getLevel());
                } catch (IllegalArgumentException iae) {
                    LOG.severe(() -> "Unknown log level " + logLevel.getValue());
                    LOG.severe(() -> "Supported log levels " + Arrays.asList(
                            Level.ALL, Level.FINEST, Level.FINER, Level.FINE,
                            Level.CONFIG, Level.INFO, Level.WARNING,
                            Level.SEVERE, Level.OFF));
                    Usage.usage(USAGE, null, commandLine, arguments, System.out);
                    System.exit(-1);
                }
            }
            if (context.wasFound() && context.getValue() == null) {
                LOG.severe("No context given");
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(-1);
            }
            if (resourcePath.wasFound() && !Files.isDirectory(resourcePath.getValue().toPath())) {                
                LOG.severe("Invalid Resource Path");
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(-1);
            }            
            Charset cs;
            if (encoding.wasFound()) {
                final String encodingValue = encoding.getValue();
                if (Charset.isSupported(encodingValue)) {
                    cs = Charset.forName(encodingValue);
                } else {
                    LOG.severe(() -> "Unknown encoding " + encodingValue);
                    LOG.severe(() -> "Supported encodings " + Charset.availableCharsets().values());
                    Usage.usage(USAGE, null, commandLine, arguments, System.out);
                    System.exit(-1);
                    throw new AssertionError();
                }
            } else {
                cs = StandardCharsets.UTF_8;
            }
            Properties properties;
            if (propertiesFile.wasFound()) {
                File f = propertiesFile.getValue();
                try {
                    properties = new Properties();
                    properties.load(Files.newInputStream(f.toPath()));
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "Error reading file", ex);
                    Usage.usage(USAGE, null, commandLine, arguments, System.out);
                    System.exit(-1);
                    throw new AssertionError();
                }
            } else if (propertiesName.wasFound()) {
                properties = System.getProperties();
            } else {
                properties = null;
            }
            new VelocityCommandLine(context.getValue(), resourcePath.getValue(), templateFile.getValue(), 
                    outputFile.getValue(), cs, escapeTool.getValue(), 
                    properties, propertiesName.getValue("Properties")).run();
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
    }
}
