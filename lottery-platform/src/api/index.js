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
  shipOrder: (id) => request.post(`/admin/orders/${id}/ship`),
  completeOrder: (id) => request.post(`/admin/orders/${id}/done`),
  getAgents: (params) => request.get('/admin/agents', { params }),
  getMarket: (params) => request.get('/admin/market', { params }),
  removeMarketItem: (id) => request.delete(`/admin/market/${id}`),
}
