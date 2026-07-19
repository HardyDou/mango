import type { HttpClient } from '@mango/api-schema';
import { create{{aggregatePascal}}Api } from '@{{projectKebab}}/{{moduleKebab}}-api';
import type { {{aggregatePascal}}Api } from '@{{projectKebab}}/{{moduleKebab}}-api';

let {{aggregateCamel}}Api: {{aggregatePascal}}Api | undefined;

export function configure{{aggregatePascal}}Api(client: HttpClient): void {
  {{aggregateCamel}}Api = create{{aggregatePascal}}Api(client);
}

export function get{{aggregatePascal}}Api(): {{aggregatePascal}}Api {
  if (!{{aggregateCamel}}Api) {
    throw new Error('{{modulePascal}} pages must be registered with an HttpClient before use');
  }
  return {{aggregateCamel}}Api;
}
