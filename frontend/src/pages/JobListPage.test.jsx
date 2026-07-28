import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { AuthContext } from '../context/AuthContextInstance'
import { jobApi } from '../api/jobApi'
import { adminApi } from '../api/adminApi'
import { JobListPage } from './JobListPage'

vi.mock('../api/jobApi')
vi.mock('../api/adminApi')

function renderAsUser(user) {
  return render(
    <AuthContext.Provider value={{ user, loading: false }}>
      <MemoryRouter>
        <JobListPage />
      </MemoryRouter>
    </AuthContext.Provider>
  )
}

describe('JobListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    jobApi.list.mockResolvedValue({
      content: [
        { id: 1, companyName: '테스트회사', location: '서울', employmentType: 'FULL_TIME' },
      ],
      totalElements: 1,
    })
    adminApi.createDummyJobPostings.mockResolvedValue([])
    adminApi.deleteDummyJobPostings.mockResolvedValue(null)
  })

  it('채용공고 목록을 불러와 보여준다', async () => {
    renderAsUser({ id: 1, role: 'USER' })

    await waitFor(() => expect(screen.getByText('테스트회사')).toBeInTheDocument())
  })

  it('검색어를 입력하면 새 파라미터로 목록을 다시 조회한다', async () => {
    renderAsUser({ id: 1, role: 'USER' })

    await waitFor(() => expect(jobApi.list).toHaveBeenCalledTimes(1))

    fireEvent.change(screen.getByLabelText('검색어'), { target: { value: '카카오' } })

    await waitFor(() => expect(jobApi.list).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: '카카오' })
    ))
  })

  it('일반회원에게는 더미데이터 버튼이 보이지 않는다', async () => {
    renderAsUser({ id: 1, role: 'USER' })

    await waitFor(() => expect(jobApi.list).toHaveBeenCalledTimes(1))

    expect(screen.queryByRole('button', { name: '더미데이터 추가' })).not.toBeInTheDocument()
  })

  it('관리자가 더미데이터 추가 버튼을 누르면 더미데이터를 생성하고 목록을 다시 불러온다', async () => {
    renderAsUser({ id: 1, role: 'ADMIN' })

    await waitFor(() => expect(jobApi.list).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByRole('button', { name: '더미데이터 추가' }))

    await waitFor(() => expect(adminApi.createDummyJobPostings).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(jobApi.list).toHaveBeenCalledTimes(2))
  })

  it('관리자가 더미데이터 삭제 버튼을 누르면 더미데이터를 삭제하고 목록을 다시 불러온다', async () => {
    renderAsUser({ id: 1, role: 'ADMIN' })

    await waitFor(() => expect(jobApi.list).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByRole('button', { name: '더미데이터 삭제' }))

    await waitFor(() => expect(adminApi.deleteDummyJobPostings).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(jobApi.list).toHaveBeenCalledTimes(2))
  })
})
