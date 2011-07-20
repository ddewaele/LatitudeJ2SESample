Text for blog article.

* Introduction

In this article, I'll show you how to use the google api client for java, in conjuction with the Google Latitude API.
The sample application will be a simple J2SE command-line application. 

The sample application will do 2 simple things

* Retrieve the current location of the user
* Retrieve the location history of the user, returning a maximum of 10 results.

* Creating the project

We'll start by creating a standard java project in Eclipse. 
We add a pom.xml to the project that includes all the dependencies for this Latitude sample.

The pom.xml looks like this :

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.google</groupId>
    <artifactId>google</artifactId>
    <version>5</version>
  </parent>
  <groupId>com.ecs.latitude</groupId>
  <artifactId>latitudesample</artifactId>
  <version>1.0-SNAPSHOT</version>
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>1.6</source>
          <target>1.6</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
  <dependencies>
   <dependency>
      <groupId>com.google.api.client</groupId>
      <artifactId>google-api-client</artifactId>
      <version>1.4.1-beta</version>
    </dependency>
   <dependency>
      <groupId>com.google.api.client</groupId>
      <artifactId>google-api-client-googleapis</artifactId>
      <version>1.4.1-beta</version>
    </dependency>    
    <dependency>
      <groupId>com.google.api.services.latitude</groupId>
      <artifactId>google-api-services-latitude-v1</artifactId>
      <version>1.1.0-beta</version> 
    </dependency>
    <dependency>
      <groupId>org.mortbay.jetty</groupId>
      <artifactId>jetty-embedded</artifactId>
      <version>6.1.24</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
  <repositories>
  	<repository>
  <id>google-api-services</id>
  <url>http://mavenrepo.google-api-java-client.googlecode.com/hg</url>
	</repository>
  </repositories>
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
</project>

What we find in this pom :

Sets the compiler version to 1.6
Includes the dependencies to google api client for java
Includes a dependency to the latitude client library
Includes a dependency to Jetty (used for the OAuth flow in this standalone application).

We enable "maven dependency management" so that our project now looks like this :
[picture before]
[picture after]

* The Latitude API.

I'll start by showing you how we can program against the Latitude API. Because we're using the generated client libraries, there is 
very little code we have to write (not taking into account the authentication / authorization part using OAuth). I'm leaving the security part 
out for now, as it's interesting to see what the following API calls will return when running un-secured, and what exactly we need to do to secure them.

* The Latitude service definition

Before we can start using the Latitude API, we need to initialize the service definition for the Latitude API.
This is done using the following code :

		HttpTransport httpTransport = new NetHttpTransport();
		Latitude latitude = new Latitude(httpTransport, new GsonFactory());

What this code does is create a reference to the Latitude API service definition, by initializing it with an HTTP transport and a GsonFactory.
We need the HTTP transport as all calls to the Latitude API are done over HTTP (REST based API), so all low level communication (intializing HTTP connections, building GET/POST requests) are handled by this HttpTransport class.
The GsonFactory is used to handle the JSON request / responses that will be sent over the HTTP protocol. The generated client libraries for Latify will ensure that all JSON request / responses will be mapped to a java model, so we 
don't need to do any JSON parsing ourselves.

* Retrieving location history

Now that we have a reference to the service definition, we can start coding against it. 

In order to retrieve the history, we use the Location collection on the Latitude service definition.
This call creates a List request, that we can send to the Latitude servers by calling its execute method.
The execute method returns a collection of Location objects. 



		com.google.api.services.latitude.Latitude.Location.List list = latitude.location.list();
		list.maxResults="10";
		
	    LocationFeed locationFeed = list.execute();
	    List<Location> locations = locationFeed.items;
	    for (Location location : locations) {
			System.out.println(location);
		}

* Retrieving current location

In order to retrieve the current location, we use the CurrentLocation endpoint on the API. From that endpoint, we create a GET request via the get() method, and execute that request using the execute() method.
For the moment, the Latitude generated libraries returns a LatitudeCurrentlocationResourceJson for the currentlocation endpoint, whereas the location endpoint (for the location history) returns Location objects.

	    LatitudeCurrentlocationResourceJson latitudeCurrentlocationResourceJson = latitude.currentLocation.get().execute();
	    System.out.println(latitudeCurrentlocationResourceJson.get("latitude") + " - " + latitudeCurrentlocationResourceJson.get("longitude"));


* Running the code 

When we try to execute the code above, it will fail with the following exception. 

Exception in thread "main" com.google.api.client.http.HttpResponseException: 401 Unauthorized
	at com.google.api.client.http.HttpRequest.execute(HttpRequest.java:380)
	at com.google.api.services.latitude.Latitude$RemoteRequest.execute(Latitude.java:550)
	at com.google.api.services.latitude.Latitude$Location$List.executeUnparsed(Latitude.java:450)
	at com.google.api.services.latitude.Latitude$Location$List.execute(Latitude.java:435)
	at com.ecs.latitude.sample.LatitudeSample.main(LatitudeSample.java:31)

