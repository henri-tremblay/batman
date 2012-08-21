package com.octo.montecarlo;

import java.awt.Dimension;
import java.util.Random;

public class PiMonteCarlo extends MonteCarloCalculator {

    private static final Dimension WINDOW_DIMENSION = new Dimension(400, 400);

    private static final Dimension POSITIVE_RANGE = new Dimension(1, 1);

    private static final Dimension POSITION_OFFSET = new Dimension(200, 200);

	private long n = 0;
	private long p = 0;

	private Random rand = new Random();

    public PiMonteCarlo(int index) {
    	super(index);
    }

	public PiMonteCarlo(int index, MonteCarloListener listener) {
		super(index, listener);
	}

	@Override
    public void calculate() {
        // random position in the circle of radius 1 centered at the origin
        double x = rand.nextDouble() * 2.0 - 1.0;
        double y = rand.nextDouble() * 2.0 - 1.0;

		// one more iteration
		n++;

		// Check if we are in the circle using Pythagore
        boolean good = (x * x + y * y <= 1.0);
		if (good) {
			// in the circle
			p++;
		}

		listener.onPoint(x, y, good);
        // statistically, we have p /n chances to be in the circle.
        // the area of a circle is pi*r*r. In our case, r = 1 so area = pi
        // the square around the circle has an area of 4
        // so the ratio of pi/4 is p/n
        // which means that pi = 4 p/n
        listener.onValue(index, p, n);
	}

    @Override
    public Dimension getWindowDimension() {
        return WINDOW_DIMENSION;
    }

    @Override
    public Dimension getPositiveRange() {
        return POSITIVE_RANGE;
    }

    @Override
    public Dimension getPositionOffset() {
        return POSITION_OFFSET;
    }

	@Override
	public double getFactor() {
		return 4.0;
	}
}
