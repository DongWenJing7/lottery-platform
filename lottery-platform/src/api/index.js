import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
}

export const playerApi = {
  getInfo: () => request.get('/player/info'),
  getLogs: (params) => request.get('/player/logs', { params }),
  getRechargeOrders: (params) => request.get('/player/recharge', { params }),
  submitRecharge: (data) => request.post('/player/recharge', data),
  confirmPayment: (id, data) => request.post(`/player/recharge/${id}/pay`, data),
  getPaymentConfig: () => request.get('/player/payment-config'),
  drawLottery: () => request.post('/lottery/draw'),
  getLotteryConfig: () => request.get('/lottery/config'),
  getWarehouse: (params) => request.get('/player/warehouse', { params }),

  deleteItem: (id) => request.delete(`/player/warehouse/${id}`),
  listForSale: (id, price) => request.post(`/player/warehouse/${id}/sell`, { price }),
  requestDelivery: (id, data) => request.post(`/player/warehouse/${id}/deliver`, data),
  getMarket: (params) => request.get('/market/list', { params }),
  buyItem: (id) => request.post(`/market/${id}/buy`),
  unsell: (id) => request.post(`/player/warehouse/${id}/unsell`),
  getOrders: (params) => request.get('/player/orders', { params }),
  updateProfile: (data) => request.put('/player/profile', data),
  bindPhone: (data) => request.put('/player/bindPhone', data),

  // 售后
  submitAfterSale: (data) => request.post('/player/after-sale', data),
  getAfterSales: (params) => request.get('/player/after-sale', { params }),
  // Chat
  getConversations: () => request.get('/player/chat/conversations'),
  getChatMessages: (params) => request.get('/player/chat/messages', { params }),
  markAsRead: (userId) => request.post('/player/chat/read', null, { params: { userId } }),
  getUnreadCount: () => request.get('/player/chat/unread-count'),
  searchUsers: (keyword) => request.get('/player/chat/search', { params: { keyword } }),
  deleteConversation: (userId) => request.delete('/player/chat/conversation', { params: { userId } }),
  getChatStatus: (userId) => request.get('/player/chat/chat-status', { params: { userId } }),
  // Friends
  sendFriendRequest: (targetId) => request.post('/player/friend/request', null, { params: { targetId } }),
  acceptFriend: (id) => request.post('/player/friend/accept', null, { params: { id } }),
  rejectFriend: (id) => request.post('/player/friend/reject', null, { params: { id } }),
  deleteFriend: (friendUserId) => request.delete(`/player/friend/${friendUserId}`),
  getFriendList: () => request.get('/player/friend/list'),
  getFriendRequests: () => request.get('/player/friend/requests'),
  getFriendPendingCount: () => request.get('/player/friend/pending-count'),
}

export const agentApi = {
  getInfo: () => request.get('/agent/info'),
  updateProfile: (data) => request.put('/agent/profile', data),
  getDashboard: () => request.get('/agent/dashboard'),
  getPlayers: (params) => request.get('/agent/players', { params }),
  getCommission: (params) => request.get('/agent/commission', { params }),
}

export const adminApi = {
  getDashboard: () => request.get('/admin/dashboard'),
  getChartData: (days = 7) => request.get('/admin/dashboard/charts', { params: { days } }),
  getPlayers: (params) => request.get('/admin/players', { params }),
  createUser: (data) => request.post('/admin/players', data),
  updateTokens: (id, data) => request.post(`/admin/players/${id}/tokens`, data),
  setStatus: (id, status) => request.post(`/admin/players/${id}/status`, { status }),
  getWarehouseItems: (userId) => request.get(`/admin/players/${userId}/warehouse`),
  addWarehouseItem: (userId, prizeId) => request.post(`/admin/players/${userId}/warehouse`, { prizeId }),
  removeWarehouseItem: (userId, itemId) => request.delete(`/admin/players/${userId}/warehouse/${itemId}`),
  getPrizes: (params) => request.get('/admin/prizes', { params }),
  createPrize: (data) => request.post('/admin/prizes', data),
  updatePrize: (id, data) => request.put(`/admin/prizes/${id}`, data),
  deletePrize: (id) => request.delete(`/admin/prizes/${id}`),
  getLotteryConfig: () => request.get('/admin/lottery/config'),
  updateLotteryConfig: (data) => request.put('/admin/lottery/config', data),
  getRechargeOrders: (params) => request.get('/admin/recharge', { params }),
  confirmRecharge: (id) => request.post(`/admin/recharge/${id}/confirm`),
  rejectRecharge: (id, reason) => request.post(`/admin/recharge/${id}/reject`, { reason }),
  getOrders: (params) => request.get('/admin/orders', { params }),
  shipOrder: (id, data) => request.post(`/admin/orders/${id}/ship`, data),
  completeOrder: (id) => request.post(`/admin/orders/${id}/done`),
  getAgents: (params) => request.get('/admin/agents', { params }),
  getMarket: (params) => request.get('/admin/market', { params }),
  removeMarketItem: (id) => request.delete(`/admin/market/${id}`),

  // 售后管理
  getAfterSales: (params) => request.get('/admin/after-sale', { params }),
  approveAfterSale: (id) => request.post(`/admin/after-sale/${id}/approve`),
  rejectAfterSale: (id, reason) => request.post(`/admin/after-sale/${id}/reject`, { reason }),
  completeAfterSale: (id) => request.post(`/admin/after-sale/${id}/done`),

  // 支付配置
  getPaymentConfig: () => request.get('/admin/config/payment'),
  savePaymentConfig: (data) => request.post('/admin/config/payment', data),

  // 导出
  exportRechargeOrders: (status) => request.get('/admin/recharge/export', { params: { status }, responseType: 'blob' }),
  exportDeliveryOrders: (status) => request.get('/admin/orders/export', { params: { status }, responseType: 'blob' }),
}
