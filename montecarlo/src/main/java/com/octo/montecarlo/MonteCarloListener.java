package com.octo.montecarlo;

public interface MonteCarloListener {

	/**
	 * Called for each randomly selected point
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param good if the point is in the function
	 */
	void onPoint(double x, double y, boolean good);
	
	/**
	 * Call for each new calculated value
	 * @param index the index of the caller
	 * @param p number of good points
	 * @param n total number of point
	 */
	void onValue(int index, long p, long n);
}
