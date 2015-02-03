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
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.google.common.base.Splitter;

import com.google.common.collect.Maps;

import org.apache.velocity.VelocityContext;

import org.apache.velocity.app.VelocityEngine;

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

    /** Velocity context. */
    private final VelocityContext velocityContext;

    /** Velocity engine. */
    private final VelocityEngine velocityEngine;

    /** Usage string. */
    private static final String USAGE = "java VelocityCommandLine -c foo=bar -t template.wm [-o output.txt]";


    /**
     * Create a new command line interface to Apache Velocity.
     *
     * @param context context, must not be null
     * @param templateFile input template file
     * @param outputFile output file
     */
    public VelocityCommandLine(final String context, final File templateFile, final File outputFile) {
        checkNotNull(context);
        this.templateFile = templateFile;
        this.outputFile = outputFile;

        velocityContext = new VelocityContext(Maps.newHashMap(Splitter.on(",").withKeyValueSeparator("=").split(context)));

        velocityEngine = new VelocityEngine();
        //Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, this);
        velocityEngine.init();
    }


    @Override
    public void run() {
        Writer writer = null;
        try {
            writer = (outputFile == null) ? new BufferedWriter(new OutputStreamWriter(System.out)) : new BufferedWriter(new FileWriter(outputFile));
            velocityEngine.mergeTemplate(templateFile.getName(), "UTF-8", velocityContext, writer);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            try {
                writer.close();
            }
            catch (Exception e) {
                // ignored
            }
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
        StringArgument context = new StringArgument("c", "context", "context as comma-separated key value pairs", true);
        FileArgument templateFile = new FileArgument("t", "template", "template file", true);
        FileArgument outputFile = new FileArgument("o", "output", "output file, default stdout", false);

        ArgumentList arguments = new ArgumentList(about, help, context, templateFile, outputFile);
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
            new VelocityCommandLine(context.getValue(), templateFile.getValue(), outputFile.getValue()).run();
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
