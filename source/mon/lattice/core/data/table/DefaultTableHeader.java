// DefaultTableHeader.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Feb 2009

package mon.lattice.core.data.table;

import java.util.List;
import java.util.LinkedList;
import mon.lattice.core.ProbeAttributeType;

/**
 * A default implementation of a TableHeader.
 */
public class DefaultTableHeader implements TableHeader {
    // A list of TableAttributes that are part of this header
    List<TableAttribute> attributes;

   /**
    * Construct a TableHeader.
    * It is done w.r.t the Table that the header will be in.
    */
    public DefaultTableHeader() {
	attributes = new LinkedList<TableAttribute>();
    }

    /**
     * Get the size of the header.
     * It should be the same size as the number of column 
     * definitions for the table that the header is in.
     */
    public int size() {
	return attributes.size();
    }

    /**
     * Get the Nth element from a header.
     */
    public TableAttribute get(int n) {
	return attributes.get(n);
    }

    /**
     * Set the Nth element of a header.
     */
    public TableHeader set(int n, TableAttribute value) {
	// set value in Nth position
	attributes.add(n, value);
	return this;
    }

    /**
     * Add a column definition to the header.
     */
    public TableHeader add(String name, ProbeAttributeType type) {
	int n = size();
	attributes.add(n , new DefaultTableAttribute(n, name, type));
	return this;
    }

    /**
     * Convert the TableHeader to a List.
     */
    public List<TableAttribute> toList() {
	return attributes;
    }

    /**
     * Equals for a DefaultTableHeader.
     */
    public boolean equals(Object obj) {
        // It is the equals() of the list
        if (obj instanceof DefaultTableHeader) {
            DefaultTableHeader other = (DefaultTableHeader)obj;
            return attributes.equals(other.attributes);
        } else {
            return false;
        }
    }

    /**
     * The hashCode for a DefaultTableHeader.
     */
    public int hashCode() {
        // It is the hashCode() of the list
        return attributes.hashCode();
    }

    /**
     * To string
     */
    public String toString() {
	return attributes.toString();
    }

}
