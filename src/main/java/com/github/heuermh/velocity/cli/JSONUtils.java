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

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;


public class JSONUtils {

    public VelocityContext fromJSONString(String jsonString) {

        final JSONTokener tokener = new JSONTokener(jsonString);
        final JSONObject object = new JSONObject(tokener);

        return fromJSONbject(object);
    }

    public VelocityContext fromJSONbject(JSONObject object) {
        
        final VelocityContext vc = new VelocityContext();

        for (final String key : object.keySet()) {
            final Object v = object.get(key);

            if (v instanceof org.json.JSONObject) {
                vc.put(key, fromJSONbject((JSONObject) v));
            } 
            else if (v instanceof org.json.JSONArray) {
                final JSONArray ja = (JSONArray) v;

                final Object[] va = new Object[ja.length()];

                for (int i = 0; i < ja.length(); i++) {

                    final Object o = ja.get(i);

                    if (o instanceof JSONObject) {
                        va[i] = fromJSONbject((JSONObject) o);
                    } else {
                        va[i] = o;
                    }
                }

                vc.put(key, va);
            } 
            else {
                vc.put(key, v);
            }
        }

        return vc;
    }

    public JSONObject toJSONObject(VelocityContext vc) {
        final JSONObject obj = new JSONObject();

        final Object[] keys = vc.getKeys();

        for (final Object o : keys) {
            final String key = (String) o;
            final Object value = vc.get(key);

            if (value instanceof VelocityContext) {
                obj.put(key, toJSONObject((VelocityContext) value));
            }
            else {
                obj.put(key, value);
            }
        }

        return obj;
    }

    public Object toJSONObject(Object obj) {

        if (obj.getClass().isArray()) {

            JSONArray jsonArray = new JSONArray();

            Object[] a = (Object[])obj;

            for (int i = 0; i < a.length; i++) {
                jsonArray.put(toJSONObject(a[i]));
            }

            return jsonArray;
        }
        else if (obj instanceof VelocityContext) {
            return toJSONObject((VelocityContext)obj);
        }

        return new JSONObject(obj);
    }

    public String toJSONString(Object obj) {

        Object jsonObj = toJSONObject(obj);

        if (jsonObj instanceof JSONObject) {
            return ((JSONObject)jsonObj).toString(4);
        }
        else if (jsonObj instanceof JSONArray) {
            return ((JSONArray)jsonObj).toString(4);
        }
        else {
            return obj.toString();
        }
    }
    
}