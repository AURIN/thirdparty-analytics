package au.org.aurin.ands.emp;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Name;
import oms3.annotations.Out;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.uq.aurin.interfaces.Statistics;
import au.edu.uq.aurin.logging.Rlogger;
import au.edu.uq.aurin.util.Rscript;
import au.edu.uq.aurin.util.Rserve;
import au.edu.uq.aurin.util.StatisticsException;
import au.org.aurin.workflow.AurinComponent;
import au.org.aurin.workflow.EnvVars;

public class WardsClustering extends AurinComponent implements Statistics {
  
  private static final Logger LOGGER = LoggerFactory
      .getLogger(WardsClustering.class);
  
  @In
  @Name("Wards Clustering Input Dataset")
  @Description("Select a spatial dataset.")
  public RConnection cIn;
  /**
   * {@link RConnection} A valid connection to a running {@link Rserve}
   * instance
   */
  
  @In
  @Name("Wards Clustering Geo-Distance Threshold (km)")
  @Description("Set maximum distance in kilometres beyond which polygons will not merge.")
  public int geodisthreshold = 20;
  /**
   * {@link int} Input Integer for geo-distance threshold
   */
  
  @In
  @Name("Wards Clustering Target Cluster Number")
  @Description("Set minimum cluster number at which algorithm will stop.")
  public int targetclusternum = 1;
  /**
   * {@link int} Input Integer for target cluster number
   */
  
  @In
  @Name("Wards Clustering Non-Spatial Attribute Selection")
  @Description("Select all non-spatial attributes required for analysis.")
  public String[] ColNames;
  /**
   * {@link String} Input String for interested column names
   */
  
  @In
  @Name("Wards Clustering Non-Spatial Attribute Weights")
  @Description("Insert comma separated values. Values must sum to 1.")
  public String interestedColWeightsString;
  /**
   * {@link String} Input String for interested column weights
   */
  
  @In
  @Name("Wards Clustering Additional Attributes For Display")
  @Description("Additional attributes for display in dataset tabular output.")
  public String[] displayColNames;
  /**
   * {@link String} Input String for display column names string
   */
  
  @In
  @Name("Wards Clustering Non-Spatial Attribute Minimum Count")
  @Description("Select minimum non-spatial attribute for polygons to be included in cluster analysis.")
  public double ignoreEmptyRowJobNum = 1;
  /**
   * {@link double} ignore data row if job numbers in all interested columns are less than this value
   */
  
  @In
  @Name("Wards Clustering Value Chain Mode")
  @Description("Perform clustering using value chain mode or not. " +
      "If false, the non-spatial attributes will be added up into a new column called 'vcvalue', " +
      "on which, the non-spatial distance will be computed and used as a factor to generate the final clustering result")
  public boolean vcmode = true;
  /**
   * {@link boolean} perform clustering using value chain mode or not. if true, the interested columns will be added up into a new column called 'vcvalue', on which, the non-spatial distance will be computed and used as a factor to generate the final clustering resul
   */
  
  @In
  @Name("Wards Clustering Spatial vs Non-Spatial Distance Weights")
  @Description("Insert comma separated values. Values must sum to 1.")
  public String spatialNonSpatialDistWeightsString;
  /**
   * {@link String} Input String for spatial and non-spatial distance weights
   */
    
  @Description("R Connection output")
  @Out
  public RConnection cOut;
  
  protected REXP worker;
  /**
   * The result of {@link WardClustering} as an {@link REXP} object
   * containing all of the results from R
   */
  
  @Initialize
  public void validateInputs() throws IllegalArgumentException {
    //RConnection
    if (this.cIn == null) {
      throw new IllegalStateException("RConnection is null");
    }
    //geodisthreshold
    if (this.geodisthreshold < 0) {
      throw new IllegalStateException("Illegal value for Geo-Distance Threshold: " + this.geodisthreshold);
    }
    //targetclusternum
    if (this.targetclusternum < 0) {
      throw new IllegalStateException("Illegal value for Target Cluster Number: " + this.targetclusternum);
    }
    //interestedColNamesString
    if (this.ColNames == null) {
      throw new IllegalStateException("Non-Spatial Attribute Selection is null: " + this.ColNames);
    }
    //Non-Spatial Attribute Weights
    if (this.interestedColWeightsString == null) {
      throw new IllegalStateException("Non-Spatial Attribute Weights is null: " + this.interestedColWeightsString);
    }
    //displayColNamesString
    if (this.displayColNames == null) {
      throw new IllegalStateException("Illegal value for Additional Attributes For Display: " + this.displayColNames);
    }
    //ignoreEmptyRowJobNum
    if (this.ignoreEmptyRowJobNum < 0) {
      throw new IllegalStateException("Illegal value for Non-Spatial Attribute Minimum Count: " + this.ignoreEmptyRowJobNum);
    }
    //Value Chain Mode
    if (this.vcmode != true & this.vcmode != false) {
      throw new IllegalStateException("Illegal value for Value Chain Mode: " + this.vcmode);
    }
    //spatialNonSpatialDistWeightsString
    if (this.spatialNonSpatialDistWeightsString == null) {
      throw new IllegalStateException("Illegal value for Spatial vs Non-Spatial Distance Weights: " + this.spatialNonSpatialDistWeightsString);
    }
  }
  
