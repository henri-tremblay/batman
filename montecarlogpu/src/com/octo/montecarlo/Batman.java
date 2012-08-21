package com.octo.montecarlo;

import java.util.Arrays;
import java.util.Random;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

public class Batman {

    private static int ELEMENTS = 1024;
    private static int ELEMENTS_PER_WORKER = 8192;
            
    public static class BatmanKernel extends Kernel {

        final float width = 10;

        final float height = 5;

        int elements, elementsPerWorker;

        int[] result;

        float[] randx, randy;

        public boolean f(float x, float y) {
            // Wings bottom
            if (pow(x, 2.0f) / 49.0f + pow(y, 2.0f) / 9.0f - 1.0f <= 0 && abs(x) >= 4.0f && -(3.0f * sqrt(33.0f)) / 7.0f <= y && y <= 0) {
                return true;
            }
            // Wings top (with the fix of the formula there are missing parenthesis
            // in the original
            if (pow(x, 2.0f) / 49.0f + pow(y, 2.0f) / 9.0f - 1.0f <= 0 && abs(x) >= 3.0f && -(3.0f * sqrt(33.0f)) / 7.0f <= y && y >= 0) {
                return true;
            }
            // Tail
            if (-3.0f <= y
                    && y <= 0
                    && -4.0f <= x
                    && x <= 4.0f
                    && (abs(x)) / 2.0f + sqrt(1.0f - pow(abs(abs(x) - 2.0f) - 1.0f, 2.0f)) - 1.0f / 112.0f * (3.0f * sqrt(33.0f) - 7.0f)
                            * pow(x, 2.0f) - y - 3.0f <= 0) {
                return true;
            }
            // Ears outside
            if (y >= 0 && 3.0f / 4.0f <= abs(x) && abs(x) <= 1.0f && -8.0f * abs(x) - y + 9.0f >= 0) {
                return true;
            }
            // Ears inside
            if (1.0f / 2.0f <= abs(x) && abs(x) <= 3.0f / 4.0f && 3.0f * abs(x) - y + 3.0f / 4.0f >= 0 && y >= 0) {
                return true;
            }
            if (abs(x) <= 1.0f / 2.0f && y >= 0 && 9.0f / 4.0f - y >= 0) {
                return true;
            }
            // Shoulders
            if (abs(x) >= 1.0f
                    && y >= 0
                    && -(abs(x)) / 2.0f - 3.0f / 7.0f * sqrt(10.0f) * sqrt(4.0f - pow(abs(x) - 1.0f, 2.0f)) - y + (6.0f * sqrt(10.0f))
                            / 7.0f + 3.0f / 2.0f >= 0) {
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            int index = getGlobalId();

            for(int i = 0; i < elementsPerWorker; i++) {
                float x = randx[index * elements + i];
                float y = randy[index * elements + i];
                if(f(x, y)) {
                    result[index]++;
                }
            }
        }

        public BatmanKernel(int elements, int elementsPerWorker) {
            this.elements = elements;
            this.elementsPerWorker = elementsPerWorker;
            result = new int[elements];
            randx = new float[elements * elementsPerWorker];
            randy = new float[elements * elementsPerWorker];
            
            Arrays.fill(result, 0);
            Random rand = new Random();
            
            for (int i = 0; i < elements * elementsPerWorker; i++) {
                randx[i] = rand.nextFloat() * width * 2.0f - width;
                randy[i] = rand.nextFloat() * height * 2.0f - height;
            }
        }

        public void showResult() {
            int p = 0;
            int n = elements * elementsPerWorker;
            for (int i = 0; i < result.length; i++) {
                p += result[i];
            }
            float finalResult = 4.0f * width * height * p / n;
            System.out.println("Final result: " + finalResult);
            System.out.println("Iterations: " + n);
        }
    }

    public static void main(String[] _args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        int elements = Integer.getInteger("elements", ELEMENTS);
        Range range = Range.create(elements);
        int elementsPerWorker = Integer.getInteger("elementsPerWorker", ELEMENTS_PER_WORKER);
        System.out.println("elements =" + elements);
        System.out.println("elementsPerWorker =" + elementsPerWorker);
        BatmanKernel kernel = new BatmanKernel(elements, elementsPerWorker);

        kernel.execute(range);
        System.out.println("Execution mode: " + kernel.getExecutionMode());
        System.out.println("Execution time " + kernel.getAccumulatedExecutionTime());
        kernel.showResult();
        kernel.dispose();
    }

}
