import request from '../utils/request'

// 后端 IP
const NODE_A_IP = 'http://10.105.232.231'
const NODE_B_IP = 'http://192.168.56.101' 

// === 8081 身份权限 ===
export const login = (data) => request.post(`${NODE_A_IP}:8081/api/auth/login`, data)
export const getMe = () => request.get(`${NODE_A_IP}:8081/api/auth/me`)

// === 8082 座位资源 ===
export const getRooms = () => request.get(`${NODE_A_IP}:8082/api/rooms`)
export const getSeats = (roomId, params) => request.get(`${NODE_A_IP}:8082/api/rooms/${roomId}/seats`, { params })
export const setSeatStatus = (seatId, data) => request.put(`${NODE_A_IP}:8082/api/admin/seats/${seatId}/status`, data)

// === 8083 订单流转 ===
export const reserveSeat = (data) => request.post(`${NODE_B_IP}:8083/api/orders/reserve`, data)
export const getMyOrders = () => request.get(`${NODE_B_IP}:8083/api/orders/my-list`)
export const cancelOrder = (orderId) => request.post(`${NODE_B_IP}:8083/api/orders/${orderId}/cancel`)
export const signInOrder = (orderId) => request.post(`${NODE_B_IP}:8083/api/orders/${orderId}/sign-in`)
export const tempLeaveOrder = (orderId) => request.post(`${NODE_B_IP}:8083/api/orders/${orderId}/temp-leave`)
export const returnBackOrder = (orderId) => request.post(`${NODE_B_IP}:8083/api/orders/${orderId}/return-back`)
export const leaveOrder = (orderId) => request.post(`${NODE_B_IP}:8083/api/orders/${orderId}/leave`)