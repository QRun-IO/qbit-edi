# EDI local demo

From this worktree with Java 21:

```powershell
./mvnw.cmd -B test-compile exec:java '-Dexec.classpathScope=test' '-Dexec.mainClass=com.kingsrook.qbits.edi.demo.EdiDemoServer' '-Dexec.args=8099'
```

Open http://localhost:8099/app/edi. Stop with Ctrl+C. On Unix use `./mvnw`.
Each launch creates a fresh in-memory H2 database, an in-memory synthetic payload key and a
fully anonymous demo session. No external credentials or services are required. Demo auth is
in test sources only and must not be installed in a real host.

The seed includes Northwind Retail (gateway), Harbor Logistics (SFTP) and Summit Market (HTTPS),
identities/agreements, guides/maps, orders with lines, shipments with cartons/items, invoices
and inventory. Forty synthetic inbound documents go through intake and the real inbox worker:
850s, 860s, validation errors, an unknown sender and a byte duplicate. Northwind's 855/856/810/846
pass the outbound worker and an in-memory gateway; incoming 997s reconcile some sent groups.
An overdue group, an exhausted send and several open exceptions provide operational examples.
Two public self-signed certificates are generated in memory; private keys are never persisted.

The SFTP/HTTPS partner settings are illustrative `.invalid` destinations. Their agreements have
no outbound transport, so they cannot contact a real partner; resulting acknowledgement send
retries are visible. Northwind alone has configured outbound documents. The in-memory gateway
is restored after the intentional dead-letter scenario. Standard Send As EDI, Test Connection,
Resend, Resend As New and Cancel actions are available. The gateway stores bytes in memory;
it does not expose a separate partner download UI.

Acceptance boots a random-port QApplicationJavalinServer, loads Next UI, renders every typed
home widget, and requests metadata, table queries and the outbound widget over HTTP.
Light/dark visual layout and production scheduler/gateway deployment remain host acceptance.
