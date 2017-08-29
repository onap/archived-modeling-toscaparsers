/*
 * @(#)Messages.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
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
