/*
 * @(#)InvalidTypeException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * exception class thrown by Util.compareValues() when comparing different type values.
 * 
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class InvalidTypeException extends KwalifyRuntimeException {
    private static final long serialVersionUID = -6937887618526171845L;

    public InvalidTypeException(String message) {
        super(message);
    }
}
