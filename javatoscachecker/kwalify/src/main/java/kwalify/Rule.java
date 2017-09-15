/*
 * @(#)Rule.java	$Rev: 4 $ $Release: 0.5.1 $
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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Added in order to satisfy onap.org code check constraints:
 *
 * This license is deemed more permisive than if it were under the Apache License, Version 2.0,
 * found at
 *        http://www.apache.org/licenses/LICENSE-2.0
 */

package kwalify;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

/**
 *  rule for validation.
 *  Validator class generates rule instances.
 *
 *  @revision    $Rev: 4 $
 *  @release     $Release: 0.5.1 $
 */
public class Rule {

    /*
     *  instance variables
     */

    private Rule    _parent;
    private String  _name       = null;
    private String  _desc       = null;
    private String  _short      = null; //added by jora: only used for map types
    private String  _ref       	= null; //added by jora: only used for ref types (references to other rules by name)
    private boolean _required   = false;
    private String  _type       = null;
    private Class   _type_class = null;
    private String  _pattern    = null;
    private Pattern _pattern_regexp = null;
    private List    _enum       = null;
    private List    _sequence   = null;
    private DefaultableHashMap _mapping = null;
    private String  _assert     = null;
    private Map     _range      = null;
    private Map     _length     = null;
    private boolean _ident      = false;
    private boolean _unique     = false;


    /*
     *  accessors
     */

    public String getName() { return _name; }
    public void setName(String name) { _name = name; }

    public String getDesc() { return _desc; }
    public void setDesc(String desc) { _desc = desc; }

    public String getShort() { return _short; }
    public void setShort(String val) { _short = val; }

    public String getReference() { return _ref; }
    public void setReference(String ref) { _ref = ref; }

    public boolean isRequired() { return _required; }
    public void setRequired(boolean required) { _required = required; }

    public String getType() { return _type; }
    public void setType(String type) { _type = type; }

    public Class getTypeClass() { return _type_class; }
    public void setTypeClass(Class type_class) { _type_class = type_class; }

    public String getPattern() { return _pattern; }
    public void setPattern(String pattern) { _pattern = pattern; }

    public Pattern getPatternRegexp() { return _pattern_regexp; }
    public void setPatternRegexp(Pattern patternRegexp) { _pattern_regexp = patternRegexp; }

    public List getEnum() { return _enum; }
    public void setEnum(List enumList) { _enum = enumList; }

    public List getSequence() { return _sequence; }
    public void setSequence(List sequence) { _sequence = sequence; }

    public DefaultableHashMap getMapping() { return _mapping; }
    public void setMapping(DefaultableHashMap mapping) { _mapping = mapping; }

    public String getAssert() { return _assert; }
    public void setAssert(String assertString) { _assert = assertString; }

    public Map getRange() { return _range; }
    public void setRange(Map range) { _range = range; }

    public Map getLength() { return _length; }
    public void setLength(Map length) { _length = length; }

    public boolean isIdent() { return _ident; }
    public void setIdent(boolean ident) { _ident = ident; }

    public boolean isUnique() { return _unique; }
    public void setUnique(boolean unique) { _unique = unique; }


    /*
     *  constructors
     */

    public Rule(Object schema, Rule parent) throws SchemaException {
        if (schema != null) {
            if (! (schema instanceof Map)) {
                throw schemaError("schema.notmap", null, "/", null, null);
            }
            Map rule_table = new IdentityHashMap();
            init((Map)schema, "", rule_table);
        }
        _parent = parent;
    }

    public Rule(Object schema) throws SchemaException {
        this(schema, null);
    }

    public Rule(Map schema, Rule parent) throws SchemaException {
        if (schema != null) {
            Map rule_table = new IdentityHashMap();
            init(schema, "", rule_table);
        }
        _parent = parent;
    }

    public Rule(Map schema) throws SchemaException {
        this(schema, null);
    }

    public Rule() throws SchemaException {
        this(null, null);
    }

    /*
     * constants
     */

