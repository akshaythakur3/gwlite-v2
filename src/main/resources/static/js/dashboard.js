document.addEventListener('DOMContentLoaded', async () => {
    const user = getUser();
    if (user) document.getElementById('user-name').textContent = user.fullName || user.email;

    await loadMyDocuments();
    await loadSharedDocuments();
});

async function loadMyDocuments() {
    const docs = await apiFetch('/documents/mine');
    const list = document.getElementById('my-documents');
    list.innerHTML = '';
    if (docs.length === 0) {
        list.innerHTML = '<li>No documents yet. Create one above.</li>';
        return;
    }
    docs.forEach(doc => {
        const li = document.createElement('li');
        li.innerHTML = `<span>${escapeHtml(doc.title)}</span><span style="color:#888;font-size:12px">v${doc.version}</span>`;
        li.onclick = () => window.location.href = `/editor.html?id=${doc.id}`;
        list.appendChild(li);
    });
}

async function loadSharedDocuments() {
    const docs = await apiFetch('/documents/shared-with-me');
    const list = document.getElementById('shared-documents');
    list.innerHTML = '';
    if (docs.length === 0) {
        list.innerHTML = '<li>Nothing shared with you yet.</li>';
        return;
    }
    docs.forEach(doc => {
        const li = document.createElement('li');
        li.innerHTML = `<span>${escapeHtml(doc.title)}</span><span style="color:#888;font-size:12px">by ${escapeHtml(doc.owner.fullName)}</span>`;
        li.onclick = () => window.location.href = `/editor.html?id=${doc.id}`;
        list.appendChild(li);
    });
}

async function createDocument() {
    const title = prompt('Document title:', 'Untitled Document');
    if (!title) return;
    const doc = await apiFetch('/documents', {
        method: 'POST',
        body: JSON.stringify({ title, content: '' })
    });
    window.location.href = `/editor.html?id=${doc.id}`;
}

async function createFolder() {
    const name = prompt('Folder name:');
    if (!name) return;
    await apiFetch('/folders', {
        method: 'POST',
        body: JSON.stringify({ name })
    });
    alert('Folder created.');
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
