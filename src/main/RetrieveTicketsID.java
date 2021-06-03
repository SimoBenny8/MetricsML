package main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class RetrieveTicketsID {
	

	private RetrieveTicketsID() {
	    throw new IllegalStateException("Utility class");
	  }


   private static String readAll(Reader rd) throws IOException {
	      StringBuilder sb = new StringBuilder();
	      int cp;
	      while ((cp = rd.read()) != -1) {
	         sb.append((char) cp);
	      }
	      return sb.toString();
	   }

   public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
      InputStream is = new URL(url).openStream();
      try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) { 
         String jsonText = readAll(rd);
         return new JSONArray(jsonText);
       } finally {
         is.close();
       }
   }

   public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
      InputStream is = new URL(url).openStream();
      try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
     
         String jsonText = readAll(rd);
         return new JSONObject(jsonText);
         
       } finally {
         is.close();
       }
   }
   
   public static List<String> getTicketId(String projName) throws IOException, JSONException{
	  
	  
	   ArrayList<String> al = new ArrayList<>();  
	   Integer j = 0;
	   Integer i = 0;
	   Integer total = 1;
      //Get JSON API for closed bugs w/ AV in the project
      do {
         //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
         j = i + 1000;
         String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projName + "%22AND%20issueType%20=%22Bug%22AND%20status%20in%20(Resolved%2C%20Closed)%20AND%20resolution%20=%20fixed%20ORDER%20BY%20created%20ASC%20&fields=key,resolutiondate,versions,created&startAt="
                + i.toString() + "&maxResults=" + j.toString();
         JSONObject json = readJsonFromUrl(url);
         JSONArray issues = json.getJSONArray("issues");
         total = json.getInt("total");
         for (; i < total && i < j; i++) {
            //Iterate through each bug
            String key = issues.getJSONObject(i%1000).get("key").toString();
            al.add(key);
         }  
      } while (i < total);
      
      return al;
      
   }
   
   
   public static String getIV(String ticket) throws IOException, JSONException{
	   //TODO: trasformarla in versione numerica 
	  
	   	 Integer i = 0;
	   	 
      
         String url = "https://issues.apache.org/jira/rest/api/latest/issue/"+ ticket;
         JSONObject json = readJsonFromUrl(url);
         JSONObject fields = json.getJSONObject("fields");
         JSONArray versions = fields.getJSONArray("versions");
         if(versions.length() != 0 ) {
            if (versions.getJSONObject(0).has("name")) {
            		return versions.getJSONObject(i).get("name").toString();
            }
         }else {
        	 return null;
         }
          
         
      return null;
      
   }
   
   public static String getFV(String ticket) throws IOException, JSONException{
	   //TODO: vedere caso con più versioni 
	   //TODO: If name = "", use closing commit date of the ticket and compare it with commit date of the versions 
	   Integer i = 0;
	   String name = "";
    
       String url = "https://issues.apache.org/jira/rest/api/latest/issue/"+ ticket;
       JSONObject json = readJsonFromUrl(url);
       JSONObject fields = json.getJSONObject("fields");
       JSONArray versions = fields.getJSONArray("fixVersions");
       for (i = 0; i < versions.length(); i++ ) {
          
          if (versions.getJSONObject(i).has("name")) {
          		name = versions.getJSONObject(i).get("name").toString();
          }
               
          }
       System.out.println(name);
    
    return name;
    
 }
   
   //Used for calculating OV
   public static String creationDateTicket(String ticket) throws JSONException, IOException {
	   
	   String date = "";
	   String url = "https://issues.apache.org/jira/rest/api/latest/issue/"+ ticket;
       JSONObject json = readJsonFromUrl(url);
       JSONObject fields = json.getJSONObject("fields");
       String createdDate = fields.get("created").toString();
       date = createdDate.substring(0,10);
     
      // System.out.println(date);
	   
	   
	   return date;
   }
   
   
   
   public static void main(String[] args) {
	   
	   try {
	//List<String> ls = getTicketId("STORM",0);
	 System.out.println(getIV("BOOKKEEPER-442"));
		   //creationDateTicket("STORM-235");
	} catch (JSONException|IOException e) {
		
		e.printStackTrace();
	} 
	   
   }
 
}
