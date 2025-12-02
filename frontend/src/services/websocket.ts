import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { AgentEvent } from '../types';

const WS_URL = import.meta.env.VITE_WS_URL 
  ? (import.meta.env.VITE_WS_URL.startsWith('http') 
      ? import.meta.env.VITE_WS_URL 
      : `${window.location.origin}${import.meta.env.VITE_WS_URL}`)
  : `${window.location.origin}/ws`;

export class WebSocketService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private onEventCallback: ((event: AgentEvent) => void) | null = null;
  private onConnectCallback: (() => void) | null = null;
  private onDisconnectCallback: (() => void) | null = null;

  connect(sessionId: string, onEvent: (event: AgentEvent) => void) {
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

  /**
   * Subscribe to a custom topic
   * Ensures client is connected before subscribing
   */
  subscribe(topic: string, callback: (message: any) => void): StompSubscription | null {
    if (!this.client) {
      console.warn('WebSocket client not initialized. Call connect() first.');
      return null;
    }

    if (!this.client.connected) {
      console.warn('WebSocket not connected yet. Subscription will be delayed.');
      // Wait for connection and then subscribe
      const originalOnConnect = this.client.onConnect;
      this.client.onConnect = (frame) => {
        if (originalOnConnect) originalOnConnect(frame);
        if (this.client) {
          return this.client.subscribe(topic, (message) => {
            try {
              const data = JSON.parse(message.body);
              callback(data);
            } catch (error) {
              console.error('Error parsing message:', error);
            }
          });
        }
        return null;
      };
      return null;
    }

    return this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });
  }
}

// Singleton instance
export const websocketService = new WebSocketService();
