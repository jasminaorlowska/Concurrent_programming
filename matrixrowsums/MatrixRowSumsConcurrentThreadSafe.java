package matrixrowsums;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntBinaryOperator;

public class MatrixRowSumsConcurrentThreadSafe {

    private static final int NUM_ROWS = 10;
    private static final int NUM_COLUMNS = 10000;

    private static class Matrix {

        private final int numRows;
        private final int numColumns;
        private final IntBinaryOperator definition;

        public Matrix(int numRows, int numColumns, IntBinaryOperator definition) {
            this.numRows = numRows;
            this.numColumns = numColumns;
            this.definition = definition;
        }

        public int[] rowSums() throws InterruptedException {
            AtomicIntegerArray sums = new AtomicIntegerArray(numRows);
            for (int i = 0; i < numRows; i++) sums.set(i, 0);

            int[] rowSums = new int[numRows];

            List<Thread> threads = new ArrayList<>();
            for (int columnNumber = 0; columnNumber < numColumns; columnNumber++) {
                Thread t = new Thread(new PerColumnDefinitionApplier(columnNumber, sums, Thread.currentThread()));
                threads.add(t);
            }
            for (Thread t : threads) t.start();

            try {
                for (Thread t : threads) t.join();
            } catch (InterruptedException e) {
                for (Thread t :threads) t.interrupt();
                throw new InterruptedException();
            }

            for (int i = 0; i < numRows; i++) rowSums[i] = sums.get(i);

            return rowSums;
        }

        private class PerColumnDefinitionApplier implements Runnable {

            private final int myColumnNumber;
            private AtomicIntegerArray sums;
            private final Thread mainThread;

            private PerColumnDefinitionApplier(
                    int myColumnNumber,
                    AtomicIntegerArray sums,
                    Thread mainThread
            ) {
                this.myColumnNumber = myColumnNumber;
                this.mainThread = mainThread;
                this.sums = sums;
            }

            @Override
            public void run() {
                for (int r = 0; r < numRows; r++) {
                    if (Thread.currentThread().isInterrupted()) return;
                    int calculation = definition.applyAsInt(r, myColumnNumber);
                    sums.getAndAdd(r, calculation);
                }
            }

        }
    }


    public static void main(String[] args) {
        Matrix matrix = new Matrix(NUM_ROWS, NUM_COLUMNS, (row, column) -> {
            int a = 2 * column + 1;
            return (row + 1) * (a % 4 - 2) * a;
        });
        try {

            int[] rowSums = matrix.rowSums();

            for (int i = 0; i < rowSums.length; i++) {
                System.out.println(i + " -> " + rowSums[i]);
            }

        } catch (InterruptedException e) {
            System.err.println("Computation interrupted");
        }
    }

}