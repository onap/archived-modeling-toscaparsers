/*
 * @(#)CommandOptionException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * Copyright (c) <2005> <kuwata lab>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Added in order to satisfy onap.org code check constraints:
 *
 * The MIT license is deemed more permisive than the Apache License, Version 2.0,
 * found at
 *        http://www.apache.org/licenses/LICENSE-2.0
 
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
