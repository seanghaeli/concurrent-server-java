package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.fsftbuffer.NotFoundException;
import cpen221.mp3.fsftbuffer.TestType;
import org.junit.Test;

import static org.junit.Assert.*;

public class FSFTBufferTests {
    @Test
    public void test1() {
        FSFTBuffer<TestType> test = new FSFTBuffer<>(5, 1);
        TestType a = new TestType("a");
        assertTrue(test.put(a));
        try {
            test.get("a");
        } catch (NotFoundException nfe) {
            fail();
        }
    }

    @Test
    public void testException() throws InterruptedException {
        FSFTBuffer<TestType> test = new FSFTBuffer<>(5, 5000);
        TestType a = new TestType("a");
        TestType b = new TestType("b");
        TestType c = new TestType("c");
        TestType d = new TestType("d");
        TestType e = new TestType("e");
        TestType f = new TestType("f");

        test.put(a);
        Thread.sleep(1);
        test.put(b);
        test.put(c);
        test.put(d);
        test.put(e);
        test.put(f);
        try {
            test.get("a");
            fail();
        } catch (NotFoundException nfe) {
            assertEquals(1, 1);
        }

    }

    @Test
    public void testTouch() throws InterruptedException {
        FSFTBuffer<TestType> test = new FSFTBuffer<>(5, 1);
        TestType a = new TestType("a");
        test.put(a);
        Thread.sleep(500);
        test.touch("a");
        Thread.sleep(500);
        try {
            test.get("a");
        } catch (NotFoundException nfe) {
            fail();
        }
        assertEquals(1, 1);
    }

    @Test
    public void testDuplicate() {
        FSFTBuffer<TestType> test = new FSFTBuffer<>(5, 1);
        TestType a = new TestType("a");
        test.put(a);
        assertFalse(test.put(a));
    }

    @Test
    public void testUpdate() {
        FSFTBuffer<TestType> test = new FSFTBuffer<>(5, 1);
        TestType a1 = new TestType("a");
        TestType a2 = new TestType("a", 1);
        test.put(a1);
        test.update(a2);
        try {
            TestType result = test.get("a");
            assertEquals(1, result.val());
        } catch (NotFoundException nfe) {
            fail();
        }
    }

    @Test
    public void testFalseTouchUpdate() {
        FSFTBuffer<TestType> test = new FSFTBuffer<>();
        TestType a = new TestType("a");
        TestType b = new TestType("b");
        test.put(a);
        assertFalse(test.touch("b"));
        assertFalse(test.update(b));
    }

    @Test
    public void testTimeout() throws InterruptedException {
        FSFTBuffer<TestType> test = new FSFTBuffer<>(5, 1);
        TestType a = new TestType("a");
        TestType b = new TestType("b");
        test.put(a);
        test.put(b);
        Thread.sleep(1001);
        assertFalse(test.touch("a"));
        assertFalse(test.touch("b"));
    }

    @Test
    public void testPutFull() throws InterruptedException {
        FSFTBuffer<TestType> test = new FSFTBuffer<>(2, 500);
        TestType a = new TestType("a");
        TestType b = new TestType("b");
        test.put(a);
        Thread.sleep(1);
        test.put(b);

        TestType c = new TestType("c");
        Thread.sleep(1);
        test.touch("a");
        test.put(c);
        assertFalse(test.touch("b"));
    }

    @Test
    public void testThreadSafetyFSFTBuffer1() {
        FSFTBuffer<TestType> buffer = new FSFTBuffer<>(5, 1);

        TestType a = new TestType("a");
        TestType b = new TestType("b");
        TestType c = new TestType("c");

        /*Add a to the buffer in thread1. After 5 ms in thread check
         * that b is in the buffer from thread2.*/
        Thread thread1 = new Thread(() -> {
            buffer.put(a);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
            try {
                assertEquals(b, buffer.get("b"));
            } catch (NotFoundException nfe) {
                nfe.printStackTrace();
                fail();
            }
        });

        /*Start thread with 1ms delay, then put b in buffer.
         * Check that a is still in buffer from thread1.*/
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
            buffer.put(b);
            try {
                assertEquals(a, buffer.get("a"));
            } catch (NotFoundException e) {
                e.printStackTrace();
                fail();
            }
        });

        /*Put c in buffer then wait 750ms before touching b.
         * Then delay the joining of threads by 500ms.*/
        Thread thread3 = new Thread(() -> {
            try {
                buffer.put(c);
                Thread.sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertTrue(buffer.touch("b"));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }

        });

        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
        /*b should still be in the buffer at 1250ms as it was touched in thread3.
         * a and c should be timed out at 1250.*/
        assertTrue(buffer.touch("b"));
        assertFalse(buffer.touch("a"));
        assertFalse(buffer.touch("c"));
    }

    // Here's the concurrent layout we're testing: https://gyazo.com/7c716d600509e329be6b5937fb16e07e
    @Test
    public void testThreadSafetyFSFTBuffer2() {
        FSFTBuffer<TestType> buffer = new FSFTBuffer<>(5, 1);

        TestType a = new TestType("a");
        TestType b = new TestType("b");
        TestType c = new TestType("c");

        Thread thread1 = new Thread(() -> {
            buffer.put(a);
            buffer.put(b);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
            buffer.put(b);
        });

        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
            assertTrue(buffer.touch("c"));
            assertTrue(buffer.touch("a"));
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
            assertTrue(buffer.touch("c"));
        });

        Thread thread3 = new Thread(() -> {
            buffer.put(c);
            try {
                Thread.sleep(2005);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
            assertTrue(buffer.touch("b"));

        });

        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

    }
}
