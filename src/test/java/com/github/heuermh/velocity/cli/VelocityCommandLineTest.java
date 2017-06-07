/*
 * Copyright 2017 renekraneis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.heuermh.velocity.cli;

import static com.github.heuermh.velocity.cli.VelocityCommandLine.refineContext;
import java.util.Collections;

import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author renekraneis
 */
public class VelocityCommandLineTest {

    @Test
    public void refineContext_should_refine_an_empty_context() {
        Map<String, String> context = new HashMap<>();
        Map<String, Object> refinedContext = refineContext(context);
        assertThat(refinedContext, is(equalTo(Collections.emptyMap())));
    }

    @Test
    public void refineContext_should_refine_a_simple_context() {
        Map<String, String> context = new HashMap<>();
        context.put("simpleKey", "value");
        context.put("anotherSimpleKey", "value");

        Map<String, Object> expectedContext = new HashMap<>(context);
        Map<String, Object> refinedContext = refineContext(context);

        assertThat(refinedContext, is(equalTo(expectedContext)));
    }

    @Test
    public void refineContext_should_refine_a_complex_context() {
        Map<String, String> context = new HashMap<>();
        context.put("complex.key", "value");
        context.put("complex.key2", "value2");

        Map<String, Map<String, String>> expectedContext = new HashMap<>();
        expectedContext.compute("complex", (k, m) -> m == null ? new HashMap<>() : m).put("key", "value");
        expectedContext.compute("complex", (k, m) -> m == null ? new HashMap<>() : m).put("key2", "value2");

        Map<String, Object> refinedContext = refineContext(context);

        assertThat(refinedContext, is(equalTo(expectedContext)));
    }

    @Test
    public void refineContext_error_for_an_illegal_context() {
        Map<String, String> context = new HashMap<>();
        context.put("complex", "value");
        context.put("complex.key", "value2");

        try {
            refineContext(context);
            fail();
        } catch (IllegalStateException ise) {
            assertThat(ise.getMessage(), is(equalTo("unexpected value")));
        }
    }

}
