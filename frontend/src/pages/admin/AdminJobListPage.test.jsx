import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { adminApi } from '../../api/adminApi'
import { AdminJobListPage } from './AdminJobListPage'

vi.mock('../../api/adminApi')

describe('AdminJobListPage', () => {
  beforeEach(() => {
    adminApi.listJobPostings.mockResolvedValue([
      { id: 1, companyName: '테스트회사', status: 'ACTIVE' },
    ])
    adminApi.deleteJobPosting.mockResolvedValue(null)
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
})
