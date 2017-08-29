/*
 * @(#)YamlSyntaxException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 *  exception class thrown by YamlParser when syntax of YAML document is wrong
 *
 *  @revision    $Rev: 4 $
 *  @release     $Release: 0.5.1 $
 *  @see         SyntaxException
 */
public class YamlSyntaxException extends SyntaxException {
    private static final long serialVersionUID = 2951669148531823857L;

    public YamlSyntaxException(String message, int linenum) {
        super(message, linenum);
    }

}
