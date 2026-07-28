import { get } from './client'

export const jobApi = {
  list: (params) => {
    const query = new URLSearchParams(
      Object.fromEntries(Object.entries(params || {}).filter(([, v]) => v))
    ).toString()
    return get(`/job-postings?${query}`)
  },
  detail: (id) => get(`/job-postings/${id}`),
}
