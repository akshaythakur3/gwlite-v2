# System Design — Google Workspace Lite

This document explains the *why* behind the architecture, not just the *what*.
Read this before an interview — it's written to be talked through out loud.

## 1. Requirements

**Functional**
- Users can create, edit, and delete documents and folders
- Multiple users can edit the same document concurrently and see each
  other's changes live
- Document owners can share access with specific users as EDITOR or VIEWER

**Non-functional**
- **Consistency** — concurrent edits shouldn't silently overwrite each other
- **Availability** — the system should tolerate a single server dying
- **Latency** — live edits should feel instant (sub-200ms round trip)

## 2. Current architecture (as built)

```
Browser --REST(JWT)--> Spring Boot Controller --> Service (permission checks) --> JPA/Hibernate --> DB
Browser --WebSocket--> DocumentWebSocketHandler --(in-memory ConcurrentHashMap)--> broadcasts to peers on the SAME instance
```

This runs correctly on **one server**. Everything below is about what breaks
when you scale past one server, and how to fix it — this is the part worth
knowing cold for an interview.

## 3. The scaling problem (the most important insight in this project)

Question to expect: *"What happens if you run two instances of this app
behind a load balancer?"*

Answer: **WebSocket broadcasting breaks.** The current `DocumentWebSocketHandler`
keeps its "who is editing what" map in local JVM memory. If User A lands on
Instance 1 and User B lands on Instance 2, and both are editing the same
document, neither will ever see the other's keystrokes — the in-memory map
on Instance 1 has no idea Instance 2 even exists.

**Fix: Redis pub/sub.**
- Every app instance subscribes to a Redis channel per active document
  (`doc:{id}`)
- When a server receives a local edit over WebSocket, instead of only
  broadcasting to its own local sessions, it **publishes** the edit to
  `doc:{id}` on Redis
- Every instance (including the originating one) is subscribed, so every
  instance broadcasts the edit to whichever of its own local sessions are
  viewing that document
- Redis becomes the shared source of truth for "who needs this edit,"
  decoupling the app servers from each other entirely

This is a textbook example of the **pub/sub fan-out pattern** used to make
stateful real-time features horizontally scalable.

## 4. Consistency model — last-write-wins vs. real conflict resolution

**What's implemented:** last-write-wins. Whichever edit reaches the database
last overwrites the `content` column. Simple, but if two people type in the
same region of a document at the same moment, one person's keystrokes can be
silently lost.

**What Google Docs actually uses:** Operational Transformation (OT) — every
incoming edit is mathematically transformed against any edits that happened
concurrently, so both survive in a consistent order.

**The modern alternative:** CRDTs (Conflict-free Replicated Data Types) —
used by Figma and Notion. Data structures designed so that no matter what
order edits arrive in across replicas, everyone converges to the identical
final state, with no central coordinator needed.

**Honest framing for an interview:** "I used last-write-wins because
implementing OT or a CRDT from scratch is its own multi-week project, but I
understand why it's insufficient at real scale, and I know the two accepted
solutions to the problem."

## 5. Database design decisions

- **`share_permissions` is a join table with an attribute (`role`)**, not a
  boolean flag on `documents`. Access isn't binary — the same document can
  have different users at different permission levels, so this needed a
  proper many-to-many-with-attributes relationship.
- **Folders self-reference (`parent_folder_id`)** to model a tree using the
  adjacency list pattern. It's simple to write and update, but "get all
  descendants of this folder" requires walking recursively (or a recursive
  CTE) rather than a single flat query. At scale, a materialized path or
  nested-set model would make descendant queries O(1) at the cost of more
  complex writes — a good trade-off to mention if asked "how would you
  speed up folder search."
- **`version` counter on documents** exists so clients can eventually detect
  "the document changed underneath me" — a stepping stone toward real
  conflict detection.

## 6. Rough scale estimation

Interviewers often want you to do napkin math out loud. Example:

> 100K daily active users, each averaging 3 open documents with live sync →
> up to ~300K concurrent WebSocket connections at peak. A single Spring Boot
> instance typically holds tens of thousands of WebSocket connections
> depending on heap and OS file descriptor limits — so you'd need somewhere
> around 6–30 instances behind a load balancer. That's exactly the point at
> which the Redis pub/sub layer above stops being optional.

## 7. Security model

- Passwords are hashed with BCrypt, never stored in plaintext
- JWT is stateless — no server-side session store, which is itself a
  scaling decision (any instance can validate any request without shared
  session state)
- Every document read/write goes through explicit permission checks in
  `DocumentService` (`requireAtLeastViewer` / `requireAtLeastEditor`) rather
  than trusting the client — access control is enforced server-side, always

## 8. What I'd build next, in priority order

1. Redis pub/sub for multi-instance WebSocket fan-out (Section 3)
2. Basic operational transform for text (or integrate an existing CRDT
   library like Yjs) to fix silent-overwrite conflicts (Section 4)
3. Document version history with rollback
4. Materialized path for folders if nesting gets deep (Section 5)
5. Rate limiting on the WebSocket endpoint to prevent abuse