    private static final int CODE_NAME     = "name".hashCode();
    private static final int CODE_DESC     = "desc".hashCode();
    private static final int CODE_SHORT    = "short".hashCode();	//jora
		private static final int CODE_RULE   	 = "rule".hashCode();		//jora
    private static final int CODE_REQUIRED = "required".hashCode();
    private static final int CODE_TYPE     = "type".hashCode();
    private static final int CODE_PATTERN  = "pattern".hashCode();
    private static final int CODE_ENUM     = "enum".hashCode();
    private static final int CODE_SEQUENCE = "sequence".hashCode();
    private static final int CODE_MAPPING  = "mapping".hashCode();
    private static final int CODE_ASSERT   = "assert".hashCode();
    private static final int CODE_RANGE    = "range".hashCode();
    private static final int CODE_LENGTH   = "length".hashCode();
    private static final int CODE_IDENT    = "ident".hashCode();
    private static final int CODE_UNIQUE   = "unique".hashCode();



    /*
     *  instance methods
     */
		public Rule getParent() {  // by jora
			return this._parent;
		}


    private static SchemaException schemaError(String errorSymbol, Rule rule, String path, Object value, Object[] args) {
        String msg = Messages.buildMessage(errorSymbol, value, args);
        return new SchemaException(msg, path, value, rule, errorSymbol);
    }


    private void init(Object elem, String path, Map rule_table) throws SchemaException {
        assert elem != null;
        if (! (elem instanceof Map)) {
            if (path == null || path.equals("")) {
                path = "/";
            }
            throw schemaError("schema.notmap", null, path, null, null);
        }
        init((Map)elem, path, rule_table);

    }


    private void init(Map hash, String path, Map rule_table) throws SchemaException {
        Rule rule = this;
        rule_table.put(hash, rule);

        // 'type:' entry
        Object type = hash.get("type");
        initTypeValue(type, rule, path);

        // other entries
        for (Iterator it = hash.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            Object value = hash.get(key);
            int code = key.hashCode();

            if (code == CODE_TYPE && key.equals("type")) {
                // done
            } else if (code == CODE_NAME && key.equals("name")) {
                initNameValue(value, rule, path);
            } else if (code == CODE_DESC && key.equals("desc")) {
                initDescValue(value, rule, path);
            } else if (code == CODE_SHORT && key.equals("short")) {
                initShortValue(value, rule, path);
            } else if (code == CODE_REQUIRED && key.equals("required")) {
                initRequiredValue(value, rule, path);
            } else if (code == CODE_PATTERN && key.equals("pattern")) {
                initPatternValue(value, rule, path);
            } else if (code == CODE_ENUM && key.equals("enum")) {
                initEnumValue(value, rule, path);
            } else if (code == CODE_ASSERT && key.equals("assert")) {
                initAssertValue(value, rule, path);
            } else if (code == CODE_RANGE && key.equals("range")) {
                initRangeValue(value, rule, path);
            } else if (code == CODE_LENGTH && key.equals("length")) {
                initLengthValue(value, rule, path);
            } else if (code == CODE_IDENT && key.equals("ident")) {
                initIdentValue(value, rule, path);
            } else if (code == CODE_UNIQUE && key.equals("unique")) {
                initUniqueValue(value, rule, path);
            } else if (code == CODE_SEQUENCE && key.equals("sequence")) {
                rule = initSequenceValue(value, rule, path, rule_table);
            } else if (code == CODE_MAPPING && key.equals("mapping")) {
                rule = initMappingValue(value, rule, path, rule_table);
            } else if (code == CODE_RULE && key.equals("rule")) {
                rule = initReferenceValue(value, rule, path, rule_table);
            } else {
							 // removed by 'jora': interferes with the usage of YAML anchor/alias in grammar files
               // throw schemaError("key.unknown", rule, path + "/" + key, key.toString() + ":", null);
            }
        }

        // confliction check
        checkConfliction(hash, rule, path);
    }


    private void initTypeValue(Object value, Rule rule, String path) throws SchemaException {
        if (value == null) {
            value = Types.getDefaultType();
        }
        if (! (value instanceof String)) {
            throw schemaError("type.notstr", rule, path + "/type", _type, null);
        }
        _type = (String)value;
        _type_class = Types.typeClass(_type);
        if (! Types.isBuiltinType(_type)) {
            throw schemaError("type.unknown", rule, path + "/type", _type, null);
        }
    }


    private void initNameValue(Object value, Rule rule, String path) throws SchemaException {
        _name = value.toString();
    }


