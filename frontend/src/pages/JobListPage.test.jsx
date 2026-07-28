import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { jobApi } from '../api/jobApi'
import { JobListPage } from './JobListPage'

vi.mock('../api/jobApi')

describe('JobListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    jobApi.list.mockResolvedValue({
      content: [
        { id: 1, companyName: '테스트회사', location: '서울', employmentType: 'FULL_TIME' },
      ],
      totalElements: 1,
    })
  })

  it('채용공고 목록을 불러와 보여준다', async () => {
    render(
      <MemoryRouter>
        <JobListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('테스트회사')).toBeInTheDocument())
  })

  it('검색어를 입력하면 새 파라미터로 목록을 다시 조회한다', async () => {
    render(
      <MemoryRouter>
        <JobListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(jobApi.list).toHaveBeenCalledTimes(1))

    fireEvent.change(screen.getByLabelText('검색어'), { target: { value: '카카오' } })

    await waitFor(() => expect(jobApi.list).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: '카카오' })
    ))
  })
})
