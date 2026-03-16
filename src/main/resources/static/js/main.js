//'use strict';
//
//const usernamePage = document.querySelector('#username-page');
//const chatPage = document.querySelector('#chat-page');
//const usernameForm = document.querySelector('#usernameForm');
//const messageForm = document.querySelector('#messageForm');
//const messageInput = document.querySelector('#message');
//const messageArea = document.querySelector('#messageArea');
//const connectingElement = document.querySelector('#connecting');
//
//let stompClient = null;
//let username = null;
//
//// Avatar color palette
//const colors = [
//    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
//    '#ffc107', '#ff85af', '#FF9800', '#39bbb0',
//    '#7c4dff', '#e91e63', '#00e676', '#304ffe'
//];
//
//// ===== CONNECT =====
//function connect(event) {
//    event.preventDefault();
//    username = document.querySelector('#name').value.trim();
//
//    if (username) {
//        usernamePage.classList.add('hidden');
//        chatPage.classList.remove('hidden');
//
//        const socket = new SockJS('/ws');
//        stompClient = Stomp.over(socket);
//
//        // Disable STOMP debug logging in production
//        stompClient.debug = null;
//
//        stompClient.connect({}, onConnected, onError);
//    }
//}
//
//// ===== ON CONNECTED =====
//function onConnected() {
//    // Subscribe to the public topic
//    stompClient.subscribe('/topic/public', onMessageReceived);
//
//    // Notify server about new user
//    stompClient.send('/app/chat.addUser', {},
//        JSON.stringify({ sender: username, type: 'JOIN' })
//    );
//
//    connectingElement.classList.add('hidden');
//}
//
//// ===== ON ERROR =====
//function onError() {
//    connectingElement.textContent = '❌ Could not connect to server. Please refresh and try again.';
//    connectingElement.style.color = '#ff5652';
//}
//
//// ===== SEND MESSAGE =====
//function sendMessage(event) {
//    event.preventDefault();
//    const messageContent = messageInput.value.trim();
//
//    if (messageContent && stompClient) {
//        const chatMessage = {
//            sender: username,
//            content: messageContent,
//            type: 'CHAT'
//        };
//
//        stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));
//        messageInput.value = '';
//    }
//}
//
//// ===== ON MESSAGE RECEIVED =====
//function onMessageReceived(payload) {
//    const message = JSON.parse(payload.body);
//    const messageElement = document.createElement('li');
//
//    if (message.type === 'JOIN') {
//        messageElement.classList.add('event-message');
//        messageElement.textContent = `🟢 ${message.sender} joined the chat!`;
//    } else if (message.type === 'LEAVE') {
//        messageElement.classList.add('event-message');
//        messageElement.textContent = `🔴 ${message.sender} left the chat!`;
//    } else {
//        messageElement.classList.add('chat-message');
//
//        // Avatar
//        const avatarElement = document.createElement('span');
//        avatarElement.classList.add('avatar');
//        const avatarText = document.createTextNode(message.sender[0].toUpperCase());
//        avatarElement.appendChild(avatarText);
//        avatarElement.style.backgroundColor = getAvatarColor(message.sender);
//        messageElement.appendChild(avatarElement);
//
//        // Username
//        const usernameElement = document.createElement('span');
//        usernameElement.classList.add('username');
//        usernameElement.textContent = message.sender;
//        messageElement.appendChild(usernameElement);
//
//        // Message content
//        const textElement = document.createElement('span');
//        textElement.classList.add('message-content');
//        textElement.textContent = message.content;
//        messageElement.appendChild(textElement);
//
//        // Timestamp
//        const timeElement = document.createElement('span');
//        timeElement.classList.add('timestamp');
//        const now = new Date();
//        timeElement.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
//        messageElement.appendChild(timeElement);
//    }
//
//    messageArea.appendChild(messageElement);
//
//    // Auto-scroll to latest message
//    messageArea.scrollTop = messageArea.scrollHeight;
//}
//
//// ===== AVATAR COLOR HASH =====
//function getAvatarColor(name) {
//    let hash = 0;
//    for (let i = 0; i < name.length; i++) {
//        hash = 31 * hash + name.charCodeAt(i);
//    }
//    const index = Math.abs(hash % colors.length);
//    return colors[index];
//}
//
//// ===== EVENT LISTENERS =====
//usernameForm.addEventListener('submit', connect, true);
//messageForm.addEventListener('submit', sendMessage, true);