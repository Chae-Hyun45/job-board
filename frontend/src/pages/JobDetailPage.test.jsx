import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { jobApi } from '../api/jobApi'
import { JobDetailPage } from './JobDetailPage'

vi.mock('../api/jobApi')

describe('JobDetailPage', () => {
  beforeEach(() => {
    jobApi.detail.mockResolvedValue({
      id: 1,
      companyName: '테스트회사',
      location: '서울',
      conditionNote: '경력 무관',
      applyStartDate: '2026-08-01',
      applyEndDate: '2026-08-31',
      applyMethod: '이메일 접수',
      salaryMin: 3000,
      salaryMax: 3500,
      salaryNote: '협의가능',
      status: 'ACTIVE',
    })
  })

  it('채용공고 상세 정보를 보여준다', async () => {
    render(
      <MemoryRouter initialEntries={['/jobs/1']}>
        <Routes>
          <Route path="/jobs/:id" element={<JobDetailPage />} />
        </Routes>
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('테스트회사')).toBeInTheDocument())
    expect(screen.getByText('이메일 접수')).toBeInTheDocument()
  })
})
