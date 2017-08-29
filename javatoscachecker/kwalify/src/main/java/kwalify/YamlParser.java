/*
 * @(#)YamlParser.java	$Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */
package kwalify;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *  yaml parser which can keep line number of path.
 *
 *  ex.
 *  <pre>
 *  String yaml_str = Util.readFile("document.yaml");
 *  YamlParser parser = new YamlParser(yaml_str);
 *  Object document = parser.parse();
 *  </pre>
 */
public class YamlParser extends PlainYamlParser {
    private Map _linenums_table = new IdentityHashMap(); // object => sequence or mapping
    private int _first_linenum = -1;
    private Object _document = null;

    public YamlParser(String yaml_str) {
        super(yaml_str);
    }

    public Object parse() throws SyntaxException {
        _document = super.parse();
        return _document;
    }

    protected String getLine() {
        String line = super.getLine();
        if (_first_linenum < 0) {
            _first_linenum = currentLineNumber();
        }
        return line;
    }


    public int getPathLineNumber(String ypath) throws InvalidPathException {
        if (_document == null) {
            return -1;
        }
        if (ypath.length() == 0 || ypath.equals("/")) {
            return 1;
        }
        String[] elems = ypath.split("/");
        String last_elem = elems.length > 0 ? elems[elems.length - 1] : null;
        int i = ypath.charAt(0) == '/' ? 1 : 0;
        int len = elems.length - 1;
        Object c = _document;   // collection
        for ( /* nothing */ ; i < len; i++) {
            if (c == null) {
                throw new InvalidPathException(ypath);
            } else if (c instanceof Map) {
                c = ((Map)c).get(elems[i]);
            } else if (c instanceof List) {
                int index = Integer.parseInt(elems[i]);
                if (index < 0 || ((List)c).size() < index) {
                    throw new InvalidPathException(ypath);
                }
                c = ((List)c).get(index);
            } else {
                throw new InvalidPathException(ypath);
            }
        }

        if (c == null) {
            throw new InvalidPathException(ypath);
        }
        Object linenums = _linenums_table.get(c); // Map or List
        int linenum = -1;
        if (c instanceof Map) {
            assert linenums instanceof Map;
            Object d = ((Map)linenums).get(last_elem);
            linenum = ((Integer)d).intValue();
        } else if (c instanceof List) {
            assert linenums instanceof List;
            int index = Integer.parseInt(last_elem);
            if (index < 0 || ((List)linenums).size() <= index) {
                throw new InvalidPathException(ypath);
            }
            Object d = ((List)linenums).get(index);
            linenum = ((Integer)d).intValue();
        } else {
            throw new InvalidPathException(ypath);
        }
        return linenum;
    }

    public void setErrorsLineNumber(List errors) throws InvalidPathException {
        for (Iterator it = errors.iterator(); it.hasNext(); ) {
            ValidationException ex = (ValidationException)it.next();
            ex.setLineNumber(getPathLineNumber(ex.getPath()));
        }
    }

    protected List createSequence(int linenum) {
        List seq = new ArrayList();
        _linenums_table.put(seq, new ArrayList());
        return seq;
    }

    protected void addSequenceValue(List seq, Object value, int linenum) {
        seq.add(value);
        List linenums = (List)_linenums_table.get(seq);
        linenums.add(new Integer(linenum));
    }

    protected void setSequenceValueAt(List seq, int index, Object value, int linenum) {
        seq.set(index, value);
        List linenums = (List)_linenums_table.get(seq);
        linenums.set(index, new Integer(linenum));
    }

    protected Map createMapping(int linenum) {
        Map map = super.createMapping(linenum);
        _linenums_table.put(map, new HashMap());
        return map;
    }

    protected void setMappingValueWith(Map map, Object key, Object value, int linenum) {
        map.put(key, value);
        Map linenums = (Map)_linenums_table.get(map);
        assert linenums != null;
        linenums.put(key, new Integer(linenum));
    }

    protected void setMappingDefault(Map map, Object value, int linenum) {
        super.setMappingDefault(map, value, linenum);
        Map linenums = (Map)_linenums_table.get(map);
        linenums.put(new Character('='), new Integer(linenum));
    }

    protected void mergeMapping(Map map, Map map2, int linenum) {
        Map linenums  = (Map)_linenums_table.get(map);
        Map linenums2 = (Map)_linenums_table.get(map2);
        assert linenums2 != null;
        for (Iterator it = map2.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            if (! map.containsKey(key)) {
                map.put(key, map2.get(key));
                linenums.put(key, linenums2.get(key));
            }
        }
    }

}
