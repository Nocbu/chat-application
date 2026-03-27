'use strict';

// ===== USER SESSION =====
const userEmail = localStorage.getItem('userEmail');
const displayName = localStorage.getItem('displayName');
const userRole = localStorage.getItem('role');
const username = (localStorage.getItem('username') || '').trim().toLowerCase();

// Redirect to login if not authenticated
if (!userEmail || !displayName) {
    window.location.href = '/index.html';
}

// ===== DOM ELEMENTS =====
const messageArea = document.getElementById('messageArea');
const dmMessageArea = document.getElementById('dmMessageArea');
const dmPanel = document.getElementById('dmPanel');
const dmSidebar = document.getElementById('dmSidebar');
const dmWithEl = document.getElementById('dmWith');

const tabGroup = document.getElementById('tabGroup');
const tabDirect = document.getElementById('tabDirect');

const dmSearchInput = document.getElementById('dmSearchInput');
const dmSearchResults = document.getElementById('dmSearchResults');

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
const replyPreviewEl = document.getElementById('reply-preview');
const replyPreviewSenderEl = document.getElementById('replyPreviewSender');
const replyPreviewTextEl = document.getElementById('replyPreviewText');
const replyPreviewCancelBtn = document.getElementById('replyPreviewCancel');
const dmPresenceDot = document.getElementById('dmPresenceDot');

let stompClient = null;
let selectedFile = null;

// ===== DM STATE =====
let mode = 'GROUP'; // GROUP | DIRECT
let activeConversationId = null;
let activeDmUsername = null;
let dmSubscription = null;

// ===== PRESENCE STATE =====
const onlineUsers = new Set(); // usernames of currently online users

// ===== REPLY STATE =====
let pendingReply = null; // { messageId, content, sender }

// Avatar colors
const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0',
    '#7c4dff', '#e91e63', '#00e676', '#304ffe'
];

// ===== INITIALIZE =====
function init() {
    userInfoElement.textContent = `${displayName} (${userRole})`;

    if (userRole === 'ADMIN') {
        adminPanel.classList.remove('hidden');
        setupAdminControls();
    }

    // default mode
    setMode('GROUP');

    loadChatHistory();
    connect();
    setupDmSearch();
    loadInitialPresence();

    // Reply cancel button
    replyPreviewCancelBtn.addEventListener('click', clearReply);
}

// ===== MODE SWITCH =====
function setMode(newMode) {
    mode = newMode;

    if (mode === 'GROUP') {
        tabGroup.classList.add('active');
        tabDirect.classList.remove('active');

        dmSidebar.classList.add('hidden');
        dmPanel.classList.add('hidden');
        messageArea.classList.remove('hidden');
    } else {
        tabDirect.classList.add('active');
        tabGroup.classList.remove('active');

        dmSidebar.classList.remove('hidden');
        dmPanel.classList.remove('hidden');
        messageArea.classList.add('hidden');
    }
}

tabGroup.addEventListener('click', () => setMode('GROUP'));
tabDirect.addEventListener('click', () => {
    setMode('DIRECT');
    if (!username) {
        alert("Your username is missing. Please re-login or complete choose-username.");
    }
});

// ===== WEBSOCKET CONNECT =====
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    // GROUP subscription
    stompClient.subscribe('/topic/public', onGroupMessageReceived);

    // PRESENCE subscription
    stompClient.subscribe('/topic/presence', onPresenceReceived);

    // Send join message (also stores username in websocket session attributes on backend)
    stompClient.send('/app/chat.addUser', {}, JSON.stringify({
        sender: displayName,
        senderEmail: userEmail,
        senderUsername: username,
        type: 'JOIN'
    }));

    connectingElement.classList.add('hidden');
}

function onError() {
    connectingElement.textContent = '❌ Connection failed. Refresh the page.';
    connectingElement.style.color = '#ff5652';
}

// ===== LOAD GROUP CHAT HISTORY =====
async function loadChatHistory() {
    try {
        const response = await fetch('/api/messages/history');
        const messages = await response.json();
        messages.forEach(msg => renderGroupMessage(msg));
        messageArea.scrollTop = messageArea.scrollHeight;
    } catch (error) {
        console.error('Failed to load history:', error);
    }
}

