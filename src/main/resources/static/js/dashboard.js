document.addEventListener('DOMContentLoaded', async () => {
    const user = getUser();
    if (user) document.getElementById('user-name').textContent = user.fullName || user.email;

    document.getElementById('create-document-form').addEventListener('submit', createDocument);
    document.getElementById('create-folder-form').addEventListener('submit', createFolder);

    await Promise.all([loadFolders(), loadMyDocuments(), loadSharedDocuments()]);
});

function openDocumentModal() {
    openCreateModal('document-modal', 'new-document-title');
}

function openFolderModal() {
    openCreateModal('folder-modal', 'new-folder-name');
}

function openCreateModal(modalId, inputId) {
    document.getElementById(modalId).classList.remove('hidden');
    const input = document.getElementById(inputId);
    input.focus();
    input.select();
}

function closeCreateModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
    const form = document.querySelector(`#${modalId} form`);
    const error = document.querySelector(`#${modalId} .form-error`);
    form.reset();
    error.textContent = '';
}

async function loadFolders() {
    const folders = await apiFetch('/folders/root');
    const list = document.getElementById('my-folders');
    list.innerHTML = '';
    if (folders.length === 0) {
        list.innerHTML = '<li>No folders yet. Create one above.</li>';
        return;
    }
    folders.forEach(folder => {
        const li = document.createElement('li');
        li.textContent = folder.name;
        list.appendChild(li);
    });
}

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

async function createDocument(event) {
    event.preventDefault();
    const title = document.getElementById('new-document-title').value.trim();
    const error = document.getElementById('document-error');
    error.textContent = '';
    try {
        const doc = await apiFetch('/documents', {
            method: 'POST',
            body: JSON.stringify({ title, content: '' })
        });
        window.location.href = `/editor.html?id=${doc.id}`;
    } catch (err) {
        error.textContent = err.message;
    }
}

async function createFolder(event) {
    event.preventDefault();
    const name = document.getElementById('new-folder-name').value.trim();
    const error = document.getElementById('folder-error');
    error.textContent = '';
    try {
        await apiFetch('/folders', {
            method: 'POST',
            body: JSON.stringify({ name })
        });
        closeCreateModal('folder-modal');
        await loadFolders();
        document.getElementById('dashboard-message').textContent = 'Folder created.';
    } catch (err) {
        error.textContent = err.message;
    }
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
