package com.ecs.latitude.sample;

import java.util.List;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.latitude.Latitude;
import com.google.api.services.latitude.model.LatitudeCurrentlocationResourceJson;
import com.google.api.services.latitude.model.Location;
import com.google.api.services.latitude.model.LocationFeed;


/**
 * 
 */
public class LatitudeSample {

	public static void main(String[] args) throws Exception {

		HttpTransport httpTransport = new NetHttpTransport();
		
		//If we don't have an access token stored, we need to authorize.
		LatitudeOAuth.authorize(httpTransport);

		Latitude latitude = new Latitude(httpTransport, new GsonFactory());

		com.google.api.services.latitude.Latitude.Location.List list = latitude.location.list();
		list.maxResults="10";
		
	    LocationFeed locationFeed = list.execute();
	    List<Location> locations = locationFeed.items;
	    for (Location location : locations) {
			System.out.println(location);
		}

	    LatitudeCurrentlocationResourceJson latitudeCurrentlocationResourceJson = latitude.currentLocation.get().execute();
	    System.out.println(latitudeCurrentlocationResourceJson.get("latitude") + " - " + latitudeCurrentlocationResourceJson.get("longitude"));
	    
	    
//	    Location location = latitude.currentLocation.get().execute();
//	    System.out.println(location);
	    
//	    HttpResponse executeUnparsed = list.executeUnparsed();
//	    InputStream content = executeUnparsed.getContent();
//	    System.out.println(convertStreamToString(content));
//	    
	    
	    


	}


}
