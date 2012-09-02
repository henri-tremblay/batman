package com.octo.montecarlo;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MonteCarloCmd implements Runnable {

    private static class Step {
        long[] pList;

        long[] nList;

        private Step() {
        }

        public Step(int length) {
            pList = new long[length];
            nList = new long[length];
            Arrays.fill(pList, 0);
            Arrays.fill(nList, 0);
        }

        /**
         * Update the current status for a thread
         * 
         * @param index
         *            index of the thread calling the listener
         * @param p
         *            total number of result "in" for the thread
         * @param n
         *            total number of iterations performed by the thread
         * @return
         */
        public Step update(int index, long p, long n) {
            // We don't want to do the update in-place because if the calculate() is called during the update
            // it might return false results
            Step s = new Step();
            s.pList = Arrays.copyOf(pList, pList.length);
            s.nList = Arrays.copyOf(nList, nList.length);
            s.pList[index] = p;
            s.nList[index] = n;
            return s;
        }

        /**
         * Calculate the current answer
         * 
         * @param factor
         *            multiplicative factor for the current formula
         * @return the current answer
         */
        public double calculate(double factor) {
            long p = 0;
            long n = 0;
            for (int i = 0; i < pList.length; i++) {
                p += pList[i];
                n += nList[i];
            }
            return factor * p / n;
        }

        /**
         * @return the sum of all iterations performed by the threads
         */
        public long loops() {
            long n = 0;
            for (int i = 0; i < pList.length; i++) {
                n += nList[i];
            }
            return n;
        }
    }

    private static final long CHECKPOINT = 1_000L; // iterations

    private static final long FEEDBACK = 5_000L; // in ms

    private static long ITERATIONS;

    private static long TIMEOUT; // in seconds

    private static final AtomicReference<Step> stepRef = new AtomicReference<>();

    private static AtomicInteger concurrencyCount = new AtomicInteger(0);

    private static CountDownLatch latch;

    private final MonteCarloCalculator calculator;

    private final long iterations;

    public MonteCarloCmd(MonteCarloCalculator calculator, long iterations) {
        this.calculator = calculator;
        this.iterations = iterations;
    }

    public static void main(String[] args) {

        if (args.length != 4) {
            usage("Missing parameters");
            return;
        }

        final String algoName = args[0];
        final String algoType = args[1];
        ITERATIONS = Long.parseLong(args[2]);
        TIMEOUT = Long.parseLong(args[3]);

        final Constructor<MonteCarloCalculator> constructor = retrieveAlgorithm(algoName);
        // This instance is only used to get the variables specific to this calculator
        final MonteCarloCalculator calculator = instantiateAlgorithm(constructor, 0);

        MonteCarloListener listener = new MonteCarloListener() {

            @Override
            public void onPoint(double x, double y, boolean good) {
            }

            @Override
            public void onValue(int index, long p, long n) {
                // Note here that to get the final result, the iterations per thread need to be
                // perfectly divided by the checkpoint
                if (n % CHECKPOINT == 0) {
                    while (true) {
                        // Try to update until we are successful. This prevents a synchronized
                        // It's a manual STM, we rollback and retry each time we fail
                        Step s = stepRef.get();
                        Step updated = s.update(index, p, n);
                        if (stepRef.compareAndSet(s, updated)) {
                            break;
                        }
                        // This counter shows how many rollback we had to performed because of contingency
                        concurrencyCount.incrementAndGet();
                    }
                }
            }

        };
        calculator.setListener(listener);

        TimerTask taskPerformer = new TimerTask() {
            @Override
            public void run() {
                Step step = stepRef.get();
                double area = step.calculate(calculator.getFactor());
                System.out.printf("%s = %1.10f on iteration %d with %d concurrent writes%n", algoName, area, step.loops(),
                        concurrencyCount.get());
            }
        };

        Timer timer = new Timer("screen feedback", true);
        timer.scheduleAtFixedRate(taskPerformer, FEEDBACK, FEEDBACK);

        long start = System.currentTimeMillis();
        switch (algoType) {
        case "sequential":
            sequential(calculator);
            break;
        case "parallel":
            parallel(constructor, listener);
            break;
        default:
            usage("Unknown algorithm type. Should be parallel or sequential");
            return;
        }

        long end = System.currentTimeMillis();

        Step step = stepRef.get();
        double area = step.calculate(calculator.getFactor());
        System.out.printf("Final: %s = %1.10f with %d iterations and %d concurrent writes in %d seconds%n", algoName, area,
                step.loops(), concurrencyCount.get(), (end - start) / 1000);
    }

    private static void sequential(MonteCarloCalculator calculator) {

        // Validate we have suitable parameters
        // We want to reach a checkpoint on the last iteration to get the final result. This is just to simplify the code
        if (ITERATIONS % CHECKPOINT != 0) {
            usage(String.format("Number of iterations should be perfectly divided by %d", CHECKPOINT));
            return;
        }
        
        stepRef.set(new Step(1));
        // The sequential process is started with an ExecutorService to allow us to have a timeout
        ExecutorService service = Executors.newSingleThreadExecutor();
        MonteCarloCmd cmd = new MonteCarloCmd(calculator, ITERATIONS);
        latch = new CountDownLatch(1);
        service.execute(cmd);
        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            service.shutdownNow();
        }
    }

    private static void parallel(Constructor<MonteCarloCalculator> constructor, MonteCarloListener listener) {
        
        int p = Runtime.getRuntime().availableProcessors();

        // Validate we have suitable parameters
        // We want the same amount of iterations on each processor. This is just to simplify the code
        if (ITERATIONS % p != 0) {
            usage(String.format("Number of iterations should be perfectly divided by the number of processors (%d)",
                    p));
            return;
        }

        // We want to reach a checkpoint on the last iteration to get the final result. This is just to simplify the code
        if ((ITERATIONS / p) % CHECKPOINT != 0) {
            usage(String.format("Number of iterations per processors (%d) should be perfectly divided by %d",
                    p, CHECKPOINT));
            return;
        }
        
        ForkJoinPool pool = new ForkJoinPool(p);
        
        long iterationsPerThread = ITERATIONS / p;
        stepRef.set(new Step(p));
        latch = new CountDownLatch(p);
        for (int i = 0; i < p; i++) {
            MonteCarloCalculator calculator = instantiateAlgorithm(constructor, i);
            calculator.setListener(listener);
            pool.execute(new MonteCarloCmd(calculator, iterationsPerThread));
        }
        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    protected static Constructor<MonteCarloCalculator> retrieveAlgorithm(String prefix) {
        String algo = MonteCarloGui.class.getPackage().getName() + "." + prefix + "MonteCarlo";
        Class<MonteCarloCalculator> algoClass;
        try {
            algoClass = (Class<MonteCarloCalculator>) Class.forName(algo);
        } catch (ClassNotFoundException e) {
            usage("Algorithm doesn't exist: " + algo);
            return null;
        }
        try {
            return algoClass.getConstructor(Integer.TYPE);
        } catch (NoSuchMethodException e) {
            usage("Constructor taking 'int' in parameter required for : " + algo);
            return null;
        }
    }

    protected static MonteCarloCalculator instantiateAlgorithm(Constructor<MonteCarloCalculator> cons, int index) {
        try {
            return cons.newInstance(index);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void usage(String message) {
        System.err.printf("%s%n%nUsage: MonteCarloCmd Batman|Pi sequential|parallel loop timeout%n", message);
        System.exit(1);
        return;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; i++) {
            calculator.calculate();
            // Check if we are interrupted but don't do it too often to prevent slowing down the process
            if(i % CHECKPOINT == 0 && Thread.currentThread().isInterrupted()) {
                break;
            }
        }
        latch.countDown();
    }
}