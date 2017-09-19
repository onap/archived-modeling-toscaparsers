/*
 * @(#)Messages.java	$Rev: 4 $ $Release: 0.5.1 $
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
 * This license is deemed more permisive than if it were under the Apache License, Version 2.0,
 * found at
 *        http://www.apache.org/licenses/LICENSE-2.0
 */

package kwalify;

import java.util.ResourceBundle;
//import java.util.Locale;

/**
 * set of utility methods around messages.
 *
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class Messages {

    private static final String __basename = "kwalify.messages";
    private static ResourceBundle __messages = ResourceBundle.getBundle(__basename);
    //private static ResourceBundle __messages = ResourceBundle.getBundle(__basename, Locale.getDefault());

    public static String message(String key) {
        return __messages.getString(key);
    }

    public static String buildMessage(String key, Object[] args) {
        return buildMessage(key, null, args);
    }

    public static String buildMessage(String key, Object value, Object[] args) {
        String msg = message(key);
        assert msg != null;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {  // don't use MessageFormat
                msg = msg.replaceFirst("%[sd]", escape(args[i]));
            }
        }
        if (value != null && !Types.isCollection(value)) {
            msg = "'" + escape(value) + "': " + msg;
        }
        return msg;
    }

    private static String escape(Object obj) {
        //return obj.toString().replaceAll("\\", "\\\\").replace("\n", "\\n");    // J2SK1.4 doesn't support String#replace(CharSequence, CharSequence)!
        return obj.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\n", "\\\\n");
    }

}
