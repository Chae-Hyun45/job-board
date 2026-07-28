import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { adminApi } from '../../api/adminApi'
import { AdminJobListPage } from './AdminJobListPage'

vi.mock('../../api/adminApi')

describe('AdminJobListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    adminApi.listJobPostings.mockResolvedValue([
      { id: 1, companyName: '테스트회사', status: 'ACTIVE' },
    ])
    adminApi.deleteJobPosting.mockResolvedValue(null)
    adminApi.createDummyJobPostings.mockResolvedValue([])
    adminApi.deleteDummyJobPostings.mockResolvedValue(null)
  })

  it('채용공고 목록을 보여주고 삭제할 수 있다', async () => {
    render(
      <MemoryRouter>
        <AdminJobListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('테스트회사')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '삭제' }))

    await waitFor(() => expect(adminApi.deleteJobPosting).toHaveBeenCalledWith(1))
  })

  it('더미데이터 추가 버튼을 누르면 더미데이터를 생성하고 목록을 다시 불러온다', async () => {
    render(
      <MemoryRouter>
        <AdminJobListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(adminApi.listJobPostings).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByRole('button', { name: '더미데이터 추가' }))

    await waitFor(() => expect(adminApi.createDummyJobPostings).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(adminApi.listJobPostings).toHaveBeenCalledTimes(2))
  })

  it('더미데이터 삭제 버튼을 누르면 더미데이터를 삭제하고 목록을 다시 불러온다', async () => {
    render(
      <MemoryRouter>
        <AdminJobListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(adminApi.listJobPostings).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByRole('button', { name: '더미데이터 삭제' }))

    await waitFor(() => expect(adminApi.deleteDummyJobPostings).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(adminApi.listJobPostings).toHaveBeenCalledTimes(2))
  })
})
