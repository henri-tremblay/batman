package com.octo.montecarlo;

import java.util.Arrays;
import java.util.Random;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

public class Batman {

    public static class BatmanKernel extends Kernel {

        static final int multiplier = 3559;

        static final int addend = 0xB;

        static final int mask = (1 << 24) - 1;

        final float width = 10;

        final float height = 5;

        int size;

        long[] result;

        int[] seed;
        int[] temp;

        public boolean f(float x, float y) {
            
            // Wings bottom
            boolean result = (pow(x, 2.0f) / 49.0f + pow(y, 2.0f) / 9.0f - 1.0f <= 0 && abs(x) >= 4.0f
                    && -(3.0f * sqrt(33.0f)) / 7.0f <= y && y <= 0);

            // Wings top (with the fix of the formula there are missing parenthesis in the original)
            result |= (pow(x, 2.0f) / 49.0f + pow(y, 2.0f) / 9.0f - 1.0f <= 0 && abs(x) >= 3.0f
                    && -(3.0f * sqrt(33.0f)) / 7.0f <= y && y >= 0);

            // Tail
            result |= (-3.0f <= y && y <= 0 && -4.0f <= x && x <= 4.0f && (abs(x)) / 2.0f
                    + sqrt(1.0f - pow(abs(abs(x) - 2.0f) - 1.0f, 2.0f)) - 1.0f / 112.0f * (3.0f * sqrt(33.0f) - 7.0f)
                    * pow(x, 2.0f) - y - 3.0f <= 0);

            // Ears outside
            result |= (y >= 0 && 3.0f / 4.0f <= abs(x) && abs(x) <= 1.0f && -8.0f * abs(x) - y + 9.0f >= 0);

            // Ears inside
            result |= (1.0f / 2.0f <= abs(x) && abs(x) <= 3.0f / 4.0f && 3.0f * abs(x) - y + 3.0f / 4.0f >= 0 && y >= 0);

            // Chest
            result |= (abs(x) <= 1.0f / 2.0f && y >= 0 && 9.0f / 4.0f - y >= 0);

            // Shoulders
            result |= (abs(x) >= 1.0f && y >= 0 && -(abs(x)) / 2.0f - 3.0f / 7.0f * sqrt(10.0f)
                    * sqrt(4.0f - pow(abs(x) - 1.0f, 2.0f)) - y + (6.0f * sqrt(10.0f)) / 7.0f + 3.0f / 2.0f >= 0);

            return result;
        }

        public float nextFloat(int index) {
            return next(index) / ((float) (1 << 24));
        }

        private int next(int index) {
            int seed = this.seed[index];
            int nextseed = (seed * multiplier + addend) & mask;
            this.seed[index] = nextseed;
            return nextseed;
        }

        @Override
        public void run() {
            int index = getGlobalId();
            float x = nextFloat(index) * width * 2.0f - width;
            float y = nextFloat(index) * height * 2.0f - height;
            temp[index]++;
            if(f(x, y)) {
                result[index]++;
            }
        }

        public BatmanKernel(int size) {
            this.size = size;
        }

        public void init() {

            result = new long[size];
            Arrays.fill(result, 0);

            seed = new int[size];
            temp = new int[size];
            Arrays.fill(temp, 0);
            
            Random rand = new Random(1);
            for (int i = 0; i < size; i++) {
                seed[i] = rand.nextInt();
            }
        }

        public void showResult(long passes) {
            long p = 0;
            long n = size * passes;
            for (int i = 0; i < result.length; i++) {
                p += result[i];
            }
            
            double finalResult = 4.0 * width * height * p / n;
            System.out.println("Final result: " + finalResult);
            System.out.println("Iterations: " + n);
        }
    }

    public static void main(String[] _args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        int size = Integer.getInteger("size", 4096);
        int iterations = Integer.getInteger("iterations", 20000);
        
        System.out.printf("size = %d%n", size);
        System.out.printf("iterations = %d%n", iterations);
        System.out.println();

        Range range = Range.create(size);
        
        BatmanKernel kernel = new BatmanKernel(size);
        kernel.init();
        
        long start = System.currentTimeMillis();
        kernel.execute(range, iterations);
        long stop = System.currentTimeMillis();
        
        System.out.printf("Execution mode: %s%n", kernel.getExecutionMode());
        System.out.printf("Time: %d ms%n", (stop - start));
        kernel.showResult(iterations);
        
        kernel.dispose();
    }

}