// ===== INITIAL PRESENCE LOAD =====
async function loadInitialPresence() {
    try {
        const res = await fetch('/api/users/online');
        const users = await res.json();
        if (Array.isArray(users)) {
            users.forEach(u => onlineUsers.add(u.toLowerCase()));
        }
        updateDmPresenceDot();
    } catch (e) {
        console.error('Failed to load presence:', e);
    }
}

// ===== PRESENCE HANDLER =====
function onPresenceReceived(payload) {
    const data = JSON.parse(payload.body);
    if (!data || !data.username) return;

    const u = data.username.toLowerCase();
    if (data.status === 'ONLINE') {
        onlineUsers.add(u);
    } else {
        onlineUsers.delete(u);
    }
    updateDmPresenceDot();
}

function updateDmPresenceDot() {
    if (!dmPresenceDot || !activeDmUsername) return;
    const isOnline = onlineUsers.has(activeDmUsername.toLowerCase());
    dmPresenceDot.className = 'presence-dot ' + (isOnline ? 'presence-online' : 'presence-offline');
    dmPresenceDot.title = isOnline ? 'Online' : 'Offline';
}

// ===== DM SEARCH =====
function setupDmSearch() {
    let timer = null;

    dmSearchInput.addEventListener('input', () => {
        clearTimeout(timer);
        const q = dmSearchInput.value.trim();

        if (!q) {
            dmSearchResults.classList.add('hidden');
            dmSearchResults.innerHTML = '';
            return;
        }

        timer = setTimeout(() => dmSearchUsers(q), 250);
    });

    document.addEventListener('click', (e) => {
        if (!dmSidebar.contains(e.target)) {
            dmSearchResults.classList.add('hidden');
        }
    });
}

async function dmSearchUsers(q) {
    try {
        const res = await fetch(`/api/users/search?q=${encodeURIComponent(q)}`);
        const users = await res.json();

        dmSearchResults.innerHTML = '';
        if (!Array.isArray(users) || users.length === 0) {
            dmSearchResults.classList.add('hidden');
            return;
        }

        users.forEach(u => {
            const row = document.createElement('div');
            row.className = 'dm-search-item';
            const isOnline = onlineUsers.has((u.username || '').toLowerCase());
            row.innerHTML = `
                <div style="display:flex;align-items:center;gap:6px;">
                    <span class="presence-dot ${isOnline ? 'presence-online' : 'presence-offline'}"></span>
                    <div>
                        <div class="u">@${escapeHtml(u.username || '')}</div>
                        <div class="d">${escapeHtml(u.displayName || '')}</div>
                    </div>
                </div>
                <div class="d">Open</div>
            `;
            row.addEventListener('click', () => openDirectChat(u.username, u.displayName));
            dmSearchResults.appendChild(row);
        });

        dmSearchResults.classList.remove('hidden');
    } catch (e) {
        console.error('DM search error', e);
    }
}

// ===== OPEN DM =====
async function openDirectChat(targetUsername, targetDisplayName) {
    if (!targetUsername) return;

    try {
        const res = await fetch('/api/conversations/direct', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ targetUsername })
        });

        const data = await res.json();
        if (!data.success) {
            alert(data.message || 'Failed to open conversation');
            return;
        }

        activeConversationId = data.conversationId;
        activeDmUsername = targetUsername.trim().toLowerCase();

        dmWithEl.textContent = `${targetDisplayName ? targetDisplayName + ' ' : ''}(@${targetUsername})`;

        // reset UI
        dmMessageArea.innerHTML = '';

        // subscribe
        if (dmSubscription) {
            dmSubscription.unsubscribe();
            dmSubscription = null;
        }
        dmSubscription = stompClient.subscribe(`/topic/direct/${activeConversationId}`, onDirectMessageReceived);

        // load history (and mark as read)
        await loadDirectHistory(activeConversationId);

        // mark messages as read
        markConversationAsRead(activeConversationId);

        // update presence dot
        updateDmPresenceDot();

        // clear any pending reply when switching conversations
        clearReply();

        // hide results dropdown
        dmSearchResults.classList.add('hidden');
        dmSearchInput.value = targetUsername;

        setMode('DIRECT');
    } catch (e) {
        console.error(e);
        alert('Failed to open direct chat');
    }
}

