/*
 * @(#)InvalidPathException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * exception class thrown by YamlParser#setErrorsLineNumber() when path is wrong
 *
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class InvalidPathException extends KwalifyRuntimeException {
    private static final long serialVersionUID = -4601461998104850880L;
    
    //private int _linenum;

    public InvalidPathException(String message) {
        super(message);
    }
}
