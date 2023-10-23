import java.util.Arrays;
import java.util.Random;

public class Vector {

    private static final int SUM_CHUNK_LENGTH = 10;
    private static final int DOT_CHUNK_LENGTH = 10;

    private final int[] elements;

    public Vector(int length) {
        this.elements = new int[length];
    }

    public Vector(int[] elements) {
        this.elements = Arrays.copyOf(elements, elements.length);
    }

    public int getCoordinate(int coordinate) throws IllegalArgumentException{
        if (coordinate < 0 || coordinate >= elements.length)
            throw new IllegalArgumentException("no such coordinate");
        return elements[coordinate];
    }
    public void setCoordinate(int coordinate, int x) {
        if (coordinate < 0 || coordinate >= elements.length)
            throw new IllegalArgumentException("no such coordinate");
        elements[coordinate] = x;
    }

    // joining helper threads in sum and dot
    private void joinThreads(int threadNum, Thread[] threads) throws InterruptedException{
        for (int i = 0; i < threadNum; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                for (int k = 0; k < threadNum; k++) threads[k].interrupt(); //ending other threads
                throw new InterruptedException();
            }

        }
    }

    public Vector sum(Vector other) throws InterruptedException {
        if (this.elements.length != other.elements.length) {
            throw new IllegalArgumentException("different lengths of summed vectors");
        }

        Vector result = new Vector(this.elements.length);

        int threadNum = (int) Math.ceil ( (double)elements.length / SUM_CHUNK_LENGTH);
        Thread[] threads = new Thread[threadNum];

        for (int i = 0; i < threadNum; i++) {
            int startPosIncl = i * SUM_CHUNK_LENGTH;
            int endPosExcl = Math.min((i+1) * SUM_CHUNK_LENGTH, elements.length);
            Summer s = new Summer(this, other, startPosIncl , endPosExcl, result);
            Thread t = new Thread(s);
            threads[i] = t;
            t.start();
        }

        joinThreads(threadNum, threads);

        return result;
    }
    private static class Summer implements Runnable {

        private final Vector mainVec;
        private final Vector otherVec;
        private final int startPosIncl; //start position inclusive
        private final int endPosExcl;
        private final Vector resVec;

        public Summer(Vector mainVec, Vector otherVec, int startPosIncl, int endPosExcl, Vector resVec) {
            this.resVec = resVec;
            this.mainVec = mainVec;
            this.otherVec = otherVec;
            this.startPosIncl = startPosIncl;
            this.endPosExcl = endPosExcl;
        }

        @Override
        public void run() {
            for (int i = startPosIncl; i < endPosExcl; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                int x = mainVec.getCoordinate(i) + otherVec.getCoordinate(i);
                resVec.setCoordinate(i, x);

            }
        }

    }

    public int dot(Vector other) throws InterruptedException {
        if (this.elements.length != other.elements.length) {
            throw new IllegalArgumentException("different lengths of dotted vectors");
        }
        int result = 0;

        int threadNum = (int) Math.ceil ( (double)elements.length / DOT_CHUNK_LENGTH);
        Thread[] threads = new Thread[threadNum];
        int res[] = new int[threadNum];

        for (int i = 0; i < threadNum; i++) {
            int startPosIncl = i * DOT_CHUNK_LENGTH;
            int endPosExcl = Math.min((i + 1) * DOT_CHUNK_LENGTH, elements.length);
            Dotter d = new Dotter(this, other, startPosIncl, endPosExcl, res, i);
            Thread t = new Thread(d);
            threads[i] = t;
            t.start();
        }

        joinThreads(threadNum, threads);

        for (int i = 0; i < threadNum; i++) result += res[i];
        return result;
    }
    private static class Dotter implements Runnable {

        private final Vector mainVec;
        private final Vector otherVec;
        private final int startPosIncl;
        private final int endPosExcl;
        private final int[] resVec;
        private final int resPos;

        public Dotter(Vector mainVec, Vector otherVec, int startPosIncl, int endPosExcl, int[] resVec, int resPos) {
            this.mainVec = mainVec;
            this.otherVec = otherVec;
            this.startPosIncl = startPosIncl;
            this.endPosExcl = endPosExcl;
            this.resVec = resVec;
            this.resPos = resPos;
        }

        @Override
        public void run() {
            int x = 0;
            for (int i = startPosIncl; i < endPosExcl; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                x += (mainVec.getCoordinate(i) * otherVec.getCoordinate(i));
            }
            resVec[resPos] = x;

        }

    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Vector)) {
            return false;
        }
        Vector other = (Vector)obj;
        return Arrays.equals(this.elements, other.elements);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.elements);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < this.elements.length; i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append(this.elements[i]);
        }
        s.append("]");
        return s.toString();
    }

    // ----------------------- TESTS -----------------------

    private static final Random RANDOM = new Random();

    private static Vector generateRandomVector(int length) {
        int[] a = new int[length];
        for (int i = 0; i < length; i++) {
            a[i] = RANDOM.nextInt(10);
        }
        return new Vector(a);
    }
    private final Vector sumSequential(Vector other) {
        if (this.elements.length != other.elements.length) {
            throw new IllegalArgumentException("different lengths of summed vectors");
        }
        Vector result = new Vector(this.elements.length);
        for (int i = 0; i < result.elements.length; i++) {
            result.elements[i] = this.elements[i] + other.elements[i];
        }
        return result;
    }
    private final int dotSequential(Vector other) {
        if (this.elements.length != other.elements.length) {
            throw new IllegalArgumentException("different lengths of dotted vectors");
        }
        int result = 0;
        for (int i = 0; i < this.elements.length; i++) {
            result += this.elements[i] * other.elements[i];
        }
        return result;
    }


    public static void main(String[] args) {
        try {
            Vector a = generateRandomVector(33);
            System.out.println(a);
            Vector b = generateRandomVector(33);
            System.out.println(b);
            Vector c = a.sum(b);
            System.out.println(c);
            assert(c.equals(a.sumSequential(b)));
            int d = a.dot(b);
            System.out.println(d);
            assert(d == a.dotSequential(b));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("computations interrupted");
        }
    }

}
