CREATE TABLE IF NOT EXISTS message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  title VARCHAR(100) DEFAULT '',
  content TEXT,
  type TINYINT DEFAULT 0,
  is_read TINYINT DEFAULT 0,
  related_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  KEY idx_student (student_id),
  KEY idx_student_read (student_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS favorite (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_student_job (student_id, job_id),
  KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO message(student_id, title, content, type, is_read) VALUES
(6, 'TZCG', 'TGCD20026DF7DJSXDS', 0, 1),
(6, 'JLTY', 'GZKHJYDS', 0, 0),
(6, 'MSYQ', 'SZXXJSCZNS', 1, 0),
(6, 'XTTZ', 'AIJLFXYGX', 2, 1);

INSERT IGNORE INTO favorite(student_id, job_id) VALUES
(6, 1), (6, 3);
