package ClimateSim;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

public class Main {
    public static void main(String[] args) {
<<<<<<< HEAD
        Simulation testSim = new Simulation(30, 0.1, 200, 0.6, 0);
        testSim.run();
        testSim.graphTemp();
//        System.out.println("Hello world");
=======
//        double[] xData = new double[] { 0.0, 1.0, 2.0 };
//        double[] yData = new double[] { 2.0, 1.0, 0.0 };


//        // Create Chart
//        XYChart chart = QuickChart.getChart("Sample Chart", "X", "Y", "y(x)", xData, yData);
//
//        // Show it
//        new SwingWrapper(chart).displayChart();


    }

    public static void runBlackBodySpectrum(double T){
        double[] waveLength = new double[30];
        double[] intensityBlackBody = new double[30];

        for(int i = 0;i<30;i++){
            waveLength[i] = i;
        }

        Simulation.blackBodyTest(T,intensityBlackBody);

        // Create Chart
        XYChart chart = QuickChart.getChart("BlackBody", "Wavelength (micro meter)", "Intensity", "y(x)", waveLength, intensityBlackBody);

        // Show it
        new SwingWrapper(chart).displayChart();
>>>>>>> ae3a191984db07799cb4c2b86830b83712c343a7
    }
}
