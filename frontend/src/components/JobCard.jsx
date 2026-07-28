import { Link } from 'react-router'

const EMPLOYMENT_TYPE_LABEL = {
  FULL_TIME: '정규직',
  CONTRACT: '계약직',
  INTERN: '인턴',
  PART_TIME: '파트타임',
}

export function JobCard({ posting }) {
  return (
    <li>
      <Link to={`/jobs/${posting.id}`}>
        <h2>{posting.companyName}</h2>
        <p>{posting.location}</p>
        <p>{EMPLOYMENT_TYPE_LABEL[posting.employmentType] ?? posting.employmentType}</p>
      </Link>
    </li>
  )
}
