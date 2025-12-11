const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const mysql = require('mysql2/promise');
const redis = require('redis');
const dotenv = require('dotenv');

// 환경 변수 로드
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// 미들웨어 설정
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// ==================== MySQL 연결 ====================
const pool = mysql.createPool({
  host: process.env.MYSQL_HOST,
  port: process.env.MYSQL_PORT,
  user: process.env.MYSQL_USER,
  password: process.env.MYSQL_PASSWORD,
  database: process.env.MYSQL_DATABASE,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

// MySQL 연결 테스트
pool.getConnection()
  .then(connection => {
    console.log('✅ MySQL Connected Successfully');
    console.log(`   Host: ${process.env.MYSQL_HOST}:${process.env.MYSQL_PORT}`);
    console.log(`   Database: ${process.env.MYSQL_DATABASE}`);
    connection.release();
  })
  .catch(err => {
    console.error('❌ MySQL Connection Error:', err);
  });

// ==================== Redis 연결 ====================
let redisClient;
let isRedisConnected = false;

async function connectRedis() {
  try {
    redisClient = redis.createClient({
      url: process.env.REDIS_URL
    });

    redisClient.on('error', (err) => {
      console.error('❌ Redis Client Error:', err);
      isRedisConnected = false;
    });

    redisClient.on('connect', () => {
      console.log('✅ Redis Connected Successfully');
      isRedisConnected = true;
    });

    await redisClient.connect();
  } catch (error) {
    console.error('❌ Redis Connection Error:', error);
    isRedisConnected = false;
  }
}

// Redis 연결 시작
connectRedis();

// Redis 헬퍼 함수
async function redisGet(key) {
  if (!isRedisConnected) return null;
  try {
    return await redisClient.get(key);
  } catch (error) {
    console.error('Redis GET error:', error);
    return null;
  }
}

async function redisSet(key, value, expirationSeconds = null) {
  if (!isRedisConnected) return false;
  try {
    if (expirationSeconds) {
      await redisClient.setEx(key, expirationSeconds, value);
    } else {
      await redisClient.set(key, value);
    }
    return true;
  } catch (error) {
    console.error('Redis SET error:', error);
    return false;
  }
}

async function redisDel(key) {
  if (!isRedisConnected) return false;
  try {
    await redisClient.del(key);
    return true;
  } catch (error) {
    console.error('Redis DEL error:', error);
    return false;
  }
}

async function redisGetHash(key) {
  if (!isRedisConnected) return null;
  try {
    return await redisClient.hGetAll(key);
  } catch (error) {
    console.error('Redis HGETALL error:', error);
    return null;
  }
}

async function redisSetHash(key, data) {
  if (!isRedisConnected) return false;
  try {
    await redisClient.hSet(key, data);
    return true;
  } catch (error) {
    console.error('Redis HSET error:', error);
    return false;
  }
}

// ==================== 위치 관리 API ====================

// 1. 사용자 위치 목록 조회
app.get('/api/locations/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const [rows] = await pool.query(
      'SELECT * FROM saved_locations WHERE user_id = ? ORDER BY display_order ASC',
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
    const { userId, locationName, latitude, longitude } = req.body;
    
    if (!userId || !locationName) {
      return res.status(400).json({
        success: false,
        error: 'userId and locationName are required'
      });
    }
    
    // 현재 최대 순서 조회
    const [maxOrder] = await pool.query(
      'SELECT COALESCE(MAX(display_order), -1) as max_order FROM saved_locations WHERE user_id = ?',
      [userId]
    );
    
    const newOrder = maxOrder[0].max_order + 1;
    
    // 위치 추가
    const [result] = await pool.query(
      'INSERT INTO saved_locations (user_id, location_name, latitude, longitude, display_order) VALUES (?, ?, ?, ?, ?)',
      [userId, locationName, latitude, longitude, newOrder]
    );
    
    res.json({
      success: true,
      message: 'Location added successfully',
      locationId: result.insertId
    });
  } catch (error) {
    console.error('Error adding location:', error);
    if (error.code === 'ER_DUP_ENTRY') {
      res.status(409).json({
        success: false,
        error: 'Location already exists for this user.'
      });
    } else {
      res.status(500).json({
        success: false,
        error: 'Failed to add location'
      });
    }
  }
});

