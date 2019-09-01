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

import java.io.PrintStream;

/**
 * About.
 */
final class About {
    private static final String ARTIFACT_ID = "${project.artifactId}";
    private static final String BUILD_TIMESTAMP = "${build-helper-maven-plugin.build.timestamp}";
    private static final String COMMIT = "${git.commit.id}";
    private static final String COPYRIGHT = "Copyright (c) 2014-2019 held jointly by the individual authors.";
    private static final String LICENSE = "Licensed under Apache License, Version 2.0";
    private static final String VERSION = "${project.version}";


    /**
     * Return the artifact id.
     *
     * @return the artifact id
     */
    public String artifactId() {
        return ARTIFACT_ID;
    }

    /**
     * Return the build timestamp.
     *
     * @return the build timestamp
     */
    public String buildTimestamp() {
        return BUILD_TIMESTAMP;
    }

    /**
     * Return the last commit.
     *
     * @return the last commit
     */
    public String commit() {
        return COMMIT;
    }

    /**
     * Return the license.
     *
     * @return the license
     */
    public String license() {
        return LICENSE;
    }

    /**
     * Return the copyright.
     *
     * @return the copyright
     */
    public String copyright() {
        return COPYRIGHT;
    }

    /**
     * Return the version.
     *
     * @return the version
     */
    public String version() {
        return VERSION;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(artifactId());
        sb.append(" ");
        sb.append(version());
        sb.append("\n");
        sb.append("Commit: ");
        sb.append(commit());
        sb.append("  Build: ");
        sb.append(buildTimestamp());
        sb.append("\n");
        sb.append(copyright());
        sb.append("\n");
        sb.append(license());
        sb.append("\n");
        return sb.toString();
    }


    /**
     * Write about text to the specified print stream.
     *
     * @param out print stream to write about text to
     */
    public static void about(final PrintStream out) {
        checkNotNull(out);
        out.print(new About().toString());
    }
}
