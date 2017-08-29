/*
 * @(#)Parser.java	$Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * interface for any parser
 * 
 * @revision    $Rev: 3 $
 * @release     $Release: 0.5.1 $
 */
public interface Parser {

    public Object parse() throws SyntaxException;

}
