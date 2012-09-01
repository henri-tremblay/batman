package com.octo.montecarlo;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
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

        public Step update(int index, long p, long n) {
            Step s = new Step();
            s.pList = Arrays.copyOf(pList, pList.length);
            s.nList = Arrays.copyOf(nList, nList.length);
            s.pList[index] = p;
            s.nList[index] = n;
            return s;
        }

        public double calculate(double factor) {
            long p = 0;
            long n = 0;
            for (int i = 0; i < pList.length; i++) {
                p += pList[i];
                n += nList[i];
            }
            return factor * p / n;
        }

        public long loops() {
            long n = 0;
            for (int i = 0; i < pList.length; i++) {
                n += nList[i];
            }
            return n;
        }
    }

    private static final long ITERATIONS = 200_000_000;

    private static final long CHECKPOINT = 1_000; // in ms

    private static final long TIMEOUT = 60; // in seconds

    private static final AtomicReference<Step> stepRef = new AtomicReference<>();

    private static AtomicInteger concurrencyCount = new AtomicInteger(0);

    private final MonteCarloCalculator calculator;

    public MonteCarloCmd(MonteCarloCalculator calculator) {
        this.calculator = calculator;
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            usage("Algoritm not provided");
            return;
        }

        final String algoName = args[0];
        final String algoType = args[1];

        final Constructor<MonteCarloCalculator> constructor = retrieveAlgorithm(algoName);
        // This instance is only used to get the variables specific to this calculator
        final MonteCarloCalculator calculator = instantiateAlgorithm(constructor, 0);

        MonteCarloListener listener = new MonteCarloListener() {

            @Override
            public void onPoint(double x, double y, boolean good) {
            }

            @Override
            public void onValue(int index, long p, long n) {
                if (n % CHECKPOINT == 0) {
                    while (true) {
                        // Try to update until we are successful. This prevents a synchronized
                        // It's a manual STM, we rollback and retry each time we fail
                        Step s = stepRef.get();
                        Step updated = s.update(index, p, n);
                        if (stepRef.compareAndSet(s, updated)) {
                            break;
                        }
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
        timer.scheduleAtFixedRate(taskPerformer, 1000, 1000);

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
        stepRef.set(new Step(1));
        new MonteCarloCmd(calculator).run();
    }

    private static void parallel(Constructor<MonteCarloCalculator> constructor, MonteCarloListener listener) {
        ForkJoinPool pool = new ForkJoinPool();
        stepRef.set(new Step(pool.getParallelism()));
        for (int i = 0; i < pool.getParallelism(); i++) {
            MonteCarloCalculator calculator = instantiateAlgorithm(constructor, i);
            calculator.setListener(listener);
            pool.execute(new MonteCarloCmd(calculator));
        }
        try {
            pool.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();
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
        System.err.printf("%s%n%nUsage: MonteCarloCmd Batman|Pi sequential|parallel%n", message);
        System.exit(1);
        return;
    }

    @Override
    public void run() {
        for (int i = 0; i < ITERATIONS; i++) {
            calculator.calculate();
        }
    }
}