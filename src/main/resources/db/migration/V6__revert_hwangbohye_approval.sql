-- 관리자 실수로 승인 처리된 황보혜 인증 신청을 대기(PENDING) 상태로 되돌림
UPDATE verification v
INNER JOIN users u ON v.user_id = u.id
SET v.status      = 'PENDING',
    v.processed_by_id = NULL,
    v.processed_at    = NULL,
    v.admin_memo      = NULL
WHERE u.name   = '황보혜'
  AND v.status = 'APPROVED';