    private void initDescValue(Object value, Rule rule, String path) throws SchemaException {
        _desc = value.toString();
    }

    private void initShortValue(Object value, Rule rule, String path) throws SchemaException {

        //the short form specification is to be interpreted as key if the type is a map or as an
        //index if the target is a sequence (as index 0 actually)  
        if (!Types.isCollectionType(_type)) {
            throw schemaError("range.notcollection", rule, path + "/short", value, null);
        }
        //we should also verify that it points to a declared key of the mapping .. not really, as it would
				//fail the overall grammar
        _short = value.toString();
    }

    private void initRequiredValue(Object value, Rule rule, String path) throws SchemaException {
        if (! (value instanceof Boolean)) {
            throw schemaError("required.notbool", rule, path + "/required", value, null);
        }
        _required = ((Boolean)value).booleanValue();
    }


    private void initPatternValue(Object value, Rule rule, String path) throws SchemaException {
        if (! (value instanceof String)) {
            throw schemaError("pattern.notstr", rule, path + "/pattern", value, null);
        }
        _pattern = (String)value;
        Matcher m = Util.matcher(_pattern, "\\A/(.*)/([mi]?[mi]?)\\z");
        if (! m.find()) {
            throw schemaError("pattern.notmatch", rule, path + "/pattern", value, null);
        }
        String pat = m.group(1);
        String opt = m.group(2);
        int flag = 0;
        if (opt.indexOf('i') >= 0) {
            flag += Pattern.CASE_INSENSITIVE;
        }
        if (opt.indexOf('m') >= 0) {
            flag += Pattern.DOTALL;   // not MULTILINE
        }
        try {
            _pattern_regexp = Pattern.compile(pat, flag);
        } catch (PatternSyntaxException ex) {
            throw schemaError("pattern.syntaxerr", rule, path + "/pattern", value, null);
        }
    }


    private void initEnumValue(Object value, Rule rule, String path) throws SchemaException {
        if (! (value instanceof List)) {
            throw schemaError("enum.notseq", rule, path + "/enum", value, null);
        }
        _enum = (List)value;
        if (Types.isCollectionType(_type)) {
            throw schemaError("enum.notscalar", rule, path, "enum:", null);
        }
        Map elem_table = new HashMap();
        for (Iterator it = _enum.iterator(); it.hasNext(); ) {
            Object elem = it.next();
            if (! Util.isInstanceOf(elem, _type_class)) {
                throw schemaError("enum.type.unmatch", rule, path + "/enum", elem, new Object[] { Types.typeName(_type) });
            }
            if (elem_table.containsKey(elem)) {
                throw schemaError("enum.duplicate", rule, path + "/enum", elem, null);
            }
            elem_table.put(elem, Boolean.TRUE);
        }
    }


    private void initAssertValue(Object value, Rule rule, String path) throws SchemaException {
        if (! (value instanceof String)) {
            throw schemaError("assert.notstr", rule, path + "/assert", value, null);
        }
        _assert = (String)value;
        if (! Util.matches(_assert, "\\bval\\b")) {
            throw schemaError("assert.noval", rule, path + "/assert", value, null);
        }
    }


