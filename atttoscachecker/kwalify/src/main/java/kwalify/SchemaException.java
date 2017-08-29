/*
 * @(#)SchemaException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * exception class thrown by Rule constructor
 * 
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class SchemaException extends BaseException {
    private static final long serialVersionUID = 4750598728284538818L;

    public SchemaException(String message, String ypath, Object value, Rule rule, String errorSymbol) {
        super(message, ypath, value, rule, errorSymbol);
    }

}
