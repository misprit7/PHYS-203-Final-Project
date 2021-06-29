package ClimateSim;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import java.io.IOException;
import java.util.List;

public class Simulation {

    private static final double STEFAN_BOLTZMANN = 5.67e-8;
    private static final double GAS_CONSTANT = 8.3145;
    private static final double PLANCK_CONSTANT = 6.626e-34;
    private static final double LIGHT_SPEED = 3.0e8;
    private static final double BOLTZMANN_CONSTANT = 1.381e-23;
    private static final double AVAGADRO_CONSTANT = 6.02e23;

    private static final double YEARS_TO_SEC = 365 * 24 * 60 * 60;


    // In K
    private static final double SUN_TEMP = 5778;
    // In m
    private static final double EARTH_SUN_DIST = 152080000000.0;
    // In m
    private static final double EARTH_RAD = 6371000;
    // In m
    private static final double SUN_RAD = 696340000;
    //In m
    private static final double SCALE_HEIGHT = 7310;

    // In K
    static final double ATM_TEMP_OFFSET = 66;

    // Effective global heat capacity, in J m^-2 K^-2
    // https://agupubs.onlinelibrary.wiley.com/doi/10.1029/2007JD008746
    // In table 3, note the massive uncertainty
    private static final double EARTH_SPECIFIC_HEAT = 0.53e9;

    private static final double WAVELENGTH_STEP = 0.01;

    private int endYear;
    private double timeStep;

    private double initT;
    private double initCO2;
    private double slopeCO2;

    public double[] time;
    public double[] temperature;
    public double[] densityCO2;
    public double[] heatOut;
    boolean useGreenhouse;

    /**
     * @param endYear the end date of the simulation
     * @param timeStep the timestep for each tick, in years
     * @param initT the initial temperature, int K
     * @param initCO2 the inital CO2 density, in moles/cm^3
     * @param slopeCO2 assuming linear gradient, how fast initCO2 is changing
     */
    public Simulation(int endYear, double timeStep, double initT, double initCO2, double slopeCO2, boolean useGreenhouse){
        this.endYear = endYear;
        this.timeStep = timeStep;

        this.initT= initT;
        this.initCO2 = initCO2;
        this.slopeCO2 = slopeCO2;
        this.useGreenhouse = useGreenhouse;

        int arrLen = (int)(endYear/timeStep);
        this.temperature = new double[arrLen];
        this.densityCO2 = new double[arrLen];
        this.heatOut = new double[arrLen];
        this.time = new double[arrLen];
    }

    /**
     * Runs the simulation
     */
    public void run(){

        double Hin = GetHin();
        double earthArea = 4 * Math.PI * Math.pow(EARTH_RAD, 2);

        // Here t is the length of time since beginning of simulation
        int i = 0;
        for(double t = 0; t < this.endYear; ++i, t += this.timeStep){
            // Set initial constants
            double earthT = i==0 ? this.initT : temperature[i - 1];
            this.densityCO2[i] = this.initCO2+this.slopeCO2*t;
            this.time[i] = t;

            // Get H2O vapour pressure
            double pressureH2O = GetH2O(earthT);
            double densityH2O = pressureH2O / GAS_CONSTANT / earthT / 1e6;

            // Wavelength calculations
            double Hout = GetHout(earthT, densityH2O, densityCO2[i], this.useGreenhouse, null, null);

            // Heat calculations
            double Hnet = Hin-Hout;
            double Enet = Hnet * timeStep * YEARS_TO_SEC;

            this.temperature[i] = earthT + Enet / (EARTH_SPECIFIC_HEAT * earthArea);

        }

    }

