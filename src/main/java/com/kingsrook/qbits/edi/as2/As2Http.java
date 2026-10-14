/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.edi.as2;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/*******************************************************************************
 ** HTTP client helpers for AS2: bounded timeouts, no redirects, optional pinning
 ** of the partner TLS certificate (otherwise the JVM trust store applies), and
 ** bounded response bodies.
 *******************************************************************************/
public final class As2Http
{
   public static final int MAX_RESPONSE_BYTES = 10 * 1024 * 1024;



   /*******************************************************************************
    ** Status, lower-case header names and the (bounded) body of a response.
    *******************************************************************************/
   public record Response(int status, Map<String, String> headers, byte[] body)
   {
   }



   /*******************************************************************************
    ** Accepts only the pinned certificate as the server leaf; the hostname is
    ** still checked by the TLS stack.
    *******************************************************************************/
   private static final class PinnedTrustManager implements X509TrustManager
   {
      private final byte[] pinned;



      /*******************************************************************************
       ** Pin the SHA-256 of the encoded certificate.
       *******************************************************************************/
      private PinnedTrustManager(X509Certificate certificate) throws Exception
      {
         pinned = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
      }



      /*******************************************************************************
       ** This client never authenticates with a client certificate.
       *******************************************************************************/
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
         throw (new CertificateException("Client certificates are not accepted"));
      }



      /*******************************************************************************
       ** Require the leaf to be exactly the pinned certificate.
       *******************************************************************************/
      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
         try
         {
            if(chain == null || chain.length == 0 || !MessageDigest.isEqual(pinned, MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded())))
            {
               throw (new CertificateException("The server certificate does not match the pinned partner TLS certificate"));
            }
         }
         catch(CertificateException e)
         {
            throw (e);
         }
         catch(Exception e)
         {
            throw (new CertificateException("The server certificate could not be checked against the pin"));
         }
      }



      /*******************************************************************************
       ** No trust anchors beyond the pin.
       *******************************************************************************/
      @Override
      public X509Certificate[] getAcceptedIssuers()
      {
         return (new X509Certificate[0]);
      }
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private As2Http()
   {
   }



   /*******************************************************************************
    ** A client using the JVM trust store, or only the pinned certificate.
    *******************************************************************************/
   public static HttpClient client(X509Certificate pinned) throws Exception
   {
      HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER);
      if(pinned != null)
      {
         SSLContext context = SSLContext.getInstance("TLS");
         context.init(null, new TrustManager[] {new PinnedTrustManager(pinned)}, new SecureRandom());
         builder.sslContext(context);
      }
      return (builder.build());
   }



   /*******************************************************************************
    ** Trust manager for pin checks in tests and diagnostics.
    *******************************************************************************/
   public static X509TrustManager pinnedTrustManager(X509Certificate pinned) throws Exception
   {
      return (new PinnedTrustManager(pinned));
   }



   /*******************************************************************************
    ** POST bytes with the given headers.
    *******************************************************************************/
   public static Response post(HttpClient client, URI uri, Map<String, String> headers, byte[] body, Duration timeout) throws Exception
   {
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(timeout).POST(HttpRequest.BodyPublishers.ofByteArray(body));
      headers.forEach(request::header);
      return (read(client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream())));
   }



   /*******************************************************************************
    ** OPTIONS probe: no body is sent.
    *******************************************************************************/
   public static Response options(HttpClient client, URI uri, Map<String, String> headers, Duration timeout) throws Exception
   {
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(timeout).method("OPTIONS", HttpRequest.BodyPublishers.noBody());
      headers.forEach(request::header);
      return (read(client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream())));
   }



   /*******************************************************************************
    ** Read the bounded body and lower-case the header names.
    *******************************************************************************/
   private static Response read(HttpResponse<InputStream> response) throws Exception
   {
      try(InputStream stream = response.body())
      {
         Map<String, String> headers = new TreeMap<>();
         response.headers().map().forEach((name, values) -> headers.put(name.toLowerCase(Locale.ROOT), values.getFirst()));
         return (new Response(response.statusCode(), headers, stream.readNBytes(MAX_RESPONSE_BYTES)));
      }
   }



   /*******************************************************************************
    ** An Authorization header value for HTTP Basic authentication.
    *******************************************************************************/
   public static String basic(String user, String password)
   {
      return ("Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
   }



   /*******************************************************************************
    ** TLS is required except on loopback, which the tests and local peers use.
    *******************************************************************************/
   public static boolean isAllowedUrl(String url)
   {
      try
      {
         URI uri = URI.create(url);
         return (uri.getHost() != null && uri.getUserInfo() == null
            && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()) && List.of("127.0.0.1", "localhost", "[::1]").contains(uri.getHost())));
      }
      catch(Exception e)
      {
         return (false);
      }
   }
}
