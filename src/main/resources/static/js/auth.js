'use strict';

/**
 * auth.js
 * - Login (user/admin) + Registration (LOCAL users)
 * - Gmail-only validation (client-side)
 * - Unique username validation (client-side)
 * - Reliable message display (fixes “box shows but no text”)
 */

const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');

/* ========= Message UI (FIXED) ========= */
function showMessage(text, type) {
  const el = document.getElementById('auth-message');
  if (!el) return;

  const msg = (text === undefined || text === null) ? '' : String(text);

  // Put text
  el.textContent = msg;

  // Ensure correct classes
  el.classList.remove('hidden', 'success', 'error');
  el.classList.add(type === 'success' ? 'success' : 'error');

  // Force visibility (in case CSS conflicts)
  el.style.display = 'block';
  el.style.visibility = 'visible';
  el.style.opacity = '1';
}
//function showMessage(text, type) {
//  const el = document.getElementById('auth-message');
//  if (!el) return;
//
//  const msg = (text === undefined || text === null) ? '' : String(text);
//
//  // Debug (you can remove later)
//  console.log('[auth-message]', { type, msg, el });
//
//  el.textContent = msg;
//
//  el.classList.remove('hidden', 'success', 'error');
//  el.classList.add(type === 'success' ? 'success' : 'error');
//
//  // Force visibility + readable text
//  el.style.display = 'block';
//  el.style.visibility = 'visible';
//  el.style.opacity = '1';
//
//  el.style.color = '#111';           // ensure text visible
//  el.style.fontSize = '14px';
//  el.style.lineHeight = '1.4';
//  el.style.whiteSpace = 'pre-wrap';  // wrap long text
//  el.style.padding = '10px 12px';
//}

/* ========= Helpers ========= */
function normalizeEmail(email) {
  return (email || '').trim().toLowerCase();
}

function isValidGmail(email) {
  return /^[a-z0-9._%+-]+@gmail\.com$/i.test((email || '').trim());
}

function normalizeUsername(username) {
  return (username || '').trim().toLowerCase();
}

function isValidUsername(username) {
  // 3–20 chars, letters/numbers/dot/underscore only
  return /^[a-z0-9._]{3,20}$/i.test((username || '').trim());
}

function isPasswordStrong(password) {
  return typeof password === 'string'
    && password.length >= 8
    && /[A-Z]/.test(password)
    && /[a-z]/.test(password)
    && /\d/.test(password)
    && /[!@#$%^&*()_+\-=\[\]{}|;':",.\/<>?]/.test(password);
}

function updateReq(id, valid) {
  const el = document.getElementById(id);
  if (!el) return;

  el.className = valid ? 'valid' : 'invalid';

  const oldText = el.textContent || '';
  const rest = oldText.length >= 2 ? oldText.substring(2) : oldText;
  el.textContent = (valid ? '✓ ' : '✗ ') + rest;
}

async function safeJson(res) {
  // Always return an object with a meaningful message
  const text = await res.text();

  if (!text) {
    return { success: false, message: `Request failed (${res.status}) with empty response.` };
  }

  try {
    return JSON.parse(text);
  } catch {
    // If backend returned plain text / HTML error
    return { success: false, message: text };
  }
}

/* ========= Redirect if already logged in ========= */
(function redirectIfAlreadyLoggedIn() {
  const email = localStorage.getItem('userEmail');
  const displayName = localStorage.getItem('displayName');
  if (email && displayName) {
    window.location.href = '/chat.html';
  }
})();

/* ========= LOGIN ========= */
if (loginForm) {
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const rawEmailOrUsername = document.getElementById('loginEmail')?.value ?? '';
    const password = document.getElementById('loginPassword')?.value ?? '';

    if (!rawEmailOrUsername.trim() || !password) {
      showMessage('Please fill in all fields.', 'error');
      return;
    }

    const emailOrUsername = rawEmailOrUsername.includes('@')
      ? normalizeEmail(rawEmailOrUsername)
      : rawEmailOrUsername.trim();

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: emailOrUsername, password })
      });

      const data = await safeJson(res);

      if (!res.ok || !data.success) {
        showMessage(data.message || 'Login failed.', 'error');
        return;
      }

      localStorage.setItem('userEmail', data.email || '');
      localStorage.setItem('displayName', data.displayName || '');
      localStorage.setItem('role', data.role || 'USER');
      localStorage.setItem('authProvider', data.authProvider || 'LOCAL');

      if (data.username) localStorage.setItem('username', data.username);

      if (data.role === 'ADMIN') {
        localStorage.setItem('adminUsername', emailOrUsername);
        localStorage.setItem('adminPassword', password);
      } else {
        localStorage.removeItem('adminUsername');
        localStorage.removeItem('adminPassword');
      }

      showMessage('Login successful! Redirecting...', 'success');
      setTimeout(() => window.location.href = '/chat.html', 400);
    } catch (err) {
      showMessage('Network error. Please check your connection and try again.', 'error');
    }
  });
}

/* ========= REGISTER ========= */
if (registerForm) {
  const passwordInput = document.getElementById('regPassword');

  if (passwordInput) {
    passwordInput.addEventListener('input', () => {
      const password = passwordInput.value || '';
      updateReq('req-length', password.length >= 8);
      updateReq('req-upper', /[A-Z]/.test(password));
      updateReq('req-lower', /[a-z]/.test(password));
      updateReq('req-digit', /\d/.test(password));
      updateReq('req-special', /[!@#$%^&*()_+\-=\[\]{}|;':",.\/<>?]/.test(password));
    });
  }

  registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const displayName = (document.getElementById('regName')?.value ?? '').trim();
    const username = normalizeUsername(document.getElementById('regUsername')?.value ?? '');
    const email = normalizeEmail(document.getElementById('regEmail')?.value ?? '');
    const password = document.getElementById('regPassword')?.value ?? '';
    const confirmPassword = document.getElementById('regConfirmPassword')?.value ?? '';

    // Basic required checks
    if (!displayName || !username || !email || !password || !confirmPassword) {
      showMessage('Please fill in all fields.', 'error');
      return;
    }

    // Username rules
    if (!isValidUsername(username)) {
      showMessage('Username must be 3-20 characters and can contain letters, numbers, dot (.) and underscore (_).', 'error');
      return;
    }

    // Gmail-only rule
    if (!isValidGmail(email)) {
      showMessage('Only Gmail addresses are allowed (example@gmail.com).', 'error');
      return;
    }

    // Password match & strength
    if (password !== confirmPassword) {
      showMessage('Passwords do not match.', 'error');
      return;
    }

    if (!isPasswordStrong(password)) {
      showMessage('Password does not meet the requirements shown below.', 'error');
      return;
    }

    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ displayName, username, email, password })
      });

      const data = await safeJson(res);

      if (!res.ok || !data.success) {
        showMessage(data.message || 'Registration failed.', 'error');
        return;
      }

      showMessage(data.message || 'Registration successful! Redirecting to login...', 'success');
      setTimeout(() => window.location.href = '/index.html', 1200);

    } catch (err) {
      showMessage('Network error. Please check your connection and try again.', 'error');
    }
  });
}