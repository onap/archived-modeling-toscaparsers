/*
 * The original version of this file was provided under the MIT license,
 * as follows:
 *
 * @(#)BaseException.java	$Rev: 3 $ $Release: 0.5.1 $
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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Modifications to this file are provided under the Apache 2.0 license,
 * as follows:

 * Copyright (c) 2017 <AT&T>.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package kwalify;

/**
 * base class of ValidationException and SchemaException.
 * 
 * @revision    $Rev: 3 $
 * @release     $Release: 0.5.1 $
 */
public abstract class BaseException extends KwalifyRuntimeException implements Comparable {

    String _ypath;
    Object _value;
    Rule   _rule;
    String _errorSymbol;
    int    _linenum = -1;
    
    public BaseException(String message, String ypath, Object value, Rule rule, String errorSymbol) {
        super(message);
        _ypath = ypath;
        _value = value;
        _rule  = rule;
        _errorSymbol = errorSymbol;
    }

    public String getPath() { return _ypath.equals("") ? "/" : _ypath; }
    //public void setPath(String ypath) { _ypath = ypath; }

    public Object getValue() { return _value; }
    //public void setValue(Object value) { _value = value; }

    public Rule getRule() { return _rule; }
    //
    //public void setRule(Rule rule) { _rule = rule; }

    public String getErrorSymbol() { return _errorSymbol; }
    //public void setErrorSymbol(String errorSymbol) { _errorSymbol = errorSymbol; }

    public int getLineNumber() { return _linenum; }
    public void setLineNumber(int linenum) { _linenum = linenum; }

    public int compareTo(Object obj) {
        int n = ((ValidationException)obj).getLineNumber();
        return _linenum - n;
    }
}
