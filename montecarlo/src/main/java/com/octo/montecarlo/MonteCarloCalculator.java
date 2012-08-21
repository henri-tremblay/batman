package com.octo.montecarlo;

import java.awt.Dimension;

public abstract class MonteCarloCalculator {

	protected MonteCarloListener listener;
	
	protected final int index;

    public MonteCarloCalculator(int index) {
    	this.index = index;
    }

	public MonteCarloCalculator(int index, MonteCarloListener listener) {
		this(index);
		this.listener = listener;
	}

    public void setListener(MonteCarloListener listener) {
        this.listener = listener;
    }

    public abstract void calculate();

    public abstract Dimension getWindowDimension();

    public abstract Dimension getPositiveRange();

    public abstract Dimension getPositionOffset();
    
    /**
     * @return factor multiplying p/n to get the result
     */
    public abstract double getFactor();
}
