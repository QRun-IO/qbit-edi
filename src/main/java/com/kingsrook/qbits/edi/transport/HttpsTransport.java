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

package com.kingsrook.qbits.edi.transport;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qqq.backend.core.utils.webhooks.WebhookSecret;
import com.kingsrook.qqq.backend.core.utils.webhooks.WebhookSigner;
import com.kingsrook.qqq.backend.core.utils.webhooks.WebhookVerifier;

/*******************************************************************************
 ** HTTP delivery with bounded timeouts and Standard Webhooks authentication.
 *******************************************************************************/
public class HttpsTransport implements EdiTransportPlugin
{
   private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();

   /*******************************************************************************
    ** Registry key.
    *******************************************************************************/
   public String getType()
   {
      return ("HTTPS");
   }

   /*******************************************************************************
    ** Require TLS except loopback test endpoints; never permit URL credentials.
    *******************************************************************************/
   public void validate(EdiTransport transport, List<String> errors)
   {
      if(!"INBOUND".equals(transport.getDirection()))
      {
         try
         {
            URI uri = URI.create(Objects.requireNonNullElse(transport.getHttpsUrl(), ""));
            if(uri.getHost() == null || uri.getUserInfo() != null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()) && List.of("127.0.0.1", "localhost", "[::1]").contains(uri.getHost())))
            {
               errors.add("httpsUrl must use TLS (HTTP is allowed on loopback only)");
            }
         }
         catch(IllegalArgumentException e)
         {
            errors.add("httpsUrl is invalid");
         }
      }
      for(String auth : List.of(Objects.requireNonNullElse(transport.getHttpsAuthType(), "NONE"), Objects.requireNonNullElse(transport.getHttpsInboundAuth(), "NONE")))
      {
         if(!List.of("NONE", "BASIC", "BEARER", "HMAC").contains(auth))
         {
            errors.add("Unsupported HTTPS authentication type");
         }
         if(!"NONE".equals(auth) && transport.getHttpsSecretEnvVar() == null)
         {
            errors.add("httpsSecretEnvVar is required");
         }
      }
      if(!"OUTBOUND".equals(transport.getDirection()) && (transport.getHttpsInboundAuth() == null || "NONE".equals(transport.getHttpsInboundAuth())))
      {
         errors.add("Inbound HTTPS requires BASIC, BEARER or HMAC authentication");
      }
   }

   /*******************************************************************************
    ** Authentication values are resolved immediately before use and never logged.
    *******************************************************************************/
   public EdiSendResult send(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport) throws Exception
   {
      byte[] bytes;
      try(InputStream stream = EdiRuntime.current().payloads().open(payload))
      {
         bytes = stream.readAllBytes();
      }
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(transport.getHttpsUrl())).timeout(Duration.ofSeconds(60))
         .header("Content-Type", "application/edi-x12").POST(HttpRequest.BodyPublishers.ofByteArray(bytes));
      if(transport.getHttpsHeadersJson() != null)
      {
         Map<String, String> headers = new ObjectMapper().readValue(transport.getHttpsHeadersJson(), new TypeReference<Map<String, String>>() {});
         for(var entry : headers.entrySet())
         {
            if(entry.getKey().equalsIgnoreCase("authorization") || entry.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("webhook-"))
            {
               throw new IllegalArgumentException("Authentication headers must use secret references");
            }
            request.header(entry.getKey(), entry.getValue());
         }
      }
      String auth = Objects.requireNonNullElse(transport.getHttpsAuthType(), "NONE");
      if("HMAC".equals(auth))
      {
         WebhookSigner.headers("edi-" + message.getId(), EdiRuntime.current().config().getClock().instant(), bytes,
            List.of(WebhookSecret.parse(secret(transport)))).forEach(request::header);
      }
      else if(!"NONE".equals(auth))
      {
         request.header("Authorization", authorization(auth, secret(transport)));
      }
      int status = client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
      return (new EdiSendResult(status >= 200 && status < 300 ? "DELIVERED" : status == 429 || status >= 500 ? "RETRY" : "FAILED", "edi-" + message.getId(), "HTTP " + status));
   }

   /*******************************************************************************
    ** Inbound verification is constant-time and enforces HMAC timestamp tolerance.
    *******************************************************************************/
   public static boolean authenticate(EdiTransport transport, Map<String, String> headers, byte[] bytes)
   {
      try
      {
         String auth = transport.getHttpsInboundAuth();
         if("HMAC".equals(auth))
         {
            new WebhookVerifier(List.of(WebhookSecret.parse(secret(transport))), Duration.ofMinutes(5), EdiRuntime.current().config().getClock()).verify(headers, bytes);
            return (true);
         }
         String received = headers.entrySet().stream().filter(e -> "authorization".equalsIgnoreCase(e.getKey())).map(Map.Entry::getValue).findFirst().orElse("");
         return (List.of("BASIC", "BEARER").contains(Objects.requireNonNullElse(auth, "")) && MessageDigest.isEqual(received.getBytes(StandardCharsets.UTF_8), authorization(auth, secret(transport)).getBytes(StandardCharsets.UTF_8)));
      }
      catch(Exception e)
      {
         return (false);
      }
   }

   /*******************************************************************************
    ** BASIC secrets contain user:password; BEARER secrets contain only the token.
    *******************************************************************************/
   private static String authorization(String auth, String secret)
   {
      return ("BASIC".equals(auth) ? "Basic " + Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8)) : "Bearer " + secret);
   }

   /*******************************************************************************
    ** Fail closed when a referenced environment variable is absent.
    *******************************************************************************/
   private static String secret(EdiTransport transport)
   {
      return (Objects.requireNonNull(EdiRuntime.current().config().getSecretsResolver().resolve(transport.getHttpsSecretEnvVar()), "HTTPS secret is unavailable"));
   }

   /*******************************************************************************
    ** HEAD probes connectivity without submitting a document or following redirects.
    *******************************************************************************/
   public EdiTestResult test(EdiTransport transport)
   {
      try
      {
         int status = client.send(HttpRequest.newBuilder(URI.create(transport.getHttpsUrl())).timeout(Duration.ofSeconds(15))
            .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
         return (new EdiTestResult(status < 500, "Endpoint responded with HTTP " + status));
      }
      catch(Exception e)
      {
         return (new EdiTestResult(false, "HTTPS connection failed"));
      }
   }
}
