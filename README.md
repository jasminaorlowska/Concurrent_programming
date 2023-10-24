# Concurrent_programming
Concurrent programming laboratory projects (small ones). 
Here you can find the descriptions of particular programs.

## Vector.java
Program that calculates the sum and dot prouct of two vectors. 

## MatrixRowSums.java
There's a class called Matrix, which represents a matrix of a given size.
It's elements are computed by the definition function based on the row and column numbers.
The method int[] rowSums() returns an array filled with the sums of elements in the matrix rows.

#### MatrixRowSumsConcurrent.java
The main goal of the program is to perform calculations on the elements of a matrix concurrently.
I created as many threads as there are columns. Each thread calculates the value of a cell in the current row.
I used cyclic barriers to synchronize the threads, ensuring that each thread calculates the cells in the same row concurrently.

#### MatrixRowSumsConcurrentThreadSafe.java
The main objective remains consistent with the previous description.
However, instead of using a cyclic barrier, an AtomicIntegerArray has been utilized to store the values of row sums.
This approach allows each thread to calculate the cell value independently,
without having to wait for other threads to complete their calculations.
Each thread can update the row sum directly, enhancing the overall calculation efficiency.

