'use strict';
const form = document.getElementById('usernameForm');
const msg = document.getElementById('auth-message');
function showMessage(text, type) {
  msg.textContent = text;
  msg.className = 'auth-message ' + type;
  msg.classList.remove('hidden');
}
function normalizeUsername(u) {
  return (u || '').trim().toLowerCase();
}
function validUsername(u) {
  return /^[a-z0-9._]{3,20}$/.test(u);
}
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const username = normalizeUsername(document.getElementById('username').value);
  if (!validUsername(username)) {
    showMessage('Username must be 3-20 chars and contain letters/numbers/dot/underscore.', 'error');
    return;
  }
  const res = await fetch('/api/user/username', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username })
  });
  const data = await res.json().catch(() => ({ success: false, message: 'Bad response' }));
  if (!res.ok || !data.success) {
    showMessage(data.message || 'Failed to save username', 'error');
    return;
  }
  localStorage.setItem('username', username);
  showMessage('Username saved! Redirecting...', 'success');
  setTimeout(() => window.location.href = '/chat.html', 500);
});