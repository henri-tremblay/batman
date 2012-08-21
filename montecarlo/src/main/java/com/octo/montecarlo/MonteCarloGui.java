package com.octo.montecarlo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

public class MonteCarloGui implements Runnable {

    private static final Dimension SCREEN_DIMENSION = Toolkit.getDefaultToolkit().getScreenSize();

    private static final int SLEEP = 1;

    private MonteCarloCalculator calculator;

    private static volatile boolean paused = false;

    private static Object mutex = new Object();

    public MonteCarloGui(MonteCarloCalculator calculator) {
        this.calculator = calculator;
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            usage("Algoritm not provided");
            return;
        }

        final String algoName = args[0];
        final Constructor<MonteCarloCalculator> constructor = retrieveAlgorithm(algoName);
        // This instance is only used to get the variables specific to this calculator
        final MonteCarloCalculator calculator = instantiateAlgorithm(constructor, Integer.MIN_VALUE);

        final AtomicReference<Double> val = new AtomicReference<>(0.0);
        final AtomicLong l = new AtomicLong(0);

        final BufferedImage image = new BufferedImage(calculator.getWindowDimension().width,
                calculator.getWindowDimension().height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, calculator.getWindowDimension().width, calculator.getWindowDimension().height);
        g.dispose();

        MonteCarloListener listener = new MonteCarloListener() {

            @Override
            public void onPoint(double x, double y, boolean good) {
                int dotX = (int) (x * calculator.getWindowDimension().width / 2.0 / calculator.getPositiveRange().width + calculator
                        .getPositionOffset().width);
                int dotY = (int) (y * calculator.getWindowDimension().height / 2.0 / calculator.getPositiveRange().height + calculator
                        .getPositionOffset().height);

                // Now the image is upside down, so reverse it
                dotX = calculator.getWindowDimension().width - dotX;
                dotY = calculator.getWindowDimension().height - dotY;

                Graphics2D g = image.createGraphics();
                g.setColor(good ? Color.BLUE : Color.YELLOW);
                g.drawLine(dotX, dotY, dotX, dotY);
                g.dispose();
            }

            @Override
            public void onValue(int index, long p, long n) {
                val.set(calculator.getFactor() * p / n);
                l.set(n);
            }

        };
        calculator.setListener(listener);

        JFrame frame = new JFrame("Monte Carlo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setFocusable(true);
        frame.getContentPane().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    paused = !paused;
                    System.out.println(paused ? "Paused" : "Unpaused");
                    if (!paused) {
                        synchronized (mutex) {
                            mutex.notifyAll();
                        }
                    }
                } else if (e.getKeyChar() == 'c') {
                    check(image);
                }
            }

            private void check(BufferedImage image) {
                int total = image.getHeight() * image.getWidth();
                int black = 0, blue = 0;
                for (int i = 0; i < image.getHeight(); i++) {
                    for (int j = 0; j < image.getWidth(); j++) {
                        int rgb = image.getRGB(j, i);
                        if (rgb == Color.BLACK.getRGB()) {
                            black++;
                        } else if (rgb == Color.BLUE.getRGB()) {
                            blue++;
                        }
                    }
                }
                System.out.println("Percentage of black: " + ((double) black / (double) total));
                System.out.println("Percentage of blue: " + ((double) blue / (double) total));
            }
        });

        final JPanel panel = new JPanel() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(image, 0, 0, null);
            }

        };

        panel.setPreferredSize(calculator.getWindowDimension());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);

        final JTextField fld = new JTextField();
        frame.getContentPane().add(fld, BorderLayout.SOUTH);

        frame.setLocation((SCREEN_DIMENSION.width - calculator.getWindowDimension().width) / 2,
                (SCREEN_DIMENSION.height - calculator.getWindowDimension().height) / 2);
        frame.pack();
        frame.setVisible(true);

        ActionListener taskPerformer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fld.setText(String.format("%s = %1.10f on iteration %d", algoName, val.get(), l.get()));
                panel.repaint();
            }
        };
        Timer timer = new Timer(20, taskPerformer);
        timer.setCoalesce(true);
        timer.start();

        MonteCarloGui m = new MonteCarloGui(calculator);
        m.run();
    }

    @SuppressWarnings("unchecked")
    private static Constructor<MonteCarloCalculator> retrieveAlgorithm(String prefix) {
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
    
    private static MonteCarloCalculator instantiateAlgorithm(Constructor<MonteCarloCalculator> cons, int index) {
    	try {
            return cons.newInstance(index);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void usage(String message) {
        System.err.printf("%s%n%nUsage: MonteCarloGui Batman|Pi%n", message);
        System.exit(1);
        return;
    }

    @Override
    public void run() {
        int i = 0;
        while (true) {
            if (paused) {
                try {
                    synchronized (mutex) {
                        mutex.wait();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            calculator.calculate();
            if (SLEEP == 0 || ++i != 10) {
                continue;
            }
            try {
                Thread.sleep(SLEEP);
            } catch (InterruptedException e) {
                break;
            }
            i = 0;
        }
    }
}
