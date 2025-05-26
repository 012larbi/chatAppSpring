 // Global variables
        let contacts = [];
        let currentChatId = null;
        let currentChatType = null;
        let websocket = null;
        let invitationSenderId = null;
        let selectedMembers = new Set();
        
        // DOM elements
        const contactsList = document.getElementById('contactsList');
        const messagesContainer = document.getElementById('messagesContainer');
        const messageInput = document.getElementById('messageInput');
        const sendButton = document.getElementById('sendButton');
        const chatHeader = document.getElementById('chatHeader');
        const searchInput = document.getElementById('searchInput');
        const searchResults = document.getElementById('searchResults');
        const tabs = document.querySelectorAll('.tab');
        const tabContents = document.querySelectorAll('.tab-content');
        const groupForm = document.getElementById('groupForm');
        const groupNameInput = document.getElementById('groupNameInput');
        const availableContacts = document.getElementById('availableContacts');
        const selectedMembersList = document.getElementById('selectedMembersList');
        const createGroupButton = document.getElementById('createGroupButton');
        const invitationModal = document.getElementById('invitationModal');
        const invitationModalBody = document.getElementById('invitationModalBody');
        const acceptInvitationButton = document.getElementById('acceptInvitationButton');
        const rejectInvitationButton = document.getElementById('rejectInvitationButton');
        const closeInvitationModal = document.getElementById('closeInvitationModal');
        const notification = document.getElementById('notification');
        const notificationMessage = document.getElementById('notificationMessage');
        const notificationClose = document.getElementById('notificationClose');
        const disconnectButton = document.getElementById('disconnectButton');
        let searchIsActive=false;
        // Initialize the app when the page loads
        document.addEventListener('DOMContentLoaded', function() {
            loadContacts();
            connectWebSocket();
            setupEventListeners();
        });
        
        function setupEventListeners() {
            // Send message when clicking send button or pressing Enter
            sendButton.addEventListener('click', sendMessage);
            messageInput.addEventListener('keypress', function(e) {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    sendMessage();
                }
            });
            
            // Search functionality
            searchInput.addEventListener('input', function() {
                const searchTerm = searchInput.value.toLowerCase();
                if (searchIsActive) {
                    console.log("serch active")
                    searchUsers();  
                }else{
                     if (searchTerm.length > 0) {
                    filterContacts(searchTerm);
                } else {
                    renderContacts();
                }
                }
               
            });
            
            // Tab switching
            tabs.forEach(tab => {
                tab.addEventListener('click', function() {
                    // Remove active class from all tabs and contents
                    tabs.forEach(t => t.classList.remove('active'));
                    tabContents.forEach(c => c.classList.remove('active'));
                    
                    // Add active class to clicked tab and corresponding content
                    this.classList.add('active');
                    const tabId = this.getAttribute('data-tab') + '-tab';
                    document.getElementById(tabId).classList.add('active');
                    
                    // If search tab is activated, clear previous results
                    if (this.getAttribute('data-tab') === 'search') {
                        searchResults.innerHTML = '';
                        searchIsActive=true;
                    }else{
                        searchIsActive=false;
                    }
                    
                    // If create group tab is activated, load contacts for selection
                    if (this.getAttribute('data-tab') === 'create-group') {
                        loadContactsForGroupSelection();
                    }
                });
            });
            
            // Group form submission
            groupForm.addEventListener('submit', function(e) {
                e.preventDefault();
                createGroup();
            });
            
            // Invitation modal buttons
            acceptInvitationButton.addEventListener('click', function() {
                acceptInvitation(invitationSenderId);
                invitationModal.style.display = 'none';
            });
            
            rejectInvitationButton.addEventListener('click', function() {
                rejectInvitation(invitationSenderId);
                invitationModal.style.display = 'none';
            });
            
            closeInvitationModal.addEventListener('click', function() {
                invitationModal.style.display = 'none';
            });
            
            // Notification close button
            notificationClose.addEventListener('click', function() {
                notification.classList.remove('show');
            });
            
            // Disconnect button
            disconnectButton.addEventListener('click', disconnect);
        }
        
        function loadContacts() {
            fetch('/contacts')
                .then(response => response.json())
                .then(data => {
                    contacts = data;
                    renderContacts();
                    loadContactsForGroupSelection();
                })
                .catch(error => {
                    console.error('Error loading contacts:', error);
                    showNotification('Failed to load contacts', 'error');
                });
        }
        
        function renderContacts() {
            contactsList.innerHTML = '';
            
            if (contacts.length === 0) {
                contactsList.innerHTML = '<div style="padding: 15px; text-align: center;">No contacts found</div>';
                return;
            }
            
            contacts.forEach(contact => {

              renderContactItem(contact);
            });
        }
        
        function filterContacts(searchTerm) {
            contactsList.innerHTML = '';
            
            const filteredContacts = contacts.filter(contact => 
                contact.name.toLowerCase().includes(searchTerm)
            );
            
            if (filteredContacts.length === 0) {
                contactsList.innerHTML = '<div style="padding: 15px; text-align: center;">No matching contacts found</div>';
                return;
            }
            
            filteredContacts.forEach(contact => {
              renderContactItem(contact);
            });
        }

        function renderContactItem(contact) {
                const contactItem = document.createElement('div');
                contactItem.className = `contact-item ${contact.type}`;
                contactItem.setAttribute('data-id', contact.id);
                contactItem.setAttribute('data-type', contact.type);
                
                const avatar = document.createElement('div');
                avatar.className = 'contact-avatar';
                avatar.textContent = contact.name.charAt(0).toUpperCase();
                contactItem.appendChild(avatar);
                
                const contactInfo = document.createElement('div');
                contactInfo.className = 'contact-info';
                
                const name = document.createElement('div');
                name.className = 'contact-name';
                name.textContent = contact.name;
                contactInfo.appendChild(name);
                
                if (contact.type === 'user') {
                    const status = document.createElement('div');
                    status.className = 'contact-status';
                    
                    const indicator = document.createElement('div');
                    indicator.className = 'status-indicator';
                    indicator.id = `status-${contact.id}`;
                    
                    const statusText = document.createElement('span');
                    statusText.textContent = 'Offline';
                    
                    status.appendChild(indicator);
                    status.appendChild(statusText);
                    contactInfo.appendChild(status);
                }
                
                contactItem.appendChild(contactInfo);
                contactItem.addEventListener('click', function() {
                    openChat(contact.id, contact.type, contact.name);
                });
                
                contactsList.appendChild(contactItem);
            
        }
        //the above code should be refined
        function loadContactsForGroupSelection() {
            availableContacts.innerHTML = '';
            selectedMembersList.innerHTML = '';
            selectedMembers.clear();
            
            const userContacts = contacts.filter(contact => contact.type === 'user');
            
            if (userContacts.length === 0) {
                availableContacts.innerHTML = '<div style="padding: 1rem; text-align: center;">No contacts available</div>';
                return;
            }
            
            userContacts.forEach(contact => {
                const contactCheckbox = document.createElement('div');
                contactCheckbox.className = 'contact-checkbox';
                
                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.id = `contact-${contact.id}`;
                checkbox.value = contact.id;
                
                checkbox.addEventListener('change', function() {
                    if (this.checked) {
                        selectedMembers.add(contact.id);
                        addSelectedMember(contact.id, contact.name);
                    } else {
                        selectedMembers.delete(contact.id);
                        removeSelectedMember(contact.id);
                    }
                });
                
                const label = document.createElement('label');
                label.htmlFor = `contact-${contact.id}`;
                label.textContent = contact.name;
                
                contactCheckbox.appendChild(checkbox);
                contactCheckbox.appendChild(label);
                availableContacts.appendChild(contactCheckbox);
            });
        }
        
        function addSelectedMember(id, name) {
            const memberTag = document.createElement('div');
            memberTag.className = 'member-tag';
            memberTag.id = `selected-member-${id}`;
            
            const nameSpan = document.createElement('span');
            nameSpan.textContent = name;
            
            const removeButton = document.createElement('button');
            removeButton.innerHTML = '&times;';
            removeButton.addEventListener('click', function() {
                selectedMembers.delete(id);
                document.getElementById(`contact-${id}`).checked = false;
                memberTag.remove();
            });
            
            memberTag.appendChild(nameSpan);
            memberTag.appendChild(removeButton);
            selectedMembersList.appendChild(memberTag);
        }
        
        function removeSelectedMember(id) {
            const memberTag = document.getElementById(`selected-member-${id}`);
            if (memberTag) {
                memberTag.remove();
            }
        }
        
        function openChat(id, type, name) {
            currentChatId = id;
            currentChatType = type;
            
            // Update UI
            chatHeader.textContent = name;
            messageInput.disabled = false;
            sendButton.disabled = false;
            
            // Remove new message indicator from the contact
            const contactItems = document.querySelectorAll('.contact-item');
            contactItems.forEach(item => {
                if (item.getAttribute('data-id') === id) {
                    item.classList.remove('new-message');
                }
            });
            
            // Load messages
            loadMessages(id);
        }
        
        function loadMessages(id) {
            messagesContainer.innerHTML = '<div style="text-align: center; padding: 20px;">Loading messages...</div>';
            
            fetch('/discussion', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id })
            })
            .then(response => response.json())
            .then(messages => {
                messagesContainer.innerHTML = '';
                
                if (messages.length === 0) {
                    messagesContainer.innerHTML = '<div style="text-align: center; padding: 20px;">No messages yet. Start the conversation!</div>';
                    return;
                }
                
                // Messages are sorted from last recent to most, so we reverse to display properly
                messages.reverse().forEach(message => {
                    addMessageToUI(message.user === 'me' ? 'sent' : 'received', message.content);
                });
                
                // Scroll to bottom
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            })
            .catch(error => {
                console.error('Error loading messages:', error);
                messagesContainer.innerHTML = '<div style="text-align: center; padding: 20px; color: red;">Failed to load messages</div>';
            });
        }
        
        function sendMessage() {
            const content = messageInput.value.trim();
            if (!content || !currentChatId) return;
            
            fetch('/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    id: currentChatId,
                    content: content
                })
            })
            .then(response => {
                if (response.ok) {
                    // Add message to UI immediately
                    addMessageToUI('sent', content);
                    messageInput.value = '';
                    
                    // Scroll to bottom
                    messagesContainer.scrollTop = messagesContainer.scrollHeight;
                } else {
                    throw new Error('Failed to send message');
                }
            })
            .catch(error => {
                console.error('Error sending message:', error);
                showNotification('Failed to send message', 'error');
            });
        }
        
        function addMessageToUI(type, content) {
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${type}`;
            messageDiv.textContent = content;
            messagesContainer.appendChild(messageDiv);
        }
        
        function connectWebSocket() {
            // Determine the WebSocket URL based on the current location
            const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
            const host = window.location.host;
            const endpoint = '/ws'; // Your WebSocket endpoint
            
            websocket = new WebSocket(protocol + host + endpoint);
            
            websocket.onopen = function() {
                console.log('WebSocket connection established');
            };
            
            websocket.onmessage = function(event) {
                try {
                    const message = JSON.parse(event.data);
                    handleWebSocketMessage(message);
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error);
                }
            };
            
            websocket.onclose = function() {
                console.log('WebSocket connection closed');
                showNotification('Connection lost. Trying to reconnect...', 'warning');
                
                // Try to reconnect after 5 seconds
               // setTimeout(connectWebSocket, 5000);
            };
            
            websocket.onerror = function(error) {
                console.error('WebSocket error:', error);
            };
        }
        
        function handleWebSocketMessage(message) {
            switch(message.type) {
                case 'message':
                    handleMessage(message);
                    break;
                case 'userstatus':
                    handleUserStatus(message);
                    break;
                case 'notification':
                    handleNotification(message);
                    break;
                default:
                    console.warn('Unknown message type:', message.type);
            }
        }
        
        function handleMessage(message) {
            // If the message is for the current chat, display it
            if (currentChatId === message.senderId) {
                addMessageToUI('received', message.content);
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            } else {
                // Otherwise, highlight the contact in the list
                const contactItems = document.querySelectorAll('.contact-item');
                contactItems.forEach(item => {
                    if (item.getAttribute('data-id') === message.senderId) {
                        item.classList.add('new-message');
                        
                        // Show a notification if the chat is not open
                        if (currentChatId !== message.senderId) {
                            const contactName = item.querySelector('.contact-name').textContent;
                            showNotification(`New message from ${contactName}`, 'success');
                        }
                    }
                });
            }
        }
        
        function handleUserStatus(message) {
            const statusElement = document.getElementById(`status-${message.userId}`);
            if (statusElement) {
                if (message.status === 'connected') {
                    console.log("a user is connected with the userId : "+ message.userId)
                    statusElement.classList.add('active');
                    const statusText = statusElement.nextElementSibling;
                    if (statusText) statusText.textContent = 'Online';
                } else {
                    statusElement.classList.remove('active');
                   console.log("a user is disconnected with the userId : "+ message.userId)
                    const statusText = statusElement.nextElementSibling;
                    if (statusText) statusText.textContent = 'Offline';
                }
            }else{
           console.log("nothing get trigered : ")

            }
        }
        
        function handleNotification(message) {
            if (message.notifType === 'invitation') {
                // Show invitation modal
                invitationModalBody.innerHTML = `
                    <p>You have received a friend request from <strong>${message.username}</strong>.</p>
                    <p>Would you like to accept this invitation?</p>
                `;
                invitationSenderId = message.id;
                invitationModal.style.display = 'flex';
            } else if (message.notifType === 'decision') {
                // Show notification about invitation decision
                if (message.status === 'accepted') {
                    showNotification(`${message.username} accepted your friend request`, 'success');
                    // Refresh contacts list
                    loadContacts();
                    searchResults.innerHTML='';
                } else {
                    showNotification(`${message.username} rejected your friend request`, 'error');
                }
            }
        }
        
        function acceptInvitation(userId) {
            fetch('/acceptInvitation', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: userId })
            })
            .then(response => {
                if (response.ok) {
                    showNotification('Invitation accepted', 'success');
                    // Refresh contacts list
                    loadContacts();
                } else {
                    throw new Error('Failed to accept invitation');
                }
            })
            .catch(error => {
                console.error('Error accepting invitation:', error);
                showNotification('Failed to accept invitation', 'error');
            });
        }
        
        function rejectInvitation(userId) {
            fetch('/rejectIvitation', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: userId })
            })
            .then(response => {
                if (response.ok) {
                    showNotification('Invitation rejected', 'success');
                } else {
                    throw new Error('Failed to reject invitation');
                }
            })
            .catch(error => {
                console.error('Error rejecting invitation:', error);
                showNotification('Failed to reject invitation', 'error');
            });
        }
        
        function searchUsers() {
            const searchTerm = searchInput.value.trim();
            if (searchTerm.length < 1) return;
            
            fetch(`/users`)
                .then(response => response.json())
                .then(users => {
                    searchResults.innerHTML = '';
                    
                    if (users.length === 0) {
                        searchResults.innerHTML = '<div style="padding: 15px; text-align: center;">No users found</div>';
                        return;
                    }
                    
                    users.forEach(user => {
                        const userDiv = document.createElement('div');
                        userDiv.className = 'user-search-result';
                        
                        const actionButton = document.createElement('button');
                        actionButton.className = `user-search-action ${user.invitationSent ? 'cancel' : 'add'}`;
                        actionButton.textContent = user.invitationSent ? 'Cancel' : 'Add';
                        actionButton.setAttribute('data-id', user.id);
                        const action = user.invitationSent ? 'cancel' : 'add';
                        actionButton.setAttribute('data-action', "allowed");

                        
                        actionButton.addEventListener('click', function() {
                            handleUserAction(user.id, action, this);
                        });
                        
                        userDiv.innerHTML = `
                            <div class="user-search-name">${user.username}</div>
                        `;
                        userDiv.appendChild(actionButton);
                        
                        searchResults.appendChild(userDiv);
                    });
                })
                .catch(error => {
                    console.error('Error searching users:', error);
                    searchResults.innerHTML = '<div style="padding: 15px; text-align: center; color: red;">Failed to search users</div>';
                });
        }
        
        function handleUserAction(userId, action, buttonElement) {
           
            if ( buttonElement.getAttribute('data-action')==="allowed") 
            fetch('/invite', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    id: userId,
                    action: action
                })
            })
            .then(response => {
                 buttonElement.setAttribute('data-action', "disallowed");
                if (response.ok) {
                    if (action === 'add') {
                        buttonElement.textContent = 'Cancel';
                        buttonElement.classList.remove('add');
                        buttonElement.classList.add('cancel');
                        buttonElement.addEventListener('click', function() {
                          
                            handleUserAction(userId, "cancel", this);
                        });
                        showNotification('Invitation sent', 'success');
                    } else {
                        buttonElement.textContent = 'Add';
                        buttonElement.classList.remove('cancel');
                        buttonElement.classList.add('add');
                         buttonElement.addEventListener('click', function() {
                          
                            handleUserAction(userId, "add", this);
                        });
                        showNotification('Invitation canceled', 'success');
                    }
                } else {
                    throw new Error('Failed to perform action');
                }
            })
            .catch(error => {
                console.error('Error performing user action:', error);
                showNotification('Failed to perform action', 'error');
            });
        }
        
        function createGroup() {
            const groupName = groupNameInput.value.trim();
            if (!groupName) {
                showNotification('Please enter a group name', 'warning');
                return;
            }
            
            if (selectedMembers.size === 0) {
                showNotification('Please select at least one member', 'warning');
                return;
            }
            
            const members = Array.from(selectedMembers);
            
            fetch('/createGroup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    name: groupName,
                    members: members
                })
            })
            .then(response => response.json())
            .then(data => {
                showNotification('Group created successfully', 'success');
                // Reset form
                groupNameInput.value = '';
                selectedMembers.clear();
                selectedMembersList.innerHTML = '';
                document.querySelectorAll('.contact-checkbox input').forEach(checkbox => {
                    checkbox.checked = false;
                });
                // Refresh contacts list
                loadContacts();
                // Switch back to contacts tab
                document.querySelector('.tab[data-tab="contacts"]').click();
            })
            .catch(error => {
                console.error('Error creating group:', error);
                showNotification('Failed to create group', 'error');
            });
        }
        
        function showNotification(message, type) {
            notificationMessage.textContent = message;
            notification.className = `notification ${type} show`;
            
            // Auto-hide after 5 seconds
            setTimeout(() => {
                notification.classList.remove('show');
            }, 5000);
        }
        
        function disconnect() {
            fetch('/disconnect')
                .then(response => {
                    if (response.redirected) {
                        window.location.href = response.url;
                    }
                })
                .catch(error => {
                    console.error('Error disconnecting:', error);
                    showNotification('Failed to disconnect', 'error');
                });
        }

        window.addEventListener("beforeunload",async ()=>{
            console.log("there is a problem ")
            const url = "/inactive"; // Replace with your server URL
    const img = new Image();
    img.src = url;
        })