import type { ProxyOptions } from 'vite';

export function createMangoApiProxy(target: string): ProxyOptions {
  return {
    target,
    ws: true,
    changeOrigin: true,
    xfwd: true,
    rewrite: path => path.replace(/^\/api/, ''),
    configure(proxy) {
      proxy.on('proxyReq', (proxyRequest, request) => {
        if (request.headers.host) {
          proxyRequest.setHeader('X-Forwarded-Host', request.headers.host);
        }
        proxyRequest.setHeader('X-Forwarded-Prefix', '/api');
      });
    },
  };
}