    private void initRangeValue(Object value, Rule rule, String path) throws SchemaException {
        if (! (value instanceof Map)) {
            throw schemaError("range.notmap", rule, path + "/range", value, null);
        }
        if (Types.isCollectionType(_type) || _type.equals("bool")) {
            throw schemaError("range.notscalar", rule, path, "range:", null);
        }
        _range = (Map)value;
        for (Iterator it = _range.keySet().iterator(); it.hasNext(); ) {
            Object rkey = it.next();
            Object rval = _range.get(rkey);
            if (rkey.equals("max") || rkey.equals("min") || rkey.equals("max-ex") || rkey.equals("min-ex")) {
                if (! Util.isInstanceOf(rval, _type_class)) {
                    String typename = Types.typeName(_type);
                    throw schemaError("range.type.unmatch", rule, path + "/range/" + rkey, rval, new Object[] { typename });
                }
            } else {
                throw schemaError("range.undefined", rule, path + "/range/" + rkey, rkey.toString() + ":", null);
            }
        }
        if (_range.containsKey("max") && _range.containsKey("max-ex")) {
            throw schemaError("range.twomax", rule, path + "/range", null, null);
        }
        if (_range.containsKey("min") && _range.containsKey("min-ex")) {
            throw schemaError("range.twomin", rule, path + "/range", null, null);
        }
        //
        Object max    = _range.get("max");
        Object min    = _range.get("min");
        Object max_ex = _range.get("max-ex");
        Object min_ex = _range.get("min-ex");
        Object[] args = null;
        //String error_symbol = null;
        if (max != null) {
            if (min != null && Util.compareValues(max, min) < 0) {
                args = new Object[] { max, min };
                throw schemaError("range.maxltmin", rule, path + "/range", null, args);
            } else if (min_ex != null && Util.compareValues(max, min_ex) <= 0) {
                args = new Object[] { max, min_ex };
                throw schemaError("range.maxleminex", rule, path + "/range", null, args);
            }
        } else if (max_ex != null) {
            if (min != null && Util.compareValues(max_ex, min) <= 0) {
                args = new Object[] { max_ex, min };
                throw schemaError("range.maxexlemin", rule, path + "/range", null, args);
            } else if (min_ex != null && Util.compareValues(max_ex, min_ex) <= 0) {
                args = new Object[] { max_ex, min_ex };
                throw schemaError("range.maxexleminex", rule, path + "/range", null, args);
            }
        }
    }


    private void initLengthValue(Object value, Rule rule, String path) throws SchemaException {
        if (! (value instanceof Map)) {
            throw schemaError("length.notmap", rule, path + "/length", value, null);
        }
        _length = (Map)value;
        if (! (_type.equals("str") || _type.equals("text"))) {
            throw schemaError("length.nottext", rule, path, "length:", null);
        }
        for (Iterator it = _length.keySet().iterator(); it.hasNext(); ) {
            Object k = it.next();
            Object v = _length.get(k);
            if (k.equals("max") || k.equals("min") || k.equals("max-ex") || k.equals("min-ex")) {
                if (! (v instanceof Integer)) {
                    throw schemaError("length.notint", rule, path + "/length/" + k, v, null);
                }
            } else {
                throw schemaError("length.undefined", rule, path + "/length/" + k, k + ":", null);
            }
        }
        if (_length.containsKey("max") && _length.containsKey("max-ex")) {
            throw schemaError("length.twomax", rule, path + "/length", null, null);
        }
        if (_length.containsKey("min") && _length.containsKey("min-ex")) {
            throw schemaError("length.twomin", rule, path + "/length", null, null);
        }
        //
        Integer max    = (Integer)_length.get("max");
        Integer min    = (Integer)_length.get("min");
        Integer max_ex = (Integer)_length.get("max-ex");
        Integer min_ex = (Integer)_length.get("min-ex");
        Object[] args = null;
        //String error_symbol = null;
        if (max != null) {
            if (min != null && max.compareTo(min) < 0) {
                args = new Object[] { max, min };
                throw schemaError("length.maxltmin", rule, path + "/length", null, args);
            } else if (min_ex != null && max.compareTo(min_ex) <= 0) {
                args = new Object[] { max, min_ex };
                throw schemaError("length.maxleminex", rule, path + "/length", null, args);
            }
        } else if (max_ex != null) {
            if (min != null && max_ex.compareTo(min) <= 0) {
                args = new Object[] { max_ex, min };
                throw schemaError("length.maxexlemin", rule, path + "/length", null, args);
            } else if (min_ex != null && max_ex.compareTo(min_ex) <= 0) {
                args = new Object[] { max_ex, min_ex };
                throw schemaError("length.maxexleminex", rule, path + "/length", null, args);
            }
        }
    }


    private void initIdentValue(Object value, Rule rule, String path) throws SchemaException {
        if (value == null || ! (value instanceof Boolean)) {
            throw schemaError("ident.notbool", rule, path + "/ident", value, null);
        }
        _ident = ((Boolean)value).booleanValue();
        _required = true;
        if (Types.isCollectionType(_type)) {
            throw schemaError("ident.notscalar", rule, path, "ident:", null);
        }
        if (path.equals("")) {
            throw schemaError("ident.onroot", rule, "/", "ident:", null);
        }
        if (_parent == null || ! _parent.getType().equals("map")) {
            throw schemaError("ident.notmap", rule, path, "ident:", null);
        }
    }


