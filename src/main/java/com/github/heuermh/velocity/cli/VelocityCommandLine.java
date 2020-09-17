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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Splitter;

import com.google.common.collect.Maps;

import org.apache.velocity.VelocityContext;

import org.apache.velocity.context.Context;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

import org.apache.velocity.tools.ToolManager;

import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;

import org.dishevelled.commandline.argument.AbstractArgument;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.StringArgument;
import org.dishevelled.commandline.argument.StringListArgument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line interface to Apache Velocity.
 */
@Immutable
@ParametersAreNonnullByDefault
public final class VelocityCommandLine implements Runnable {
    /** Input template file. */
    private final File templateFile;

    /** Output file. */
    private final File outputFile;

    /** Charset. */
    private final Charset charset;

    /** Velocity context. */
    private final Context velocityContext;

    /** Velocity engine. */
    private final VelocityEngine velocityEngine;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(VelocityCommandLine.class);

    /** Usage string. */
    private static final String USAGE = "velocity -t template.wm\n  [-c foo=bar,baz=qux]\n  [-r /resource/path]\n  [-o output.txt]\n  [-e euc-jp]\n  [-g date,math]\n  [--verbose]";


    /**
     * Create a new command line interface to Apache Velocity
     * with an empty context.
     *
     * @since 2.2
     * @param resourcePath resource path, if any
     * @param templateFile input template file, must not be null
     * @param outputFile output file, if any
     * @param charset charset, must not be null
     */
    public VelocityCommandLine(@Nullable final File resourcePath,
                               final File templateFile,
                               @Nullable final File outputFile,
                               final Charset charset) {

        this(null, resourcePath, templateFile, outputFile, charset);
    }

    /**
     * Create a new command line interface to Apache Velocity.
     *
     * @param context context, if any
     * @param resourcePath resource path, if any
     * @param templateFile input template file, must not be null
     * @param outputFile output file, if any
     * @param charset charset, must not be null
     */
    public VelocityCommandLine(@Nullable final String context,
                               @Nullable final File resourcePath,
                               final File templateFile,
                               @Nullable final File outputFile,
                               final Charset charset) {

        this(context, resourcePath, templateFile, outputFile, null, charset);
    }

    /**
     * Create a new command line interface to Apache Velocity.
     *
     * @since 2.2
     * @param context context, if any
     * @param resourcePath resource path, if any
     * @param templateFile input template file, must not be null
     * @param outputFile output file, if any
     * @param tools list of tools to install, if any
     * @param charset charset, must not be null
     */
    public VelocityCommandLine(@Nullable final String context,
                               @Nullable final File resourcePath,
                               final File templateFile,
                               @Nullable final File outputFile,
                               @Nullable final List<String> tools,
                               final Charset charset) {

        checkNotNull(templateFile);
        checkNotNull(charset);
        this.templateFile = templateFile;
        this.outputFile = outputFile;
        this.charset = charset;

        ToolManager toolManager = new ToolManager(true);
        velocityContext = toolManager.createContext();

        if (tools != null && !tools.isEmpty()) {
            installGenericTools(tools, velocityContext);
        }

        if (context != null) {
            Map<String, Object> map = Maps.newHashMap(Splitter.on(",").withKeyValueSeparator("=").split(context));
            logger.info("Using {} as context", map);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                velocityContext.put(entry.getKey(), entry.getValue());
            }
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
     * Install generic tools into the specified Velocity context.
     *
     * @param tools list of tools to install
     * @param velocityContext velocity context
     */
    private static void installGenericTools(final List<String> tools, final Context velocityContext) {
        for (String tool : tools) {
            if ("alt".equals(tool)) {
                velocityContext.put("alt", new org.apache.velocity.tools.generic.AlternatorTool());
            }
            else if ("class".equals(tool)) {
                velocityContext.put("class", new org.apache.velocity.tools.generic.ClassTool());
            }
            else if ("context".equals(tool)) {
                velocityContext.put("context", new org.apache.velocity.tools.generic.ContextTool());
            }
            else if ("convert".equals(tool)) {
                velocityContext.put("convert", new org.apache.velocity.tools.generic.ConversionTool());
            }
            else if ("date".equals(tool)) {
                velocityContext.put("date", new org.apache.velocity.tools.generic.ComparisonDateTool());
            }
            else if ("disp".equals(tool)) {
                velocityContext.put("disp", new org.apache.velocity.tools.generic.DisplayTool());
            }
            else if ("esc".equals(tool)) {
                velocityContext.put("esc", new org.apache.velocity.tools.generic.EscapeTool());
            }
            else if ("field".equals(tool)) {
                velocityContext.put("field", new org.apache.velocity.tools.generic.FieldTool());
            }
            else if ("json".equals(tool)) {
                velocityContext.put("json", new org.apache.velocity.tools.generic.JsonTool());
            }
            else if ("link".equals(tool)) {
                velocityContext.put("link", new org.apache.velocity.tools.generic.LinkTool());
            }
            else if ("log".equals(tool)) {
                velocityContext.put("log", new org.apache.velocity.tools.generic.LogTool());
            }
            else if ("math".equals(tool)) {
                velocityContext.put("math", new org.apache.velocity.tools.generic.MathTool());
            }
            else if ("number".equals(tool)) {
                velocityContext.put("number", new org.apache.velocity.tools.generic.NumberTool());
            }
            else if ("render".equals(tool)) {
                velocityContext.put("render", new org.apache.velocity.tools.generic.RenderTool());
            }
            else if ("sort".equals(tool)) {
                velocityContext.put("sort", new org.apache.velocity.tools.generic.SortTool());
            }
            else if ("text".equals(tool)) {
                velocityContext.put("text", new org.apache.velocity.tools.generic.ResourceTool());
            }
            else if ("xml".equals(tool)) {
                velocityContext.put("xml", new org.apache.velocity.tools.generic.XmlTool());
            }
        }
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
        FileArgument resourcePath = new FileArgument("r", "resource", "resource path", false);
        FileArgument outputFile = new FileArgument("o", "output", "output file, default stdout", false);
        StringListArgument tools = new StringListArgument("g", "tools", "comma-separated list of generic tools to install", false);
        CharsetArgument charset = new CharsetArgument("e", "encoding", "encoding, default UTF-8", false);

        ArgumentList arguments = new ArgumentList(about, help, templateFile, context, resourcePath, outputFile, charset, tools, verbose);
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
                                    resourcePath.getValue(),
                                    templateFile.getValue(),
                                    outputFile.getValue(),
                                    tools.getValue(),
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
    }
}
