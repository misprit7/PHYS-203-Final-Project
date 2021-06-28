package ClimateSim;
import org.knowm.xchart.PieChart;

public class Simulation {

    private static final double  STEFAN_BOLTZMANN = 5.67e-8;
    private static final double GAS_CONSTANT = 8.3145;
    private static final double PLANCK_CONSTANT = 6.626e-34;
    private static final double LIGHT_SPEED = 3.0e8;
    private static final double BOLTZMANN_CONSTANT = 1.381e-23;

    // In K
    private static final double SUN_TEMP = 5778;
    // In m
    private static final double EARTH_SUN_DIST = 152080000000.0;
    // In m
    private static final double EARTH_RAD = 6371000;
    //In m
    private static final double SCALE_HEIGHT = 7310;

    // In K
    // Need an actual number for this
    private static final double ATM_TEMP_OFFSET = 66;

    // Effective global heat capacity, in GJ m^-2 K^-2
    // https://agupubs.onlinelibrary.wiley.com/doi/10.1029/2007JD008746
    // In table 3, note the massive uncertainty
    private static final double EARTH_SPECIFIC_HEAT = 0.53;

    private static final double WAVELENGTH_STEP = 0.01;

    private int endYear;
    private double timeStep;

    private int initT;
    private int initCO2;
    private int slopeCO2;

    public double[] temperature;
    public double[] densityCO2;
    public double[] heatOut;

    /**
     * @param endYear the end date of the simulation
     * @param timeStep the timestep for each tick, in years
     * @param initT the initial temperature, int K
     * @param initCO2 the inital CO2 density, in moles/m^3
     * @param slopeCO2 assuming linear gradient, how fast initCO2 is changing
     */
    public Simulation(int endYear, double timeStep, int initT, int initCO2, int slopeCO2){
        this.endYear = endYear;
        this.timeStep = timeStep;

        this.initT= initT;
        this.initCO2 = initCO2;
        this.slopeCO2 = slopeCO2;
    }

    /**
     * Runs the simulation
     */
    public void run(){

        double Hin = GetHin();

        // Here t is the length of time since beginning of simulation
        for(int t = 1, i = 1; t < this.endYear; ++i, t += this.timeStep){
            // Set initial constants
            double earthT = i==0 ? this.initT : temperature[i - 1];
            double atmT = earthT - ATM_TEMP_OFFSET;
            this.densityCO2[i] = this.initCO2+this.slopeCO2*t;

            // Get H2O vapour pressure
            double pressureH2O = GetH2O(earthT);
            double densityH2O = pressureH2O / GAS_CONSTANT / earthT;

            // Wavelength calculations
            double Hout = 0;
            for(double wavelength = 2; wavelength < 30; wavelength += WAVELENGTH_STEP){
                double blackbodyIntensityEarth = GetBlackbody(earthT, wavelength);
                double blackbodyIntensityAtm = GetBlackbody(atmT, wavelength);
                double blackbodyIntensity = blackbodyIntensityAtm;

                if(wavelength < 8 || wavelength > 19){
                    blackbodyIntensity = blackbodyIntensityAtm;
                } else if (wavelength > 8 && wavelength < 14){
                    blackbodyIntensity = blackbodyIntensityEarth;
                } else if (wavelength > 14 && wavelength < 19){
                    double sigmaH2O = 4.045e-21;
                    double sigmaCO2 = wavelength > 14.3 && wavelength < 15.6 ? 0.613e-18 : 0;
                    double transmitH2O = Math.exp(-densityH2O * sigmaH2O * SCALE_HEIGHT);
                    double transmitCO2 = Math.exp(-densityCO2[i] * sigmaCO2 * SCALE_HEIGHT);
                    double transmitTotal = transmitCO2 * transmitH2O;
                    blackbodyIntensity = blackbodyIntensityEarth * transmitTotal +
                        blackbodyIntensityAtm * (1-transmitTotal);
                }
                Hout += blackbodyIntensity * WAVELENGTH_STEP;
            }

            // Heat calculations
            double Hnet = Hin-Hout;
            double Enet = Hnet * timeStep;

            double earthArea = 4 * Math.PI * Math.pow(EARTH_RAD, 2);
            this.temperature[i] = earthT + Enet / (EARTH_SPECIFIC_HEAT * earthArea);

        }

    }

    /**
     * Gets power in from sun
     * @return Hin in W/m^2
     */
    private static double GetHin(){
        double earthArea = Math.PI * Math.pow(EARTH_RAD, 2);
        double SunSphereArea = 4*Math.PI*Math.pow(EARTH_SUN_DIST, 2);
        return STEFAN_BOLTZMANN * Math.pow(SUN_TEMP, 4) * earthArea / SunSphereArea;
    }

    /**
     * Gets amount of H2O in atmosphere from vapour pressure
     * Third equation:
     * https://en.wikipedia.org/wiki/Vapour_pressure_of_water
     * @param T the temperature to look at
     * @return The vapour pressure in kPa
     */
    private static double GetH2O(double T){
        return 0.61094 * Math.exp(17.625 * T / (T + 243.04));
    }

    /**
     * Gets blackbody intensity
     * @param T temperature in K
     * @param wavelength wavelength in m
     * @return the blackbody intensity for this wavelength
     */
    private static double GetBlackbody(double T, double wavelength){
        return 2 * Math.PI * PLANCK_CONSTANT * Math.pow(LIGHT_SPEED, 2) /
            Math.pow(wavelength, 5) / (Math.exp(PLANCK_CONSTANT * LIGHT_SPEED /
            (wavelength * BOLTZMANN_CONSTANT * T))-1);
    }

    public static void blackBodyTest(double T, double[] inputArray){
        for (int i=1;i<30;i++){
            inputArray[i] = GetBlackbody(T,i);
        }
    }

}
