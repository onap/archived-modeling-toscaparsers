/*
 * @(#)Types.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
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
        return (Class)__type_classes.get(type);
    }

    public static String typeName(String type) {
        String name = (String)__type_names.get(type);
        if (name == null) name = type;
        return name;
    }

    public static final String DEFAULT_TYPE = "str";

    public static String getDefaultType() { return DEFAULT_TYPE; }

    private static Map __type_classes;
    private static Map __type_names;
    static {
        //
        __type_classes = new HashMap();
        __type_classes.put("str",       String.class);
        __type_classes.put("int",       Integer.class);
        __type_classes.put("float",     Double.class);
        __type_classes.put("number",    Number.class);
        __type_classes.put("text",      null);
        __type_classes.put("bool",      Boolean.class);
        __type_classes.put("map",       Map.class);
        __type_classes.put("seq",       List.class);
        __type_classes.put("timestamp", Date.class);
        __type_classes.put("date",      Date.class);
        __type_classes.put("symbol",    String.class);
        __type_classes.put("scalar",    null);
        __type_classes.put("any",       Object.class);
        __type_classes.put("ref",       Object.class);  // by jora
        //__type_classes.put("null",      null);

        //
        __type_names = new HashMap();
        __type_names.put("map",  "mapping");
        __type_names.put("seq",  "sequence");
        __type_names.put("str",  "string");
        __type_names.put("int",  "integer");
        __type_names.put("bool", "boolean");
        __type_names.put("ref",  "reference");					//by jora
    }


    public static boolean isBuiltinType(String type) {
        return __type_classes.containsKey(type);
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
        Class type_class = typeClass(type);
        if (type_class != null) {
            return type_class.isInstance(obj);
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
