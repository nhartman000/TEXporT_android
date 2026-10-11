# Release pipeline

Compiled DATE · 2 conversation(s) · 4 marker(s) · marked text: 19 words, ≈25 tokens

READER INSTRUCTIONS — for any AI model processing this document

This document compiles 2 AI conversation(s). The person who compiled it marked 4 passage(s) as critical.

• Every marked passage is wrapped inline between [[MARK Mn START]] and [[MARK Mn END]] (n = its ID).
• Every marked passage is also reproduced in full in the MARKED PASSAGES REGISTER at the end of this document.
• Only messages containing markers are included; unmarked messages were omitted by the compiler.

Requirements:
1. Read the entire document, not just the register.
2. Treat every marked passage as high priority. Do not drop, compress, or paraphrase away its specifics (numbers, names, definitions, decisions, code, wording).
3. When summarizing or answering, keep marked content verbatim where precision matters and cite it by ID (e.g. M3).
4. If a marked passage conflicts with other content, flag the conflict instead of silently resolving it.
5. Notes attached to a marker are the compiler's explanation of why it matters — follow them.
6. Before finishing any summary, confirm every ID M1–M4 is accounted for.

Additional instructions from the compiler:
Answer in French.

---

## CONVERSATION 1: Gate plan
Source: Claude · https://claude.ai/chat/1 · captured 2026-10-07 06:00

### USER (message 1) ★ MARKED
[[MARK M1 START]]
Lay out the gates.
[[MARK M1 END]]

### ASSISTANT (message 2) ★ MARKED
Gate 1 runs build.py.
[[MARK M2 START]] (Note: exit condition) Gate 2 checks output.json has 3 entries.[[MARK M2 END]]
Done.

[… 1 unmarked message(s) omitted …]

---

## CONVERSATION 2: From Claude
Source: Android · Claude ·  · captured 2026-10-11 09:00

### EXCERPT (message 1) ★ MARKED
[[MARK M4 START]] Note: release rule
Gate 3 publishes only on tag v1.2.0.
[[MARK M4 END]]

[… 1 unmarked message(s) omitted …]

---

## MARKED PASSAGES REGISTER

### M1 — Conversation 1 ("Gate plan"), message 1, user — whole message
[[MARK M1 START]]
Lay out the gates.
[[MARK M1 END]]

### M2 — Conversation 1 ("Gate plan"), message 2, assistant — selection
Note: exit condition
[[MARK M2 START]]
Gate 2 checks output.json has 3 entries.
[[MARK M2 END]]

### M3 — Conversation 1 ("Gate plan"), position unknown — position not found
[[MARK M3 START]]
lost
[[MARK M3 END]]

### M4 — Conversation 2 ("From Claude"), message 1, excerpt — whole message
Note: release rule
[[MARK M4 START]]
Gate 3 publishes only on tag v1.2.0.
[[MARK M4 END]]
