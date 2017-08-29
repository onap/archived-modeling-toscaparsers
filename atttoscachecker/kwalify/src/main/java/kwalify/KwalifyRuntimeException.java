/*
 * @(#)KwalifyRuntimeException.java	$Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * base class of all runtime exception class in Kwalify
 *
 * @revision    $Rev: 3 $
 * @release     $Release: 0.5.1 $
 */
public abstract class KwalifyRuntimeException extends RuntimeException {
    public KwalifyRuntimeException(String message) {
        super(message);
    }
}
