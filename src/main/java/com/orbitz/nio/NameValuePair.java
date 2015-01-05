package com.orbitz.nio;

/**
 * <p>
 * A name value pair class for HTTP headers and parameters.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public final class NameValuePair {
    private final String key;
    private final String value;

    /**
     * Construct a name value pair.
     *
     * @param   key The key (i.e. name) of the pair.
     * @param   value The value of the pair.
     */
    public NameValuePair(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("name and value required");
        }

        this.key = key;
        this.value = value;
    }

    /**
     * Return the key of the pair.
     *
     * @return  The key.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Returns the value of the pair.
     *
     * @return  The value.
     */
    public String getValue() {
        return this.value;
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NameValuePair)) return false;

        final NameValuePair nameValuePair = (NameValuePair) o;

        if (key != null ? !key.equals(nameValuePair.key)
            : nameValuePair.key != null) return false;
        if (value != null ? !value.equals(nameValuePair.value)
            : nameValuePair.value != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (key != null ? key.hashCode() : 0);
        result = 29 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}