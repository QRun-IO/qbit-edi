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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

/*******************************************************************************
 ** MINA streaming SFTP with explicit host-key trust and atomic remote publication.
 *******************************************************************************/
public class SftpTransport implements EdiTransportPlugin
{
   /*******************************************************************************
    ** Registry key.
    *******************************************************************************/
   public String getType()
   {
      return ("SFTP");
   }

   /*******************************************************************************
    ** Refuse unpinned hosts and incomplete pickup settings before saving.
    *******************************************************************************/
   public void validate(EdiTransport transport, List<String> errors)
   {
      if(transport.getSftpHost() == null || transport.getSftpUsername() == null || transport.getSftpHostKeyFingerprint() == null)
      {
         errors.add("SFTP host, username and host key fingerprint are required");
      }
      if(transport.getSftpPasswordEnvVar() == null && transport.getSftpPrivateKeyEnvVar() == null)
      {
         errors.add("SFTP requires a password or private-key environment reference");
      }
      if("MOVE".equals(transport.getSftpAfterPickup()) && transport.getSftpArchivePath() == null)
      {
         errors.add("SFTP archive path is required for MOVE");
      }
   }

   /*******************************************************************************
    ** Stream to a temporary remote name then publish through remote rename.
    *******************************************************************************/
   public EdiSendResult send(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport) throws Exception
   {
      return (connected(transport, sftp ->
      {
         String name = "edi-" + message.getId() + ".x12";
         String path = path(transport.getSftpOutboundPath(), name);
         String temporary = path + Objects.requireNonNullElse(transport.getSftpTempSuffix(), ".tmp");
         if(temporary.equals(path))
         {
            throw new IllegalArgumentException("SFTP temporary suffix is required");
         }
         try(InputStream input = EdiRuntime.current().payloads().open(payload); OutputStream output = sftp.write(temporary))
         {
            input.transferTo(output);
         }
         sftp.rename(temporary, path, SftpClient.CopyMode.Overwrite);
         return (new EdiSendResult("SENT", name, null));
      }));
   }

   /*******************************************************************************
    ** Durable intake commits before DELETE/MOVE. A failed cleanup retries safely.
    *******************************************************************************/
   public void poll(EdiTransport transport, InboundSink sink) throws Exception
   {
      connected(transport, sftp ->
      {
         String directory = Objects.requireNonNull(transport.getSftpInboundPath(), "SFTP inbound path is required");
         String pattern = Objects.requireNonNullElse(transport.getSftpFilePattern(), ".*\\.x12");
         for(var entry : sftp.readDir(directory))
         {
            String name = entry.getFilename();
            if(!entry.getAttributes().isRegularFile() || !name.matches(pattern) || name.contains("/") || name.contains("\\"))
            {
               continue;
            }
            String remote = path(directory, name);
            String id = transport.getId() + ":" + remote + ":" + entry.getAttributes().getModifyTime() + ":" + entry.getAttributes().getSize();
            try(InputStream input = sftp.read(remote))
            {
               sink.receive(new InboundPayload("SFTP", id, name, transport.getSftpHost(), input));
            }
            if("MOVE".equals(transport.getSftpAfterPickup()))
            {
               sftp.rename(remote, path(transport.getSftpArchivePath(), name));
            }
            else
            {
               sftp.remove(remote);
            }
         }
         return (null);
      });
   }

   /*******************************************************************************
    ** Verify host identity and credentials without sending a document.
    *******************************************************************************/
   public EdiTestResult test(EdiTransport transport)
   {
      try
      {
         return (connected(transport, sftp -> new EdiTestResult(true, "SFTP authenticated with pinned host key")));
      }
      catch(Exception e)
      {
         return (new EdiTestResult(false, "SFTP connection or host-key verification failed"));
      }
   }

   /*******************************************************************************
    ** Remote POSIX paths never touch a local filesystem.
    *******************************************************************************/
   private static String path(String directory, String name)
   {
      return (Objects.requireNonNullElse(directory, ".").replaceAll("/+$", "") + "/" + name);
   }

   /*******************************************************************************
    ** Short-lived resources prevent credentials and sessions crossing transports.
    *******************************************************************************/
   private <T> T connected(EdiTransport transport, Operation<T> operation) throws Exception
   {
      try(SshClient client = SshClient.setUpDefaultClient())
      {
         client.setServerKeyVerifier((session, address, key) -> transport.getSftpHostKeyFingerprint() != null && transport.getSftpHostKeyFingerprint().equals(KeyUtils.getFingerPrint(key)));
         client.start();
         try(ClientSession session = client.connect(transport.getSftpUsername(), transport.getSftpHost(), Objects.requireNonNullElse(transport.getSftpPort(), 22)).verify(Duration.ofSeconds(15)).getSession())
         {
            var secrets = EdiRuntime.current().config().getSecretsResolver();
            if(transport.getSftpPasswordEnvVar() != null)
            {
               session.addPasswordIdentity(Objects.requireNonNull(secrets.resolve(transport.getSftpPasswordEnvVar()), "SFTP password is unavailable"));
            }
            if(transport.getSftpPrivateKeyEnvVar() != null)
            {
               try(InputStream key = new ByteArrayInputStream(Objects.requireNonNull(secrets.resolve(transport.getSftpPrivateKeyEnvVar()), "SFTP key is unavailable").getBytes(StandardCharsets.UTF_8)))
               {
                  for(var pair : SecurityUtils.loadKeyPairIdentities(session, NamedResource.ofName("EDI environment key"), key, FilePasswordProvider.EMPTY))
                  {
                     session.addPublicKeyIdentity(pair);
                  }
               }
            }
            session.auth().verify(Duration.ofSeconds(15));
            try(SftpClient sftp = SftpClientFactory.instance().createSftpClient(session))
            {
               return (operation.run(sftp));
            }
         }
      }
   }

   /*******************************************************************************
    ** Work using an authenticated streaming client.
    *******************************************************************************/
   @FunctionalInterface
   private interface Operation<T>
   {
      /*******************************************************************************
       ** Perform one remote operation.
       *******************************************************************************/
      T run(SftpClient client) throws Exception;
   }
}
