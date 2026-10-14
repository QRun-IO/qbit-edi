# EDI guide JSON

The files in `src/main/resources/edi/guides/` are original QQQ baseline guides, not licensed
X12/TR3 documentation or complete code sets. JSON loads into `guide.Guide` with
`BuiltInGuideLoader.read(InputStream)`. `GuideRepository.load(id)` reconstructs the same model from
the normalized `edi_guide*` and code-list tables.

The root has `name`, `standard`, `version`, `transactionSet`, `functionalIdCode`, ordered
`segments`, and optional `rules`. A segment has:

- `segmentId`, `usage` (`REQUIRED`, `SITUATIONAL`, `NOT_USED`), `minUse`, `maxUse`;
- optional `loopId`, `parentLoopId`, and `loopMaxRepeat`; the first segment starts each loop
  occurrence, with parents declared before children;
- optional boolean `condition`, `elements`, and `rules`.

An element has one-based `position` and optional `componentPosition`, `elementId`, `dataType`
(`AN`, `ID`, `N0`–`N9`, `R`, `DT`, `TM`, `B`), `minLength`, `maxLength`, `usage`, `fixedValue`,
`repeatMax`, and `condition`. `codes` is an optional array of permitted ID values. Omitted/null
means no code constraint; an explicitly empty array permits no values. DT accepts YYMMDD with
a 1950–2049 century window or CCYYMMDD; TM accepts HHMM, HHMMSS, and one/two fractional digits.
N types accept integer lexical values with an implied decimal point.

Rules use `type` (`PAIRED`, `REQUIRED`, `EXCLUSION`, `CONDITIONAL`, `LIST_CONDITIONAL`, or
`SITUATIONAL`), `positions` for syntax notes, or `expression` for situational rules. Optional
`snipLevel`, `severity`, `message`, `ak3Code`, `ak4Code`, `ik3Code`, and `ik4Code` customize findings.
Syntax-note positions are one-based elements. Segment/element `condition` means “required when
this expression is true.” Explicit `when(...)` rules express prohibitions and other relationships.

Paths include `BEG03`, `REF01-2`, `N1/N101`, `N1[N101=ST]/N301`, `PO1[*]/PO102`, and
`N1[2]/N101`. Indexes start at one; unindexed paths select all occurrences. Loop scopes come from
the guide, so a qualified parent selects its child segments. Functions are `present`, `absent`,
`count`, `sum`, `eq`, `in`, `matches`, `date`, `and`, `or`, `not`, and `when`. String literals use
single quotes; numeric literals use decimal notation. `date` tests calendar validity. Examples:

```text
when(eq(N101, 'ST'), present(N3))
eq(sum(PO102), 12)
and(in(BEG01, '00', '01'), date(BEG05))
```

No script engine runs. PHI validation suppresses all excerpts and custom rule messages. Functional
acks omit submitted values; situational 999 CTX records carry only trigger positions, following
[X12's CTX interpretation](https://x12.org/resources/requests-for-interpretation/rfi-1311-999ctx-context).

At host startup, after schema creation and service-session initialization, call
`EdiRuntime.current().initialize()`. Unchanged resource checksums are no-ops. A changed resource
creates a new revision and retires the former active built-in revision in one database transaction;
historical rows and their children remain addressable by id. A reverted resource also creates a
new revision. Built-in guide writes require the loader's security key (`edi.guide.loader`), or a
system service session. Configuration editors can receive `CUSTOM`/`IMPORTED` values of the
`edi_guide_source` key without receiving permission to edit `BUILT_IN` rows.

## EDIFACT baselines

Set `standard` to `EDIFACT`, `transactionSet` to the UNH message type, and `version` to the
concatenated UNH version/release (`D96A`, `D01B`). The ten bundled resources are in-house retail
subsets with short field names and only the code values needed by the maps. They contain no
copied directory descriptions and are not complete UNECE directories or EANCOM certifications.

UNH and UNT appear as actual guide segments. UNB/UNZ and UNG/UNE are checked by the envelope
reader. UNH association codes, such as `EAN008`, remain available as component 5 of UNH02.
EDIFACT mandatory (`M`) maps to `REQUIRED`; conditional (`C`) maps to `SITUATIONAL` with
`condition` or rules where needed. A component uses the existing `position` plus
`componentPosition`, for example NAD02 component 1. EDIFACT decimal values accept `.` or `,`;
DECIMAL/UNIT inbound transforms normalize them and outbound DECIMAL uses the agreement's mark.

Segment groups use the existing loop tree: SG1 starts with RFF, SG2 with NAD, and the retail
line loop SG25 starts with LIN. These are stable local guide loop labels, not a claim that every
message/directory assigns the same published group numbers. DESADV uses SG10 (CPS) containing
SG17 (LIN), matching carton/item associations. Map examples are `NAD[NAD01=BY]/NAD02-1`,
`SG25/LIN/LIN03-1` and `SG25[*]/QTY/QTY01-2`. Named loops take precedence over element parsing,
so `SG25` is a loop rather than an element of a hypothetical `SG` segment.

CONTRL baselines include UCI, optional UCF group responses, UCM message responses and nested
UCS/UCD positional errors. Shared validator diagnostics are translated to EDIFACT 0085 codes;
submitted erroneous values are not copied into the acknowledgement. `GROUP_ONLY` reports the
interchange outcome, `ALL_TRANSACTIONS` includes accepted messages, and `ERRORS_ONLY` includes
rejected message details. A CONTRL is never itself acknowledged. APERAK is deferred.
