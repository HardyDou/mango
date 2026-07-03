const noopRequest = async () => undefined;

export const get = noopRequest;
export const post = noopRequest;
export const put = noopRequest;
export const del = noopRequest;

export const request = {
  get: noopRequest,
  post: noopRequest,
  put: noopRequest,
  delete: noopRequest,
};

export type RequestConfig = Record<string, unknown>;
