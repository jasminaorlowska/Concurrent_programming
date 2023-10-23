import java.util.concurrent.CyclicBarrier;
import java.util.function.IntBinaryOperator;
import java.util.concurrent.BrokenBarrierException;

public class MatrixRowSumsConcurrent {

    private static final int NUM_ROWS = 10;
    private static final int NUM_COLUMNS = 1000;

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

            int[] rowSums = new int[numRows];
            int[] row = new int[numColumns];

            Runnable rowSummer = new RowSummer(rowSums, row);
            CyclicBarrier barrier = new CyclicBarrier(numColumns, rowSummer);
            Thread[] threads = new Thread[numColumns];

            for (int c = 0; c < numColumns; c++) {
                Thread t = new Thread(new PerColumnDefinitionApplier(c, barrier, row, Thread.currentThread()));
                threads[c] = t;
                t.start();
            }
            try {
                for (int c = 0; c < numColumns; c++) threads[c].join();
            } catch (InterruptedException e) {
                for (int c = 0; c < numColumns; c++) threads[c].interrupt();
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            return rowSums;
        }

        private class PerColumnDefinitionApplier implements Runnable {

            private final int myColumnNo;
            private final CyclicBarrier barrier;
            private final int[] row;
            private final Thread mainThread;

            private PerColumnDefinitionApplier(
                    int myColumnNo,
                    CyclicBarrier barrier,
                    int[] row,
                    Thread mainThread
            ) {
                this.myColumnNo = myColumnNo;
                this.barrier = barrier;
                this.row = row;
                this.mainThread = mainThread;
            }

            @Override
            public void run() {

                for (int r = 0; r < numRows; r++) {
                    if (Thread.currentThread().isInterrupted()) return;

                    int calculation = definition.applyAsInt(r, myColumnNo);
                    row[myColumnNo] = calculation;

                    try {
                        barrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        mainThread.interrupt();
                    }

                }
            }

        }


        private class RowSummer implements Runnable {

            private final int[] rowSums;
            private final int[] row;
            private int currentRowNo;

            private RowSummer(int[] rowSums, int[] row) {
                this.rowSums = rowSums;
                this.row = row;
                this.currentRowNo = 0;
            }

            @Override
            public void run() {
                int sum = 0;
                for (int c = 0; c < numColumns; c++) {
                    sum += row[c];
                }
                rowSums[currentRowNo] = sum;
                currentRowNo++;
            }

        }
    }


    public static void main(String[] args) {
        Matrix matrix = new Matrix(NUM_ROWS, NUM_COLUMNS, (row, column) -> {
            int a = 2 * column + 1;
            return (row + 1) * (a % 4 - 2) * a;
        });
        int[] rowSums = new int[matrix.numRows];
        try {
            rowSums = matrix.rowSums();
        } catch (InterruptedException e) {
            System.err.println("Calculation interrupted");
        }
            for (int i = 0; i < rowSums.length; i++) {
                System.out.println(i + " -> " + rowSums[i]);
            }

    }

}