This is to be expected, as the Latitude API does not support anonymous requests. In order for the sample application to use the Latitude API, the user needs to authorize the application in order for it to access its latitude data. 
The authentication / authorization part is handled by OAuth.

* Securing the API calls using OAuth

All calls to the Latitude API need to be properly signed. Before we can start signing the calls we make against the Latitude API, we need to get a hold of an OAuth access token. 

* Getting a consumer key / consumer secret.

Before we can start securing the calls using OAuth, we first need to get a hold of a consumer key and a consumer secret. 
Although most of the Google APIs don't require you to sign up for a consumer key / secret (allowing you to use anonymous/anonymous instead), the Latitude API does force you to register a domain through Google.  
We don't need to provide a certificate to sign our requests, as we'll be using a mechanism called HMAC-SHA1, capable of generating signatures to sign your requests. 
No certificate is required for HMAC-SHA1 signatures.  Instead, Google generates an OAuth consumer secret value, which is displayed on your domain's registration page after you have registered.

As a developer, we use the Manage Your Domains page (https://www.google.com/accounts/ManageDomains) to register a domain.

After succesfull registration, Google will provide you with an OAuth Consumer Key / Secret that you'll need to perform the OAuth flow.

We'll create the following class to store that information.

	package com.ecs.latitude.sample;
	
	public class LatitudeCredentials {
	
		/**
		 * OAuth Consumer Key obtained from the <a
		 * href="https://www.google.com/accounts/ManageDomains">Manage your
		 * domains</a> page or {@code anonymous} by default.
		 */
		public static final String ENTER_OAUTH_CONSUMER_KEY = "ecommitconsulting.be";
	
		/**
		 * OAuth Consumer Secret obtained from the <a
		 * href="https://www.google.com/accounts/ManageDomains">Manage your
		 * domains</a> page or {@code anonymous} by default.
		 */
		public static final String ENTER_OAUTH_CONSUMER_SECRET = "DAAFBVHjAeqCkXkNNCfQcpaG";
		
		/**
		 * Application identifier to be shown on the Google authorization page.
		 */
		public static final String APP_DESCRIPTION = "Latitude Sample J2SE App";
	}

Keep in mind that Maven uses the src/main/java folder to store the java source code, so make sure you add the sources there.
[picture]
After adding your java classes to the project, make sure to update the project configuration (right click on the project), in order for the maven plugin to configure the correct build path for your project.
[picture]

In order for our Latitude API calls work, we need to properly sign them, and that in turn means we need to get a hold of an OAuth access token.
The access token is the token that allows us to generate the signature needed to perform the API calls.
This also means that we'll need to turn our anonymous HttpTransport into an authorized HttpTransport, capable of generating signed requests. 

In order to get such an access token, we need to go through the OAuth flow that is for some part web based. 
The OAuth flow is a series of steps that we need to go through and where the access token retrieval is the final step. 
For more information on how the OAuth process works, check out the following link 
[insert link to blog post here]

OAuth is considered by most developers to be very tricky, and this is where most people get stuck.
I'll go over all the steps that are required to get the sample up and running using OAuth.

The google api client for java has full support for both OAuth 1.0 and 2.0. The Latitude API uses OAuth 1.0.

* OAuth - Getting the request token

The first part of the OAuth flow is to retrieve a request token. The google api client for java contain a GoogleOAuthGetTemporaryToken that represents a generic Google OAuth 1.0a URL. 
This URL is used to request a temporary credentials token (or "request token") from the Google Authorization server.   

We need to attach our httpTransport to this object, as well as a OAuthHmacSigner. The signer object needs to know our consumer secret.
We also add the following properties that are required to get our request token.

consumerKey
scope
displayName
callback

      GoogleOAuthGetTemporaryToken temporaryToken = new GoogleOAuthGetTemporaryToken();
      temporaryToken.transport = httpTransport;
      signer = new OAuthHmacSigner();
      signer.clientSharedSecret = LatitudeCredentials.ENTER_OAUTH_CONSUMER_SECRET;
      temporaryToken.signer = signer;
      temporaryToken.consumerKey = LatitudeCredentials.ENTER_OAUTH_CONSUMER_KEY;
      temporaryToken.scope = "https://www.googleapis.com/auth/latitude";
      temporaryToken.displayName = Constants.APP_DESCRIPTION;
      temporaryToken.callback = callbackServer.getCallbackUrl();
      
      // we capture the response to a request for the temporary token.
      // the response contains our token secret, that we'll put on the signer so that we can signing requests.
      OAuthCredentialsResponse tempCredentials = temporaryToken.execute();
      signer.tokenSharedSecret = tempCredentials.tokenSecret;

When calling the execute method, an HTTP request is executed to retrieve a temporary request token.
That temporary request token contains a token scret that we'll now put on the signer so that we can start signing requests.

* OAuth - Getting the authorize token

Now that we have our request token, we can prepare exchanging this for a an authorized token. 
In order to do that, user interaction is involved. We are going to pop a browser, so that the user can login (if not already logged into his/her google account), and authorize our application.

The OAuthAuthorizeTemporaryTokenUrl represents an OAuth 1.0a URL builder for an authorization web page to allow the end user to authorize the temporary token.

      // Prepare the request for the authorization token. 
      // For Latitude, we need to use a different URL as authorization endpoint.
      OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl("https://www.google.com/latitude/apps/OAuthAuthorizeToken?domain=ecommitconsulting.be&location=all&granularity=best");
      authorizeUrl.set("scope", temporaryToken.scope);
      authorizeUrl.set("domain", LatitudeCredentials.ENTER_OAUTH_CONSUMER_KEY);
      authorizeUrl.set("xoauth_displayname", Constants.APP_DESCRIPTION);
      authorizeUrl.temporaryToken = tempToken = tempCredentials.token;

Our sample will launch a browser that will guide the user through the OAuth process. This involves logging into your google account, and authorizing this sample application to access your latitude information.
For that, we'll use a LoginCallbackServer that can be found in most Google api java client samples, and was inspired by oacurl.
It basically starts a Jetty that will host a landing page, used to capture the access token once the OAuth flow completes.

The code to pop the browser with our authorization URL can be found here : 

      // We build the actual URL, and launch that URL in a browser.
      String authorizationUrl = authorizeUrl.build();
      // launch in browser
      boolean browsed = false;
      if (Desktop.isDesktopSupported()) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Action.BROWSE)) {
          desktop.browse(URI.create(authorizationUrl));
          browsed = true;
        }
      }
      if (!browsed) {
        String browser = "google-chrome";
        Runtime.getRuntime().exec(new String[] {browser, authorizationUrl});
      }

