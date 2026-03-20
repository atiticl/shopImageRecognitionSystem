import { io, Socket } from 'socket.io-client'

export interface ProgressMessage {
  taskId: number
  type: string  // PROGRESS, COMPLETE, ERROR
  progress?: number
  message?: string
  error?: string
  data?: {
    processedImages?: number
    totalImages?: number
    accuracy?: number
    successCount?: number
    failedCount?: number
    processingTime?: string
  }
  timestamp?: string
}

export class WebSocketService {
  private socket: Socket | null = null
  private connected = false
  private subscribers: Map<string, (message: any) => void> = new Map()

  constructor() {
    this.initSocket()
  }

  private initSocket() {
    this.socket = io('http://localhost:5000', {
      autoConnect: false,
      transports: ['polling'],
      upgrade: false,
      timeout: 20000,
      forceNew: false,
      reconnection: true
    })

    this.socket.on('connect', () => {
      console.log('WebSocket Connected to Python backend')
      this.connected = true
      this.resubscribeAll()
    })

    this.socket.on('disconnect', () => {
      console.log('WebSocket Disconnected from Python backend')
      this.connected = false
    })

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket Connection Error:', error)
      this.connected = false
    })

    this.socket.on('progress', (message: ProgressMessage) => {
      console.log('Received progress message:', message)
      const callback = this.subscribers.get('progress')
      if (callback) {
        callback(message)
      }
    })

  }

  connect() {
    if (!this.socket) {
      this.initSocket()
    }
    
    if (this.socket && !this.socket.connected) {
      try {
        this.socket.connect()
      } catch (error) {
        console.error('Failed to connect WebSocket:', error)
      }
    }
  }

  disconnect() {
    if (this.socket) {
      try {
        this.socket.disconnect()
        this.connected = false
      } catch (error) {
        console.error('Failed to disconnect WebSocket:', error)
      }
    }
  }

  subscribeToProgress(callback: (message: ProgressMessage) => void) {
    this.subscribers.set('progress', callback)
    
    if (this.connected && this.socket) {
      // Socket.IO已经在initSocket中设置了progress事件监听
      return true
    } else {
      // 如果未连接，先保存回调，连接后会自动处理
      this.connect()
      return false
    }
  }

  private resubscribeAll() {
    // Socket.IO会自动重新订阅事件，无需手动处理
    console.log('WebSocket reconnected, all subscriptions are active')
  }

  unsubscribe(eventName: string) {
    this.subscribers.delete(eventName)
  }

  isConnected() {
    return this.connected
  }
}

// 单例实例
export const webSocketService = new WebSocketService()
