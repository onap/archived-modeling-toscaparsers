/*
 * @(#)CommandOptionException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * exception class thrown if command-line option is wrong
 * 
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class CommandOptionException extends KwalifyException {
    private static final long serialVersionUID = 6433387612335104714L;

    private String _error_symbol = null;
    private char _option;

    public CommandOptionException(String message, char option, String error_symbol) {
        super(message);
        _option = option;
        _error_symbol = error_symbol;
    }

    public String getErrorSymbol() { return _error_symbol; }
    public void setErrorSymbol(String error_symbol) { _error_symbol = error_symbol; }

    public char getOption() { return _option; }
    public void setOption(char option) { _option = option; }

}
