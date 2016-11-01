package edu.nyu.cs.cs2580;

/**
 * Created by sanchitmehta on 31/10/16.
 */
public class Mutable<T> {
    protected T value;

    /**
     * @param value
     */
    public Mutable(T value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public T getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(T value) {
        this.value = value;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return value.toString();
    }
}