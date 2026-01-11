import api from './axios';

export const messengerAPI = {
  // Chats
  createOrGetChat: async (userId) => {
    const response = await api.post('/api/messenger/chats', { userId });
    return response.data;
  },

  getUserChats: async () => {
    const response = await api.get('/api/messenger/chats');
    return response.data;
  },

  getChat: async (chatId) => {
    const response = await api.get(`/api/messenger/chats/${chatId}`);
    return response.data;
  },

  // Messages
  createMessage: async (chatId, content) => {
    const response = await api.post('/api/messenger/messages', {
      chatId,
      content,
    });
    return response.data;
  },

  getChatMessages: async (chatId, limit = 50, offset = 0) => {
    const response = await api.get(`/api/messenger/messages/chat/${chatId}`, {
      params: { limit, offset },
    });
    return response.data;
  },

  markMessagesAsRead: async (chatId) => {
    const response = await api.put(`/api/messenger/messages/chat/${chatId}/read`);
    return response.data;
  },
};