// 3. 위치 삭제
app.delete('/api/locations/:userId/:location', async (req, res) => {
  try {
    const { userId, location } = req.params;
    
    const [result] = await pool.query(
      'DELETE FROM saved_locations WHERE user_id = ? AND location_name = ?',
      [userId, decodeURIComponent(location)]
    );
    
    if (result.affectedRows === 0) {
      return res.status(404).json({
        success: false,
        error: 'Location not found.'
      });
    }

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

// 4. 위치 순서 업데이트
app.put('/api/locations/order', async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    const { userId, locations } = req.body;
    
    console.log('📍 Update order request:', { userId, locations });
    
    if (!userId || !Array.isArray(locations) || locations.length === 0) {
      return res.status(400).json({
        success: false,
        error: 'userId and locations array are required'
      });
    }
    
    await connection.beginTransaction();
    
    for (let i = 0; i < locations.length; i++) {
      const [result] = await connection.query(
        'UPDATE saved_locations SET display_order = ? WHERE user_id = ? AND location_name = ?',
        [i, userId, locations[i]]
      );
      
      if (result.affectedRows === 0) {
        await connection.rollback();
        connection.release();
        return res.status(404).json({
          success: false,
          error: `Location '${locations[i]}' not found for user '${userId}'`
        });
      }
    }
    
    await connection.commit();
    
    console.log('✅ Location order updated successfully');
    
    res.json({
      success: true,
      message: 'Location order updated successfully'
    });
    
  } catch (error) {
    await connection.rollback();
    console.error('❌ Error updating location order:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update location order'
    });
  } finally {
    connection.release();
  }
});

// ==================== 회원 관리 API (Redis 사용) ====================

// 5. 회원가입
app.post('/api/register', async (req, res) => {
  try {
    const { userId, password, name, phone } = req.body;
    
    console.log('📝 Register request:', { userId, name, phone });
    
    if (!userId || !password || !name || !phone) {
      return res.status(400).json({
        success: false,
        error: 'All fields are required'
      });
    }
    
    // Redis에서 중복 체크
    const existingUser = await redisGet(`user:${userId}`);
    if (existingUser) {
      return res.status(409).json({
        success: false,
        error: 'User ID already exists'
      });
    }
    
    // Redis에 사용자 정보 저장
    const userData = JSON.stringify({ userId, password, name, phone });
    await redisSet(`user:${userId}`, userData);
    
    // 이름과 전화번호로 아이디 찾기를 위한 인덱스
    await redisSet(`user:byphone:${phone}`, userId);
    
    console.log('✅ User registered:', userId);
    
    res.json({
      success: true,
      message: '회원가입이 완료되었습니다'
    });
  } catch (error) {
    console.error('❌ Register error:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to register user'
    });
  }
});

// 6. 아이디 중복 확인
app.post('/api/check-id', async (req, res) => {
  try {
    const { userId } = req.body;
    
    if (!userId) {
      return res.status(400).json({
        success: false,
        error: 'userId is required'
      });
    }
    
    const exists = await redisGet(`user:${userId}`);
    
    res.json({
      success: true,
      available: !exists,
      message: exists ? '이미 사용 중인 아이디입니다' : '사용 가능한 아이디입니다'
    });
  } catch (error) {
    console.error('Error checking user ID:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to check user ID'
    });
  }
});

// 7. 로그인
app.post('/api/login', async (req, res) => {
  try {
    const { userId, password } = req.body;
    
    console.log('🔐 Login attempt:', userId);
    
    if (!userId || !password) {
      return res.status(400).json({
        success: false,
        error: 'userId and password are required'
      });
    }
    
    const userData = await redisGet(`user:${userId}`);
    
    if (!userData) {
      console.log('❌ User not found:', userId);
      return res.status(404).json({
        success: false,
        error: '존재하지 않는 아이디입니다'
      });
    }
    
    const user = JSON.parse(userData);
    
    if (user.password !== password) {
      console.log('❌ Wrong password for:', userId);
      return res.status(401).json({
        success: false,
        error: '비밀번호가 일치하지 않습니다'
      });
    }
    
    // 세션 ID 생성
    const sessionId = `session_${userId}_${Date.now()}`;
    
    // 세션 정보 Redis에 저장 (24시간 유효)
    await redisSet(`session:${sessionId}`, userId, 86400);
    
    console.log('✅ Login success:', userId);
    
    res.json({
      success: true,
      sessionId: sessionId,
      message: '로그인 성공'
    });
  } catch (error) {
    console.error('❌ Login error:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to login'
    });
  }
});

