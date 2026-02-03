# PostZen API Examples

## Base URL
```
http://localhost:8080/api
```

---

## Authentication

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "username": "johndoe",
    "password": "secret123"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "secret123"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "username": "johndoe",
    "role": "USER"
  }
}
```

### Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

### Logout
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

---

## Posts

### List Published Posts
```bash
curl http://localhost:8080/api/posts?page=0&size=10
```

### Get Post by Slug
```bash
curl http://localhost:8080/api/posts/my-first-post
```

### Create Post
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My First Post",
    "content": "# Hello World\n\nThis is my first post!",
    "status": "PUBLISHED"
  }'
```

### Create Scheduled Post
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Future Post",
    "content": "This will be published later.",
    "status": "SCHEDULED",
    "scheduledAt": "2024-12-31T12:00:00"
  }'
```

### Update Post
```bash
curl -X PUT http://localhost:8080/api/posts/<post-id> \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "content": "Updated content"
  }'
```

### Delete Post
```bash
curl -X DELETE http://localhost:8080/api/posts/<post-id> \
  -H "Authorization: Bearer <access_token>"
```

---

## Comments

### List Comments
```bash
curl http://localhost:8080/api/posts/<post-id>/comments?page=0&size=20
```

### Create Comment
```bash
curl -X POST http://localhost:8080/api/posts/<post-id>/comments \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Great post!"
  }'
```

### Delete Comment
```bash
curl -X DELETE http://localhost:8080/api/comments/<comment-id> \
  -H "Authorization: Bearer <access_token>"
```

---

## Files

### Upload Image
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <access_token>" \
  -F "file=@/path/to/image.jpg"
```

### Get File
```bash
curl http://localhost:8080/api/files/<filename>
```

---

## Admin (ADMIN role required)

### List All Users
```bash
curl http://localhost:8080/api/admin/users?page=0&size=20 \
  -H "Authorization: Bearer <admin_token>"
```
