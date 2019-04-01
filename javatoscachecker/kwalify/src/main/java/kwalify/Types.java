/*
 * The original version of this file was provided under the MIT license,
 * as follows:
 *
 * @(#)Types.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * Copyright (c) <2005> <kuwata lab>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Modifications to this file are provided under the Apache 2.0 license,
 * as follows:

 * Copyright (c) 2017 <AT&T>.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package kwalify;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Date;

/**
 * utility methods for type (str, int, ...).
 *
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class Types {

    public static Class typeClass(String type) {
        return (Class)typeClasses.get(type);
    }

    public static String typeName(String type) {
        String name = (String)typeNames.get(type);
        if (name == null) name = type;
        return name;
    }

    public static final String DEFAULT_TYPE = "str";

    public static String getDefaultType() { return DEFAULT_TYPE; }

    private static Map typeClasses;
    private static Map typeNames;
    static {
        //
        typeClasses = new HashMap();
        typeClasses.put("str",       String.class);
        typeClasses.put("int",       Integer.class);
        typeClasses.put("float",     Double.class);
        typeClasses.put("number",    Number.class);
        typeClasses.put("text",      null);
        typeClasses.put("bool",      Boolean.class);
        typeClasses.put("map",       Map.class);
        typeClasses.put("seq",       List.class);
        typeClasses.put("timestamp", Date.class);
        typeClasses.put("date",      Date.class);
        typeClasses.put("symbol",    String.class);
        typeClasses.put("scalar",    null);
        typeClasses.put("any",       Object.class);
        typeClasses.put("ref",       Object.class);  // by jora
        //__type_classes.put("null",      null);

        //
        typeNames = new HashMap();
        typeNames.put("map",  "mapping");
        typeNames.put("seq",  "sequence");
        typeNames.put("str",  "string");
        typeNames.put("int",  "integer");
        typeNames.put("bool", "boolean");
        typeNames.put("ref",  "reference");					//by jora
    }


    public static boolean isBuiltinType(String type) {
        return typeClasses.containsKey(type);
    }

    public static boolean isCollectionType(String type) {
        return type.equals("map") || type.equals("seq");
    }

    public static boolean isMapType(String type) {
        return type.equals("map");
    }

    public static boolean isScalarType(String type) {
        return !isCollectionType(type);
    }

    public static boolean isCollection(Object obj) {
        return obj instanceof Map || obj instanceof List;
    }

    public static boolean isScalar(Object obj) {
        return !isCollection(obj);
    }

    public static boolean isCorrectType(Object obj, String type) {
        Class typeClassObj = typeClass(type);
        if (typeClassObj != null) {
            return typeClassObj.isInstance(obj);
        }
        if (type.equals("null")) {
            return obj == null;
        } else if (type.equals("text")) {
            return obj instanceof String || obj instanceof Number;
        } else if (type.equals("scalar")) {
            return obj instanceof Number || obj instanceof String || obj instanceof Boolean || obj instanceof Date;
        }
        return false;
    }

}
