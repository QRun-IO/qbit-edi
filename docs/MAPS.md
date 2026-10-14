# Inbound maps

A map comprises an `edi_map` row and ordered `edi_map_rule` rows. EDI paths resolve through the
guide's loop tree; target paths support one association level. `condition`, `defaultValue` and
`required` apply within each loop occurrence. Missing optional values preserve child alignment.

`transformArgs` is a JSON object with string values. Omitted transforms copy the first value.

| Transform | Arguments / result |
| --- | --- |
| DATE | CCYYMMDD or YYMMDD; fixed 1950–2049 century window; QQQ date |
| TIME | HHMM, HHMMSS, fractional seconds; normalized string |
| DATETIME | Date and time values, or `time` argument; `timezone` defaults UTC |
| DECIMAL | `scale` implied decimal places, default 0 |
| QUALIFIER | Sibling predicate in `ediPath`, e.g. `N1[N101=ST]/N104` |
| CODE | Map-local translation object, e.g. `{"EA":"each","CA":"case"}` |
| UNIT | `factor`, `divisor`, `scale` (defaults 1, 1, 6), HALF_UP |
| CONSTANT | `value`; needs no EDI input |
| CONCAT | `separator`, default empty; joins selected values |
| SPLIT | Literal `separator` (comma), zero-based `index` (0) |
| SUBSTRING | Zero-based `start` inclusive, `end` exclusive |
| TRIM / UPPER | Whitespace trim / locale-independent uppercase |
| LOOKUP | `table`, `key`, `result` (default id); exactly one permitted row |
| SEQUENCE / HL | Outbound placeholders, reject inbound execution |

Supply the partner timezone explicitly for DATETIME. UNIT uses configured conversion factors/pack
sizes; host-dependent field lookups and code-list translation can register named transforms through
`EdiRuntime.current().transforms().register(name, transform)`. Transforms execute as the service
session. LOOKUP explicitly enforces permissions.

Samples reference encrypted PayloadStore objects. Preview JSON contains `values` and recursive
`associations`; `expectedResultJson` uses the same shape. Comparisons show structured before/after
JSON as Result lines. Preview never invokes target application. See the synthetic 850 resources
under `src/main/resources/edi/maps` and [GUIDE-FORMAT.md](GUIDE-FORMAT.md).
