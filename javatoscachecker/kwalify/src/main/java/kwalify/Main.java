/*
 * @(#)Main.java	$Rev: 4 $ $Release: 0.5.1 $
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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.regex.Matcher;
import java.io.IOException;

/**
 * class for main program
 * 
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class Main {

    private String _command;
    private boolean _flag_help     = false;  // print help
    private boolean _flag_version  = false;  // print version
    private boolean _flag_silent   = false;  // suppress messages
    private boolean _flag_meta     = false;  // meta validation
    private boolean _flag_untabify = false;  // expand tab charactor to spaces
    private boolean _flag_emacs    = false;  // show errors in emacs style
    private boolean _flag_linenum  = false;  // show line number on where errors happened
    private boolean _flag_debug    = false;  // internal use only
    private String  _schema_filename = null; // schema filename
    private Map _properties = new HashMap();


    boolean isDebug() { return _flag_debug; }


    public String inspect() {
        StringBuffer sb = new StringBuffer();
        sb.append("command       : ").append(_command       ).append('\n');
        sb.append("flag_help     : ").append(_flag_help     ).append('\n');
        sb.append("flag_version  : ").append(_flag_version  ).append('\n');
        sb.append("flag_silent   : ").append(_flag_silent   ).append('\n');
        sb.append("flag_meta     : ").append(_flag_meta     ).append('\n');
        sb.append("flag_untabify : ").append(_flag_untabify ).append('\n');
        sb.append("flag_emacs    : ").append(_flag_emacs    ).append('\n');
        sb.append("flag_linenum  : ").append(_flag_linenum  ).append('\n');
        sb.append("flag_debug    : ").append(_flag_debug    ).append('\n');
        sb.append("schema_filename : ").append(_schema_filename).append('\n');
        sb.append("properties:\n");
        for (Iterator it = _properties.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            Object val = _properties.get(key);
            sb.append("  ").append(key).append(": ").append(val).append('\n');
        }
        return sb.toString();
    }


    private static final String REVISION = "$Release: 0.5.1 $";
    private static final String HELP = ""
        + "Usage1: %s [-hvstlE] -f schema.yaml doc.yaml [doc2.yaml ...]\n"
        + "Usage2: %s [-hvstlE] -m schema.yaml [schema2.yaml ...]\n"
        + "  -h, --help      :  help\n"
        + "  -v              :  version\n"
        + "  -s              :  silent\n"
        + "  -f schema.yaml  :  schema definition file\n"
        + "  -m              :  meta-validation mode\n"
        + "  -t              :  expand tab character automatically\n"
        + "  -l              :  show linenumber when errored (experimental)\n"
        + "  -E              :  show errors in emacs-style (implies '-l')\n"
        ;


    public Main(String command) {
        _command = command;
    }


    public String execute(String[] args) throws IOException, CommandOptionException, SyntaxException {
        // parse command-line options
        String[] filenames = parseOptions(args);

        // help or version
        StringBuffer sb = new StringBuffer();
        if (_flag_version) {
            sb.append(version()).append('\n');
        }
        if (_flag_help) {
            sb.append(help());
        }
        if (sb.length() > 0) {
            return sb.toString();
        }

        // main
        String s = null;
        if (_flag_meta) {
            s = metaValidate(filenames);
        } else if (_schema_filename == null) {
            throw optionError("command.option.noaction", '\0');
        } else if (_flag_debug) {
            s = inspectSchemaFile(_schema_filename);
        } else {
            s = validate(filenames, _schema_filename);
        }

        //
        return s;
    }


    private String[] parseOptions(String[] args) throws CommandOptionException {
        Object[] ret = null;
        try {
            ret = Util.parseCommandOptions(args, "hvsmtlED", "f", null);
        } catch (CommandOptionException ex) {
            String error_symbol = ex.getErrorSymbol();
            if (error_symbol.equals("command.option.noarg")) {
                switch (ex.getOption()) {
                case 'f':   error_symbol = "command.option.noschema";  break;
                default:
                    assert false;
                }
            }
            throw optionError(error_symbol, ex.getOption());
        }
        //
        Map options        = (Map)ret[0];
        Map properties     = (Map)ret[1];
        String[] filenames = (String[])ret[2];
        //
        _flag_help     = options.get("h") != null;
        _flag_version  = options.get("v") != null;
        _flag_silent   = options.get("s") != null;
        _flag_meta     = options.get("m") != null;
        _flag_untabify = options.get("t") != null;
        _flag_emacs    = options.get("E") != null;
        _flag_linenum  = options.get("l") != null || _flag_emacs;
        _flag_debug    = options.get("D") != null;
        _schema_filename = (String)options.get("f");
        //
        //
        _properties = properties;
        if (_properties.get("help") != null) {
            _flag_help = true;
        }
        //
        return filenames;
    }


    private String validate(String[] filenames, String schema_filename) throws IOException, SyntaxException {
        String str = Util.readFile(schema_filename);
        if (_flag_untabify) {
            str = Util.untabify(str);
        }
        YamlParser parser = new YamlParser(str);
        Object schema = parser.parse();
        Validator validator = new Validator(schema);
        String s = validateFiles(validator, filenames);
        return s;
    }


    private String validateFiles(Validator validator, String[] filenames) throws IOException, SyntaxException {
        if (filenames.length == 0) {
            filenames = new String[] { null };
        }
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < filenames.length; j++) {
            String filename = filenames[j];
            String str = null;
            if (filename == null) {
                str = Util.readInputStream(System.in);
                filename = "(stdin)";
            } else {
                str = Util.readFile(filename);
            }
            if (_flag_untabify) {
                str = Util.untabify(str);
            }
            YamlParser parser = new YamlParser(str);
            int i = 0;
            while (parser.hasNext()) {
                Object doc = parser.parse();
                validateDocument(sb, validator, doc, filename, i, parser);
                i++;
            }
        }
        return sb.toString();
    }


    private void validateDocument(StringBuffer sb, Validator validator, Object doc, String filename, int i, YamlParser parser) {
        if (doc == null) {
            Object[] args = { filename, new Integer(i) };
            String msg = Messages.buildMessage("validation.empty", null, args);
            sb.append(msg).append('\n');
            return;
        }
        List errors = validator.validate(doc);
        Object[] args = { filename, new Integer(i) };
        if (errors == null || errors.size() == 0) {
            if (! _flag_silent) {
                String msg = Messages.buildMessage("validation.valid", args);
                sb.append(msg).append('\n');
            }
        } else {
            String msg = Messages.buildMessage("validation.invalid", args);
            sb.append(msg).append('\n');
            if (_flag_linenum) {
                assert parser != null;
                parser.setErrorsLineNumber(errors);
                Collections.sort(errors);
            }
            for (Iterator it = errors.iterator(); it.hasNext(); ) {
                ValidationException error = (ValidationException)it.next();
                if (_flag_emacs) {
                    assert _flag_linenum;
                    sb.append(filename).append(":").append(error.getLineNumber()).append(":");
                } else if (_flag_linenum) {
                    sb.append("  - (line ").append(error.getLineNumber()).append(")");
                } else {
                    sb.append("  -");
                }
                sb.append(" [").append(error.getPath()).append("] ").append(error.getMessage()).append('\n');
            }
        }
    }


    private String metaValidate(String[] filenames) throws IOException, SyntaxException {
        Validator meta_validator = MetaValidator.instance();
        String s = validateFiles(meta_validator, filenames);
        return s;
    }


    private String inspectSchemaFile(String schema_filename) throws IOException, SyntaxException {
        String filename = schema_filename;
        String content = filename != null ? Util.readFile(filename) : Util.readInputStream(System.in);
        YamlParser parser = new YamlParser(content);
        Object schema = parser.parse();
        if (schema == null) {
            return null;
        }
        Validator validator = new Validator(schema);  // SchemaException is thrown when schema is wrong
        String s = validator.getRule().inspect();
        if (s.charAt(s.length() - 1) != '\n') {
            s = s + '\n';
        }
        return s;
    }


    private static CommandOptionException optionError(String error_symbol, char option) {
        Object[] args = { Character.toString(option) };
        String message = Messages.buildMessage(error_symbol, null, args);
        return new CommandOptionException(message, option, error_symbol);
    }


    private String version() {
        Matcher m = Util.matcher(REVISION, "[.\\d]+");
        m.find();
        String version = m.group(0);
        return version;
    }


    private String help() {
        String help_msg = Messages.buildMessage("command.help", null, new Object[] { _command, _command });
        //String help = HELP.replaceAll("%s", _command);
        return help_msg;
    }


    public static void main(String[] args) throws Exception {
        int status = 0;
        Main main = null;
        try {
            main = new Main("kwalify-java");
            String result = main.execute(args);
            if (result != null) {
                System.out.println(result);
            }
        } catch (Exception ex) {
            if (main != null && main.isDebug()) {
                throw ex;
            }
            if (    ex instanceof CommandOptionException
                 || ex instanceof SyntaxException
                 || ex instanceof IOException) {
                System.err.println("ERROR: " + ex.getMessage());
                status = 1;
            }
        }
        System.exit(status);
    }

}
