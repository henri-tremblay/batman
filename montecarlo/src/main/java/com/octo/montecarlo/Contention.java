package com.octo.montecarlo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Contention {

    public static void main(String[] args) {
        final Object o = new Object();
        final Set<String> set = Collections.synchronizedSet(new HashSet<String>());
        
        ExecutorService threadExecutor = Executors.newFixedThreadPool(10);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                while(System.currentTimeMillis() < now + 60_000) {
                    synchronized (o) {
                        set.add("" + System.currentTimeMillis());
                        try {
                            Thread.sleep(10);
                        } catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        
        for(int i = 0; i < 10; i++) {
            threadExecutor.execute(r);
        }
        System.out.println("Started!");
        threadExecutor.shutdown();
    }

}
