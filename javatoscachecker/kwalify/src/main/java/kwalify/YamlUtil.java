/*
 * @(#)YamlUtil.java	$Rev: 3 $ $Release: 0.5.1 $
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
