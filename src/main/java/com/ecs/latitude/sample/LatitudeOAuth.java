package com.ecs.latitude.sample;


import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.net.URI;

import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetAccessToken;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetTemporaryToken;
import com.google.api.client.http.HttpTransport;

public class LatitudeOAuth {

  private static OAuthHmacSigner signer;

  private static OAuthCredentialsResponse credentials;

  static void authorize(HttpTransport httpTransport) throws Exception {
    // callback server
    LoginCallbackServer callbackServer = null;
    String verifier = null;
    String tempToken = null;
    try {
      callbackServer = new LoginCallbackServer();
      callbackServer.start();
      // We prepare the request to obtain a temporary token
      GoogleOAuthGetTemporaryToken temporaryToken = new GoogleOAuthGetTemporaryToken();
      temporaryToken.transport = httpTransport;
      signer = new OAuthHmacSigner();
      signer.clientSharedSecret = LatitudeCredentials.ENTER_OAUTH_CONSUMER_SECRET;
      temporaryToken.signer = signer;
      temporaryToken.consumerKey = LatitudeCredentials.ENTER_OAUTH_CONSUMER_KEY;
      temporaryToken.scope = "https://www.googleapis.com/auth/latitude";
      temporaryToken.displayName = LatitudeCredentials.APP_DESCRIPTION;
      temporaryToken.callback = callbackServer.getCallbackUrl();
      
      // we capture the response to a request for the temporary token.
      // the response contains our token secret, that we'll put on the signer so that we can signing requests.
      OAuthCredentialsResponse tempCredentials = temporaryToken.execute();
      signer.tokenSharedSecret = tempCredentials.tokenSecret;

      // Prepare the request for the authorization token. 
      // For Latitude, we need to use a different URL as authorization endpoint.
      OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl("https://www.google.com/latitude/apps/OAuthAuthorizeToken?domain=ecommitconsulting.be&location=all&granularity=best");
      authorizeUrl.set("scope", temporaryToken.scope);
      authorizeUrl.set("domain", LatitudeCredentials.ENTER_OAUTH_CONSUMER_KEY);
      authorizeUrl.set("xoauth_displayname", LatitudeCredentials.APP_DESCRIPTION);
      authorizeUrl.temporaryToken = tempToken = tempCredentials.token;
      
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
      
      // We wait for the user to finish the OAuth flow, by authorizing our application access.
      // This is a blocking call.
      verifier = callbackServer.waitForVerifier(tempToken);
    } finally {
      if (callbackServer != null) {
        callbackServer.stop();
      }
    }
    
    // Once we have retrieved the authorize token, we can now upgrade it to an access token.
    // The access token is required to perform the actual API requests.
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
  }

  static void revoke(HttpTransport httpTransport) {
    if (credentials != null) {
      try {
        GoogleOAuthGetAccessToken.revokeAccessToken(httpTransport, createOAuthParameters());
      } catch (Exception e) {
        e.printStackTrace(System.err);
      }
    }
  }

  private static OAuthParameters createOAuthParameters() {
    OAuthParameters authorizer = new OAuthParameters();
    authorizer.consumerKey = LatitudeCredentials.ENTER_OAUTH_CONSUMER_KEY;
    authorizer.signer = signer;
    authorizer.token = credentials.token;
    return authorizer;
  }
}
