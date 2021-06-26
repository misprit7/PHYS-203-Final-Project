package ClimateSim;

public class Simulation {

    private final int CURRENT_YEAR =  2021;

    private int endDate;
    private double timeStep;

    private int initT;
    private int initCO2;
    private int slopeCO2;

    public double[] temperature;
    public double[] densityCO2;
    public double[] heatOut;

    /**
     * @param endDate the end date of the simulation
     * @param timeStep the timestep for each tick, in years
     * @param initT the initial temperature, int K
     * @param initCO2 the inital CO2 density, in moles/m^3
     * @param slopeCO2 assuming linear gradient, how fast initCO2 is changing
     */
    public Simulation(int endDate, double timeStep, int initT, int initCO2, int slopeCO2){
        this.endDate = endDate;
        this.timeStep = timeStep;

        this.initT= initT;
        this.initCO2 = initCO2;
        this.slopeCO2 = slopeCO2;
    }

    /**
     * Runs the simulation
     */
    public void run(){

        for(int t = 0; t < this.endDate-this.CURRENT_YEAR; t += this.timeStep){
            this.densityCO2[this.densityCO2.length] = this.initCO2+this.slopeCO2*t;
        }

    }

}