async function loadDirectHistory(conversationId) {
    try {
        const res = await fetch(`/api/messages/direct/${conversationId}`);
        const messages = await res.json();

        // Expect list of ChatMessage
        if (Array.isArray(messages)) {
            messages.forEach(m => renderDirectMessage(m));
            dmMessageArea.scrollTop = dmMessageArea.scrollHeight;
        }
    } catch (e) {
        console.error('Failed to load direct history', e);
    }
}

async function markConversationAsRead(conversationId) {
    try {
        await fetch(`/api/messages/direct/${conversationId}/read`, { method: 'POST' });
    } catch (e) {
        console.error('markAsRead failed', e);
    }
}

// ===== SEND MESSAGE (GROUP OR DIRECT) =====
messageForm.addEventListener('submit', (e) => {
    e.preventDefault();

    if (selectedFile) {
        uploadAndSendFile();
        return;
    }

    const content = messageInput.value.trim();
    if (!content || !stompClient) return;

    if (mode === 'GROUP') {
        const payload = {
            sender: displayName,
            senderEmail: userEmail,
            senderUsername: username,
            content,
            type: 'CHAT'
        };
        if (pendingReply) {
            payload.replyToMessageId = pendingReply.messageId;
            payload.replyToContent = pendingReply.content;
            payload.replyToSender = pendingReply.sender;
        }
        stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(payload));
    } else {
        if (!activeConversationId) return alert('Open a direct chat first.');
        const payload = {
            conversationId: activeConversationId,
            sender: displayName,
            senderEmail: userEmail,
            senderUsername: username,
            content,
            type: 'CHAT'
        };
        if (pendingReply) {
            payload.replyToMessageId = pendingReply.messageId;
            payload.replyToContent = pendingReply.content;
            payload.replyToSender = pendingReply.sender;
        }
        stompClient.send('/app/direct.sendMessage', {}, JSON.stringify(payload));
    }

    messageInput.value = '';
    clearReply();
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

        if (!data.success) {
            alert('Upload failed: ' + (data.message || 'Unknown error'));
            return;
        }

        // build payload
        const payload = {
            sender: displayName,
            senderEmail: userEmail,
            senderUsername: username,
            content: messageInput.value.trim() || '',
            type: 'FILE',
            fileId: data.fileId,
            fileName: data.fileName,
            fileType: data.fileType,
            fileSize: data.fileSize
        };
        if (pendingReply) {
            payload.replyToMessageId = pendingReply.messageId;
            payload.replyToContent = pendingReply.content;
            payload.replyToSender = pendingReply.sender;
        }

        if (mode === 'GROUP') {
            stompClient.send('/app/chat.sendFile', {}, JSON.stringify(payload));
        } else {
            if (!activeConversationId) return alert('Open a direct chat first.');
            stompClient.send('/app/direct.sendFile', {}, JSON.stringify({
                ...payload,
                conversationId: activeConversationId
            }));
        }

        // Clear file selection
        selectedFile = null;
        fileInput.value = '';
        filePreview.classList.add('hidden');
        messageInput.value = '';
        clearReply();

    } catch (error) {
        alert('Upload error: ' + error.message);
    }
}

// ===== GROUP HANDLER =====
function onGroupMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    if (message.type === 'SYSTEM') {
        handleSystemMessage(message);
        return;
    }

    // Guard: ignore any message that is explicitly scoped to DIRECT
    if (message.scope === 'DIRECT') {
        return;
    }

    renderGroupMessage(message);
    messageArea.scrollTop = messageArea.scrollHeight;
}

// ===== DM HANDLER =====
function onDirectMessageReceived(payload) {
    const body = JSON.parse(payload.body);

    // UPDATE payload (delete events / read receipts)
    if (body && body.type === 'UPDATE') {
        applyDmUpdate(body);
        return;
    }

    // Normal ChatMessage
    renderDirectMessage(body);
    dmMessageArea.scrollTop = dmMessageArea.scrollHeight;

    // If a new message arrives and this DM panel is open and active, mark as read
    if (activeConversationId && body.senderUsername && body.senderUsername.toLowerCase() !== username) {
        markConversationAsRead(activeConversationId);
    }
}

