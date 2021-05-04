package cpen221.mp3;

import cpen221.mp3.fsftbuffer.Page;
import cpen221.mp3.fsftbuffer.TestType;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestTypeTests {
    @Test
    public void testEquals1() {
        TestType a = new TestType("a");
        int b = 1;
        assertFalse(a.equals(b));
    }

    @Test
    public void testEqual2() {
        TestType a1 = new TestType("a");
        TestType a2 = new TestType("a");
        assertTrue(a1.equals(a2));
    }

    @Test
    public void testEqual3() {
        TestType a = new TestType("a");
        TestType b = new TestType("b");
        assertFalse(a.equals(b));
    }

    @Test
    public void testEqual4() {
        TestType a1 = new TestType("a", 1);
        TestType a2 = new TestType("a", 2);
        assertFalse(a1.equals(a2));
    }

    @Test
    public void testEqual5() {
        TestType a = new TestType("a");
        assertTrue(a.equals(a));
    }

    @Test
    public void testPageEquals1() {
        Page p1 = new Page("p1", "a");
        Page p2 = new Page("p1", "a");
        assertTrue(p1.equals(p2));
    }

    @Test
    public void testPageEquals2() {
        Page p1 = new Page("p1", "a");
        Page p2 = new Page("p2", "a");
        assertFalse(p1.equals(p2));
    }

    @Test
    public void testPageEquals3() {
        Page p1 = new Page("p1", "a");
        Page p2 = new Page("p1", "b");
        assertFalse(p1.equals(p2));
    }

    @Test
    public void testPageEquals4() {
        Page p1 = new Page("p1", "a");
        int a = 1;
        assertFalse(p1.equals(a));
    }

    @Test
    public void testPageEquals5() {
        Page p1 = new Page("p1", "a");
        assertTrue(p1.equals(p1));
    }
}
