'use strict';

// ===== USER SESSION =====
const userEmail = localStorage.getItem('userEmail');
const displayName = localStorage.getItem('displayName');
const userRole = localStorage.getItem('role');

// Redirect to login if not authenticated
if (!userEmail || !displayName) {
    window.location.href = '/index.html';
}

// ===== DOM ELEMENTS =====
const messageArea = document.getElementById('messageArea');
const messageForm = document.getElementById('messageForm');
const messageInput = document.getElementById('message');
const connectingElement = document.getElementById('connecting');
const userInfoElement = document.getElementById('user-info');
const logoutBtn = document.getElementById('logoutBtn');
const fileInput = document.getElementById('fileInput');
const filePreview = document.getElementById('file-preview');
const filePreviewName = document.getElementById('file-preview-name');
const filePreviewCancel = document.getElementById('file-preview-cancel');
const adminPanel = document.getElementById('admin-panel');

let stompClient = null;
let selectedFile = null;

// Avatar colors
const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0',
    '#7c4dff', '#e91e63', '#00e676', '#304ffe'
];

// ===== INITIALIZE =====
function init() {
    userInfoElement.textContent = `${displayName} (${userRole})`;

    // Show admin panel if admin
    if (userRole === 'ADMIN') {
        adminPanel.classList.remove('hidden');
        setupAdminControls();
    }

    // Load chat history
    loadChatHistory();

    // Connect to WebSocket
    connect();
}

// ===== WEBSOCKET CONNECT =====
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // disable debug logs

    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Send join message
    stompClient.send('/app/chat.addUser', {}, JSON.stringify({
        sender: displayName,
        senderEmail: userEmail,
        type: 'JOIN'
    }));

    connectingElement.classList.add('hidden');
}

function onError() {
    connectingElement.textContent = '❌ Connection failed. Refresh the page.';
    connectingElement.style.color = '#ff5652';
}

// ===== LOAD CHAT HISTORY =====
async function loadChatHistory() {
    try {
        const response = await fetch('/api/messages/history');
        const messages = await response.json();
        messages.forEach(msg => renderMessage(msg));
        messageArea.scrollTop = messageArea.scrollHeight;
    } catch (error) {
        console.error('Failed to load history:', error);
    }
}

// ===== SEND MESSAGE =====
messageForm.addEventListener('submit', (e) => {
    e.preventDefault();

    // If file is selected, upload it first
    if (selectedFile) {
        uploadAndSendFile();
        return;
    }

    const content = messageInput.value.trim();
    if (content && stompClient) {
        stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
            sender: displayName,
            senderEmail: userEmail,
            content: content,
            type: 'CHAT'
        }));
        messageInput.value = '';
    }
});

// ===== FILE HANDLING =====
fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        selectedFile = file;
        filePreviewName.textContent = `📄 ${file.name} (${formatFileSize(file.size)})`;
        filePreview.classList.remove('hidden');
    }
});

filePreviewCancel.addEventListener('click', () => {
    selectedFile = null;
    fileInput.value = '';
    filePreview.classList.add('hidden');
});

async function uploadAndSendFile() {
    if (!selectedFile) return;

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('uploaderEmail', userEmail);
    formData.append('uploaderName', displayName);

    try {
        const response = await fetch('/api/files/upload', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (data.success) {
            // Send file message via WebSocket
            stompClient.send('/app/chat.sendFile', {}, JSON.stringify({
                sender: displayName,
                senderEmail: userEmail,
                content: messageInput.value.trim() || '',
                type: 'FILE',
                fileId: data.fileId,
                fileName: data.fileName,
                fileType: data.fileType,
                fileSize: data.fileSize
            }));

            // Clear file selection
            selectedFile = null;
            fileInput.value = '';
            filePreview.classList.add('hidden');
            messageInput.value = '';
        } else {
            alert('Upload failed: ' + data.message);
        }
    } catch (error) {
        alert('Upload error: ' + error.message);
    }
}

// ===== ON MESSAGE RECEIVED =====
function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    // Handle system messages from admin
    if (message.type === 'SYSTEM') {
        handleSystemMessage(message);
        return;
    }

    renderMessage(message);
    messageArea.scrollTop = messageArea.scrollHeight;
}