function applyDmUpdate(update) {
    const messageId = update.messageId;

    if (update.action === 'READ_RECEIPT') {
        // Mark all messages from me as read (upgrade ✓ to ✓✓)
        const readerUsername = (update.readerUsername || '').toLowerCase();
        const messageIds = update.messageIds || [];
        messageIds.forEach(id => {
            const el = dmMessageArea.querySelector(`[data-message-id="${id}"]`);
            if (!el) return;
            const tick = el.querySelector('.read-receipt');
            if (tick) {
                tick.textContent = '✓✓';
                tick.classList.add('read');
                tick.title = `Read by ${readerUsername}`;
            }
        });
        return;
    }

    if (!messageId) return;

    const el = dmMessageArea.querySelector(`[data-message-id="${messageId}"]`);
    if (!el) return;

    if (update.action === 'DELETED_FOR_EVERYONE') {
        // replace bubble content
        const contentEl = el.querySelector('.message-content');
        if (contentEl) {
            contentEl.textContent = 'This message was deleted';
            contentEl.classList.add('deleted-placeholder');
        }

        // remove file attachment UI if present
        const fileAttachment = el.querySelector('.file-attachment');
        if (fileAttachment) fileAttachment.remove();
        const img = el.querySelector('.chat-image-preview');
        if (img) img.remove();

        // remove receipt tick if present
        const tick = el.querySelector('.read-receipt');
        if (tick) tick.remove();

    } else if (update.action === 'DELETED_FOR_ME') {
        // Only remove if the update is for me
        if ((update.username || '').toLowerCase() === username) {
            el.style.animation = 'fadeOut 0.3s ease';
            setTimeout(() => el.remove(), 300);
        }
    }
}

// ===== RENDER GROUP MESSAGE (existing behavior) =====
function renderGroupMessage(message) {
    // reuse your old renderMessage body with messageArea target
    renderMessageInto(messageArea, message, { allowAdminDelete: userRole === 'ADMIN', allowDmMenu: false });
}

// ===== RENDER DIRECT MESSAGE =====
function renderDirectMessage(message) {
    // if message deleted-for-everyone and backend blanked it, show placeholder anyway
    const opts = { allowAdminDelete: false, allowDmMenu: true };
    renderMessageInto(dmMessageArea, message, opts);
}

