/*
 * @(#)Util.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.File;


/**
 * set of utility methods
 *
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */

public class Util {

    /**
     *  inspect List or Map
     */
    public static String inspect(Object obj) {
        StringBuffer sb = new StringBuffer();
        inspect(obj, sb, null);
        return sb.toString();
    }

    private static void inspect(Object obj, StringBuffer sb, Map done) {
        if (obj == null) {
            sb.append("nil");   // null?
        } else if (obj instanceof String) {
            inspect((String)obj, sb, done);
        } else if (obj instanceof Map) {
            if (done == null) {
                done = new IdentityHashMap();
            }
            if (done.containsKey(obj)) {
                sb.append("{...}");
            } else {
                done.put(obj, Boolean.TRUE);
                inspect((Map)obj, sb, done);
            }
        } else if (obj instanceof List) {
            if (done == null) {
                done = new IdentityHashMap();
            }
            if (done.containsKey(obj)) {
                sb.append("[...]");
            } else {
                done.put(obj, Boolean.TRUE);
                inspect((List)obj, sb, done);
            }
        } else {
            sb.append(obj.toString());
        }
    }

    private static void inspect(Map map, StringBuffer sb, Map done) {
        sb.append('{');
        List list = new ArrayList(map.keySet());
        Collections.sort(list);
        int i = 0;
        for (Iterator it = list.iterator(); it.hasNext(); i++) {
            Object key   = it.next();
            Object value = map.get(key);
            if (i > 0) {
                sb.append(", ");
            }
            inspect(key, sb, done);
            sb.append("=>");
            inspect(value, sb, done);
        }
        sb.append('}');
    }

