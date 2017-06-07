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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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

    /** Input template file. */
    private final File templateFile;

    /** Output file. */
    private final File outputFile;

    /** Encoding. */
    private final Charset charset;

    /** Escapetool. */
    private final String escapetool;

    /** Velocity context. */
    private final VelocityContext velocityContext;

    /** Velocity engine. */
    private final VelocityEngine velocityEngine;

    /** Usage string. */
    private static final String USAGE = "java VelocityCommandLine -c foo=bar -t template.vm [-o output.txt] [-e encoding] [-x escapetool]";

    private static final Logger LOG = Logger.getLogger(VelocityCommandLine.class.getName());

    /**
     * Create a new command line interface to Apache Velocity.
     *
     * @param context context, must not be null
     * @param templateFile input template file, must not be null
     * @param outputFile output file
     * @param charset charset, must not be null
     * @param escapetool escapetool
     */
    public VelocityCommandLine(final String context, final File templateFile, final File outputFile, final Charset charset, final String escapetool) {
        requireNonNull(context);
        requireNonNull(templateFile);
        this.templateFile = templateFile;
        this.outputFile = outputFile;
        this.charset = charset;
        this.escapetool = escapetool;

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
            velocityContext.put(escapetool, new EscapeTool());
        }

        velocityEngine = new VelocityEngine();
        Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, JdkLogChute.class);
        velocityEngine.init();
    }

    /** Convert {@code {foo.bar=a,foo.baz=b}} to {@code {foo.{bar=a,baz=b}}}. */
    static Map<String, Object> refineContext(Map<String, String> context) {
        Map<String, Object> result = new HashMap<>();
        Pattern dot = literalPattern(".");
        context.entrySet().forEach(e -> {
            String key = e.getKey();
            String value = e.getValue();

            Map<String, Object> m = result;
            String[] parts = dot.split(key);
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                Object o = m.computeIfAbsent(part, __ -> new HashMap<>());
                if (o instanceof Map) {
                    m = (Map) o;
                } else {
                    throw new IllegalStateException("unexpected " + o);
                }
            }
            m.put(parts[parts.length - 1], value);
        });
        return result;
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
            System.exit(1);
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
        FileArgument templateFile = new FileArgument("t", "template", "template file", true);
        FileArgument outputFile = new FileArgument("o", "output", "output file, default stdout", false);
        StringArgument encoding = new StringArgument("e", "encoding", "encoding, default utf-8", false);
        StringArgument escapeTool = new StringArgument("x", "escapetool", "add escapetool into context", false);

        ArgumentList arguments = new ArgumentList(about, help, context, templateFile, outputFile, encoding, escapeTool);
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
            Charset cs;
            if (encoding.wasFound()) {
                final String encodingValue = encoding.getValue();
                if (Charset.isSupported(encodingValue)) {
                    cs = Charset.forName(encodingValue);
                } else {
                    LOG.severe(() -> "Unknown encoding " + encodingValue);
                    LOG.severe(() -> "Supported encodings " + Charset.availableCharsets().values());
                    System.exit(-1);
                    throw new AssertionError();
                }
            } else {
                cs = StandardCharsets.UTF_8;
            }
            new VelocityCommandLine(context.getValue(), templateFile.getValue(), outputFile.getValue(), cs, escapeTool.getValue()).run();
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
