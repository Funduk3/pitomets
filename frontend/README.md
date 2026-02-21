# Pitomets Frontend

React frontend for the Pitomets pet marketplace application.

## Features

- User authentication (Login/Register) with JWT tokens
- User profile management with avatar upload
- Seller profile creation and management
- Listings CRUD operations
- Search functionality
- Favourites management
- Reviews system
- Photo uploads for listings and user avatars

## Setup

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

The app will run on http://localhost:3001

## Backend Configuration

API base URL is provided at build time via Docker build arg `VITE_API_BASE_URL`.

For Docker builds from `frontend/`, pass API URL explicitly:
`docker buildx build --build-arg VITE_API_BASE_URL=https://pitomets.com/api ...`

The frontend is configured to communicate with the backend API. CORS is already configured in the backend to allow requests from localhost:3001.

## Project Structure

- `src/api/` - API service layer for backend communication
- `src/components/` - Reusable React components
- `src/context/` - React context providers (Auth)
- `src/pages/` - Page components for different routes
- `src/App.jsx` - Main app component with routing
- `src/main.jsx` - Entry point

## API Endpoints Used

- `/register` - User registration
- `/login` - User login
- `/refresh` - Token refresh
- `/logout` - User logout
- `/profile/me` - Get current user profile
- `/seller/profile` - Create/update seller profile
- `/listings/` - Listings CRUD
- `/listings/{id}/photos` - Listing photos
- `/search/listings` - Search listings
- `/favourites` - Favourites management
- `/users/photos/avatar` - User avatar management