// Core renderer that can render into group or dm list
function renderMessageInto(targetUl, message, opts) {
    const li = document.createElement('li');
    li.setAttribute('data-message-id', message.id || '');
    li.style.position = 'relative';

    // JOIN/LEAVE only relevant for group; ignore in DM
    if (message.type === 'JOIN' || message.type === 'LEAVE') {
        li.classList.add('event-message');
        li.textContent = message.type === 'JOIN'
            ? `🟢 ${message.sender} joined the chat!`
            : `🔴 ${message.sender} left the chat!`;

        targetUl.appendChild(li);
        return;
    }

    // decide styling
    const isFile = message.type === 'FILE';
    li.classList.add(isFile ? 'file-message' : 'chat-message');

    // Avatar
    const avatar = createAvatar(message.sender);
    li.appendChild(avatar);

    // Username label
    const usernameEl = document.createElement('span');
    usernameEl.classList.add('username');
    usernameEl.textContent = message.sender;
    li.appendChild(usernameEl);

    // Reply quote block (shown if this message is a reply)
    if (message.replyToMessageId && message.replyToSender) {
        const quoteEl = document.createElement('div');
        quoteEl.classList.add('reply-quote');

        const quoteSender = document.createElement('span');
        quoteSender.classList.add('reply-quote-sender');
        quoteSender.textContent = message.replyToSender;
        quoteEl.appendChild(quoteSender);

        const quoteText = document.createElement('span');
        quoteText.classList.add('reply-quote-text');
        quoteText.textContent = message.replyToContent
            ? truncateText(message.replyToContent, 80)
            : '📎 File';
        quoteEl.appendChild(quoteText);

        // Clicking the quote scrolls to the original message
        quoteEl.style.cursor = 'pointer';
        quoteEl.addEventListener('click', () => {
            const orig = targetUl.querySelector(`[data-message-id="${message.replyToMessageId}"]`);
            if (orig) {
                orig.scrollIntoView({ behavior: 'smooth', block: 'center' });
                orig.classList.add('highlight-flash');
                setTimeout(() => orig.classList.remove('highlight-flash'), 1500);
            }
        });

        li.appendChild(quoteEl);
    }

    // Content
    const contentEl = document.createElement('span');
    contentEl.classList.add('message-content');

    // Deleted placeholder
    const deletedForEveryone = !!message.deletedForEveryone;
    if (deletedForEveryone) {
        contentEl.textContent = 'This message was deleted';
        contentEl.classList.add('deleted-placeholder');
    } else {
        contentEl.textContent = message.content || '';
    }
    li.appendChild(contentEl);

    // File attachment UI (if not deleted)
    if (isFile && !deletedForEveryone) {
        const fileBox = document.createElement('div');
        fileBox.classList.add('file-attachment');

        const iconEl = document.createElement('span');
        iconEl.classList.add('file-icon');
        iconEl.textContent = getFileIcon(message.fileType || '');
        fileBox.appendChild(iconEl);

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

        const dlBtn = document.createElement('a');
        dlBtn.classList.add('file-download-btn');
        dlBtn.href = `/api/files/download/${message.fileId}`;
        dlBtn.textContent = '⬇ Download';
        dlBtn.target = '_blank';
        fileBox.appendChild(dlBtn);

        li.appendChild(fileBox);

        if (message.fileType && message.fileType.startsWith('image/')) {
            const img = document.createElement('img');
            img.classList.add('chat-image-preview');
            img.src = `/api/files/download/${message.fileId}`;
            img.alt = message.fileName;
            img.onclick = () => window.open(img.src, '_blank');
            li.appendChild(img);
        }
    }

    // Timestamp
    const timeEl = createTimestamp(message.timestamp);
    li.appendChild(timeEl);

    // Read receipt tick (DM only, for my own messages)
    if (opts.allowDmMenu && !deletedForEveryone) {
        const mineByUsername = message.senderUsername && username && message.senderUsername.toLowerCase() === username;
        const mineByEmail = message.senderEmail && message.senderEmail === userEmail;
        if (mineByUsername || mineByEmail) {
            const tick = document.createElement('span');
            tick.classList.add('read-receipt');
            const readByOther = message.readBy && Array.isArray(message.readBy)
                ? message.readBy.some(u => u.toLowerCase() !== username)
                : (message.readBy instanceof Set ? [...message.readBy].some(u => u.toLowerCase() !== username) : false);
            if (readByOther) {
                tick.textContent = '✓✓';
                tick.classList.add('read');
                tick.title = 'Read';
            } else {
                tick.textContent = '✓';
                tick.title = 'Sent';
            }
            li.appendChild(tick);
        }
    }

    // Reply button (shown on hover for non-deleted messages)
    if (!deletedForEveryone && message.id) {
        const replyBtn = document.createElement('button');
        replyBtn.className = 'msg-reply-btn';
        replyBtn.textContent = '↩';
        replyBtn.title = 'Reply';
        replyBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            setReply({
                messageId: message.id,
                content: message.content || (isFile ? '📎 File' : ''),
                sender: message.sender || ''
            });
        });
        li.appendChild(replyBtn);
    }

    // Admin delete button (group only)
    if (opts.allowAdminDelete) {
        li.appendChild(createDeleteButton(message.id));
    }

    // DM delete menu (direct only)
    if (opts.allowDmMenu && mode === 'DIRECT') {
        // Only show menu for messages that are mine (best-effort check)
        // Prefer senderUsername if present; fallback to senderEmail
        const mineByUsername = message.senderUsername && username && message.senderUsername.toLowerCase() === username;
        const mineByEmail = message.senderEmail && message.senderEmail === userEmail;
        const isMine = mineByUsername || mineByEmail;

        if (isMine && message.id) {
            li.appendChild(createDmMenuButton(message));
        }
    }

    targetUl.appendChild(li);
}

// ===== REPLY HELPERS =====
function setReply(reply) {
    pendingReply = reply;
    replyPreviewSenderEl.textContent = reply.sender;
    replyPreviewTextEl.textContent = truncateText(reply.content, 80);
    replyPreviewEl.classList.remove('hidden');
    messageInput.focus();
}

