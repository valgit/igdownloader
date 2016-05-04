import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
	
public class RequestMgr
  extends Thread
{
  String hashtag = "";
  JSONParser json = null;
  File outputDir;
    private HttpClient client;
  private volatile Boolean stopFlag = false;
  ArrayList<String> prevDownloaded = null;
  Logger logger;
  Boolean debug = false;
    String next_url = null;
  String APIurl;
  
  public RequestMgr(String hashtag, File outputDir, ArrayList<String> prevDownloaded, String APIurl) {
    this.hashtag = hashtag.trim();
    this.APIurl = APIurl;
    this.outputDir = outputDir;
    this.client = new DefaultHttpClient();
    this.prevDownloaded = (prevDownloaded == null ? new ArrayList() : prevDownloaded);
    this.json = new JSONParser();
    this.logger = new Logger("reqmgr-log");
  } 
  
  public ArrayList<String> getResults()
  {
    next_url = getIGramUrlStr();
    this.logger.log("Starting Request Process: " + next_url);
    
    ArrayList results = new ArrayList();
    HttpClient client = new DefaultHttpClient();

      while ( next_url != null) {
          HttpGet req = new HttpGet(next_url);

          //HttpHost target = new HttpHost("proxy", 3128, "https");
          HttpHost proxy = new HttpHost("proxy", 3128, "http");

          client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                  proxy);
          RequestConfig config = RequestConfig.custom()
                  .setProxy(proxy)
                  .build();

          req.setConfig(config);

          HttpResponse response = getHttpResponse(client, req);

          if (response != null) {
              /*
              Header[] headers = response.getAllHeaders();

              for(Header h:headers){
                  //System.out.println(h.getName() + ": " + h.getValue());
                  if (h.getName().equals("X-Ratelimit-Remaining"))
              }
              */
              int  ratelimit = Integer.parseInt(response.getFirstHeader("X-Ratelimit-Remaining").getValue());
              // X-Ratelimit-Remaining: 4675
              //System.out.println("rate limit : " + ratelimit);
                if (ratelimit < 10) {
                    this.logger.log("rate limiting : "+ratelimit);
                }
              ArrayList images = getImageListFromEntity(response.getEntity());
              results = downloadImages(images);
              //System.out.println("next url : "+ next_url);
          } else {
              this.logger.log("No Valid Response");
          }
        if (this.stopFlag)
            break;

        // don't arm IG
        try {
          Thread.sleep(500);                 //1000 milliseconds is one second.
          System.out.println("sleep");
        } catch (InterruptedException ex) {
          //Thread.currentThread().interrupt();
          System.out.println("intr");
        }

      } // next_url
    
    return results;
  } 
  
  private String getIGramUrlStr() {
    return APIurl;
  }
  
  protected HttpResponse getHttpResponse(HttpClient client, HttpGet req) {
    HttpResponse response = null;
    
    if (this.stopFlag) {
      return response;
    } 
    try
    {
		
			
      response = client.execute(req);
    } catch (ClientProtocolException e) {
      this.logger.log("Could not make request Client Protocol Exception: " + e.getMessage());
      e.printStackTrace();
    } catch (SSLPeerUnverifiedException e) {
    	// at some point, the ssl connection gets messed up. refresh the client.
        this.logger.log("SSL Connection Error. Recreating client.");
    	this.client = new DefaultHttpClient();
    } catch (IOException e) {
      this.logger.log("Could not make request IO Exception: " + e.getMessage());
      e.printStackTrace();
    } 
    return response;
  } 
  
  protected ArrayList<String> getImageListFromEntity(HttpEntity entity) {
    String content = null;
    ArrayList imageUrls = new ArrayList();
	//ArrayList videoUrls = new ArrayList();
    try
    {
      content = EntityUtils.toString(entity);
    } catch (org.apache.http.ParseException e) {
      this.logger.log("Could not parse response content: " + e.getMessage());
      //this.logger.log("Content was: " + content);
      e.printStackTrace();
    } catch (IOException e) {
      this.logger.log("Could not read response content: " + e.getMessage());
        // this.logger.log("Content was: " + content);
      e.printStackTrace();
    } 
    
    JSONObject results = null;
    try {
      results = (JSONObject)this.json.parse(content);
    } catch (org.json.simple.parser.ParseException e) {
      this.logger.log("Could not parse json content: " + e.getMessage());
      this.logger.log("Content was: " + content);
      e.printStackTrace();
      return imageUrls;
    }


     this.next_url = (String) (((JSONObject)results.get("pagination")).get("next_url"));
     this.logger.log("next url: " + next_url);

    JSONArray data = (JSONArray)results.get("data");
    for (int i = 0; i < data.size(); i++) {
	  String type = (String)((JSONObject)data.get(i)).get("type");
	  //this.logger.log("adding type: " + type);
      //JSONArray user = (JSONArray)((JSONObject)data.get(i)).get("user");
        HashMap<String, String> user = (HashMap<String, String>) ((JSONObject)data.get(i)).get("user");
        //TODO: System.out.println("uid: "+ user.get("username"));
	  if (type.equals("image")) {	  
		JSONObject images = (JSONObject)((JSONObject)data.get(i)).get("images");
		String low_url = (String)((JSONObject)images.get("standard_resolution")).get("url");
        String url = low_url.replaceFirst("s640x640","");
		//this.logger.log("adding: " + url);
        imageUrls.add(url);
	  } else {
		JSONObject images = (JSONObject)((JSONObject)data.get(i)).get("videos");
		String low_url = (String)((JSONObject)images.get("standard_resolution")).get("url");
        String url = low_url.replaceFirst("s640x640","");
		//this.logger.log("adding: " + images);
		imageUrls.add(url);
	  }
	  
    } 
    
    return imageUrls;
  } 
  
  public static String getFileNameFromUrl(URL url) {

    String urlString = url.getFile();

    return urlString.substring(urlString.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0];
}

  protected ArrayList<String> downloadImages(ArrayList<String> images) {
    URL url = null;
    ArrayList downloaded = new ArrayList();
    Boolean success = true;
    
    for (int i = 0; i < images.size(); i++)
    {
      if (!this.stopFlag)
      {
        try
        {
			this.logger.log("fetching: " + (String)images.get(i));
			System.out.println("fetching: " + (String)images.get(i));
			
          url = new URL((String)images.get(i));
        } catch (MalformedURLException e) {
          this.logger.log("Url is malformed. " + e.getMessage());
          success = false;
          e.printStackTrace();
        } 
		
        byte[] responseBody = null;
		
        if (!this.prevDownloaded.contains(getFileNameFromUrl(url)))
        {
			this.logger.log("will fetch: " + url);
			
          try
          {
		  // This is where you'd define the proxy's host name and port.
             //SocketAddress address = new InetSocketAddress("proxy", 3128);
             
             // Create an HTTP Proxy using the above SocketAddress.
             //Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
			 
            //image = ImageIO.read(url);
			HttpClient client = new DefaultHttpClient();
			HttpResponse response = null;
			HttpGet req = new HttpGet(url.toString());

			HttpHost proxy = new HttpHost("proxy", 3128, "http");

			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                    proxy);
			RequestConfig config = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();
            
			req.setConfig(config);

			response = client.execute(req);
			this.logger.log("HTTP code : " + response.getStatusLine().getStatusCode());
			
			responseBody = EntityUtils.toByteArray(response.getEntity());
			 			
          } catch (IOException e) {
            this.logger.log("Could not read Image from " + url.toString());
            success = false;
            e.printStackTrace();
          } 
		  
          
          String fullpath = null;
          try {
		    //System.out.println("byte size: "+responseBody.length);
            if (responseBody != null) {
				
              //String[] parts = url.getFile().split("/");
			  String name = getFileNameFromUrl(url);
			  
              //String name = parts[(parts.length - 1)];
			  
              fullpath = this.outputDir.getAbsolutePath() + File.separatorChar + name;
			  File fout = new File(fullpath);
              /*
			  if (!fout.exists()) {
				fout.createNewFile();
			  } else {
				System.out.println("already exist "+ fullpath);
			  }
			  */
			  System.out.println("saving "+ fullpath);
              //ImageIO.write((RenderedImage)image, "jpg", new File(fullpath));
			  FileOutputStream fos = new FileOutputStream(fout);
			  fos.write(responseBody);
			  fos.flush();
			  fos.close();

            } else 
				this.logger.log("Could not find image");
          } catch (IOException e) {
            this.logger.log("Could not write Image to " + fullpath);
            success = false;
            e.printStackTrace();
          } 
          
          if (success) {
            downloaded.add((String)images.get(i));
            this.prevDownloaded.add(getFileNameFromUrl(url));
			//this.logger.log("done: " + getFileNameFromUrl(url));
          } 
        } 
      } 
	  /*
	  if (i % 20 == 0) {
          try {
              Thread.sleep(500);                 //1000 milliseconds is one second.
              System.out.println("sleep");
          } catch (InterruptedException ex) {
              //Thread.currentThread().interrupt();
              System.out.println("intr");
          }
      }
      */
    } 
    return downloaded;
  } 
  
  private String getNameFromUrl(URL url) {
    String[] parts = url.getFile().split("/");
    String name = parts[(parts.length - 1)];
    return name;
  } 
  
  public void run() {
    this.logger.log("Request Manager Run Called");
    
    while (!this.stopFlag) {
      getResults();
      try {
        Thread.sleep(2000L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } 
    } 
    this.logger.close();
  } 
  
  public synchronized void end()
  {
    this.logger.log("Called End");
    this.stopFlag = true;
  } 
  
  public void setDebug(Boolean debug) {
    this.logger.setEnabled(debug);
  } 
} 
