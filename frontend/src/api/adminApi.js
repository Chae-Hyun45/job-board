import { get, post, put, del, postForm, patch } from './client'

export const adminApi = {
  extractPdf: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return postForm('/admin/job-postings/extract', formData)
  },
  createJobPosting: (data) => post('/admin/job-postings', data),
  listJobPostings: () => get('/admin/job-postings'),
  createDummyJobPostings: () => post('/admin/job-postings/dummy'),
  deleteDummyJobPostings: () => del('/admin/job-postings/dummy'),
  updateJobPosting: (id, data) => put(`/admin/job-postings/${id}`, data),
  deleteJobPosting: (id) => del(`/admin/job-postings/${id}`),
  listUsers: () => get('/admin/users'),
  updateUserRole: (id, role) => patch(`/admin/users/${id}/role`, { role }),
}
