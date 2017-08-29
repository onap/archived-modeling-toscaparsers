/*
 * @(#)Defaultable.java	$Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * interface to have default value
 * 
 * @revision    $Rev: 3 $
 * @release     $Release: 0.5.1 $
 */
public interface Defaultable {
    public Object getDefault();
    public void setDefault(Object value);
}