    private void initUniqueValue(Object value, Rule rule, String path) throws SchemaException {
        if (! (value instanceof Boolean)) {
            throw schemaError("unique.notbool", rule, path + "/unique", value, null);
        }
        _unique = ((Boolean)value).booleanValue();
        if (Types.isCollectionType(_type)) {
            throw schemaError("unique.notscalar", rule, path, "unique:", null);
        }
        if (path.equals("")) {
            throw schemaError("unique.onroot", rule, "/", "unique:", null);
        }
        //if (_parent == null || _parent.getType() == "map") {
        //    throw schemaError("sequence.notseq", rule, path + "/unique", value);
        //}
    }


    private Rule initSequenceValue(Object value, Rule rule, String path, Map rule_table) throws SchemaException {
        if (value != null && ! (value instanceof List)) {
            throw schemaError("sequence.notseq", rule, path + "/sequence", value.toString(), null);
        }
        _sequence = (List)value;
        if (_sequence  == null || _sequence.size() == 0) {
            throw schemaError("sequence.noelem", rule, path + "/sequence", value, null);
        }
        if (_sequence.size() > 1) {
            throw schemaError("sequence.toomany", rule, path + "/sequence", value, null);
        }
        Object elem = _sequence.get(0);
        if (elem == null) {
            elem = new HashMap();
        }
        int i = 0;
        // Rule rule;
        rule = (Rule)rule_table.get(elem);
        if (rule == null) {
            rule = new Rule(null, this);
            rule.init(elem, path + "/sequence/" + i, rule_table);
        }
        _sequence = new ArrayList();
        _sequence.add(rule);
        return rule;
    }


