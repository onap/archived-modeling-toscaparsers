/*
 * @(#)YamlUtil.java	$Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 *  utilify class for yaml.
 *
 *  @version   $Rev: 3 $
 *  @release   $Release: 0.5.1 $
 */
public class YamlUtil {

    public static Object load(String yaml_str) throws SyntaxException {
        PlainYamlParser parser = new PlainYamlParser(yaml_str);
        Object doc = parser.parse();
        return doc;
    }

    public static Object loadFile(String filename, String charset) throws IOException, SyntaxException {
        Object doc = null;
        InputStream input = null;
        Reader reader = null;
        try {
            input = new FileInputStream(filename);
            reader = new InputStreamReader(input, charset);
            StringBuffer sb = new StringBuffer();
            int ch;
            while ((ch = reader.read()) >= 0) {
                sb.append((char)ch);
            }
            doc = load(sb.toString());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {}
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignore) {}
            }
        }
        return doc;
    }

    public static Object loadFile(String filename) throws IOException, SyntaxException {
        String encoding = System.getProperty("file.encoding");
        return loadFile(filename, encoding);
    }

}
