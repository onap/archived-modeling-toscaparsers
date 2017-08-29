/*
 * @(#)KwalifyException.java	$Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * base exception class of all exception in Kwalify
 * 
 * @revision    $Rev: 3 $
 * @release     $Release: 0.5.1 $
 * @see         KwalifyRuntimeException
 */
public abstract class KwalifyException extends Exception {
    public KwalifyException(String message) {
        super(message);
    }
}