* Capturing the authorized token in our callback.

After the user has ended the flow in his browser, we'll get an authorized token in our callback URL that we specified in the beginning will be called, landing on our Jetty Server we started earlier.

The callback URL we entered looks something like this. 

http://localhost:59385/OAuthCallback?oauth_verifier=IKvDOUvD4j_ZqsdqsdfkAFCu&oauth_token=4%2FKpedqsdqsdbgnC7dUD2okLAiGV

As you can see, it contains 

oauth_verifier
oauth_token

The following hook on our callbackServer is used to capture these 2 tokens from our callback URL.

      // We wait for the user to finish the OAuth flow, by authorizing our application access.
      // This is a blocking call.
      verifier = callbackServer.waitForVerifier(tempToken);


* Exchange the authorized token for an access token

We need these 2 keys to upgrade the authorize token into an actual access token that we need to perform the Latitude API calls.
Once we have retrieved the authorize token, we can now upgrade it to an access token.
The access token is required to perform the actual API requests.


    GoogleOAuthGetAccessToken accessToken = new GoogleOAuthGetAccessToken();
    accessToken.transport = httpTransport;
    accessToken.temporaryToken = tempToken;
    accessToken.signer = signer;
    accessToken.consumerKey = LatitudeCredentials.ENTER_OAUTH_CONSUMER_KEY;
    accessToken.verifier = verifier;

    // The access token provides us with a token secret, required to perform the API calls.
    credentials = accessToken.execute();
    signer.tokenSharedSecret = credentials.tokenSecret;

    OAuthParameters authorizer = new OAuthParameters();
    authorizer.consumerKey = LatitudeCredentials.ENTER_OAUTH_CONSUMER_KEY;
    authorizer.signer = signer;
    authorizer.token = credentials.token;

    authorizer.signRequestsUsingAuthorizationHeader(httpTransport);

   
* Executing the sample
   
When executing our sample application with the authorized HTTP transport, you should see something like this :

{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310534147429"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310533388945"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310531588936"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310529789147"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310527989231"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310526188905"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310524388929"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310522588936"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310520788900"}
{"accuracy":8308,"kind":"latitude#location","latitude":50.98788,"longitude":3.52449,"timestampMs":"1310518988916"}
50.98788 - 3.52449

* References

Latitude API
Oauth
OAuth overview
google api client for java
oacurl

Authentication and Authorization for Google APIs
http://code.google.com/intl/nl/apis/accounts/docs/OAuth.html

Manage Domains
https://www.google.com/accounts/ManageDomains