/*
 * @(#)ValidationException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * exception class which represents validation error.
 *
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class ValidationException extends BaseException {
    private static final long serialVersionUID = -2991121377463453973L;

    public ValidationException(String message, String path, Object value, Rule rule, String error_symbol) {
        super(message, path, value, rule, error_symbol);
    }

    public ValidationException(String message, String path) {
        this(message, path, null, null, null);
    }

}