function clearReply() {
    pendingReply = null;
    replyPreviewEl.classList.add('hidden');
    replyPreviewSenderEl.textContent = '';
    replyPreviewTextEl.textContent = '';
}

// ===== DM MENU =====
function createDmMenuButton(message) {
    const btn = document.createElement('button');
    btn.className = 'msg-menu-btn';
    btn.textContent = '⋮';
    btn.title = 'Message options';

    btn.addEventListener('click', (e) => {
        e.stopPropagation();

        // close existing menus
        document.querySelectorAll('.msg-menu').forEach(m => m.remove());

        const menu = document.createElement('div');
        menu.className = 'msg-menu';

        const delMe = document.createElement('button');
        delMe.textContent = 'Delete for me';
        delMe.onclick = async () => {
            menu.remove();
            await dmDeleteForMe(message.id);
        };

        const delEveryone = document.createElement('button');
        delEveryone.textContent = 'Delete for everyone';
        delEveryone.onclick = async () => {
            menu.remove();
            await dmDeleteForEveryone(message.id);
        };

        menu.appendChild(delMe);
        menu.appendChild(delEveryone);

        btn.parentElement.appendChild(menu);
    });

    // click outside closes menu
    document.addEventListener('click', () => {
        document.querySelectorAll('.msg-menu').forEach(m => m.remove());
    }, { once: true });

    return btn;
}

async function dmDeleteForMe(messageId) {
    try {
        const res = await fetch(`/api/messages/direct/${messageId}/me`, { method: 'DELETE' });
        const data = await res.json().catch(() => ({}));
        if (res.ok && data.success) return;
        alert(data.message || 'Delete failed');
    } catch (e) {
        alert('Delete failed');
    }
}

async function dmDeleteForEveryone(messageId) {
    try {
        const res = await fetch(`/api/messages/direct/${messageId}/everyone`, { method: 'DELETE' });
        const data = await res.json().catch(() => ({}));
        if (res.ok && data.success) return;
        alert(data.message || 'Delete for everyone failed (only sender can do this)');
    } catch (e) {
        alert('Delete failed');
    }
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

// ===== ADMIN CONTROLS (unchanged from your current file) =====
function setupAdminControls() {
    const adminUsername = localStorage.getItem('adminUsername');
    const adminPassword = localStorage.getItem('adminPassword');

    const toggleBtn = document.getElementById('toggleAdmin');
    const controls = document.getElementById('admin-controls');

    toggleBtn.addEventListener('click', () => {
        controls.classList.toggle('hidden');
    });

    document.getElementById('clearChatBtn').addEventListener('click', async () => {
        if (!confirm('Are you sure you want to clear ALL messages?')) return;
        await adminAction({ action: 'CLEAR_CHAT' }, adminUsername, adminPassword);
    });

    document.getElementById('banUserBtn').addEventListener('click', async () => {
        const username = document.getElementById('banUserEmail').value.trim();
        if (!username) return alert('Enter username to ban');
        if (!confirm(`Ban user "${username}"?`)) return;
        await adminAction({ action: 'BAN_USER', targetUsername: username }, adminUsername, adminPassword);
    });

    document.getElementById('unbanUserBtn').addEventListener('click', async () => {
        const username = document.getElementById('banUserEmail').value.trim();
        if (!username) return alert('Enter username to unban');
        await adminAction({ action: 'UNBAN_USER', targetUsername: username }, adminUsername, adminPassword);
    });

    document.getElementById('kickUserBtn').addEventListener('click', async () => {
        const username = document.getElementById('banUserEmail').value.trim();
        if (!username) return alert('Enter username to kick');
        await adminAction({ action: 'KICK_USER', targetUsername: username }, adminUsername, adminPassword);
    });

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

async function adminDeleteMessage(messageId) {
    if (!confirm('Delete this message?')) return;
    const adminUsername = localStorage.getItem('adminUsername');
    const adminPassword = localStorage.getItem('adminPassword');
    await adminAction({ action: 'DELETE_MESSAGE', messageId }, adminUsername, adminPassword);
}

// ===== HELPERS =====
function truncateText(text, maxLength) {
    if (!text) return '';
    return text.length > maxLength ? text.slice(0, maxLength) + '…' : text;
}

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

function escapeHtml(str) {
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
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