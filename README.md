# Google Workspace Lite v2

I got tired of Google Docs lagging and wanted to see how real-time editing actually works. So I built my own version.

It's not Google Docs. It's buggy. But it works and I learned a ton 😂

## What works right now
- Login/Signup with JWT
- Create folders and docs. You can nest them
- Open the same doc in 2 tabs and see edits live. WebSocket magic
- Share with people: Owner, Editor, or Viewer
- H2 DB by default so you can run it in 10 seconds. Use MySQL if you want data to stay

## Tech I used
Java 17 + Spring Boot 3.2
Spring Security + JWT because auth is pain
H2 for demo. MySQL for real
Vanilla JS frontend. Didn't want to touch React yet

## How to run
git clone <repo>
cd google-workspace-lite
mvn spring-boot:run

Go to http://localhost:8080/index.html

## What's broken / Next
v1 has "last-write-wins". If 2 people type at once, someone loses their text.
v2 fixes this with Operational Transform. Also adding: presence cursors, comments, and proper conflict resolution.

---
Built by Akshaya. Star the repo if you found it useful ⭐