/*
 * The original version of this file was provided under the MIT license,
 * as follows:
 *
 * @(#)PlainYamlParser.java	$Rev: 4 $ $Release: 0.5.1 $
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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * plain yaml parser class which is a parent of YamlParser class.
 *
 * ex.
 * <pre>
 *  String str = kwalify.Util.readFile("document.yaml");
 *  kwalify.Parser parser = new kwalify.PlainYamlParser(str);
 *  Object doc = parser.parse();
 * </pre>
 *
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class PlainYamlParser implements Parser {

    public static class Alias {
        private String _label;
        private int    _linenum;

        public Alias(String label, int linenum) {
            _label   = label;
            _linenum = linenum;
        }

        public String getLabel() { return _label; }
        public void setLabel(String label) { _label = label; }

        public int getLineNumber() { return _linenum; }
        public void setLineNumber(int linenum) { _linenum = linenum; }
    }


    private String[] _lines;
    private String _line = null;
    private int _linenum = 0;
    private Map _anchors = new HashMap();
    private Map _aliases = new HashMap();   // key: label, value: Integer
    private Object _end_flag = null;
    private String _sbuf = null;
    private int _index = 0;

    public PlainYamlParser(String yaml_str) {
        // split yaml_str into _lines
        List list = Util.toListOfLines(yaml_str);
        int len = list.size();
        _lines = new String[len + 1];
        for (int i = 0; i < len; i++) {
            _lines[i + 1] = (String)list.get(i);
        }
    }

    public Object parse() throws SyntaxException {
        Object data = parseChild(0);
        if (data == null && _end_flag == ENDFLAG_DOC_BEGIN) {
            data = parseChild(0);
        }
        if (_aliases.size() > 0) {
            resolveAliases(data);
        }
        //System.err.println("*** debug: data = " + Util.inspect(data));
        //System.err.println("*** debug: data = " + data.toString());
        return data;
    }

    public boolean hasNext() {
        return _end_flag != ENDFLAG_EOF;
    }

    public Object[] parseAll() throws SyntaxException {
        List docs = new ArrayList();
        while (hasNext()) {
            Object doc = parse();
            docs.add(doc);
        }
        return docs.toArray();
    }


    protected List createSequence(int linenum) {
        return new ArrayList();
    }

    //private List createSequence() {
    //    return createSequence(_linenum);
    //}

    protected void addSequenceValue(List seq, Object value, int linenum) {
        seq.add(value);
    }

    protected void setSequenceValueAt(List seq, int index, Object value, int linenum) {
        seq.set(index, value);
    }

    protected Map createMapping(int linenum) {
        return new DefaultableHashMap();
    }

    //private Map createMapping() {
    //    return createMapping(_linenum);
    //}

    protected void setMappingValueWith(Map map, Object key, Object value, int linenum) {
        map.put(key, value);
    }

    protected void setMappingDefault(Map map, Object value, int linenum) {
        if (map instanceof Defaultable) {
            ((Defaultable)map).setDefault(value);
        }
    }

    protected void mergeMapping(Map map, Map map2, int linenum) {
        for (Iterator it = map2.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            if (! map.containsKey(key)) {
                Object value = map2.get(key);
                map.put(key, value);
            }
        }
    }

    protected void mergeList(Map map, List maplist, int linenum) throws SyntaxException {
        for (Iterator it = maplist.iterator(); it.hasNext(); ) {
            Object elem = it.next();
            mergeCollection(map, elem, linenum);
        }
    }

    protected void mergeCollection(Map map, Object collection, int linenum) throws SyntaxException {
        if (collection instanceof Map) {
            mergeMapping(map, (Map)collection, linenum);
        } else if (collection instanceof List) {
            mergeList(map, (List)collection, linenum);
        } else {
            throw syntaxError("'<<' requires collection (mapping, or sequence of mapping).");
        }
    }

    protected Object createScalar(Object value, int linenum) {
        return value;
    }

    private Object createScalar(Object value) {
        return createScalar(value, _linenum);
    }

    protected String currentLine() {
        return _line;
    }

    protected int currentLineNumber() {
        return _linenum;
    }

    protected String getLine() {
        String line;
        do {
            line = _getLine_();
        } while (line != null && Util.matches(line, "^\\s*($|#)"));
        return line;
    }

    protected String _getLine_() {
        if (++_linenum < _lines.length) {
            _line = _lines[_linenum];
            if (Util.matches(_line, "^\\.\\.\\.$")) {
                _line = null;
                _end_flag = ENDFLAG_DOC_END;
            } else if (Util.matches(_line, "^---( [!%].*)?$")) {
                _line = null;
                _end_flag = ENDFLAG_DOC_BEGIN;
            }
        } else {
            _line = null;
            _end_flag = ENDFLAG_EOF;
        }
        return _line;
    }

    protected static final String ENDFLAG_EOF       = "<EOF>";
    protected static final String ENDFLAG_DOC_BEGIN = "---";
    protected static final String ENDFLAG_DOC_END   = "...";

    private void resetBuffer(String str) {
        _sbuf = str.charAt(str.length() - 1) == '\n' ? str : str + "\n";
        _index = -1;
    }


    private int _getChar_() {
        if (_index + 1 < _sbuf.length()) {
            _index++;
        } else {
            String line = getLine();
            if (line == null) return -1;
            resetBuffer(line);
            _index++;
        }
        int ch = _sbuf.charAt(_index);
        return ch;
    }

    private int getChar() {
        int ch;
        do {
            ch = _getChar_();
        } while (ch >= 0 && isWhite(ch));
        return ch;
    }

    private int getCharOrNewline() {
        int ch;
        do {
            ch = _getChar_();
        } while (ch >= 0 && isWhite(ch) && ch != '\n');
        return ch;
    }

    private int currentChar() {
        return _sbuf.charAt(_index);
    }

    private SyntaxException syntaxError(String message, int linenum) {
        return new YamlSyntaxException(message, linenum);
    }

    private SyntaxException syntaxError(String message) {
        return new SyntaxException(message, _linenum);
    }

    private Object parseChild(int column) throws SyntaxException {
        String line = getLine();
        if (line == null) {
            return createScalar(null);
        }
        Matcher m = Util.matcher(line, "^( *)(.*)");
        if (! m.find()) {
            assert false;
            return null;
        }
        int indent = m.group(1).length();
        if (indent < column) {
            return createScalar(null);
        }
        String value = m.group(2);
        return parseValue(column, value, indent);
    }

    private Object parseValue(int column, String value, int value_start_column) throws SyntaxException {
        Object data;
        if        (Util.matches(value, "^-( |$)")) {
            data = parseSequence(value_start_column, value);
        } else if (Util.matches(value, "^((?::?[-.\\w]+|'.*?'|\".*?\"|=|<<) *):(( +)(.*))?$")) {
            data = parseMapping(value_start_column, value);
        } else if (Util.matches(value, "^[\\[\\{]")) {
            data = parseFlowStyle(column, value);
        } else if (Util.matches(value, "^\\&[-\\w]+( |$)")) {
            data = parseAnchor(column, value);
        } else if (Util.matches(value, "^\\*[-\\w]+( |$)")) {
            data = parseAlias(column, value);
        } else if (Util.matches(value, "^[|>]")) {
            data = parseBlockText(column, value);
        } else if (Util.matches(value, "^!")) {
            data = parseTag(column, value);
        } else if (Util.matches(value, "^\\#")) {
            data = parseChild(column);
        } else {
            data = parseScalar(column, value);
        }
        return data;
    }

    private static boolean isWhite(int ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
    }


    private Object parseFlowStyle(int column, String value) throws SyntaxException {
        resetBuffer(value);
        getChar();
        Object data = parseFlow(0);
        int ch = currentChar();
        assert ch == ']' || ch == '}';
        ch = getCharOrNewline();
        if (ch != '\n' && ch != '#' && ch >= 0) {
            throw syntaxError("flow style sequence is closed buf got '" + ((char)ch) + "'.");
        }
        if (ch >= 0) getLine();
        return data;
    }

    private Object parseFlow(int depth) throws SyntaxException {
        int ch = currentChar();
        //ch = getChar();
        if (ch < 0) {
            throw syntaxError("found EOF when parsing flow style.");
        }
        Object data;
        if (ch == '[') {
            data = parseFlowSequence(depth);
        } else if (ch == '{') {
            data = parseFlowMapping(depth);
        } else {
            data = parseFlowScalar(depth);
        }
        return data;
    }

    private List parseFlowSequence(int depth) throws SyntaxException {
        assert currentChar() == '[';
        List seq = createSequence(_linenum);
        int ch = getChar();
        if (ch != '}') {
            int linenum = currentLineNumber();
            //seq.add(parseFlowSequenceItem(depth+1);
            addSequenceValue(seq, parseFlowSequenceItem(depth + 1), linenum);
            while ((ch = currentChar()) == ',') {
                ch = getChar();
                if (ch == '}') {
                    throw syntaxError("sequence item required (or last comma is extra).");
                }
                //if (ch == '?') break;
                linenum = currentLineNumber();
                //seq.add(parseFlowSequenceItem(depth+1);
                addSequenceValue(seq, parseFlowSequenceItem(depth + 1), linenum);
            }
        }
        if (currentChar() != ']') {
            throw syntaxError("flow style sequence requires ']'.");
        }
        if (depth > 0) getChar();
        return seq;
    }

    private Object parseFlowSequenceItem(int depth) throws SyntaxException {
        return parseFlow(depth);
    }

    private Map parseFlowMapping(int depth) throws SyntaxException {
        assert currentChar() == '{';
        Map map = createMapping(_linenum);
        int ch = getChar();
        if (ch != '}') {
            int linenum = currentLineNumber();
            Object[] pair = parseFlowMappingItem(depth + 1);
            Object key   = pair[0];
            Object value = pair[1];
            //map[ke] = value
            setMappingValueWith(map, key, value, linenum);
            while ((ch = currentChar()) == ',') {
                ch = getChar();
                if (ch == '}') {
                    throw syntaxError("mapping item required (or last comman is extra.");
                }
                //if (ch == '}') break;
                linenum = currentLineNumber();
                pair = parseFlowMappingItem(depth + 1);
                key   = pair[0];
                value = pair[1];
                //map.put(key) = value;
                setMappingValueWith(map, key, value, linenum);
            }
        }
        if (currentChar() != '}') {
            throw syntaxError("flow style mapping requires '}'.");
        }
        if (depth > 0) getChar();
        return map;
    }

    private Object[] parseFlowMappingItem(int depth) throws SyntaxException {
        Object key = parseFlow(depth);
        int ch = currentChar();
        if (ch != ':') {
            String s = ch >= 0 ? "'" + ((char)ch) + "'" : "EOF";
            throw syntaxError("':' expected but got " + s);
        }
        getChar();
        Object value = parseFlow(depth);
        return new Object[] { key, value };
    }

    private Object parseFlowScalar(int depth) throws SyntaxException {
        int ch = currentChar();
        Object scalar = null;
        StringBuffer sb = new StringBuffer();
        if (ch == '"' || ch == '\'') {
            int endch = ch;
            while ((ch = _getChar_()) >= 0 && ch != endch) {
                sb.append((char)ch);
            }
            getChar();
            scalar = sb.toString();
        } else {
            sb.append((char)ch);
            while ((ch = _getChar_()) >= 0 && ch != ':' && ch != ',' && ch != ']' && ch != '}') {
                sb.append((char)ch);
            }
            scalar = toScalar(sb.toString().trim());
        }
        return createScalar(scalar);
    }

    private Object parseTag(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^!\\S+");
        Matcher m = Util.matcher(value, "^!(\\S+)((\\s+)(.*))?$");
        if (! m.find()) {
            assert false;
            return null;
        }
        String tag = m.group(1);
        String space = m.group(3);
        String value2 = m.group(4);
        Object data;
        if (value2 != null && value2.length() > 0) {
            int value_start_column = column + 1 + tag.length() + space.length();
            data = parseValue(column, value2, value_start_column);
        } else {
            data = parseChild(column);
        }
        return data;
    }

    private Object parseAnchor(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^\\&([-\\w]+)(( *)(.*))?$");
        Matcher m = Util.matcher(value, "^\\&([-\\w]+)(( *)(.*))?$");
        if (! m.find()) {
            assert false;
            return null;
        }
        String label  = m.group(1);
        String space  = m.group(3);
        String value2 = m.group(4);
        Object data;
        if (value2 != null && value2.length() > 0) {
            int value_start_column = column + 1 + label.length() + space.length();
            data = parseValue(column, value2, value_start_column);
        } else {
            data = parseChild(column);
        }
        registerAnchor(label, data);
        return data;
    }

    private void registerAnchor(String label, Object data) throws SyntaxException {
        if (_anchors.containsKey(label)) {
            throw syntaxError("anchor '" + label + "' is already used.");
        }
        _anchors.put(label, data);
    }

    private Object parseAlias(int column, String value) throws SyntaxException {
        assert value.matches("^\\*([-\\w]+)(( *)(.*))?$");
        Matcher m = Util.matcher(value, "^\\*([-\\w]+)(( *)(.*))?$");
        if (! m.find()) {
            assert false;
            return null;
        }
        String label  = m.group(1);
        //String space  = m.group(3);
        String value2 = m.group(4);
        if (value2 != null && value2.length() > 0 && value2.charAt(0) != '#') {
            throw syntaxError("alias cannot take any data.");
        }
        Object data = _anchors.get(label);
        if (data == null) {
            //throw syntaxError("anchor '" + label "' not found (cannot refer to backward or child anchor).");
            data = registerAlias(label);
        }
        getLine();
        return data;
    }

    private Alias registerAlias(String label) throws SyntaxException {
        Integer count = (Integer)_aliases.get(label);
        if (count == null) {
            _aliases.put(label, new Integer(1));
        } else {
            _aliases.put(label, new Integer(count.intValue() + 1));
        }
        return new Alias(label, _linenum);
    }


    private void resolveAliases(Object data) throws SyntaxException {  // List or Map
        Map resolved = new IdentityHashMap();
        resolveAliases(data, resolved);
    }


    private void resolveAliases(Object data, Map resolved) throws SyntaxException {
        if (resolved.containsKey(data)) {
            return;
        }
        resolved.put(data, data);
        if (data instanceof List) {
            resolveAliases((List)data, resolved);
        } else if (data instanceof Map) {
            resolveAliases((Map)data, resolved);
        } else {
            assert !(data instanceof Alias);
        }
        if (data instanceof Defaultable) {
            Object default_value = ((Defaultable)data).getDefault();
            if (default_value != null) {
                resolveAliases(default_value, resolved);
            }
        }
    }

    private void resolveAliases(List seq, Map resolved) throws SyntaxException {
        int len = seq.size();
        for (int i = 0; i < len; i++) {  // don't use itrator not to raise java.util.ConcurrentModificationException
            Object val = seq.get(i);
            if (val instanceof Alias) {
                Alias alias = (Alias)val;
                String label = alias.getLabel();
                if (_anchors.containsKey(label)) {
                    //seq.set(i, _anchors.get(label);
                    setSequenceValueAt(seq, i, _anchors.get(label), alias.getLineNumber());
                } else {
                    throw syntaxError("anchor '" + alias.getLabel() + "' not found.");
                }
            } else if (val instanceof List || val instanceof Map) {
                resolveAliases(val, resolved);
            }
        }
    }

    private void resolveAliases(Map map, Map resolved) throws SyntaxException {
        for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            Object val = map.get(key);
            if (val instanceof Alias) {
                Alias alias = (Alias)val;
                String label = alias.getLabel();
                if (_anchors.containsKey(label)) {
                    //map.put(key, _anchors.get(label));
                    setMappingValueWith(map, key, _anchors.get(label), alias.getLineNumber());
                } else {
                    throw syntaxError("anchor '" + alias.getLabel() + "' not found.", alias.getLineNumber());
                }
            } else if (val instanceof List || val instanceof Map) {
                resolveAliases(val, resolved);
            }
        }
    }



/*
    private Object parseBlockText(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^[>|\\|]");
        Matcher m = Util.matcher(value, "^([>|\\|])([-+]?)\\s*(.*)$");
        if (! m.find()) {
            assert false;
            return null;
        }
        String blockchar = m.group(1);
        String indicator = m.group(2);
        String sep = blockchar.equals("|") ? "\n" : " ";
        //String text = m.group(3).length() > 0 ? "" : m.group(3) + sep;
        String text = m.group(3);
        StringBuffer sb = new StringBuffer();
        StringBuffer empty = new StringBuffer();
        int min_indent = -1;
        String line;
        Pattern pat2 = Pattern.compile("^( *)(.*)");
        while ((line = _getLine_()) != null) {
            (m = pat2.matcher(line)).find();
            int indent = m.group(1).length();
            if (m.group(2).length() == 0) {
                empty.append("\n");
            } else if (indent < column) {
                break;
            } else {
                if (min_indent < 0 || min_indent > indent) {
                    min_indent = indent;
                }
                sb.append(empty.toString());
                sb.append(line);
                empty.delete(0, empty.length());
            }
        }
        if (indicator.equals("+")) {
            sb.append(empty);
        } else if (indicator.equals("-")) {
            sb.deleteCharAt(sb.length() - 1);
        }
        String s;
        if (min_indent <= 0) {
            s = sb.toString();
        } else {
            StringBuffer regex = new StringBuffer("(?m)^");
            for (int i = 0; i < min_indent; i++) regex.append(" ");
            s = sb.toString().replaceAll(regex.toString(), "");
        }
        if (blockchar.equals(">")) {
            StringBuffer sb2 = new StringBuffer();
            int len = s.length();
            int n = 0;
            for (int i = 0; i < len; i++) {
                char ch = s.charAt(i);
                if (ch == '\n') {
                    n++;
                } else {
                    if (n == 1) {
                        sb2.append(' ');   n = 0;
                    } else if (n > 1) {
                        sb2.append('\n');  n = 0;
                    }
                    sb2.append(ch);
                }
            }
            s = sb2.toString();
        }
        if (currentLine() != null && Util.matches(currentLine(), "^\\s*#")) getLine();
        return createScalar(text + s);
    }
*/

    private Object parseBlockText(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^[>|]");
        Matcher m = Util.matcher(value, "^([>|])([-+]?)(\\d*)\\s*(.*)$");
        if (! m.find()) {
            assert false;
            return null;
        }
        char blockchar = m.group(1).length() > 0 ? m.group(1).charAt(0) : '\0';
        char indicator = m.group(2).length() > 0 ? m.group(2).charAt(0) : '\0';
        int indent     = m.group(3).length() > 0 ? Integer.parseInt(m.group(3)) : -1;
        String text    = m.group(4);
        char sep = blockchar == '|' ? '\n' : ' ';
        String line;
        StringBuffer sb = new StringBuffer();
        int n = 0;
        while ((line = _getLine_()) != null) {
            m = Util.matcher(line, "^( *)(.*)$");
            m.find();
            String space = m.group(1);
            String str   = m.group(2);
            if (indent < 0) indent = space.length();
            if (str.length() == 0) {   // empty line
                n++;
            } else {
                int slen = space.length();
                if (slen < column) {
                    break;
                } else if (slen < indent) {
                    throw syntaxError("invalid indent in block text.");
                } else {
                    if (n > 0) {
                        if (blockchar == '>' && sb.length() > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        for (int i = 0; i < n; i++) {
                            sb.append('\n');
                        }
                        n = 0;
                    }
                    str = line.substring(indent);
                }
            }
            sb.append(str);
            if (blockchar == '>') {
                if (sb.charAt(sb.length() - 1) == '\n') {
                    sb.setCharAt(sb.length() - 1, ' ');
                }
            }
        }
        if (line != null && Util.matches(line, "^ *#")) {
            getLine();
        }
        switch (indicator) {
        case '+':
            if (n > 0) {
                if (blockchar == '>') {
                    sb.setCharAt(sb.length() - 1, '\n');
                }
                for (int i = 0; i < n; i++) {
                    sb.append('\n');
                }
            }
            break;
        case '-':
            if (sb.charAt(sb.length() - 1) == sep) {
                sb.deleteCharAt(sb.length() - 1);
            }
            break;
        default:
            if (blockchar == '>') {
                sb.setCharAt(sb.length() - 1, '\n');
            }
        }
        return createScalar(text + sb.toString());
    }


    private List parseSequence(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^-(( +)(.*))?$");
        List seq = createSequence(_linenum);
        while (true) {
            Matcher m = Util.matcher(value, "^-(( +)(.*))?$");
            if (! m.find()) {
                throw syntaxError("sequence item is expected.");
            }
            String space  = m.group(2);
            String value2 = m.group(3);
            int column2   = column + 1;
            int linenum   = currentLineNumber();
            //
            Object elem;
            if (value2 == null || value2.length() == 0) {
                elem = parseChild(column2);
            } else {
                int value_start_column = column2 + space.length();
                elem = parseValue(column2, value2, value_start_column);
            }
            addSequenceValue(seq, elem, linenum);
            //
            String line = currentLine();
            if (line == null) break;
            Matcher m2 = Util.matcher(line, "^( *)(.*)");
            m2.find();
            int indent = m2.group(1).length();
            if (indent < column) {
                break;
            } else if (indent > column) {
                throw syntaxError("invalid indent of sequence.");
            }
            value = m2.group(2);
        }
        return seq;
    }


    private Map parseMapping(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^((?::?[-.\\w]+|'.*?'|\".*?\"|=|<<) *):(( +)(.*))?$");
        Map map = createMapping(_linenum);
        while (true) {
            Matcher m = Util.matcher(value, "^((?::?[-.\\w]+|'.*?'|\".*?\"|=|<<) *):(( +)(.*))?$");
            if (! m.find()) {
                throw syntaxError("mapping item is expected.");
            }
            String v = m.group(1).trim();
            Object key = toScalar(v);
            String value2 = m.group(4);
            int column2 = column + 1;
            int linenum = currentLineNumber();
            //
            Object elem;
            if (value2 == null || value2.length() == 0) {
                elem = parseChild(column2);
            } else {
                int value_start_column = column2 + m.group(1).length() + m.group(3).length();
                elem = parseValue(column2, value2, value_start_column);
            }
            if (v.equals("=")) {
                setMappingDefault(map, elem, linenum);
            } else if (v.equals("<<")) {
                mergeCollection(map, elem, linenum);
            } else {
                setMappingValueWith(map, key, elem, linenum);
            }
            //
            String line = currentLine();
            if (line == null) {
                break;
            }
            Matcher m2 = Util.matcher(line, "^( *)(.*)");
            m2.find();
            int indent = m2.group(1).length();
            if (indent < column) {
                break;
            } else if (indent > column) {
                throw syntaxError("invalid indent of mapping.");
            }
            value = m2.group(2);
        }
        return map;
    }


    private Object parseScalar(int indent, String value) throws SyntaxException {
        Object data = createScalar(toScalar(value));
        getLine();
        return data;
    }


    private Object toScalar(String value) {
        Matcher m;
        m = Util.matcher(value, "^\"(.*)\"([ \t]*#.*$)?");
        if(m.find()) {
            return m.group(1);
        }

        m = Util.matcher(value, "^'(.*)'([ \t]*#.*$)?");
        if (m.find()) {
            return m.group(1);
        }

        m = Util.matcher(value, "^(.*\\S)[ \t]*#");
        if (m.find()) {
            value = m.group(1);
        }

        if (Util.matches(value, "^-?0x\\d+$"))
            return Integer.valueOf(value, 16);
        else if (Util.matches(value, "^-?0\\d+$"))
            return Integer.valueOf(value, 8);
        else if (Util.matches(value, "^-?\\d+$"))
            return Integer.valueOf(value, 10);
        else if (Util.matches(value, "^-?\\d+\\.\\d+$"))
            return Double.valueOf(value);
        else if (Util.matches(value, "^(true|yes|on)$"))
            return Boolean.TRUE;
        else if (Util.matches(value, "^(false|no|off)$"))
            return Boolean.FALSE;
        else if (Util.matches(value, "^(null|~)$"))
            return null;
        else if (Util.matches(value, "^:(\\w+)$"))
            return value;

        m = Util.matcher(value, "^(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)$");
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day = Integer.parseInt(m.group(3));
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, 0, 0, 0);
            return cal.getTime();
        }

        m = Util.matcher(value, "^(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)(?:[Tt]|[ \t]+)(\\d\\d?):(\\d\\d):(\\d\\d)(\\.\\d*)?(?:Z|[ \t]*([-+]\\d\\d?)(?::(\\d\\d))?)?$");
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day = Integer.parseInt(m.group(3));
            int hour = Integer.parseInt(m.group(4));
            int min = Integer.parseInt(m.group(5));
            int sec = Integer.parseInt(m.group(6));
            // int usec = Integer.parseInt(m.group(7));
            // int tzone_h = Integer.parseInt(m.group(8));
            // int tzone_m = Integer.parseInt(m.group(9));
            String timezone = "GMT" + m.group(8) + ":" + m.group(9);
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, hour, min, sec);
            cal.setTimeZone(TimeZone.getTimeZone(timezone));
            return cal.getTime();
        }
        return value;
    }

/*
    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "test.yaml";
        String s = Util.readFile(filename);
        PlainYamlParser parser = new PlainYamlParser(s);
        while (parser.hasNext()) {
            Object doc = parser.parse();
            System.out.println(Util.inspect(doc));
        }
    }
*/

}
