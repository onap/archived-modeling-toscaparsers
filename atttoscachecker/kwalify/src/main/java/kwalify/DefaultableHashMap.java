/*
 * @(#)DefaultableHashMap.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import java.util.Map;
import java.util.HashMap;

/**
 * hash map which can have default value
 * 
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class DefaultableHashMap extends HashMap implements Defaultable {

    private static final long serialVersionUID = -5224819562023897380L;

    private Object _default = null;

    public DefaultableHashMap() {
        super();
    }

    public DefaultableHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public DefaultableHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public DefaultableHashMap(Map m) {
        super(m);
    }

    public Object getDefault() { return _default; }

    public void setDefault(Object value) { _default = value; }

    public Object get(Object key) {
        return containsKey(key) ? super.get(key) : _default;
    }

}
