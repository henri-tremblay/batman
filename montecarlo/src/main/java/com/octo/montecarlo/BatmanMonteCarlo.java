package com.octo.montecarlo;

import static java.lang.Math.*;

import java.awt.Dimension;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BatmanMonteCarlo extends MonteCarloCalculator {

    private static final Dimension WINDOW_DIMENSION = new Dimension(1600, 600);

    private static final Dimension POSITIVE_RANGE = new Dimension(10, 5);

    private static final Dimension POSITION_OFFSET = new Dimension(800, 300);

    private static final double FACTOR = 4.0 * POSITIVE_RANGE.width * POSITIVE_RANGE.height;

    // x^2/49+y^2/9-1<=0 and abs(x)>=4 and -(3 sqrt(33))/7<=y<=0 or abs(x)>=3 and y>=0 or -3<=y<=0 and -4<=x<=4 and (abs(x))/2+sqrt(1-(abs(abs(x)-2)-1)^2)-1/112 (3 sqrt(33)-7) x^2-y-3<=0 or y>=0 and 3/4<=abs(x)<=1 and -8 abs(x)-y+9>=0 or 1/2<=abs(x)<=3/4 and 3 abs(x)-y+3/4>=0 and y>=0 or abs(x)<=1/2 and y>=0 and 9/4-y>=0 or abs(x)>=1 and y>=0 and -(abs(x))/2-3/7 sqrt(10) sqrt(4-(abs(x)-1)^2)-y+(6 sqrt(10))/7+3/2>=0
    // A = 955/48-2/7 (-3 sqrt(10)+2 sqrt(33)+7 pi+3 sqrt(10) pi)+21 cos^(-1)(3/7)+21 cos^(-1)(4/7) = 48.4243

    /** Number of performed iterations */
    private long n = 0;

    /** Number of "in" iterations */
    private long p = 0;

    /** Each calculator will run in a given thread. We improve performance by used a ThreadLocalRandom which impose no synchronization */
    private Random rand = ThreadLocalRandom.current();

    public BatmanMonteCarlo(int index) {
    	super(index);
    }

    public BatmanMonteCarlo(int index, MonteCarloListener listener) {
        super(index, listener);
    }

    @Override
    public void calculate() {
        // random position in range
        double x = rand.nextDouble() * POSITIVE_RANGE.width * 2.0 - POSITIVE_RANGE.width;
        double y = rand.nextDouble() * POSITIVE_RANGE.height * 2.0 - POSITIVE_RANGE.height;

        // one more iteration
        n++;

        // Check if we are in the batman sign;
        boolean good = f(x, y);
        if (good) {
            // in the sign
            p++;
        }

        listener.onPoint(x, y, good);
        
        // statistically, we have p /n chances to be in the batman.
        // the square has an area of positive_range.width * positive_range.height * 4
        // so for 100% of the square, we have the full area
        // for p/n of the square, we have z
        // z = area * p/n
        listener.onValue(index, p, n);
    }

    boolean f(double x, double y) {
        // Wings bottom
        if (pow(x, 2.0) / 49.0 + pow(y, 2.0) / 9.0 - 1.0 <= 0 && abs(x) >= 4.0 && -(3.0 * sqrt(33.0)) / 7.0 <= y && y <= 0) {
            return true;
        }
        // Wings top (with the fix of the formula there are missing parenthesis in the original
        if (pow(x, 2.0) / 49.0 + pow(y, 2.0) / 9.0 - 1.0 <= 0 && abs(x) >= 3.0 && -(3.0 * sqrt(33.0)) / 7.0 <= y && y >= 0) {
            return true;
        }
        // Tail
        if (-3.0 <= y
                && y <= 0
                && -4.0 <= x
                && x <= 4.0
                && (abs(x)) / 2.0 + sqrt(1.0 - pow(abs(abs(x) - 2.0) - 1.0, 2.0)) - 1.0 / 112.0 * (3.0 * sqrt(33.0) - 7.0)
                        * pow(x, 2.0) - y - 3.0 <= 0) {
            return true;
        }
        // Ears outside
        if (y >= 0 && 3.0 / 4.0 <= abs(x) && abs(x) <= 1.0 && -8.0 * abs(x) - y + 9.0 >= 0) {
            return true;
        }
        // Ears inside
        if (1.0 / 2.0 <= abs(x) && abs(x) <= 3.0 / 4.0 && 3.0 * abs(x) - y + 3.0 / 4.0 >= 0 && y >= 0) {
            return true;
        }
        // Chest
        if (abs(x) <= 1.0 / 2.0 && y >= 0 && 9.0 / 4.0 - y >= 0) {
            return true;
        }
        // Shoulders
        if (abs(x) >= 1.0
                && y >= 0
                && -(abs(x)) / 2.0 - 3.0 / 7.0 * sqrt(10.0) * sqrt(4.0 - pow(abs(x) - 1.0, 2.0)) - y + (6.0 * sqrt(10.0)) / 7.0
                        + 3.0 / 2.0 >= 0) {
            return true;
        }
        return false;
    }

    @Override
    public Dimension getWindowDimension() {
        return WINDOW_DIMENSION;
    }

    @Override
    public Dimension getPositionOffset() {
        return POSITION_OFFSET;
    }

    @Override
    public Dimension getPositiveRange() {
        return POSITIVE_RANGE;
    }

	@Override
	public double getFactor() {
		return FACTOR;
	}

}
