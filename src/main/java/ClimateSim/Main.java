package ClimateSim;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

public class Main {
    public static void main(String[] args) {
        Simulation testSim = new Simulation(30, 0.1, 200, 0.6, 0);
        testSim.run();
        testSim.graphTemp();
//        System.out.println("Hello world");
    }
}
