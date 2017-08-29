/*
 * @(#)BaseException.java	$Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * base class of ValidationException and SchemaException.
 * 
 * @revision    $Rev: 3 $
 * @release     $Release: 0.5.1 $
 */
public abstract class BaseException extends KwalifyRuntimeException implements Comparable {

    String _ypath;
    Object _value;
    Rule   _rule;
    String _errorSymbol;
    int    _linenum = -1;
    
    public BaseException(String message, String ypath, Object value, Rule rule, String errorSymbol) {
        super(message);
        _ypath = ypath;
        _value = value;
        _rule  = rule;
        _errorSymbol = errorSymbol;
    }

    public String getPath() { return _ypath.equals("") ? "/" : _ypath; }
    //public void setPath(String ypath) { _ypath = ypath; }

    public Object getValue() { return _value; }
    //public void setValue(Object value) { _value = value; }

    public Rule getRule() { return _rule; }
    //
    //public void setRule(Rule rule) { _rule = rule; }

    public String getErrorSymbol() { return _errorSymbol; }
    //public void setErrorSymbol(String errorSymbol) { _errorSymbol = errorSymbol; }

    public int getLineNumber() { return _linenum; }
    public void setLineNumber(int linenum) { _linenum = linenum; }

    public int compareTo(Object obj) {
        int n = ((ValidationException)obj).getLineNumber();
        return _linenum - n;
    }
}