// ===== RENDER MESSAGE =====
function renderMessage(message) {
    const li = document.createElement('li');
    li.setAttribute('data-message-id', message.id || '');

    if (message.type === 'JOIN') {
        li.classList.add('event-message');
        li.textContent = `🟢 ${message.sender} joined the chat!`;

    } else if (message.type === 'LEAVE') {
        li.classList.add('event-message');
        li.textContent = `🔴 ${message.sender} left the chat!`;

    } else if (message.type === 'FILE') {
        li.classList.add('file-message');
        li.style.position = 'relative';

        // Avatar
        const avatar = createAvatar(message.sender);
        li.appendChild(avatar);

        // Username
        const usernameEl = document.createElement('span');
        usernameEl.classList.add('username');
        usernameEl.textContent = message.sender;
        li.appendChild(usernameEl);

        // Caption text (if any)
        if (message.content) {
            const captionEl = document.createElement('span');
            captionEl.classList.add('message-content');
            captionEl.textContent = message.content;
            li.appendChild(captionEl);
        }

        // File attachment box
        const fileBox = document.createElement('div');
        fileBox.classList.add('file-attachment');

        // File icon
        const iconEl = document.createElement('span');
        iconEl.classList.add('file-icon');
        iconEl.textContent = getFileIcon(message.fileType || '');
        fileBox.appendChild(iconEl);

        // File details
        const detailsEl = document.createElement('div');
        detailsEl.classList.add('file-details');
        const nameEl = document.createElement('div');
        nameEl.classList.add('file-name');
        nameEl.textContent = message.fileName || 'File';
        detailsEl.appendChild(nameEl);
        const sizeEl = document.createElement('div');
        sizeEl.classList.add('file-size');
        sizeEl.textContent = formatFileSize(message.fileSize || 0);
        detailsEl.appendChild(sizeEl);
        fileBox.appendChild(detailsEl);

        // Download button
        const dlBtn = document.createElement('a');
        dlBtn.classList.add('file-download-btn');
        dlBtn.href = `/api/files/download/${message.fileId}`;
        dlBtn.textContent = '⬇ Download';
        dlBtn.target = '_blank';
        fileBox.appendChild(dlBtn);

        li.appendChild(fileBox);

        // Image preview if it's an image
        if (message.fileType && message.fileType.startsWith('image/')) {
            const img = document.createElement('img');
            img.classList.add('chat-image-preview');
            img.src = `/api/files/download/${message.fileId}`;
            img.alt = message.fileName;
            img.onclick = () => window.open(img.src, '_blank');
            li.appendChild(img);
        }

        // Timestamp
        const timeEl = createTimestamp(message.timestamp);
        li.appendChild(timeEl);

        // Admin delete button
        if (userRole === 'ADMIN') {
            li.appendChild(createDeleteButton(message.id));
        }

    } else {
        // Normal CHAT message
        li.classList.add('chat-message');
        li.style.position = 'relative';

        // Avatar
        const avatar = createAvatar(message.sender);
        li.appendChild(avatar);

        // Username
        const usernameEl = document.createElement('span');
        usernameEl.classList.add('username');
        usernameEl.textContent = message.sender;
        li.appendChild(usernameEl);

        // Content
        const contentEl = document.createElement('span');
        contentEl.classList.add('message-content');
        contentEl.textContent = message.content;
        li.appendChild(contentEl);

        // Timestamp
        const timeEl = createTimestamp(message.timestamp);
        li.appendChild(timeEl);

        // Admin delete button
        if (userRole === 'ADMIN') {
            li.appendChild(createDeleteButton(message.id));
        }
    }

    messageArea.appendChild(li);
}

// ===== HANDLE SYSTEM MESSAGES (Admin actions) =====
function handleSystemMessage(message) {
    const content = message.content;

    if (content === 'CHAT_CLEARED') {
        messageArea.innerHTML = '';
        const li = document.createElement('li');
        li.classList.add('event-message');
        li.textContent = '🛡️ Admin cleared the chat';
        messageArea.appendChild(li);

    } else if (content.startsWith('MESSAGE_DELETED:')) {
        const msgId = content.split(':')[1];
        const el = document.querySelector(`[data-message-id="${msgId}"]`);
        if (el) {
            el.style.animation = 'fadeOut 0.3s ease';
            setTimeout(() => el.remove(), 300);
        }

    } else if (content.startsWith('USER_BANNED:')) {
        const bannedEmail = content.split(':')[1];
        if (bannedEmail === userEmail) {
            alert('You have been banned by the admin.');
            logout();
        }
        const li = document.createElement('li');
        li.classList.add('event-message');
        li.textContent = `🛡️ A user has been banned by admin`;
        messageArea.appendChild(li);

    } else if (content.startsWith('USER_KICKED:')) {
        const kickedEmail = content.split(':')[1];
        if (kickedEmail === userEmail) {
            alert('You have been kicked by the admin.');
            logout();
        }
    }

    messageArea.scrollTop = messageArea.scrollHeight;
}

