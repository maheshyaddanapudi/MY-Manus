import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AgentEvent } from '../types';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export class WebSocketService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private sessionId: string | null = null;
  private onEventCallback: ((event: AgentEvent) => void) | null = null;
  private onConnectCallback: (() => void) | null = null;
  private onDisconnectCallback: (() => void) | null = null;

  connect(sessionId: string, onEvent: (event: AgentEvent) => void) {
    this.sessionId = sessionId;
    this.onEventCallback = onEvent;

    // Create STOMP client with SockJS
    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      debug: (str) => {
        console.log('STOMP:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      console.log('WebSocket connected for session:', sessionId);

      // Subscribe to session topic
      if (this.client) {
        this.subscription = this.client.subscribe(
          `/topic/agent/${sessionId}`,
          (message) => {
            try {
              const event: AgentEvent = JSON.parse(message.body);
              console.log('Received event:', event.type, event);

              if (this.onEventCallback) {
                this.onEventCallback(event);
              }
            } catch (error) {
              console.error('Error parsing WebSocket message:', error);
            }
          }
        );
      }

      if (this.onConnectCallback) {
        this.onConnectCallback();
      }
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
    };

    this.client.onWebSocketClose = () => {
      console.log('WebSocket disconnected');
      if (this.onDisconnectCallback) {
        this.onDisconnectCallback();
      }
    };

    this.client.activate();
  }

  disconnect() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }

    this.sessionId = null;
    this.onEventCallback = null;
  }

  onConnect(callback: () => void) {
    this.onConnectCallback = callback;
  }

  onDisconnect(callback: () => void) {
    this.onDisconnectCallback = callback;
  }

  isConnected(): boolean {
    return this.client?.connected || false;
  }
}

// Singleton instance
export const websocketService = new WebSocketService();
