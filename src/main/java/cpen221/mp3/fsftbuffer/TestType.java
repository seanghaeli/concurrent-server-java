package cpen221.mp3.fsftbuffer;

public class TestType implements Bufferable {
    private static final int DVAL = -1;

    private final String id;
    private final int val;
    /**
     * Representation Invariant:
     * id and val are not null
     */
    /**
     * Abstraction Function:
     * Represents a simple object with a String
     * identifier and an integer value.
     */

    /**
     * Creates an instance of TestType with a given
     * id and val.
     *
     * @param id  id corresponding to this instance of TestType
     * @param val val corresponding to this instance of TestType
     */
    public TestType(String id, int val) {
        this.id = id;
        this.val = val;
    }

    /**
     * Create an instance of TestType with id
     * and default val.
     *
     * @param id id corresponding to this instance of TestType
     */
    public TestType(String id) {
        this(id, DVAL);
    }

    /**
     * Returns the id of this instance of TestType
     *
     * @return id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the val of this instance of TestType
     *
     * @return val
     */
    public int val() {
        return val;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TestType)) {
            return false;
        }
        TestType that = (TestType) obj;
        return this.id.equals(that.id()) && (this.val == that.val());
    }
}