// ===== ADMIN CONTROLS =====
function setupAdminControls() {
    const adminUsername = localStorage.getItem('adminUsername');
    const adminPassword = localStorage.getItem('adminPassword');

    const toggleBtn = document.getElementById('toggleAdmin');
    const controls = document.getElementById('admin-controls');

    toggleBtn.addEventListener('click', () => {
        controls.classList.toggle('hidden');
    });

    // Clear Chat
    document.getElementById('clearChatBtn').addEventListener('click', async () => {
        if (!confirm('Are you sure you want to clear ALL messages?')) return;
        await adminAction({ action: 'CLEAR_CHAT' }, adminUsername, adminPassword);
    });

    // Ban User — now uses username, NOT email
    document.getElementById('banUserBtn').addEventListener('click', async () => {
        const username = document.getElementById('banUserEmail').value.trim();
        if (!username) return alert('Enter username to ban');
        if (!confirm(`Ban user "${username}"?`)) return;
        await adminAction({ action: 'BAN_USER', targetUsername: username }, adminUsername, adminPassword);
    });

    // Unban User
    document.getElementById('unbanUserBtn').addEventListener('click', async () => {
        const username = document.getElementById('banUserEmail').value.trim();
        if (!username) return alert('Enter username to unban');
        await adminAction({ action: 'UNBAN_USER', targetUsername: username }, adminUsername, adminPassword);
    });

    // Kick User
    document.getElementById('kickUserBtn').addEventListener('click', async () => {
        const username = document.getElementById('banUserEmail').value.trim();
        if (!username) return alert('Enter username to kick');
        await adminAction({ action: 'KICK_USER', targetUsername: username }, adminUsername, adminPassword);
    });

    // View Users
    document.getElementById('viewUsersBtn').addEventListener('click', async () => {
        try {
            const res = await fetch('/api/admin/users', {
                headers: {
                    'X-Admin-Username': adminUsername,
                    'X-Admin-Password': adminPassword
                }
            });
            const users = await res.json();
            const output = document.getElementById('admin-output');
            output.classList.remove('hidden');
            output.textContent = users.map(u =>
                `👤 ${u.displayName} | ${u.email} | ${u.authProvider} | ${u.enabled ? '✅ Active' : '🚫 Banned'}`
            ).join('\n');
        } catch (e) {
            alert('Error fetching users');
        }
    });

    // View Stats
    document.getElementById('viewStatsBtn').addEventListener('click', async () => {
        try {
            const res = await fetch('/api/admin/stats', {
                headers: {
                    'X-Admin-Username': adminUsername,
                    'X-Admin-Password': adminPassword
                }
            });
            const stats = await res.json();
            const output = document.getElementById('admin-output');
            output.classList.remove('hidden');
            output.textContent = `Total Users: ${stats.totalUsers}\nTotal Messages: ${stats.totalMessages}`;
        } catch (e) {
            alert('Error fetching stats');
        }
    });
}

async function adminAction(body, adminUsername, adminPassword) {
    try {
        const res = await fetch('/api/admin/action', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Admin-Username': adminUsername,
                'X-Admin-Password': adminPassword
            },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        alert(data.message);
    } catch (e) {
        alert('Admin action failed');
    }
}

// Admin: delete specific message
async function adminDeleteMessage(messageId) {
    if (!confirm('Delete this message?')) return;
    const adminUsername = localStorage.getItem('adminUsername');
    const adminPassword = localStorage.getItem('adminPassword');
    await adminAction({ action: 'DELETE_MESSAGE', messageId }, adminUsername, adminPassword);
}

// ===== HELPER FUNCTIONS =====
function createAvatar(name) {
    const avatar = document.createElement('span');
    avatar.classList.add('avatar');
    avatar.textContent = name ? name[0].toUpperCase() : '?';
    avatar.style.backgroundColor = getAvatarColor(name || '');
    return avatar;
}

function createTimestamp(timestamp) {
    const timeEl = document.createElement('span');
    timeEl.classList.add('timestamp');
    if (timestamp) {
        const date = new Date(timestamp);
        timeEl.textContent = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else {
        const now = new Date();
        timeEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return timeEl;
}

function createDeleteButton(messageId) {
    const btn = document.createElement('button');
    btn.classList.add('msg-delete-btn');
    btn.textContent = '✕';
    btn.title = 'Delete message';
    btn.onclick = (e) => {
        e.stopPropagation();
        adminDeleteMessage(messageId);
    };
    return btn;
}

function getAvatarColor(name) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = 31 * hash + name.charCodeAt(i);
    }
    return colors[Math.abs(hash % colors.length)];
}

function getFileIcon(contentType) {
    if (!contentType) return '📄';
    if (contentType.startsWith('image/')) return '🖼️';
    if (contentType.includes('pdf')) return '📕';
    if (contentType.includes('word') || contentType.includes('document')) return '📘';
    if (contentType.includes('excel') || contentType.includes('spreadsheet')) return '📗';
    if (contentType.includes('zip') || contentType.includes('rar')) return '📦';
    if (contentType.includes('text')) return '📝';
    return '📄';
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

// ===== LOGOUT =====
function logout() {
    localStorage.clear();
    fetch('/api/auth/logout', { method: 'POST' }).finally(() => {
        window.location.href = '/index.html';
    });
}

logoutBtn.addEventListener('click', logout);

// ===== START =====
init();