    private Rule initMappingValue(Object value, Rule rule, String path, Map rule_table) throws SchemaException {

        // error check
        if (value != null && !(value instanceof Map)) {
            throw schemaError("mapping.notmap", rule, path + "/mapping", value.toString(), null);
        }
        Object default_value = null;
        if (value instanceof Defaultable) {
            default_value = ((Defaultable)value).getDefault();
        }
        if (value == null || ((Map)value).size() == 0 && default_value == null) {
            throw schemaError("mapping.noelem", rule, path + "/mapping", value, null);
        }
        // create hash of rule
        _mapping = new DefaultableHashMap();
        if (default_value != null) {
            rule = (Rule)rule_table.get(default_value);
            if (rule == null) {
                rule = new Rule(null, this);
                rule.init(default_value, path + "/mapping/=", rule_table);
            }
            _mapping.setDefault(rule);
        }
        // put rules into _mapping
        Map map = (Map)value;
        for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
            Object k  = it.next();
            Object v = map.get(k);  // DefaultableHashMap
            if (v == null) {
                v = new DefaultableHashMap();
            }
            rule = (Rule)rule_table.get(v);
            if (rule == null) {
                rule = new Rule(null, this);
                rule.init(v, path + "/mapping/" + k, rule_table);
            }
            if (k.equals("=")) {
                _mapping.setDefault(rule);
            } else {
                _mapping.put(k, rule);
            }
        }
        return rule;
    }

    private Rule initReferenceValue(Object value, Rule rule, String path, Map rule_table) throws SchemaException {

			this._ref = (String)value;
			if (this._ref == null)
				throw schemaError("required.novalue", rule, path, "rule", null);
			//make sure a rule with this name is in scope
			Rule refed = (Rule)
										rule_table.values().stream()
										.filter(val -> ((Rule)val).getName() != null && ((Rule)val).getName().equals(this._ref))
										.findFirst()
										.orElse(null);
			if (null == refed)
				throw schemaError("ref.nosuchrule", rule, path, "reference", new Object[] { value });

			return rule;
		}

    private void checkConfliction(Map hash, Rule rule, String path) {
        if (_type.equals("seq")) {
            if (! hash.containsKey("sequence")) throw schemaError("seq.nosequence", rule, path, null, null);
            if (_enum    != null)  throw schemaError("seq.conflict", rule, path, "enum:",    null);
            if (_pattern != null)  throw schemaError("seq.conflict", rule, path, "pattern:", null);
            if (_mapping != null)  throw schemaError("seq.conflict", rule, path, "mapping:", null);
            if (_range   != null)  throw schemaError("seq.conflict", rule, path, "range:",   null);
            if (_length  != null)  throw schemaError("seq.conflict", rule, path, "length:",  null);
        } else if (_type.equals("map")) {
            if (! hash.containsKey("mapping"))  throw schemaError("map.nomapping", rule, path, null, null);
            if (_enum     != null) throw schemaError("map.conflict", rule, path, "enum:",     null);
            if (_pattern  != null) throw schemaError("map.conflict", rule, path, "pattern:",  null);
            if (_sequence != null) throw schemaError("map.conflict", rule, path, "sequence:", null);
            if (_range    != null) throw schemaError("map.conflict", rule, path, "range:",    null);
            if (_length   != null) throw schemaError("map.conflict", rule, path, "length:",   null);
        } else {
            if (_sequence != null) throw schemaError("scalar.conflict", rule, path, "sequence:", null);
            if (_mapping  != null) throw schemaError("scalar.conflict", rule, path, "mapping:",  null);
            if (_enum != null) {
                if (_range != null)   throw schemaError("enum.conflict", rule, path, "range:",   null);
                if (_length != null)  throw schemaError("enum.conflict", rule, path, "length:",  null);
                if (_pattern != null) throw schemaError("enum.conflict", rule, path, "pattern:", null);
            }
        }
    }


    public String inspect() {
        StringBuffer sb = new StringBuffer();
        int level = 0;
        Map done = new IdentityHashMap();
        inspect(sb, level, done);
        return sb.toString();
    }

    private void inspect(StringBuffer sb, int level, Map done) {
        done.put(this, Boolean.TRUE);
        String indent = Util.repeatString("  ", level);
        if (_name != null)    { sb.append(indent).append("name:     ").append(_name).append("\n"); }
        if (_desc != null)    { sb.append(indent).append("desc:     ").append(_desc).append("\n"); }
        if (_type != null)    { sb.append(indent).append("type:     ").append(_type).append("\n"); }
        if (_required)        { sb.append(indent).append("required: ").append(_required).append("\n"); }
        if (_pattern != null) { sb.append(indent).append("pattern:  ").append(_pattern).append("\n"); }
        if (_pattern_regexp != null)  { sb.append(indent).append("regexp:   ").append(_pattern_regexp).append("\n"); }
        if (_assert != null)  { sb.append(indent).append("assert:   ").append(_assert).append("\n"); }
        if (_ident)           { sb.append(indent).append("ident:    ").append(_ident).append("\n"); }
        if (_unique)          { sb.append(indent).append("unique:   ").append(_unique).append("\n"); }
        if (_enum != null) {
            sb.append(indent).append("enum:\n");
            for (Iterator it = _enum.iterator(); it.hasNext(); ) {
                sb.append(indent).append("  - ").append(it.next().toString()).append("\n");
            }
        }
        if (_range != null) {
            sb.append(indent).append("range:     { ");
            String[] keys = new String[] { "max", "max-ex", "min", "min-ex", };
            String colon = "";
            for (int i = 0; i < keys.length; i++) {
                Object val = _range.get(keys[i]);
                if (val != null) {
                    sb.append(colon).append(keys[i]).append(": ").append(val);
                    colon = ", ";
                }
            }
            sb.append(" }\n");
        }
        if (_sequence != null) {
            for (Iterator it = _sequence.iterator(); it.hasNext(); ) {
                Rule rule = (Rule)it.next();
                if (done.containsKey(rule)) {
                    sb.append(indent).append("  ").append("- ...\n");
                } else {
                    sb.append(indent).append("  ").append("- \n");
                    rule.inspect(sb, level + 2, done);
                }
            }
        }
        if (_mapping != null) {
            for (Iterator it = _mapping.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Object key = entry.getKey();
                Rule rule = (Rule)entry.getValue();
                sb.append(indent).append("  ").append(Util.inspect(key));
                if (done.containsKey(rule)) {
                    sb.append(": ...\n");
                } else {
                    sb.append(":\n");
                    rule.inspect(sb, level + 2, done);
                }
            }
        }
    }

}
