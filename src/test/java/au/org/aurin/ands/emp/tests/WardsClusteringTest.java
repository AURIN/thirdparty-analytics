package au.org.aurin.ands.emp.tests;

import org.junit.Assert;
import org.junit.Test;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.uq.aurin.util.Rscript;
import au.org.aurin.ands.emp.WardsClustering;
import au.org.aurin.workflow.EnvVars;

public class WardsClusteringTest {
  
  private static final String LOG_LEVEL = "DEBUG";
  private static final String LOG_DIR = System.getProperty("java.io.tmpdir");

  private static final Logger LOG = LoggerFactory.getLogger(WardsClusteringTest.class);

  @Test
  public void test() throws Exception {
    
    try {
      LOG.info("========= Test case NewWards");
      String path  = this.getClass().getClassLoader().getResource("data/testSample/FootScray_Employment_SUM_2011/").getPath();
      path += "/" + "FootScray_Employment_SUM_2011";
    
    
      String rWorkingDir = this.getClass().getClassLoader().getResource("outputs").getPath();
      System.out.println(rWorkingDir);
      
      RConnection cOut = new RConnection();
      cOut.assign("script", Rscript.load("/rscripts/geoJSON2DataFrame.r"));
      cOut.assign("shpFilePath", new REXPString(path));
      cOut.assign("geoJSONFilePath", new REXPString(path + ".geojson"));
      //cOut.assign("geojSONString", new REXPString(this.geojSONString));
      cOut.assign("spatialDataFormatMode", new REXPInteger(0));

      cOut.assign("rWorkingDir", new REXPString(rWorkingDir));
      
      cOut.eval("try(eval(parse(text=script)),silent=FALSE)");
       
      WardsClustering wc = new WardsClustering();

      wc.cIn = cOut;
    
      wc.geodisthreshold = 10;
      wc.targetclusternum = 5;
//    wc.interestedColNamesString = "X2310,X2412,X8500";
      String[] selectedAttributes = {"X2310","X2412","X8500"};    
      wc.ColNames = selectedAttributes;
      String[] selectedAttributesDisp = {"LGA_CODE","LGA","ZONE_CODE"};   
      wc.displayColNames = selectedAttributesDisp;
      wc.interestedColWeightsString = "0.333,0.333,0.333";
      wc.spatialNonSpatialDistWeightsString = "0.9,0.1";
      wc.ignoreEmptyRowJobNum = 1;
      wc.vcmode = true;
      
   // 2.5 setup logger
      wc.newVar(EnvVars.LOG_LEVEL, LOG_LEVEL);
      wc.newVar(EnvVars.LOG_DIRECTORY, LOG_DIR);
      
      wc.compute();

    } catch (final RserveException e) {
      e.printStackTrace();
      Assert.fail("ContigSWM Test failed");
    }
  }
  

}