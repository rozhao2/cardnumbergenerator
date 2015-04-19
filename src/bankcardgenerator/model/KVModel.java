/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package bankcardgenerator.model;

import java.io.Serializable;

/**
 *
 * @author Administrator
 */
public class KVModel implements Serializable{
    public String key;
    public String value;

    @Override
    public String toString() {
        return this.value;
    }
}
