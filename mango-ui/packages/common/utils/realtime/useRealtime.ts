import { computed, onBeforeUnmount, ref, shallowRef } from 'vue';
import { createRealtimeClient } from './client';
import type {
  RealtimeClient,
  RealtimeMessage,
  RealtimeMessageHandler,
  RealtimeOptions,
  RealtimeProtocol,
  RealtimeStatus,
} from './types';

export function useRealtime(options: RealtimeOptions = {}) {
  const client = shallowRef<RealtimeClient>(createRealtimeClient(options));
  const status = ref<RealtimeStatus>(client.value.getStatus());
  const protocol = ref<RealtimeProtocol | null>(client.value.getProtocol());
  const lastMessage = ref<RealtimeMessage | null>(null);
  const messages = ref<RealtimeMessage[]>([]);
  const error = ref<Error | null>(null);

  const offStatus = client.value.on('status', value => {
    status.value = value;
    protocol.value = client.value.getProtocol();
  });
  const offMessage = client.value.on('message', message => {
    lastMessage.value = message;
    messages.value = [...messages.value, message];
  });
  const offError = client.value.on('error', value => {
    error.value = value;
  });
  const offTransport = client.value.on('transport-change', value => {
    protocol.value = value.to;
  });

  onBeforeUnmount(() => {
    offStatus();
    offMessage();
    offError();
    offTransport();
    client.value.disconnect('component-unmount');
  });

  return {
    client,
    status,
    protocol,
    connected: computed(() => status.value === 'connected'),
    lastMessage,
    messages,
    error,
    connect: () => client.value.connect(),
    disconnect: (reason?: string) => client.value.disconnect(reason),
    reconnect: () => client.value.reconnect(),
    send: (message: Partial<RealtimeMessage>) => client.value.send(message),
    subscribe: (type: string, handler: RealtimeMessageHandler) => client.value.subscribe(type, handler),
    on: client.value.on.bind(client.value),
    getMetrics: () => client.value.getMetrics(),
  };
}
