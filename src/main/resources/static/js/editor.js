const params = new URLSearchParams(window.location.search);
const docId = params.get('id');

let socket = null;
let saveTimeout = null;
let isRemoteUpdate = false;

document.addEventListener('DOMContentLoaded', async () => {
    if (!docId) {
        alert('No document specified.');
        window.location.href = '/dashboard.html';
        return;
    }

    await loadDocument();
    connectWebSocket();

    const contentArea = document.getElementById('doc-content');
    contentArea.addEventListener('input', onLocalEdit);

    document.getElementById('doc-title').addEventListener('blur', saveTitle);
});

async function loadDocument() {
    const doc = await apiFetch(`/documents/${docId}`);
    document.getElementById('doc-title').value = doc.title;
    document.getElementById('doc-content').value = doc.content || '';
}

function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    socket = new WebSocket(`${protocol}://${window.location.host}/ws/document/${docId}`);

    socket.onopen = () => setStatus('Connected — live sync active');

    socket.onmessage = (event) => {
        isRemoteUpdate = true;
        const contentArea = document.getElementById('doc-content');
        const cursorPos = contentArea.selectionStart;
        contentArea.value = event.data;
        // best-effort cursor preservation for the local typist
        contentArea.setSelectionRange(cursorPos, cursorPos);
        isRemoteUpdate = false;
        setStatus('Synced from collaborator');
    };

    socket.onclose = () => setStatus('Disconnected — reconnecting...');
    socket.onerror = () => setStatus('Connection error');
}

function onLocalEdit() {
    if (isRemoteUpdate) return;

    setStatus('Editing...');
    const content = document.getElementById('doc-content').value;

    // Broadcast over WebSocket immediately for live collaboration
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(content);
    }

    // Debounced REST save as a durability fallback
    clearTimeout(saveTimeout);
    saveTimeout = setTimeout(() => saveDocument(content), 800);
}

async function saveDocument(content) {
    try {
        await apiFetch(`/documents/${docId}`, {
            method: 'PUT',
            body: JSON.stringify({ content })
        });
        setStatus('Saved');
    } catch (err) {
        setStatus('Save failed: ' + err.message);
    }
}

async function saveTitle() {
    const title = document.getElementById('doc-title').value;
    try {
        await apiFetch(`/documents/${docId}`, {
            method: 'PUT',
            body: JSON.stringify({ title })
        });
        setStatus('Saved');
    } catch (err) {
        setStatus('Save failed: ' + err.message);
    }
}

function setStatus(text) {
    document.getElementById('save-status').textContent = text;
}

// ---- Sharing ----
function openShareModal() {
    document.getElementById('share-modal').classList.remove('hidden');
    loadCollaborators();
}
function closeShareModal() {
    document.getElementById('share-modal').classList.add('hidden');
}

async function shareDocument() {
    const email = document.getElementById('share-email').value;
    const role = document.getElementById('share-role').value;
    if (!email) return;
    try {
        await apiFetch(`/documents/${docId}/share`, {
            method: 'POST',
            body: JSON.stringify({ userEmail: email, role })
        });
        document.getElementById('share-email').value = '';
        loadCollaborators();
    } catch (err) {
        alert(err.message);
    }
}

async function loadCollaborators() {
    const collaborators = await apiFetch(`/documents/${docId}/share`);
    const list = document.getElementById('collaborator-list');
    list.innerHTML = '';
    collaborators.forEach(c => {
        const li = document.createElement('li');
        li.textContent = `${c.user.email} — ${c.role}`;
        list.appendChild(li);
    });
}
