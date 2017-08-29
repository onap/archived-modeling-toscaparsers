/*
 * @(#)SyntaxException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 *  exception class thrown by parser when syntax is wrong.
 *
 *  @revision    $Rev: 4 $
 *  @release     $Release: 0.5.1 $
 *  @see         Parser, YamlSyntaxException
 */
public class SyntaxException extends KwalifyException {
    private static final long serialVersionUID = 2480059811372002740L;

    private int _linenum;

    public SyntaxException(String message, int linenum) {
        super(message);
        _linenum = linenum;
    }

    public int getLineNumber() { return _linenum; }
    public void setLineNumber(int linenum) { _linenum = linenum; }
}
