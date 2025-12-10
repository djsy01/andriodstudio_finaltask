const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const mysql = require('mysql2/promise');

const app = express();
const PORT = 3000;

// 미들웨어 설정
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// MySQL 연결 풀 생성
const pool = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: 'your_password',
  database: 'weather_app',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

// 연결 테스트
pool.getConnection()
  .then(connection => {
    console.log('MySQL Connected Successfully');
    connection.release();
  })
  .catch(err => {
    console.error('MySQL Connection Error:', err);
  });

// ==================== API 라우트 ====================

// 1. 사용자 위치 목록 조회
app.get('/api/locations/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const [rows] = await pool.query(
      'SELECT * FROM user_locations WHERE user_id = ? ORDER BY display_order ASC',
      [userId]
    );
    
    res.json({
      success: true,
      locations: rows
    });
  } catch (error) {
    console.error('Error fetching locations:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to fetch locations'
    });
  }
});

// 2. 위치 추가
app.post('/api/locations', async (req, res) => {
  try {
    const { userId, location } = req.body;
    
    if (!userId || !location) {
      return res.status(400).json({
        success: false,
        error: 'userId and location are required'
      });
    }
    
    // 현재 최대 순서 조회
    const [maxOrder] = await pool.query(
      'SELECT COALESCE(MAX(display_order), -1) as max_order FROM user_locations WHERE user_id = ?',
      [userId]
    );
    
    const newOrder = maxOrder[0].max_order + 1;
    
    // 위치 추가
    const [result] = await pool.query(
      'INSERT INTO user_locations (user_id, location, display_order) VALUES (?, ?, ?)',
      [userId, location, newOrder]
    );
    
    res.json({
      success: true,
      message: 'Location added successfully',
      locationId: result.insertId
    });
  } catch (error) {
    console.error('Error adding location:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to add location'
    });
  }
});

// 3. 위치 삭제
app.delete('/api/locations/:userId/:location', async (req, res) => {
  try {
    const { userId, location } = req.params;
    
    await pool.query(
      'DELETE FROM user_locations WHERE user_id = ? AND location = ?',
      [userId, decodeURIComponent(location)]
    );
    
    res.json({
      success: true,
      message: 'Location deleted successfully'
    });
  } catch (error) {
    console.error('Error deleting location:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to delete location'
    });
  }
});

// 4. 위치 순서 업데이트 (핵심 엔드포인트)
app.put('/api/locations/order', async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    const { userId, locations } = req.body;
    
    console.log('Update order request:', { userId, locations });
    
    // 입력 검증
    if (!userId || !Array.isArray(locations) || locations.length === 0) {
      return res.status(400).json({
        success: false,
        error: 'userId and locations array are required'
      });
    }
    
    // 트랜잭션 시작
    await connection.beginTransaction();
    
    // 각 위치의 순서 업데이트
    for (let i = 0; i < locations.length; i++) {
      await connection.query(
        'UPDATE user_locations SET display_order = ? WHERE user_id = ? AND location = ?',
        [i, userId, locations[i]]
      );
    }
    
    // 트랜잭션 커밋
    await connection.commit();
    
    console.log('Location order updated successfully');
    
    res.json({
      success: true,
      message: 'Location order updated successfully'
    });
    
  } catch (error) {
    // 트랜잭션 롤백
    await connection.rollback();
    console.error('Error updating location order:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update location order'
    });
  } finally {
    connection.release();
  }
});

// 5. 사용자 정보 조회
app.get('/api/users/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const [rows] = await pool.query(
      'SELECT * FROM users WHERE user_id = ?',
      [userId]
    );
    
    if (rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'User not found'
      });
    }
    
    res.json({
      success: true,
      user: rows[0]
    });
  } catch (error) {
    console.error('Error fetching user:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to fetch user'
    });
  }
});

// 6. 사용자 생성/업데이트
app.post('/api/users', async (req, res) => {
  try {
    const { userId, userName } = req.body;
    
    if (!userId) {
      return res.status(400).json({
        success: false,
        error: 'userId is required'
      });
    }
    
    // INSERT ... ON DUPLICATE KEY UPDATE
    await pool.query(
      'INSERT INTO users (user_id, user_name) VALUES (?, ?) ON DUPLICATE KEY UPDATE user_name = ?',
      [userId, userName || userId, userName || userId]
    );
    
    res.json({
      success: true,
      message: 'User created/updated successfully'
    });
  } catch (error) {
    console.error('Error creating/updating user:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to create/update user'
    });
  }
});

// 헬스 체크
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date() });
});

// 404 핸들러
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: 'Endpoint not found',
    method: req.method,
    path: req.path
  });
});

// 에러 핸들러
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({
    success: false,
    error: 'Internal server error'
  });
});

// 서버 시작
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server is running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
});