    private static void inspect(List list, StringBuffer sb, Map done) {
        sb.append('[');
        int i = 0;
        for (Iterator it = list.iterator(); it.hasNext(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object item = it.next();
            inspect(item, sb, null);
        }
        sb.append(']');
    }

    private static void inspect(String str, StringBuffer sb, Map done) {
        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
              case '"':   sb.append("\\\"");  break;
              case '\n':  sb.append("\\n");   break;
              case '\r':  sb.append("\\r");   break;
              case '\t':  sb.append("\\t");   break;
              default:    sb.append(ch);      break;
            }
        }
        sb.append('"');
    }



    /**
     *
     */
    protected static HashMap __patterns = new HashMap();

    /**
     *  match pattern and return Mather object.
     *
     *  ex.
     *  <pre>
     *   String target = " name = foo\n mail = foo@mail.com\m";
     *   Matcher m = Util.matcher(target, "^\\s*(\\w+)\\s*=\\s*(.*)$");
     *   while (m.find()) {
     *     String key   = m.group(1);
     *     String value = m.gropu(2);
     *   }
     *  </pre>
     */
    public static Matcher matcher(String target, String regexp) {
        Pattern pat = (Pattern)__patterns.get(regexp);
        if (pat == null) {
            pat = Pattern.compile(regexp);
            __patterns.put(regexp, pat);
        }
        return pat.matcher(target);
    }


    public static Matcher matcher(String target, Pattern regexp) {
        return regexp.matcher(target);
    }


    /**
     *  return if pattern matched or not.
     *
     *  ex.
     *  <pre>
     *   String target = " name = foo\n";
     *   if (Util.matches(target, "^\\s*(\\w+)\\s*=\\s*(.*)$")) {
     *     System.out.println("matched.");
     *   }
     *  </pre>
     */
    public static boolean matches(String target, String regexp) {
        Matcher m = matcher(target, regexp);
        return m.find();
    }


    public static boolean matches(String target, Pattern regexp) {
        Matcher m = regexp.matcher(target);
        return m.find();
    }


    /**
     *  shift array and return new array shifted
     */
    public static String[] arrayShift(String[] array) {
        String[] new_array = new String[array.length - 1];
        for (int i = 0; i < new_array.length; i++) {
            new_array[i] = array[i + 1];
        }
        return new_array;
    }


    /**
     *  pop up array an dreturn new array popped
     */
    public static String[] arrayPop(String[] array) {
        String[] new_array = new String[array.length - 1];
        for (int i = 0; i < new_array.length; i++) {
            new_array[i] = array[i];
        }
        return new_array;
    }


    /**
     *  concatenate all elements of array with separator
     */
    public static String join(Object[] array, String separator) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }


    /**
     *  concatenate all elements of list with separator
     */
    public static String join(List list, String separator) {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        for (Iterator it = list.iterator(); it.hasNext(); i++) {
            Object item = it.next();
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(item);
        }
        return sb.toString();
    }


    /**
     *  split string into list of line
     */
    public static List toListOfLines(String str) {
        List list = new ArrayList();
        int len = str.length();
        int head = 0;
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == '\n') {
                int tail = i + 1;
                String line = str.substring(head, tail);
                list.add(line);
                head = tail;
            }
        }
        if (head != len) {
            String line = str.substring(head, len);
            list.add(line);
        }
        return list;
    }


    /**
     *  split string into array of line
     */
    public static String[] toLines(String str) {
        List list = toListOfLines(str);
        String[] lines = new String[list.size()];
        list.toArray(lines);
        return lines;
    }


    /**
     *  return object id
     */
    public static Integer getId(Object obj) {
        int id = System.identityHashCode(obj);
        return new Integer(id);
    }


    /**
     *  return true if 'instance' is an instance of 'klass'
     */
    public static boolean isInstanceOf(Object instance, Class klass) {
        if (instance == null || klass == null) {
            return false;
        }
        Class c = instance.getClass();
        if (klass.isInterface()) {
            while (c != null) {
                Class[] interfaces = c.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    if (interfaces[i] == klass) {
                        return true;
                    }
                }
                c = c.getSuperclass();
            }
        } else {
            while (c != null) {
                if (c == klass) {
                    return true;
                }
                c = c.getSuperclass();
            }
        }
        return false;
    }


    /**
     *  read file content with default encoding of system
     */
    public static String readFile(String filename) throws IOException {
        String charset = System.getProperty("file.encoding");
        return readFile(filename, charset);
    }


    /**
     *  read file content with specified encoding
     */
    public static String readFile(String filename, String encoding) throws IOException {
        InputStream stream  = null;
        String      content = null;
        try {
            stream = new FileInputStream(filename);
            content = readInputStream(stream, encoding);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignore) {}
            }
        }
        return content;
    }

    /**
     *
     */
    public static String readInputStream(InputStream stream) throws IOException {
        String encoding = System.getProperty("file.encoding");
        return readInputStream(stream, encoding);
    }

    
    /**
     *
     */
    public static String readInputStream(InputStream stream, String encoding) throws IOException {
        Reader reader  = null;
        String content = null;
        try {
            reader = new InputStreamReader(stream, encoding);
            StringBuffer sb = new StringBuffer();
            int ch;
            while ((ch = reader.read()) >= 0) {
                sb.append((char)ch);
            }
            content = sb.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {}
            }
        }
        return content;
    }


    /**
     *
     */
    public static void writeFile(String filename, String content) throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter(filename);
            writer.write(content);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }


    public static void makeDir(String path) throws IOException {
        File dir = new File(path);
        dir.mkdir();
    }


    public static void renameFile(String old_path, String new_path) throws IOException {
        File old_file = new File(old_path);
        File new_file = new File(new_path);
        new_file.delete();
        old_file.renameTo(new_file);
    }


    public static void moveFile(String filepath, String dirpath) throws IOException {
        File old_file = new File(filepath);
        File new_file = new File(dirpath + "/" + old_file.getName());
        new_file.delete();
        old_file.renameTo(new_file);
    }


    public static String untabify(CharSequence str) {
        return untabify(str, 8);
    }


    public static String untabify(CharSequence str, int tab_width) {
        StringBuffer sb = new StringBuffer();
        int len = str.length();
        int col = -1;
        for (int i = 0; i < len; i++) {
            col = ++col % tab_width;
            char ch = str.charAt(i);
            //if (ch == '\t') {
            //    int n = tab_width - col;
            //    while (--n >= 0)
            //        sb.append(' ');
            //    col = -1;  // reset col
            //} else {
            //    sb.append(ch);
            //    if (ch == '\n')
            //        col = -1; // reset col
            //}
            switch (ch) {
            case '\t':
                int n = tab_width - col;
                while (--n >= 0) {
                    sb.append(' ');
                }
                col = -1;  // reset col
                break;
            case '\n':
                sb.append(ch);
                col = -1;  // reset col;
                break;
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }


    private static final int VALUE_INTEGER =  1;
    private static final int VALUE_DOUBLE  =  2;
    private static final int VALUE_STRING  =  4;
    private static final int VALUE_BOOLEAN =  8;
    private static final int VALUE_DATE    = 16;
    private static final int VALUE_OBJECT  = 32;

    public static int compare(Object value1, Object value2) throws InvalidTypeException {
        if (! (value1 instanceof Comparable)) {
            throw new InvalidTypeException(value1.toString() + "is not Comparable.");
        }
        if (! (value2 instanceof Comparable)) {
            throw new InvalidTypeException(value2.toString() + "is not Comparable.");
        }
        return ((Comparable)value1).compareTo((Comparable)value2);
    }

    public static int compareValues(Object value1, Object value2) throws InvalidTypeException {
        int vtype = (valueType(value1) << 8) | valueType(value2);
        switch (vtype) {
        case (VALUE_INTEGER << 8) | VALUE_INTEGER :
            return ((Integer)value1).compareTo((Integer)value2);
        case (VALUE_DOUBLE  << 8) | VALUE_DOUBLE :
            return ((Double)value1).compareTo((Double)value2);
        case (VALUE_STRING  << 8) | VALUE_STRING :
            return ((String)value1).compareTo((String)value2);
        case (VALUE_BOOLEAN << 8) | VALUE_BOOLEAN :
            //return ((Boolean)value1).compareTo((Boolean)value2);     // J2SDK1.4 doesn't support Boolean#compareTo()!
            boolean b1 = ((Boolean)value1).booleanValue();
            boolean b2 = ((Boolean)value2).booleanValue();
            return b1 == b2 ? 0 : (b1 ? 1 : -1);
            //if (b1 == b2) return 0;
            //if (b1 && !b2) return 1;
            //if (!b1 && b2) return -1;
            //assert false;
        case (VALUE_DATE    << 8) | VALUE_DATE :
            return ((Date)value1).compareTo((Date)value2);
        //
        case (VALUE_DOUBLE  << 8) | VALUE_INTEGER :
        case (VALUE_INTEGER << 8) | VALUE_DOUBLE  :
            double d1 = ((Number)value1).doubleValue();
            double d2 = ((Number)value2).doubleValue();
            return d1 > d2 ? 1 : (d1 < d2 ? -1 : 0);
        }
        throw new InvalidTypeException("cannot compare '" + value1.getClass().getName() + "' with '" + value2.getClass().getName());
    }

    private static int valueType(Object value) {
        if (value instanceof Integer) return VALUE_INTEGER;
        if (value instanceof Double)  return VALUE_DOUBLE;
        if (value instanceof String)  return VALUE_STRING;
        if (value instanceof Boolean) return VALUE_BOOLEAN;
        if (value instanceof Date)    return VALUE_DATE;
        return VALUE_OBJECT;
    }

    public static String repeatString(String str, int times) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }


    public static String[] subarray(String[] array, int begin, int end) {
        if (begin >= end) {
            return null;
        }
        if (end > array.length) {
            end = array.length;
        }
        int size = end - begin;
        String[] array2 = new String[size];
        int i, j;
        for (i = begin, j = 0; i < end; i++, j++) {
            array2[j] = array[i];
        }
        return array2;
    }

    public static String[] subarray(String[] array, int begin) {
        if (begin < 0) {
            begin += array.length;
        }
        return subarray(array, begin, array.length);
    }


    /**
     * parse command-line options.
     *
     * ex.
     * <pre>
     *   public static void main(String[] arg) {
     *      String singles = "hv";    // options which takes no argument.
     *      String requireds = "fI";  // options which requires an argument.
     *      String optionals = "i";   // options which can take optional argument.
     *      try {
     *         Object[] ret = parseCommandOptions(args, singles, requireds, optionals);
     *         Map options        = (Map)ret[0];
     *         Map properties     = (Map)ret[1];
     *         String[] filenames = (String[])ret[2];
     *         //...
     *      } catch (CommandOptionException ex) {
     *         char option = ex.getOption();
     *         String error_symbol = ex.getErrorSymbol();
     *         Systen.err.println("*** error: " + ex.getMessage());
     *      }
     *   }
     * </pre>
     *
     * @param args      command-line strings
     * @param singles   options which takes no argument
     * @param requireds options which requires an argument.
     * @param optionals otpions which can take optional argument.
     * @return array of options(Map), properties(Map), and filenames(String[])
     */
    public static Object[] parseCommandOptions(String[] args, String singles, String requireds, String optionals) throws CommandOptionException {
        Map options = new HashMap();
        Map properties = new HashMap();
        String[] filenames = null;
        //
        int i;
        for (i = 0; i < args.length; i++) {
            if (args[i].length() == 0 || args[i].charAt(0) != '-') {
                break;
            }
            String opt = args[i];
            int len = opt.length();
            if (len == 1) {   // option '-' means "don't parse arguments!"
                i++;
                break;
            }
            assert len > 1;
            if (opt.charAt(1) == '-') {  // properties (--pname=pvalue)
                String pname;
                Object pvalue;
                int idx = opt.indexOf('=');
                if (idx >= 0) {
                    pname  = opt.substring(2, idx);
                    pvalue = idx + 1 < opt.length() ? opt.substring(idx + 1) : "";
                } else {
                    pname  = opt.substring(2);
                    pvalue = Boolean.TRUE;
                }
                properties.put(pname, pvalue);
            } else {              // command-line options
                for (int j = 1; j < len; j++) {
                    char ch = opt.charAt(j);
                    String chstr = Character.toString(ch);
                    if (singles != null && singles.indexOf(ch) >= 0) {
                        options.put(chstr, Boolean.TRUE);
                    } else if (requireds != null && requireds.indexOf(ch) >= 0) {
                        String arg = null;
                        if (++j < len) {
                            arg = opt.substring(j);
                        } else if (++i < args.length) {
                            arg = args[i];
                        } else {
                            throw new CommandOptionException("-" + ch + ": filename required.", ch, "command.option.noarg");
                        }
                        options.put(chstr, arg);
                        break;
                    } else if (optionals != null && optionals.indexOf(ch) >= 0) {
                        Object arg = null;
                        if (++j < len) {
                            arg = opt.substring(j);
                        } else {
                            arg = Boolean.TRUE;
                        }
                        options.put(chstr, arg);
                        break;
                    } else {
                        throw new CommandOptionException("-" + ch + "invalid option.", ch, "command.option.invalid");
                    }
                }
            }
        }

        // filenames
        //String[] filenames = i == args.length ? new String[0] : Util.subarray(args, i);
        assert i <= args.length;
        int n = args.length - i;
        filenames = new String[n];
        for (int j = 0; i < args.length; i++, j++) {
            filenames[j] = args[i];
        }

        //
        return new Object[] { options, properties, filenames };
    }

}
