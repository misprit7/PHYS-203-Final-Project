package ClimateSim;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main {

    private static final boolean SAVE_GRAPHS = true;

    public static void main(String[] args) {
        generateTempEqm();
        generateBlackbody();
        generateCO2();
    }


    public static void generateTempEqm() {
        Simulation H2OSim = new Simulation(30, 0.1, 270, 9.6e15 / 6.02e23, 0, true);
        H2OSim.run();
        Simulation noH2OSim = new Simulation(30, 0.1, 270, 9.6e15 / 6.02e23, 0, false);
        noH2OSim.run();
        Simulation.graphData("Temperature of Earth Coming to Equilibrium", "Time (years)",
            "Temperature (K)",
            Arrays.asList("Greenhouse Gases", "No Greenhouse Gases"), H2OSim.time,
            Arrays.asList(H2OSim.temperature, noH2OSim.temperature), SAVE_GRAPHS);
    }

    public static void generateBlackbody() {
        double[] wavelengths = new double[100000];
        double[] spectrumGreenhouse = new double[100000];
        Simulation.GetHout(273 + 14, 6.69e-7, 1.6e-8, true, wavelengths, spectrumGreenhouse);

        double[] spectrumEarth = new double[100000];
        double[] spectrumAtm = new double[100000];
        Simulation.blackBodyTest(273 + 14, spectrumEarth);
        Simulation.blackBodyTest(273 + 14 - Simulation.ATM_TEMP_OFFSET, spectrumAtm);


        int beginIndex = 0;
        int endIndex = 3000;
        Simulation.graphData("Earth Blackbody Spectrum", "Wavelength (um)", "Intensity (W/m^2/m)",
            Arrays.asList("Earth Spectrum", "Edge of Atmosphere Spectrum", "Greenhouse Spectrum"),
            Arrays.copyOfRange(wavelengths, beginIndex, endIndex),
            Arrays.asList(Arrays.copyOfRange(spectrumEarth, beginIndex, endIndex),
                Arrays.copyOfRange(spectrumAtm, beginIndex, endIndex),
                Arrays.copyOfRange(spectrumGreenhouse, beginIndex, endIndex)), SAVE_GRAPHS);
    }

    public static void generateCO2() {
        double maxCO2 = 15e14 / 6.02e23;
        int divisions = 20;

        List<Simulation> sims = new ArrayList<>();
        double[] densitiesCO2 = new double[divisions];
        double[] eqmTemp = new double[divisions];

        for (int i = 0; i < divisions; ++i) {
            double CO2 = maxCO2 / divisions * i;
            Simulation sim = new Simulation(30, 0.5, 281, CO2, 0, true);
            sim.run();
            sims.add(sim);
            densitiesCO2[i] = CO2;
            eqmTemp[i] = sim.temperature[sim.temperature.length-1];
        }

        Simulation.graphData("Equilibrium Temperatures of Earth by CO2 Concentration",
            "CO2 Molar Density (mol/cm^3)", "Equilibrium Temperature (K)",
            Collections.singletonList("CO2 concentrations"), densitiesCO2,
            Collections.singletonList(eqmTemp), SAVE_GRAPHS);
    }


    public static void runBlackBodySpectrum(double T) {
        double[] waveLength = new double[30];
        double[] intensityBlackBody = new double[30];

        for (int i = 0; i < 30; i++) {
            waveLength[i] = i;
        }

        Simulation.blackBodyTest(T, intensityBlackBody);

        // Create Chart
        XYChart chart = QuickChart
            .getChart("BlackBody", "Wavelength (micro meter)", "Intensity", "y(x)", waveLength,
                intensityBlackBody);

        // Show it
        new SwingWrapper(chart).displayChart();
    }
}
