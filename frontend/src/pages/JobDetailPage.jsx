import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { jobApi } from '../api/jobApi'

export function JobDetailPage() {
  const { id } = useParams()
  const [posting, setPosting] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    jobApi.detail(id).then(setPosting).catch((err) => setError(err.message))
  }, [id])

  if (error) return <p className="alert" role="alert">{error}</p>
  if (!posting) return <p className="muted">불러오는 중...</p>

  return (
    <article className="card">
      <h1>{posting.companyName}</h1>
      {posting.status === 'CLOSED' && <p className="alert">마감된 공고입니다.</p>}
      <p>위치: {posting.location}</p>
      <p>채용조건: {posting.conditionNote}</p>
      <p>지원기간: {posting.applyStartDate} ~ {posting.applyEndDate}</p>
      <p>지원방법: <span>{posting.applyMethod}</span></p>
      <p>예상급여: {posting.salaryMin}만원 ~ {posting.salaryMax}만원 ({posting.salaryNote})</p>
    </article>
  )
}
