# EDI local demo

From this worktree with Java 21:

```powershell
./mvnw.cmd -B test-compile exec:java '-Dexec.classpathScope=test' '-Dexec.mainClass=com.kingsrook.qbits.edi.demo.EdiDemoServer' '-Dexec.args=8099'
```

Open http://localhost:8099/app/edi. Stop with Ctrl+C. On Unix use `./mvnw`.
Next UI table routes are `/app/<tableName>` and record routes are `/app/<tableName>/<id>`.
Each launch creates a fresh in-memory H2 database, an in-memory synthetic payload key and a
fully anonymous demo session. No external credentials or services are required. Demo auth is
in test sources only and must not be installed in a real host.

The seed includes Northwind Retail (gateway), Harbor Logistics (SFTP) and Summit Market (HTTPS),
with codes NORTHWIND, HARBOR and SUMMIT and synthetic contacts at `example.invalid`.
It includes identities/agreements, guides/maps, orders with lines, shipments with cartons/items,
invoices and inventory. Forty synthetic inbound documents go through intake and the real inbox worker:
850s, 860s, validation errors, an unknown sender and a byte duplicate. Northwind's 855/856/810/846
pass the outbound worker and an in-memory gateway; incoming 997s reconcile some sent groups.
An overdue group, an exhausted send and several open exceptions provide operational examples.
Public self-signed certificates are generated in memory (two for expiry alerts, three for AS2); private keys are never persisted.

Pacific Wholesale (code PACIFIC) is the AS2 partner. An in-process BouncyCastle peer listens on a
random loopback port and the seed generates synthetic RSA certificates in memory (our ACTIVE and NEXT
certificates and the partner's; the private keys live only in the demo secrets map and are never
written to disk). The seed sends three 850s to the application's own AS2 hook (signed, encrypted;
two with base64 transfer encoding), which answers with signed MDNs (`edi_as2_mdn` rows, shown as
"AS2 MDNs" on the inbound message); the 997s go back over AS2 as well. An 855 built from a source record is sent
over AS2 to the peer, which verifies the signature and MIC and returns a signed synchronous MDN
(the outbound message becomes DELIVERED with its `transport_mic`). One more message goes to the
asynchronous transport `pacific-as2-async`: it stays SENT until the peer posts the MDN to
`/qqq/hooks/edi/as2/pacific-as2-async/mdn`. The running demo serves
`POST /qqq/hooks/edi/as2/pacific-as2` (and `.../mdn`) on the same port; Test Connection,
Export Public Certificate and Promote Certificate (on `demo-as2-next`) work on the AS2 records.

Phase d adds warehouse and logistics traffic, seeded last so the retail scenarios above are
unchanged. Harbor Logistics (the 3PL) gets a gateway OUTBOX transport (the partner pulls over SFTP)
and sends three valid 940 shipping orders and one with a bad unit; the orders and lines land in
`sample_wms_order`, and two shipments (one short) go out as 945s. A fourth partner, Ridgeline Freight
(code RIDGELINE, gateway transport), receives two 204 load tenders (the second a change) and sends
four 214 status events (one rejected with a bad status code) that build `sample_shipment_status`.
The in-memory gateway accepts every send, and the rejected documents produce rejecting 997s.

The SFTP/HTTPS partner settings are illustrative `.invalid` destinations. Their agreements have
no outbound transport, so they cannot contact a real partner; resulting acknowledgement send
retries are visible. Northwind alone has configured outbound documents. The in-memory gateway
is restored after the intentional dead-letter scenario. Standard Send As EDI, Test Connection,
Resend, Resend As New and Cancel actions are available. The gateway stores bytes in memory;
it does not expose a separate partner download UI.

Acceptance boots a random-port QApplicationJavalinServer, loads Next UI, renders and serializes
every registered widget through core RenderWidgetAction/JsonUtils, and POSTs every home and
child-list widget over HTTP using seeded parent records. It also checks configuration JSON,
metadata, table queries, labels, field order, foreign-key value sources and PHI-safe record labels.
Next UI defaults table queries to descending ID (newest records first); activity child lists use
descending creation date and ID. The home exception queue puts OPEN before IN_PROGRESS, newest
first within each status. RC.5 has no table-level default-sort metadata to override Next's sort.
Inbound message labels use external IDs because filenames are classified as PHI.
Light/dark visual layout and production scheduler/gateway deployment remain host acceptance.