  @Override
  public String prettyPrint() throws StatisticsException {
    final StringBuilder s = new StringBuilder();
    return s.toString();
  }
  
  @Execute
  public void compute() throws StatisticsException {
    
      LOGGER.debug("Wards Clustering compute executed");
      // setup the script to execute
      // 1. load the required script
    try {
      this.cIn.assign("script", Rscript.load("/wardsClustering.r"));
      
      
      // 2. setup the inputs
      this.cIn.assign("geodisthreshold", new REXPInteger(this.geodisthreshold));
      this.cIn.assign("targetclusternum", new REXPInteger(this.targetclusternum));
      this.cIn.assign("displayColNames", new REXPString(this.displayColNames));
      this.cIn.assign("interestedColNames", new REXPString(ColNames));
      
      double[] interestedColWeights = {};
      try {
        interestedColWeights = this.convertStringArraytoDoubleArray(this.interestedColWeightsString.split(","));
        
        // test if interestedColWeights add up to 1
        double sumWeight = 0.0;
        for(int i=0; i<interestedColWeights.length; i++){
          sumWeight = sumWeight + interestedColWeights[i];
        }
        if (sumWeight<0.99 || sumWeight>1.0){
          throw new Exception("weights of interested columns should add up to 1");
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return;
        }
      
      this.cIn.assign("interestedColWeights", new REXPDouble(interestedColWeights));
      
      double[] spatialNonSpatialDistWeights = {0.5, 0.5};
      try {
        spatialNonSpatialDistWeights = this.convertStringArraytoDoubleArray(this.spatialNonSpatialDistWeightsString.split(","));
        // test if spatialNonSpatialDistWeights add up to 1
        double sumWeight = 0.0;
        for(int i=0; i<spatialNonSpatialDistWeights.length; i++){
          sumWeight = sumWeight + spatialNonSpatialDistWeights[i];
        }
        if (sumWeight<0.99 || sumWeight>1.0){
          throw new Exception("weights of spatial and non-spatial distances should add up to 1");
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return;
      }
      
      this.cIn.assign("spatialNonSpatialDistWeights", new REXPDouble(spatialNonSpatialDistWeights));
      this.cIn.assign("gIgnoreEmptyRowJobNum", new REXPDouble(this.ignoreEmptyRowJobNum));
      this.cIn.assign("gVcMode", new REXPLogical(this.vcmode));
      this.cIn.assign("gErrorOccurs", new REXPLogical(false));
      
      // 2.5 Setup R logging
      Rlogger.logger(getEnvVar(EnvVars.LOG_LEVEL), getEnvVar(EnvVars.LOG_DIRECTORY));
      this.cIn.assign("optionsLogging", Rlogger.getLogOptions());

      LOGGER.debug("RLOGGER" + Rlogger.getLogOptions().toDebugString());

      // 3. call the function defined in the script
      LOGGER.debug("about to execute eval...");
      this.worker = cIn.eval("try(eval(parse(text=script)),silent=FALSE)");
      LOGGER.debug("worker result: {}", this.worker.toDebugString());

      // 5. setup the output RConnection with results
      this.cOut = this.cIn; 

    } catch (final IllegalArgumentException ie) {
      LOGGER.error("Invalid Input: {}", ie.getMessage());
      throw new StatisticsException(ie.getMessage(), ie);
    } catch (final REngineException e) {
      LOGGER.error("REngine error: {}", e.getMessage());
      throw new StatisticsException(e.getMessage(), e);
    } catch (final StatisticsException s) {
      LOGGER.error("SpatialStatistics: {}", s);
      throw s;
    }
    
  }
  
  private double[] convertStringArraytoDoubleArray(String[] sarray) throws NumberFormatException {
    if (sarray != null) {
    double rltarray[] = new double[sarray.length];
    for (int i = 0; i < sarray.length; i++) {
      rltarray[i] = Double.parseDouble(sarray[i]);
    }
      return rltarray;
    }
      return null;
    }
}
