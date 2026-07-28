const API_BASE = '/api';

function getToken() { return localStorage.getItem('gwlite_token'); }
function getUser() { return JSON.parse(localStorage.getItem('gwlite_user') || 'null'); }

function saveSession(authResponse) {
    localStorage.setItem('gwlite_token', authResponse.token);
    localStorage.setItem('gwlite_user', JSON.stringify({
        email: authResponse.email,
        fullName: authResponse.fullName
    }));
}

function logout() {
    localStorage.removeItem('gwlite_token');
    localStorage.removeItem('gwlite_user');
    window.location.href = '/index.html';
}

async function apiFetch(path, options = {}) {
    const headers = options.headers || {};
    headers['Content-Type'] = 'application/json';
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(API_BASE + path, { ...options, headers });
    if (res.status === 401 || res.status === 403) {
        // token invalid/expired -> force re-login
        if (path !== '/auth/login' && path !== '/auth/register') {
            logout();
        }
    }
    const data = await res.json().catch(() => null);
    if (!res.ok) throw new Error((data && data.error) || 'Request failed');
    return data;
}

// ---- Auth page logic (only runs on index.html) ----
function showTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('login-form').classList.toggle('hidden', tab !== 'login');
    document.getElementById('register-form').classList.toggle('hidden', tab !== 'register');
    event.target.classList.add('active');
}

const loginForm = document.getElementById('login-form');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        try {
            const res = await apiFetch('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });
            saveSession(res);
            window.location.href = '/dashboard.html';
        } catch (err) {
            showAuthError(err.message);
        }
    });
}

const registerForm = document.getElementById('register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fullName = document.getElementById('register-name').value;
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        try {
            const res = await apiFetch('/auth/register', {
                method: 'POST',
                body: JSON.stringify({ fullName, email, password })
            });
            saveSession(res);
            window.location.href = '/dashboard.html';
        } catch (err) {
            showAuthError(err.message);
        }
    });
}

function showAuthError(msg) {
    const el = document.getElementById('auth-error');
    el.textContent = msg;
    el.classList.remove('hidden');
}

// Guard: redirect to login if not authenticated (for dashboard/editor pages)
if ((window.location.pathname.includes('dashboard') || window.location.pathname.includes('editor')) && !getToken()) {
    window.location.href = '/index.html';
}
