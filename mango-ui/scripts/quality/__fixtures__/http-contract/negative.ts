import type { ApiId, HttpClient } from '../../../../packages/api-schema/src/index';

const numericId: ApiId = 100;

const incompatibleClient: HttpClient = {
  request() {
    return Promise.resolve({ axiosResponse: true });
  },
};

void numericId;
void incompatibleClient;