// 8. 로그아웃
app.post('/api/logout', async (req, res) => {
  try {
    const { sessionId } = req.body;
    
    if (sessionId) {
      await redisDel(`session:${sessionId}`);
      console.log('👋 Logout:', sessionId);
    }
    
    res.json({
      success: true,
      message: '로그아웃 되었습니다'
    });
  } catch (error) {
    console.error('Error logging out:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to logout'
    });
  }
});

// 9. 아이디 찾기
app.post('/api/find-id', async (req, res) => {
  try {
    const { name, phone } = req.body;
    
    console.log('🔍 Find ID request:', { name, phone });
    
    if (!name || !phone) {
      return res.status(400).json({
        success: false,
        error: '이름과 전화번호를 입력하세요'
      });
    }
    
    // 전화번호로 아이디 찾기
    const userId = await redisGet(`user:byphone:${phone}`);
    
    if (!userId) {
      console.log('❌ User not found by phone');
      return res.status(404).json({
        success: false,
        error: '일치하는 사용자 정보를 찾을 수 없습니다'
      });
    }
    
    // 사용자 정보 확인
    const userData = await redisGet(`user:${userId}`);
    if (!userData) {
      return res.status(404).json({
        success: false,
        error: '일치하는 사용자 정보를 찾을 수 없습니다'
      });
    }
    
    const user = JSON.parse(userData);
    
    // 이름 확인
    if (user.name !== name) {
      return res.status(404).json({
        success: false,
        error: '일치하는 사용자 정보를 찾을 수 없습니다'
      });
    }
    
    console.log('✅ Found user ID:', userId);
    res.json({
      success: true,
      userId: userId
    });
  } catch (error) {
    console.error('❌ Find ID error:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to find user ID'
    });
  }
});

// 10. 비밀번호 찾기
app.post('/api/find-password', async (req, res) => {
  try {
    const { userId, name, phone } = req.body;
    
    console.log('🔍 Find password request:', { userId, name, phone });
    
    if (!userId || !name || !phone) {
      return res.status(400).json({
        success: false,
        error: '모든 필드를 입력하세요'
      });
    }
    
    const userData = await redisGet(`user:${userId}`);
    
    if (!userData) {
      console.log('❌ User ID not found:', userId);
      return res.status(404).json({
        success: false,
        error: '존재하지 않는 아이디입니다'
      });
    }
    
    const user = JSON.parse(userData);
    
    if (user.name !== name || user.phone !== phone) {
      console.log('❌ Info mismatch for:', userId);
      return res.status(404).json({
        success: false,
        error: '입력하신 정보와 일치하는 사용자를 찾을 수 없습니다'
      });
    }
    
    console.log('✅ Found password for:', userId);
    
    res.json({
      success: true,
      password: user.password
    });
  } catch (error) {
    console.error('❌ Find password error:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to find password'
    });
  }
});

// ==================== 기타 ====================

// 헬스 체크
app.get('/health', (req, res) => {
  res.json({ 
    status: 'OK', 
    timestamp: new Date(),
    redis: isRedisConnected ? 'Connected' : 'Disconnected',
    mysql: 'Connected'
  });
});

// 404 핸들러
app.use((req, res) => {
  console.log('❌ 404 Not Found:', req.method, req.path);
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
  console.log('='.repeat(60));
  console.log(`🚀 Weather App Server`);
  console.log(`📍 Port: ${PORT}`);
  console.log(`📍 Health: http://localhost:${PORT}/health`);
  console.log('='.repeat(60));
  console.log('🗄️  Database:');
  console.log(`   MySQL: ${process.env.MYSQL_HOST}:${process.env.MYSQL_PORT}`);
  console.log(`   Redis: ${isRedisConnected ? 'Connected' : 'Connecting...'}`);
  console.log('='.repeat(60));
});

// Graceful shutdown
process.on('SIGTERM', async () => {
  console.log('👋 SIGTERM received, closing connections...');
  if (redisClient) {
    await redisClient.quit();
  }
  await pool.end();
  process.exit(0);
});