    /**
     * Gets heat going out of earth
     * @param earthT temperature of earth in K
     * @param densityH2O molar density of water, in mol/cm^3
     * @param densityCO2 molar density of CO2, in mol/cm^3
     * @param wavelengths wavelengths for data plotting returned, if wanted must be of length 100000, can be null
     * @param spectrum spectrum for data plotting returned, can be null
     * @return total heat out
     */
    public static double GetHout(double earthT, double densityH2O, double densityCO2, boolean useGreenhouse, double[] wavelengths, double[] spectrum){
        double earthArea = 4 * Math.PI * Math.pow(EARTH_RAD, 2);
        double atmT = earthT - ATM_TEMP_OFFSET;
        double Hout = 0;
        int i = 0;
        for(double wavelength = 0.01; wavelength < 1000; ++i, wavelength += WAVELENGTH_STEP){
            double blackbodyIntensityEarth = GetBlackbody(earthT, wavelength/1e6);
            double blackbodyIntensityAtm = GetBlackbody(atmT, wavelength/1e6);
            double blackbodyIntensity = blackbodyIntensityEarth;

            if(useGreenhouse) {
                if (wavelength < 8 || wavelength > 19) {
                    blackbodyIntensity = blackbodyIntensityAtm;
                } else if (wavelength > 8 && wavelength < 14) {
                    blackbodyIntensity = blackbodyIntensityEarth;
                } else if (wavelength > 14 && wavelength < 19) {
                    double sigmaH2O = 4.045e-22;
                    double sigmaCO2 = wavelength > 14.3 && wavelength < 15.6 ? 0.613e-18 : 0;

                    double transmitH2O =
                        Math.exp(-densityH2O * AVAGADRO_CONSTANT * sigmaH2O * SCALE_HEIGHT);
                    double transmitCO2 =
                        Math.exp(-densityCO2 * AVAGADRO_CONSTANT * sigmaCO2 * SCALE_HEIGHT);
                    double transmitTotal = transmitCO2 * transmitH2O;

                    blackbodyIntensity = blackbodyIntensityEarth * transmitTotal +
                        blackbodyIntensityAtm * (1 - transmitTotal);
                }
            } else {
                blackbodyIntensity = blackbodyIntensityEarth;
            }

            double dI = blackbodyIntensity;
            if(wavelengths != null && spectrum != null) {
                wavelengths[i] = wavelength;
                spectrum[i] = dI;
            }
            Hout += dI * earthArea * WAVELENGTH_STEP / 1e6;
        }
        return Hout;
    }

    /**
     * Gets power in from sun
     * @return Hin in W/m^2
     */
    private static double GetHin(){
        double earthDiskArea = Math.PI * Math.pow(EARTH_RAD, 2);
        double sunArea = 4 * Math.PI * Math.pow(SUN_RAD, 2);
        double SunSphereArea = 4*Math.PI*Math.pow(EARTH_SUN_DIST, 2);
        return STEFAN_BOLTZMANN * Math.pow(SUN_TEMP, 4) * earthDiskArea * sunArea / SunSphereArea;
    }

    /**
     * Gets amount of H2O in atmosphere from vapour pressure
     * Third equation:
     * https://en.wikipedia.org/wiki/Vapour_pressure_of_water
     * @param T the temperature to look at
     * @return The vapour pressure in Pa
     */
    private static double GetH2O(double T){
        return 1e3 * 0.61094 * Math.exp(17.625 * (T-273) / ((T-273) + 243.04));
    }

    /**
     * Gets blackbody intensity
     * @param T temperature in K
     * @param wavelength wavelength in m
     * @return the blackbody intensity for this wavelength in W/m^2
     */
    private static double GetBlackbody(double T, double wavelength) {
        return 2.78 * Math.PI * PLANCK_CONSTANT * Math.pow(LIGHT_SPEED, 2) /
            Math.pow(wavelength, 5) / (Math.exp(PLANCK_CONSTANT * LIGHT_SPEED /
            (wavelength * BOLTZMANN_CONSTANT * T)) - 1);
    }

    /**
     * Graphs data
     * @param title title of graph
     * @param xAxis x axis title
     * @param yAxis y axis title
     * @param dataNames names of data series
     * @param xData x data of plots
     * @param yData list of y data to plot
     */
    public static void graphData(String title, String xAxis, String yAxis, List<String> dataNames, double[] xData, List<double[]> yData, boolean save){
        XYChart chart = new XYChartBuilder().width(800).height(600)
            .title(title).xAxisTitle(xAxis)
            .yAxisTitle(yAxis).theme(Styler.ChartTheme.GGPlot2).build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSE);
        for(int i = 0; i < dataNames.size(); ++i) {
            chart.addSeries(dataNames.get(i), xData, yData.get(i));
        }
        if (save) {
            try {
                BitmapEncoder
                    .saveBitmap(chart, "./charts/" + title, BitmapEncoder.BitmapFormat.PNG);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new SwingWrapper(chart).displayChart();
    }

    public static void blackBodyTest(double T, double[] inputArray){
        int i = 0;
        for (double wavelength=0.01; wavelength<1000; ++i, wavelength+=WAVELENGTH_STEP){
            inputArray[i] = GetBlackbody(T,wavelength/1e6);
        }
    }

}
